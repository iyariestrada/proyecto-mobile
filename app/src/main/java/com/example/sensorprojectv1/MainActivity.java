package com.example.sensorprojectv1;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.os.BatteryManager;
import android.content.Context;
import android.content.IntentFilter;
import android.content.Intent;
import android.provider.Settings;
import android.os.Build;
import android.os.Vibrator;
import android.media.MediaPlayer;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.material.navigation.NavigationView;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity
        implements SensorEventListener, NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;

    private SensorManager sensorManager;
    private Sensor gyroscopeSensor, accelerometerSensor;
    private float gyroX, gyroY, gyroZ;
    private float accX, accY, accZ;

    // ===== STEP DETECTION - Basado en literatura científica =====
    // Referencias: Pan & Lin (2011), Zhao (2010) - Umbrales validados
    // Detección sobre eje vertical dinámico para uso activo del teléfono

    // Configuración de buffer y warm-up
    private static final int SAMPLE_SIZE = 25; // Ventana de análisis (~500ms con SENSOR_DELAY_GAME)
    private static final long WARM_UP_TIME_MS = 2000; // 2 segundos de warm-up
    private long detectionStartTime = 0; // Timestamp de inicio de detección

    // Umbrales de aceleración (m/s²) - Ajustados para uso activo del teléfono
    // Valores reducidos para detectar pasos cuando el usuario usa el dispositivo
    private static final float STEP_THRESHOLD_MIN = 0.5f; // Umbral mínimo (uso activo del teléfono)
    private static final float STEP_THRESHOLD_MAX = 3.5f; // Umbral máximo (caminata rápida con teléfono)
    private static final float DYNAMIC_FACTOR = 1.5f; // Factor para umbral dinámico adaptativo

    // Restricciones temporales (ms) - Basado en cadencia humana
    // Caminata humana: 0.5-2.0 pasos/segundo → 500-2000ms entre pasos
    private static final long MIN_STEP_INTERVAL = 300; // ~200 pasos/min (muy rápido)
    private static final long MAX_STEP_INTERVAL = 2000; // ~30 pasos/min (muy lento)

    // Filtros para señal - Ajustados para mejor respuesta
    private static final float ALPHA_LOW_PASS = 0.5f; // Filtro paso bajo más suave (permite más señal)
    private static final float ALPHA_HIGH_PASS = 0.95f; // Filtro paso alto más conservador

    // Filtro de gravedad - Recomendación oficial de Android
    private static final float ALPHA_GRAVITY = 0.8f; // Filtro low-pass para separar gravedad
    private float[] gravity = new float[3]; // Vector de gravedad filtrado

    // Buffers y estado
    private float[] accBuffer = new float[SAMPLE_SIZE];
    private int bufferIndex = 0;
    private int samplesCollected = 0;
    private boolean bufferReady = false;

    // Variables de filtrado
    private float accFiltered = 0; // Señal filtrada (paso bajo)
    private float accMean = 0.0f; // Media móvil para aceleración vertical

    // Detección de picos
    private boolean aboveThreshold = false;
    private float lastPeakValue = 0;
    private long lastStepTime = 0;

    // Contadores y estado
    private int stepCount = 0;
    private boolean isWalking = false;
    private String walkingSpeed = "Ninguna";
    private float currentVariance = 0.0f;

    // Métricas avanzadas (para envío al backend)
    private float verticalAcc = 0.0f;
    private float dynamicThreshold = 0.0f;
    private float stdDev = 0.0f;

    // Para validación de patrón (evitar falsos positivos)
    private static final int STEPS_WINDOW = 4; // Ventana para validar patrón
    private long[] recentStepTimes = new long[STEPS_WINDOW];
    private int stepTimeIndex = 0;

    private static final float PHONE_USE_GYRO_THRESHOLD = 0.2f;
    private static final float PHONE_TILT_MIN = 20.0f;
    private static final float PHONE_TILT_MAX = 85.0f;
    private boolean isUsingPhone = false;

    private boolean isWalkingAndUsingPhone = false;
    private int totalAlerts = 0;

    // Throttling para envío de datos
    private static final long DATA_SEND_INTERVAL_MS = 1000; // Enviar cada segundo (reducir carga en DB)
    private long lastDataSendTime = 0;

    private BatteryManager batteryManager;
    private PreferencesManager preferencesManager;
    private Vibrator vibrator;
    private MediaPlayer alertSound;

    private HomeFragment homeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar PreferencesManager
        preferencesManager = new PreferencesManager(this);

        // Solo iniciar sesión si el usuario está autenticado o eligió continuar como
        // anónimo
        // LoginActivity redirige aquí solo después de login o continuar anónimo
        initializeSession();

        // Configurar Toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Configurar Navigation Drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Actualizar header con info de usuario
        updateNavigationHeader();

        // Cargar fragment inicial
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
            navigationView.setCheckedItem(R.id.nav_inicio);
        }

        // Inicializar sensores
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (gyroscopeSensor != null) {
            sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_GAME);
        }

        if (accelerometerSensor != null) {
            // SENSOR_DELAY_GAME = ~50Hz (20ms entre muestras)
            // 25 muestras = 500ms, tiempo adecuado para detectar un paso
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Actualizar header cuando vuelve de otra actividad (ej: LoginActivity)
        updateNavigationHeader();
    }

    public void updateNavigationHeader() {
        TextView headerUserInfo = navigationView.getHeaderView(0).findViewById(R.id.nav_header_user_info);
        if (preferencesManager.isUserLoggedIn()) {
            headerUserInfo.setText(preferencesManager.getUserEmail());
        } else {
            headerUserInfo.setText(R.string.login_prompt);
        }
    }

    private void loadFragment(Fragment fragment) {
        if (fragment instanceof HomeFragment) {
            homeFragment = (HomeFragment) fragment;
        }
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.content_frame, fragment);
        transaction.commit();
    }

    public void loadHomeFragment() {
        loadFragment(new HomeFragment());
        navigationView.setCheckedItem(R.id.nav_inicio);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        Fragment fragment = null;
        int itemId = item.getItemId();

        if (itemId == R.id.nav_inicio) {
            fragment = new HomeFragment();
        } else if (itemId == R.id.nav_perfil) {
            fragment = new ProfileFragment();
        } else if (itemId == R.id.nav_avisos) {
            fragment = new AlertsFragment();
        } else if (itemId == R.id.nav_estadisticas) {
            fragment = new StatisticsFragment();
        } else if (itemId == R.id.nav_ajustes) {
            fragment = new SettingsFragment();
        }

        if (fragment != null) {
            loadFragment(fragment);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void sendSensorData() {
        // Throttling: solo enviar cada 1 segundo
        long now = System.currentTimeMillis();
        if (now - lastDataSendTime < DATA_SEND_INTERVAL_MS) {
            return; // Demasiado pronto, no enviar
        }

        // Solo enviar datos si hay una sesión activa
        long sessionId = preferencesManager.getSessionId();

        if (sessionId == -1) {
            return; // Sin sesión activa
        }

        // Actualizar timestamp del último envío
        lastDataSendTime = now;

        // NUEVO COMPORTAMIENTO:
        // - Usuarios anónimos: SIEMPRE envían datos (sin token)
        // - Usuarios registrados que participan: envían datos con token
        // - Usuarios registrados que NO participan: envían datos SIN token (como anónimos)
        //
        // IMPORTANTE: La sesión (sessionId) determina qué userId se usa en el backend
        // Solo necesitamos decidir si enviar token o no

        boolean isLoggedIn = preferencesManager.isUserLoggedIn();
        boolean isParticipating = preferencesManager.isParticipateEnabled();

        // Determinar si enviar token (participantes registrados) o null (anónimos y no participantes)
        String tokenToSend;

        if (isLoggedIn && isParticipating) {
            // Usuario registrado que participa → enviar con token
            tokenToSend = preferencesManager.getUserToken();
        } else {
            // Usuario anónimo O usuario registrado que NO participa → enviar sin token
            tokenToSend = null;
        }

        try {
            JSONObject json = new JSONObject();

            // Datos de acelerómetro y giroscopio
            json.put("acc_x", accX);
            json.put("acc_y", accY);
            json.put("acc_z", accZ);
            json.put("gyro_x", gyroX);
            json.put("gyro_y", gyroY);
            json.put("gyro_z", gyroZ);

            json.put("step_count", stepCount);

            // Estado de detección
            json.put("is_walking", isWalking);
            json.put("is_using_phone", isUsingPhone);

            // En sendSensorData(), AGREGAR:
            json.put("vertical_acceleration", verticalAcc); // Aceleración vertical proyectada
            json.put("gravity_x", gravity[0]); // Vector de gravedad
            json.put("gravity_y", gravity[1]);
            json.put("gravity_z", gravity[2]);
            json.put("dynamic_threshold", dynamicThreshold); // Umbral adaptativo
            json.put("std_dev", stdDev); // Desviación estándar

            // Información del dispositivo
            json.put("battery_level", getBatteryLevel());
            json.put("battery_status", getBatteryStatus());
            json.put("screen_brightness", getScreenBrightness());
            json.put("screen_on", isScreenOn());

            // Timestamp
            json.put("recorded_at", System.currentTimeMillis());

            // Usar el token determinado anteriormente (null para anónimos y no participantes)
            ApiService.sendSensorData(sessionId, json, tokenToSend, new ApiService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    Log.d("SENSOR_DATA", "Datos enviados exitosamente");
                }

                @Override
                public void onError(String error) {
                    Log.e("SENSOR_DATA", "Error al enviar datos: " + error);
                }
            });

        } catch (Exception e) {
            Log.e("SENSOR_DATA", "Error al preparar datos: " + e.toString());
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    /* PROCESS FUNCTIONS */

    private float getBatteryLevel() {
        if (batteryManager != null) {
            return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        }
        return -1;
    }

    private String getBatteryStatus() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);

        if (batteryStatus != null) {
            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;

            if (status == BatteryManager.BATTERY_STATUS_FULL) {
                return "full";
            } else if (isCharging) {
                return "charging";
            } else {
                return "discharging";
            }
        }
        return "unknown";
    }

    private int getScreenBrightness() {
        try {
            return Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            return -1;
        }
    }

    private boolean isScreenOn() {
        return true;
    }

    private String getDeviceModel() {
        return Build.MANUFACTURER + " " + Build.MODEL;
    }

    private String getAndroidVersion() {
        return Build.VERSION.RELEASE;
    }

    /**
     * Detección de pasos basada en algoritmos científicos
     * Referencias:
     * - Pan & Lin (2011): "An improved human activity recognition system"
     * - Zhao (2010): "A robust step counting algorithm"
     *
     * OPTIMIZADO para uso activo del teléfono:
     * 1. Filtro de gravedad (low-pass) para separar componente gravitacional
     * 2. Aceleración lineal = señal cruda - gravedad
     * 3. Proyección sobre eje vertical dinámico (no asume orientación fija)
     * 4. Detección de picos sobre señal vertical
     * 5. Umbrales adaptados para movimiento con teléfono
     */
    private void detectWalking(float accX, float accY, float accZ) {
        // 1️⃣ SEPARAR GRAVEDAD correctamente (filtro low-pass recomendado por Android)
        gravity[0] = ALPHA_GRAVITY * gravity[0] + (1 - ALPHA_GRAVITY) * accX;
        gravity[1] = ALPHA_GRAVITY * gravity[1] + (1 - ALPHA_GRAVITY) * accY;
        gravity[2] = ALPHA_GRAVITY * gravity[2] + (1 - ALPHA_GRAVITY) * accZ;

        // 2️⃣ ACELERACIÓN LINEAL (sin gravedad)
        float linX = accX - gravity[0];
        float linY = accY - gravity[1];
        float linZ = accZ - gravity[2];

        // 3️⃣ PROYECCIÓN sobre eje VERTICAL DINÁMICO
        // La gravedad define la vertical real, independiente de la orientación del
        // teléfono
        float gravityMagnitude = (float) Math.sqrt(
                gravity[0] * gravity[0] +
                        gravity[1] * gravity[1] +
                        gravity[2] * gravity[2]);

        // Evitar división por cero
        if (gravityMagnitude < 0.1f) {
            return; // Gravedad no inicializada aún
        }

        // Proyección de aceleración lineal sobre eje vertical
        verticalAcc = (linX * gravity[0] +
                linY * gravity[1] +
                linZ * gravity[2]) / gravityMagnitude;

        // 4️⃣ FILTRO PASO BAJO (eliminar ruido de alta frecuencia)
        accFiltered = ALPHA_LOW_PASS * accFiltered + (1 - ALPHA_LOW_PASS) * verticalAcc;

        // 5️⃣ BUFFER CIRCULAR
        accBuffer[bufferIndex] = accFiltered;
        bufferIndex = (bufferIndex + 1) % SAMPLE_SIZE;

        // 6️⃣ WARM-UP: Esperar tiempo suficiente antes de detectar
        long now = System.currentTimeMillis();

        // Inicializar timestamp en la primera muestra
        if (detectionStartTime == 0) {
            detectionStartTime = now;
            accMean = verticalAcc; // CRÍTICO: inicializar con verticalAcc
            Log.i("STEP_WARMUP", "Iniciando warm-up de " + WARM_UP_TIME_MS + "ms");
        }

        long elapsedTime = now - detectionStartTime;

        if (elapsedTime < WARM_UP_TIME_MS) {
            samplesCollected++;
            // Durante warm-up, inicializar media con valores reales de verticalAcc
            accMean = accMean * 0.9f + verticalAcc * 0.1f; // Convergencia suave

            // Log periódico durante warm-up (cada 500ms)
            if (samplesCollected % 25 == 0) {
                Log.d("STEP_WARMUP", String.format("Warm-up: %dms/%dms | Samples: %d | Vertical: %.2f | Mean: %.2f",
                        elapsedTime, WARM_UP_TIME_MS, samplesCollected, verticalAcc, accMean));
            }
            return; // ⛔ NO detectar pasos aún
        }

        samplesCollected++;

        // Marcar buffer como listo
        if (!bufferReady) {
            bufferReady = true;
            Log.i("STEP_DETECTION", "Buffer listo - iniciando detección de pasos");
        }

        // 7️⃣ FILTRO PASO ALTO (eliminar componente de drift)
        // Media móvil exponencial que se adapta lentamente
        accMean = ALPHA_HIGH_PASS * accMean + (1 - ALPHA_HIGH_PASS) * accFiltered;

        // Señal centrada (elimina offset)
        float centeredAcc = accFiltered - accMean;

        // 8️⃣ CÁLCULO DE UMBRAL DINÁMICO con stdDev CORREGIDA
        // Calcular media REAL del buffer (no usar accMean que es una EMA)
        float bufferMean = calculateMean(accBuffer);
        stdDev = calculateStdDev(accBuffer, bufferMean);
        dynamicThreshold = Math.max(
                STEP_THRESHOLD_MIN,
                Math.min(STEP_THRESHOLD_MAX, stdDev * DYNAMIC_FACTOR));

        // 9️⃣ DETECCIÓN DE PICO = PASO
        // Algoritmo de cruce de umbral con histéresis
        if (centeredAcc > dynamicThreshold && !aboveThreshold) {
            // Cruce ascendente detectado
            aboveThreshold = true;
            lastPeakValue = centeredAcc;

        } else if (aboveThreshold && centeredAcc > lastPeakValue) {
            // Actualizar pico máximo
            lastPeakValue = centeredAcc;

        } else if (aboveThreshold && centeredAcc < dynamicThreshold * 0.5f) {
            // Cruce descendente = FIN DE PICO → REGISTRAR PASO
            aboveThreshold = false;

            // CRÍTICO: Si es el primer paso (lastStepTime == 0), aceptarlo sin validar
            // intervalo
            if (lastStepTime == 0) {
                // Primer paso detectado - registrar sin validación de intervalo
                recentStepTimes[stepTimeIndex] = now;
                stepTimeIndex = (stepTimeIndex + 1) % STEPS_WINDOW;

                stepCount++;
                lastStepTime = now;
                isWalking = true;
                walkingSpeed = "Normal"; // Asumir velocidad normal para primer paso

                Log.d("STEP_DETECTED", String.format(
                        "✓ PASO #%d (PRIMERO) | Peak: %.2f | Threshold: %.2f",
                        stepCount, lastPeakValue, dynamicThreshold));
            } else {
                // Pasos subsecuentes - validar intervalo temporal
                long stepInterval = now - lastStepTime;

                // Validar intervalo temporal (evitar pasos imposibles)
                if (stepInterval > MIN_STEP_INTERVAL && stepInterval < MAX_STEP_INTERVAL) {

                    // Registrar tiempo del paso
                    recentStepTimes[stepTimeIndex] = now;
                    stepTimeIndex = (stepTimeIndex + 1) % STEPS_WINDOW;

                    // Validar patrón de pasos (evitar movimientos aislados)
                    if (isValidStepPattern()) {
                        stepCount++;
                        lastStepTime = now;
                        isWalking = true;

                        // Calcular velocidad de caminata basada en cadencia
                        updateWalkingSpeed(stepInterval);

                        Log.d("STEP_DETECTED", String.format(
                                "✓ PASO #%d | Intervalo: %dms | Peak: %.2f | Threshold: %.2f | Velocidad: %s",
                                stepCount, stepInterval, lastPeakValue, dynamicThreshold, walkingSpeed));
                    }
                } else {
                    Log.d("STEP_REJECTED", String.format(
                            "✗ Paso inválido | Intervalo: %dms (válido: %d-%d)",
                            stepInterval, MIN_STEP_INTERVAL, MAX_STEP_INTERVAL));
                }
            }

            lastPeakValue = 0;
        }

        // 🔟 VERIFICAR SI DEJÓ DE CAMINAR
        // Si no hay pasos en 2.5s, asumir que está detenido
        if (lastStepTime > 0 && (now - lastStepTime > 2500)) {
            if (isWalking) {
                Log.i("STEP_DETECTION", "Usuario detenido - reiniciando estado de caminata");
            }
            isWalking = false;
            walkingSpeed = "Ninguna";

            // CRÍTICO: Si la pausa es MUY larga (>5s), reiniciar lastStepTime
            // Esto permite que el siguiente paso sea aceptado como "primer paso"
            if (now - lastStepTime > 5000) {
                Log.i("STEP_DETECTION", "Pausa larga detectada - reiniciando contador de tiempo");
                lastStepTime = 0; // El próximo paso será tratado como "primer paso"
            }
        }

        // 1️⃣1️⃣ CALCULAR VARIANZA (para compatibilidad con código existente)
        currentVariance = calculateVariance(accBuffer);

        // 1️⃣2️⃣ LOG PERIÓDICO (cada 100 muestras ≈ cada 2s con SENSOR_DELAY_GAME)
        if (samplesCollected % 100 == 0) {
            Log.d("STEP_STATUS", String.format(
                    "Vertical: %.2f | Filt: %.2f | Mean: %.2f | Centered: %.2f | " +
                            "Threshold: %.2f | StdDev: %.2f | Steps: %d | Walking: %s (%s)",
                    verticalAcc, accFiltered, accMean, centeredAcc,
                    dynamicThreshold, stdDev, stepCount,
                    isWalking ? "SI" : "NO", walkingSpeed));
        }
    }

    /**
     * Calcula la media real del buffer
     * CRÍTICO: No usar accMean (que es una EMA), sino la media aritmética del
     * buffer
     */
    private float calculateMean(float[] buffer) {
        float sum = 0;
        for (float value : buffer) {
            sum += value;
        }
        return sum / buffer.length;
    }

    /**
     * Calcula desviación estándar del buffer con la media CORRECTA
     */
    private float calculateStdDev(float[] buffer, float mean) {
        float sumSquaredDiff = 0;
        for (float value : buffer) {
            float diff = value - mean;
            sumSquaredDiff += diff * diff;
        }
        return (float) Math.sqrt(sumSquaredDiff / buffer.length);
    }

    /**
     * Valida que los pasos recientes formen un patrón consistente
     * Evita falsos positivos por movimientos únicos del teléfono
     */
    private boolean isValidStepPattern() {
        // Contar cuántos pasos hay en los últimos 3 segundos
        long now = System.currentTimeMillis();
        int recentSteps = 0;

        for (long stepTime : recentStepTimes) {
            if (stepTime > 0 && (now - stepTime) < 3000) {
                recentSteps++;
            }
        }

        // Permitir primeros 2 pasos para establecer patrón
        // Después, necesitamos al menos 2 pasos en 3 segundos para confirmar caminata
        if (stepCount < 2) {
            return true; // Permitir los primeros 2 pasos sin validación
        }

        // A partir del tercer paso, validar patrón temporal
        return recentSteps >= 2;
    }

    /**
     * Determina velocidad de caminata basada en cadencia (pasos/minuto)
     * Literatura: Lento <100, Normal 100-120, Rápido >120 pasos/min
     */
    private void updateWalkingSpeed(long stepInterval) {
        // Convertir intervalo a pasos/minuto
        float cadence = 60000.0f / stepInterval; // pasos/min

        if (cadence < 80) {
            walkingSpeed = "Lenta"; // <80 pasos/min
        } else if (cadence < 120) {
            walkingSpeed = "Normal"; // 80-120 pasos/min
        } else {
            walkingSpeed = "Rapida"; // >120 pasos/min
        }
    }

    private float calculateVariance(float[] samples) {
        float sum = 0;
        for (float sample : samples) {
            sum += sample;
        }
        float mean = sum / samples.length;

        float varianceSum = 0;
        for (float sample : samples) {
            varianceSum += Math.pow(sample - mean, 2);
        }

        return varianceSum / samples.length;
    }

    private void detectPhoneUsage(float gyroX, float gyroY, float gyroZ,
            float accX, float accY, float accZ) {
        // Magnitud del giroscopio (movimiento rotacional)
        float gyroMagnitude = (float) Math.sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ);

        // Calcular orientación del teléfono
        float pitch = (float) Math.toDegrees(Math.atan2(accX, Math.sqrt(accY * accY + accZ * accZ)));
        float roll = (float) Math.toDegrees(Math.atan2(accY, accZ));

        // Verificar si está en posición de uso
        boolean isPhoneOriented = (Math.abs(pitch) > PHONE_TILT_MIN && Math.abs(pitch) < PHONE_TILT_MAX) ||
                (Math.abs(roll) > PHONE_TILT_MIN && Math.abs(roll) < PHONE_TILT_MAX);

        // Determinar si está usando el teléfono
        boolean gyroActive = gyroMagnitude > PHONE_USE_GYRO_THRESHOLD;
        isUsingPhone = gyroActive || isPhoneOriented;

        // Log detallado con códigos de detección
        String detectionReason = "";
        if (gyroActive && isPhoneOriented) {
            detectionReason = "GYRO+ORIENTACION";
        } else if (gyroActive) {
            detectionReason = "GYRO";
        } else if (isPhoneOriented) {
            detectionReason = "ORIENTACION";
        } else {
            detectionReason = "NINGUNO";
        }

        Log.d("PHONE_USAGE", String.format(
                "GyroMag: %.3f | Pitch: %.2f° | Roll: %.2f° | Orientado: %s | Activo: %s | Usando: %s | Razon: %s",
                gyroMagnitude, pitch, roll,
                isPhoneOriented ? "SI" : "NO",
                gyroActive ? "SI" : "NO",
                isUsingPhone ? "SI" : "NO",
                detectionReason));
    }

    private void detectWalkingAndPhoneUse() {
        boolean previousState = isWalkingAndUsingPhone;

        // Validación robusta: requiere caminata confirmada + uso del teléfono
        // Evita alertas por un solo paso o movimientos aislados
        long now = System.currentTimeMillis();
        int stepsInLast2Seconds = 0;

        // Contar pasos recientes (últimos 2 segundos)
        for (long stepTime : recentStepTimes) {
            if (stepTime > 0 && (now - stepTime) < 2000) {
                stepsInLast2Seconds++;
            }
        }

        // Confirmar caminata: al menos 2 pasos en 2 segundos
        boolean walkingConfirmed = isWalking && stepsInLast2Seconds >= 2;

        // Confirmar uso del teléfono (puede mantener tu validación de gyro)
        boolean phoneConfirmed = isUsingPhone;

        isWalkingAndUsingPhone = walkingConfirmed && phoneConfirmed;

        // Construir mensaje de estado detallado
        String status = "";
        String statusColor = "#4CAF50";

        if (isWalkingAndUsingPhone) {
            status = String.format("ALERTA: Caminando (%s) y usando el telefono", walkingSpeed);
            statusColor = "#F44336";
        } else if (isWalking) {
            status = String.format("Caminando - Velocidad: %s", walkingSpeed);
            statusColor = "#FF9800";
        } else if (isUsingPhone) {
            status = "Usando telefono (estatico)";
            statusColor = "#2196F3";
        } else {
            status = "Estado seguro - Sin actividad";
            statusColor = "#4CAF50";
        }

        // Información detallada
        status += String.format("\n\n--- DETECCION ---" +
                "\nPasos totales: %d" +
                "\nCaminando: %s" +
                "\nTipo caminata: %s" +
                "\nVarianza: %.3f" +
                "\nUsando telefono: %s" +
                "\nAlertas totales: %d",
                stepCount,
                isWalking ? "SI" : "NO",
                walkingSpeed,
                currentVariance,
                isUsingPhone ? "SI" : "NO",
                totalAlerts);

        // Log de estado combinado
        Log.i("DETECTION_STATUS", String.format(
                "=== ESTADO GENERAL === | Caminando: %s (%s) | Telefono: %s | ALERTA: %s | Varianza: %.3f",
                isWalking ? "SI" : "NO",
                walkingSpeed,
                isUsingPhone ? "SI" : "NO",
                isWalkingAndUsingPhone ? "ACTIVA" : "NO",
                currentVariance));

        // Actualizar HomeFragment si está visible
        String finalStatus = status;
        runOnUiThread(() -> {
            if (homeFragment != null) {
                homeFragment.updateSensorStatus(finalStatus);
            }
        });

        // Si cambió el estado a alerta, activar sonido/vibración
        if (!previousState && isWalkingAndUsingPhone) {
            totalAlerts++;
            Log.w("ALERT_TRIGGERED", String.format(
                    "NUEVA ALERTA #%d - Caminata: %s | Varianza: %.3f",
                    totalAlerts, walkingSpeed, currentVariance));
            triggerAlert();
            sendAlertToServer();
        }
    }

    private void triggerAlert() {
        runOnUiThread(() -> {
            // Vibración
            if (preferencesManager.isVibrationAlertEnabled() && vibrator != null) {
                long[] pattern = { 0, 500, 200, 500 };
                vibrator.vibrate(pattern, -1);
            }

            // Sonido
            if (preferencesManager.isSoundAlertEnabled()) {
                playAlertSound();
            }
        });
    }

    private void playAlertSound() {
        try {
            if (alertSound == null) {
                alertSound = MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI);
            }
            if (alertSound != null && !alertSound.isPlaying()) {
                alertSound.start();
            }
        } catch (Exception e) {
            Log.e("ALERT_SOUND", "Error al reproducir sonido: " + e.toString());
        }
    }

    public void updateAlertSettings() {
        // Método llamado desde SettingsFragment cuando cambian las configuraciones
        Log.d("SETTINGS", "Configuraciones de alerta actualizadas");
    }

    /**
     * Detiene las mediciones de sensores
     * Llamado al cerrar sesión
     */
    public void stopSensorMeasurements() {
        Log.d("SENSORS", "Deteniendo mediciones de sensores");

        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }

        // Finalizar sesión actual si existe
        finalizeSession();

        // Liberar recursos de audio
        if (alertSound != null) {
            alertSound.release();
            alertSound = null;
        }

        Log.i("SENSORS", "Mediciones detenidas exitosamente");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gyroX = event.values[0];
            gyroY = event.values[1];
            gyroZ = event.values[2];

            String gyroData = String.format("Giroscopio:\nX: %.3f\nY: %.3f\nZ: %.3f", gyroX, gyroY, gyroZ);
            runOnUiThread(() -> {
                if (homeFragment != null) {
                    homeFragment.updateGyroData(gyroData);
                }
            });
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accX = event.values[0];
            accY = event.values[1];
            accZ = event.values[2];

            String accData = String.format("Acelerometro:\nX: %.3f\nY: %.3f\nZ: %.3f", accX, accY, accZ);
            runOnUiThread(() -> {
                if (homeFragment != null) {
                    homeFragment.updateAccData(accData);
                }
            });

            detectWalking(accX, accY, accZ);
        }

        detectPhoneUsage(gyroX, gyroY, gyroZ, accX, accY, accZ);
        detectWalkingAndPhoneUse();
        sendSensorData();
    }

    private void sendAlertToServer() {
        // Solo enviar alerta si hay una sesión activa
        long sessionId = preferencesManager.getSessionId();

        if (sessionId == -1) {
            Log.d("ALERT", "No hay sesión activa - no se envía alerta");
            return;
        }

        try {
            long userId = preferencesManager.getUserId();
            if (userId == -1) {
                userId = 1; // Usuario anónimo
            }

            String token = preferencesManager.getUserToken();

            // Determinar severidad según velocidad de caminata
            String severidad = calculateSeverity(walkingSpeed);

            // Determinar tipo de alerta
            String tipoAlerta = "walking_using_phone";

            // Descripción legible
            String descripcion = String.format(
                    "Usuario detectado caminando a velocidad %s mientras usa el teléfono",
                    walkingSpeed.toLowerCase());

            // Crear contexto JSON con información adicional
            JSONObject contexto = new JSONObject();
            contexto.put("walking_speed", walkingSpeed);
            contexto.put("variance", currentVariance);
            contexto.put("step_count", stepCount);
            contexto.put("battery_level", getBatteryLevel());
            contexto.put("screen_brightness", getScreenBrightness());
            contexto.put("alert_number", totalAlerts);

            // Timestamp de detección
            long detectedAt = System.currentTimeMillis();

            Log.d("ALERT", String.format(
                    "Enviando alerta - Tipo: %s, Severidad: %s, Velocidad: %s",
                    tipoAlerta, severidad, walkingSpeed));

            ApiService.sendAlerta(sessionId, userId, tipoAlerta, severidad,
                    descripcion, contexto, detectedAt, token, new ApiService.ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            Log.i("ALERT", "Alerta enviada exitosamente al servidor");
                        }

                        @Override
                        public void onError(String error) {
                            Log.e("ALERT", "Error al enviar alerta: " + error);
                        }
                    });

        } catch (Exception e) {
            Log.e("ALERT", "Error al preparar alerta: " + e.toString());
        }
    }

    /**
     * Calcula la severidad de la alerta según la velocidad de caminata
     * Lenta -> baja, Normal -> media, Rápida -> alta
     */
    private String calculateSeverity(String walkingSpeed) {
        switch (walkingSpeed) {
            case "Lenta":
                return "baja";
            case "Normal":
                return "media";
            case "Rapida":
                return "alta";
            default:
                return "baja";
        }
    }

    public int getStepCount() {
        return stepCount;
    }

    public int getTotalAlerts() {
        return totalAlerts;
    }

    /**
     * Inicializa o recupera la sesión actual (usuarios registrados y anónimos)
     * SIEMPRE crea una nueva sesión al abrir la app
     */
    private void initializeSession() {
        Log.d("SESSION", "=== INICIANDO PROCESO DE SESIÓN ===");

        long existingSessionId = preferencesManager.getSessionId();
        long sessionStartTime = preferencesManager.getSessionStart();
        long currentTime = System.currentTimeMillis();

        // Si hay una sesión previa, validar si es muy antigua (>5 minutos)
        if (existingSessionId != -1) {
            long sessionAge = currentTime - sessionStartTime;
            boolean isSessionOld = sessionAge > 5 * 60 * 1000; // 5 minutos

            if (isSessionOld) {
                Log.w("SESSION", String.format(
                        "Sesión antigua detectada: %d (edad: %.1f min) - Limpiando localmente",
                        existingSessionId, sessionAge / 60000.0));

                // Sesión muy antigua, solo limpiar local sin intentar finalizar en backend
                preferencesManager.setSessionId(-1);
                preferencesManager.setSessionStart(0);
            } else {
                Log.w("SESSION", String.format(
                        "Sesión reciente sin finalizar: %d (edad: %.1f seg) - Finalizando en backend",
                        existingSessionId, sessionAge / 1000.0));

                // Sesión reciente, intentar finalizar en backend
                String token = preferencesManager.getUserToken();
                ApiService.endSession(existingSessionId, token, new ApiService.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        Log.i("SESSION", "Sesión anterior finalizada exitosamente");
                    }

                    @Override
                    public void onError(String error) {
                        // Si el error es "ya finalizada", está OK
                        if (error.contains("ya ha sido finalizada")) {
                            Log.i("SESSION", "Sesión ya estaba finalizada en el backend");
                        } else {
                            Log.e("SESSION", "Error al finalizar sesión anterior: " + error);
                        }
                    }
                });

                // Limpiar sessionId local inmediatamente
                preferencesManager.setSessionId(-1);
                preferencesManager.setSessionStart(0);
            }
        }

        // Determinar userId y token según estado de login y participación
        long userId;
        String token;
        boolean isLoggedIn = preferencesManager.isUserLoggedIn();
        boolean isParticipating = preferencesManager.isParticipateEnabled();
        boolean isAnonymous;

        if (!isLoggedIn) {
            // Usuario NO logueado → anónimo
            userId = 1;
            token = null;
            isAnonymous = true;
            Log.d("SESSION", "Usuario ANÓNIMO detectado - usando userId=1");
        } else if (!isParticipating) {
            // Usuario registrado que NO participa → tratado como anónimo (datos sin asociar)
            userId = 1; // ← CAMBIO CRÍTICO: usar userId=1 para no participantes
            token = null; // ← Sin token para que no se asocie al usuario
            isAnonymous = true; // ← Marcar como anónimo para el backend
            Log.d("SESSION", "Usuario REGISTRADO sin participación - usando userId=1 (anónimo)");
        } else {
            // Usuario registrado que SÍ participa → usar sus credenciales
            userId = preferencesManager.getUserId();
            token = preferencesManager.getUserToken();
            isAnonymous = false;
            Log.d("SESSION", "Usuario REGISTRADO participante - userId=" + userId);
        }

        // Verificar/registrar dispositivo primero
        ensureDeviceRegistered(userId, token, isAnonymous, () -> {
            // Una vez registrado el dispositivo, crear sesión NUEVA
            createNewSession(userId, token, isAnonymous);
        });
    }

    /**
     * Inicializa una nueva sesión anónima después del logout
     * Método público para ser llamado desde ProfileFragment
     */
    public void initializeAnonymousSession() {
        Log.d("SESSION", "=== INICIANDO SESIÓN ANÓNIMA DESPUÉS DE LOGOUT ===");

        // Forzar creación de nueva sesión anónima
        long userId = 1; // Usuario anónimo
        String token = null;
        boolean isAnonymous = true;

        // Verificar/registrar dispositivo primero
        ensureDeviceRegistered(userId, token, isAnonymous, () -> {
            // Una vez registrado el dispositivo, crear sesión
            createNewSession(userId, token, isAnonymous);
        });
    }

    /**
     * Asegura que el dispositivo esté registrado en el backend
     * El backend maneja la lógica de duplicados: mismo UUID con diferentes usuarios
     * está permitido,
     * pero no creará duplicados para la misma combinación UUID+userId
     */
    private void ensureDeviceRegistered(long userId, String token, boolean isAnonymous,
            Runnable onSuccess) {
        // Obtener o generar device_uuid
        String deviceUUID = preferencesManager.getDeviceUUID();
        if (deviceUUID.isEmpty()) {
            deviceUUID = Settings.Secure.getString(
                    getContentResolver(),
                    Settings.Secure.ANDROID_ID);
            preferencesManager.setDeviceUUID(deviceUUID);
        }

        String deviceModel = Build.MANUFACTURER + " " + Build.MODEL;
        String androidVersion = Build.VERSION.RELEASE;

        Log.d("SESSION", String.format(
                "Verificando/registrando dispositivo - UUID: %s, userId: %d %s",
                deviceUUID, userId, isAnonymous ? "(ANÓNIMO)" : ""));

        Log.d("SESSION", String.format(
                "TOKEN: %s", token));

        // Siempre llamar al backend para registrar/verificar
        // El backend retornará el dispositivo existente si ya está registrado con este
        // usuario
        // O creará uno nuevo si es la primera vez que este usuario usa este UUID
        ApiService.registerDevice(userId, deviceUUID, deviceModel, androidVersion, token,
                new ApiService.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        try {
                            if (response.getBoolean("success")) {
                                JSONObject deviceData = response.getJSONObject("data");
                                long deviceId = deviceData.getLong("id_dispositivo");
                                String message = response.optString("message", "");

                                preferencesManager.setDeviceId(deviceId);

                                if (message.contains("existente")) {
                                    Log.i("SESSION", "Dispositivo existente recuperado: " + deviceId);
                                } else {
                                    Log.i("SESSION", "Dispositivo registrado exitosamente: " + deviceId);
                                }

                                onSuccess.run();
                            }
                        } catch (Exception e) {
                            Log.e("SESSION", "Error al procesar dispositivo: " + e.toString());
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("SESSION", "Error al registrar dispositivo: " + error);
                    }
                });
    }

    /**
     * Crea una nueva sesión en el backend
     */
    private void createNewSession(long userId, String token, boolean isAnonymous) {
        long deviceId = preferencesManager.getDeviceId();

        if (deviceId == -1) {
            Log.e("SESSION", "No se puede iniciar sesión: falta deviceId");
            return;
        }

        String contexto = isAnonymous ? "app_start_anonymous" : "app_start";

        Log.d("SESSION", String.format(
                "Creando sesión - userId: %d, deviceId: %d, contexto: %s",
                userId, deviceId, contexto));

        ApiService.startSession(userId, deviceId, contexto, token, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    Log.d("SESSION", "RESPUESTA SESIÓN: " + response.toString());
                    if (response.getBoolean("success")) {
                        JSONObject sessionData = response.getJSONObject("data");
                        long sessionId = sessionData.getLong("id_sesion");

                        Log.d("SESSION", "Sesión creada exitosamente: " + sessionId);

                        // Guardar ID de sesión
                        preferencesManager.setSessionId(sessionId);
                        preferencesManager.setSessionStart(System.currentTimeMillis());

                        // Reiniciar estado de detección de pasos para nueva sesión
                        resetStepDetection();

                        Log.i("SESSION", String.format(
                                "Sesión iniciada exitosamente: %d (Usuario: %s)",
                                sessionId, isAnonymous ? "ANÓNIMO" : "REGISTRADO"));
                    }
                } catch (Exception e) {
                    Log.e("SESSION", "Error al procesar respuesta de sesión: " + e.toString());
                }
            }

            @Override
            public void onError(String error) {
                Log.e("SESSION", "Error al iniciar sesión: " + error);
            }
        });
    }

    /**
     * Reinicia todo el estado de detección de pasos
     * Se llama al iniciar una nueva sesión
     */
    private void resetStepDetection() {
        // Reiniciar buffers
        accBuffer = new float[SAMPLE_SIZE];
        bufferIndex = 0;
        samplesCollected = 0;
        bufferReady = false;

        // Reiniciar warm-up
        detectionStartTime = 0;

        // Reiniciar filtros
        gravity = new float[3]; // Reiniciar vector de gravedad
        accFiltered = 0;
        accMean = 0.0f; // Reiniciar a 0 (se inicializa con verticalAcc)

        // Reiniciar detección de picos
        aboveThreshold = false;
        lastPeakValue = 0;
        lastStepTime = 0;

        // Reiniciar contadores
        stepCount = 0;
        isWalking = false;
        walkingSpeed = "Ninguna";
        currentVariance = 0.0f;

        // Reiniciar métricas avanzadas
        verticalAcc = 0.0f;
        dynamicThreshold = 0.0f;
        stdDev = 0.0f;

        // Reiniciar ventana de validación
        recentStepTimes = new long[STEPS_WINDOW];
        stepTimeIndex = 0;

        // Reiniciar alertas
        totalAlerts = 0;

        // Reiniciar throttling de envío de datos
        lastDataSendTime = 0;

        Log.i("STEP_DETECTION", "Estado de detección de pasos reiniciado");
    }

    private boolean isFinalizingSession = false;

    /**
     * Finaliza la sesión actual si existe
     * Evita múltiples llamadas simultáneas
     */
    private void finalizeSession() {
        // Evitar múltiples llamadas simultáneas
        if (isFinalizingSession) {
            Log.d("SESSION", "Ya se está finalizando la sesión, ignorando llamada duplicada");
            return;
        }

        long sessionId = preferencesManager.getSessionId();

        Log.i("SESSION", "FINALIZE START - ID: " + sessionId);

        if (sessionId == -1) {
            Log.d("SESSION", "No hay sesión activa para finalizar");
            return;
        }

        isFinalizingSession = true;
        String token = preferencesManager.getUserToken();

        ApiService.endSession(sessionId, token, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    if (response.getBoolean("success")) {
                        Log.i("SESSION", "Sesión finalizada exitosamente: " + sessionId);

                        // Limpiar datos de sesión
                        preferencesManager.setSessionId(-1);
                        preferencesManager.setSessionStart(0);
                    }
                } catch (Exception e) {
                    Log.e("SESSION", "Error al procesar fin de sesión: " + e.toString());
                } finally {
                    isFinalizingSession = false;
                }
            }

            @Override
            public void onError(String error) {
                Log.e("SESSION", "Error al finalizar sesión: " + error);
                // Aunque falle, limpiar el ID local
                preferencesManager.setSessionId(-1);
                preferencesManager.setSessionStart(0);
                isFinalizingSession = false;
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Guardar estado pero mantener sesión activa
        Log.d("LIFECYCLE", "MainActivity onPause - sesión se mantiene activa");
    }

    @Override
    protected void onStop() {
        super.onStop();
        // La app está en segundo plano pero la sesión sigue activa
        Log.d("LIFECYCLE", "MainActivity onStop - sesión se mantiene activa");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d("LIFECYCLE", "MainActivity onDestroy - finalizando sesión");

        // Finalizar sesión antes de destruir la actividad
        // El flag isFinalizingSession previene llamadas duplicadas
        finalizeSession();

        sensorManager.unregisterListener(this);
        if (alertSound != null) {
            alertSound.release();
            alertSound = null;
        }
    }
}
