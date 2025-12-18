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

    // Umbrales de varianza para diferentes tipos de caminata
    private static final float WALKING_VARIANCE_SLOW = 0.15f; // Caminata lenta
    private static final float WALKING_VARIANCE_NORMAL = 0.6f; // Caminata normal
    private static final float WALKING_VARIANCE_FAST = 1.2f; // Caminata rápida

    private static final int SAMPLE_SIZE = 20;
    private static final long WALKING_CHECK_INTERVAL = 250;
    private float[] accMagnitudeHistory = new float[SAMPLE_SIZE];
    private int sampleIndex = 0;
    private long lastWalkingCheck = 0;
    private int stepCount = 0;
    private boolean isWalking = false;
    private String walkingSpeed = "Ninguna"; // "Lenta", "Normal", "Rapida", "Ninguna"
    private float currentVariance = 0.0f;

    private static final float PHONE_USE_GYRO_THRESHOLD = 0.2f;
    private static final float PHONE_TILT_MIN = 20.0f;
    private static final float PHONE_TILT_MAX = 85.0f;
    private boolean isUsingPhone = false;

    private boolean isWalkingAndUsingPhone = false;
    private int totalAlerts = 0;

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

        // Iniciar sesión al abrir la app
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
            sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        if (accelerometerSensor != null) {
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void updateNavigationHeader() {
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
        // Solo enviar datos si hay una sesión activa
        long sessionId = preferencesManager.getSessionId();

        if (sessionId == -1) {
            return; // Sin log repetitivo
        }

        // Los usuarios anónimos siempre envían datos
        // Los usuarios registrados pueden optar por no participar
        if (preferencesManager.isUserLoggedIn() && !preferencesManager.isParticipateEnabled()) {
            return; // Usuario registrado que no participa
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

            // Estado de detección
            json.put("is_walking", isWalking);
            json.put("is_using_phone", isUsingPhone);
            json.put("walking_speed", walkingSpeed);
            json.put("variance", currentVariance);

            // Información del dispositivo
            json.put("battery_level", getBatteryLevel());
            json.put("battery_status", getBatteryStatus());
            json.put("screen_brightness", getScreenBrightness());
            json.put("screen_on", isScreenOn());

            // Timestamp
            json.put("timestamp", System.currentTimeMillis());

            String token = preferencesManager.getUserToken();

            // Usar el nuevo método de ApiService
            ApiService.sendSensorData(sessionId, json, token, new ApiService.ApiCallback() {
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

    private void detectWalking(float accX, float accY, float accZ) {
        // Calcular magnitud de aceleración
        float magnitude = (float) Math.sqrt(accX * accX + accY * accY + accZ * accZ);

        // Almacenar en historial
        accMagnitudeHistory[sampleIndex] = magnitude;
        sampleIndex = (sampleIndex + 1) % SAMPLE_SIZE;

        long currentTime = System.currentTimeMillis();

        // Analizar cada 250ms
        if (currentTime - lastWalkingCheck >= WALKING_CHECK_INTERVAL) {
            lastWalkingCheck = currentTime;

            // Calcular varianza de las muestras
            currentVariance = calculateVariance(accMagnitudeHistory);

            // Calcular frecuencia de pasos (detectar patrón repetitivo)
            float frequency = calculateFrequency(accMagnitudeHistory);

            // Determinar tipo de caminata según varianza y frecuencia
            String previousSpeed = walkingSpeed;

            if (currentVariance >= WALKING_VARIANCE_FAST) {
                isWalking = true;
                walkingSpeed = "Rapida";
                if (!previousSpeed.equals("Rapida")) {
                    stepCount++;
                }
            } else if (currentVariance >= WALKING_VARIANCE_NORMAL) {
                isWalking = true;
                walkingSpeed = "Normal";
                if (!previousSpeed.equals("Normal") && !previousSpeed.equals("Rapida")) {
                    stepCount++;
                }
            } else if (currentVariance >= WALKING_VARIANCE_SLOW) {
                isWalking = true;
                walkingSpeed = "Lenta";
                if (!previousSpeed.equals("Lenta") && !previousSpeed.equals("Normal")
                        && !previousSpeed.equals("Rapida")) {
                    stepCount++;
                }
            } else {
                isWalking = false;
                walkingSpeed = "Ninguna";
            }

            // Log detallado
            Log.d("WALKING_DETECTION", String.format(
                    "Varianza: %.3f | Frecuencia: %.2f Hz | Magnitud: %.3f | Estado: %s | Caminata: %s | Pasos: %d",
                    currentVariance, frequency, magnitude,
                    isWalking ? "CAMINANDO" : "ESTATICO",
                    walkingSpeed, stepCount));
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

    private float calculateFrequency(float[] samples) {
        // Detectar picos en la señal para estimar frecuencia de pasos
        int peakCount = 0;
        float mean = 0;

        for (float sample : samples) {
            mean += sample;
        }
        mean /= samples.length;

        // Contar cruces por encima de la media (picos)
        boolean aboveMean = samples[0] > mean;
        for (int i = 1; i < samples.length; i++) {
            boolean currentAboveMean = samples[i] > mean;
            if (currentAboveMean && !aboveMean) {
                peakCount++;
            }
            aboveMean = currentAboveMean;
        }

        // Frecuencia en Hz (muestras por segundo / muestras totales * picos)
        // Con SAMPLE_SIZE=20 y WALKING_CHECK_INTERVAL=250ms, tenemos 4 análisis por
        // segundo
        float samplesPerSecond = 1000.0f / WALKING_CHECK_INTERVAL;
        float frequency = (peakCount * samplesPerSecond) / SAMPLE_SIZE;

        return frequency;
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
        isWalkingAndUsingPhone = isWalking && isUsingPhone;

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

        String token = preferencesManager.getUserToken();
        String tipoEvento = "ALERTA_CAMINAR_TELEFONO";
        String descripcion = String.format(
                "Alerta: Usuario caminando (%s) mientras usa el teléfono. Varianza: %.3f",
                walkingSpeed, currentVariance);

        ApiService.sendEvento(sessionId, tipoEvento, descripcion, token, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                Log.i("ALERT", "Alerta enviada exitosamente al servidor");
            }

            @Override
            public void onError(String error) {
                Log.e("ALERT", "Error al enviar alerta: " + error);
            }
        });
    }

    public int getStepCount() {
        return stepCount;
    }

    public int getTotalAlerts() {
        return totalAlerts;
    }

    /**
     * Inicializa o recupera la sesión actual (usuarios registrados y anónimos)
     */
    private void initializeSession() {
        Log.d("SESSION", "=== INICIANDO PROCESO DE SESIÓN ===");

        long existingSessionId = preferencesManager.getSessionId();

        // Si ya hay una sesión activa, usarla
        if (existingSessionId != -1) {
            Log.i("SESSION", "Sesión existente recuperada: " + existingSessionId);
            return;
        }

        // Determinar userId: 0 para anónimo, userId real para registrados
        long userId;
        String token;
        boolean isAnonymous = !preferencesManager.isUserLoggedIn();

        if (isAnonymous) {
            userId = 0; // Usuario anónimo genérico
            token = null;
            Log.d("SESSION", "Usuario ANÓNIMO detectado - usando userId=0");
        } else {
            userId = preferencesManager.getUserId();
            token = preferencesManager.getUserToken();
            Log.d("SESSION", "Usuario REGISTRADO detectado - userId=" + userId);
        }

        // Verificar/registrar dispositivo primero
        ensureDeviceRegistered(userId, token, isAnonymous, () -> {
            // Una vez registrado el dispositivo, crear sesión
            createNewSession(userId, token, isAnonymous);
        });
    }

    /**
     * Asegura que el dispositivo esté registrado en el backend
     */
    private void ensureDeviceRegistered(long userId, String token, boolean isAnonymous,
            Runnable onSuccess) {
        long existingDeviceId = preferencesManager.getDeviceId();

        // Si ya tenemos deviceId y no es anónimo, continuar directamente
        if (existingDeviceId != -1 && !isAnonymous) {
            Log.d("SESSION", "Dispositivo ya registrado: " + existingDeviceId);
            onSuccess.run();
            return;
        }

        // Si es anónimo o no tenemos deviceId, registrar/verificar dispositivo
        String deviceUUID = preferencesManager.getDeviceUUID();
        if (deviceUUID.isEmpty()) {
            deviceUUID = Settings.Secure.getString(
                    getContentResolver(),
                    Settings.Secure.ANDROID_ID);
            preferencesManager.setDeviceUUID(deviceUUID);
        }

        String deviceModel = Build.MANUFACTURER + " " + Build.MODEL;
        String androidVersion = Build.VERSION.RELEASE;

        Log.d("SESSION", "Registrando dispositivo - UUID: " + deviceUUID);

        ApiService.registerDevice(userId, deviceUUID, deviceModel, androidVersion, token,
                new ApiService.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        try {
                            if (response.getBoolean("success")) {
                                JSONObject deviceData = response.getJSONObject("data");
                                long deviceId = deviceData.getLong("id");

                                preferencesManager.setDeviceId(deviceId);
                                Log.i("SESSION", "Dispositivo registrado exitosamente: " + deviceId);

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
                    if (response.getBoolean("success")) {
                        JSONObject sessionData = response.getJSONObject("data");
                        long sessionId = sessionData.getLong("id");

                        // Guardar ID de sesión
                        preferencesManager.setSessionId(sessionId);
                        preferencesManager.setSessionStart(System.currentTimeMillis());

                        Log.i("SESSION", String.format(
                                "✓ Sesión iniciada exitosamente: %d (Usuario: %s)",
                                sessionId, isAnonymous ? "ANÓNIMO" : "REGISTRADO"));
                    }
                } catch (Exception e) {
                    Log.e("SESSION", "Error al procesar respuesta de sesión: " + e.toString());
                }
            }

            @Override
            public void onError(String error) {
                Log.e("SESSION", "✗ Error al iniciar sesión: " + error);
            }
        });
    }

    /**
     * Finaliza la sesión actual si existe
     */
    private void finalizeSession() {
        long sessionId = preferencesManager.getSessionId();

        if (sessionId == -1) {
            Log.d("SESSION", "No hay sesión activa para finalizar");
            return;
        }

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
                }
            }

            @Override
            public void onError(String error) {
                Log.e("SESSION", "Error al finalizar sesión: " + error);
                // Aunque falle, limpiar el ID local
                preferencesManager.setSessionId(-1);
                preferencesManager.setSessionStart(0);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Finalizar sesión antes de destruir la actividad
        finalizeSession();

        sensorManager.unregisterListener(this);
        if (alertSound != null) {
            alertSound.release();
            alertSound = null;
        }
    }
}
