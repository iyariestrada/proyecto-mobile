package com.example.sensorprojectv1;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.TextView;
import android.util.Log;
import android.os.BatteryManager;
import android.content.Context;
import android.content.IntentFilter;
import android.content.Intent;
import android.provider.Settings;
import android.os.Build;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor gyroscopeSensor, accelerometerSensor;
    private float gyroX, gyroY, gyroZ;
    private float accX, accY, accZ;
    private TextView textViewGyro, textViewAcc;

    private static final String API_URL = "http://192.168.1.80:3001/api/sensor/write";

    private static final float WALKING_VARIANCE_THRESHOLD = 0.5f; // Umbral de varianza para detectar pasos
    private static final int SAMPLE_SIZE = 20; // Número de muestras para análisis
    private static final long WALKING_CHECK_INTERVAL = 250; // ms
    private float[] accMagnitudeHistory = new float[SAMPLE_SIZE];
    private int sampleIndex = 0;
    private long lastWalkingCheck = 0;
    private int stepCount = 0;
    private boolean isWalking = false;

    private static final float PHONE_USE_GYRO_THRESHOLD = 0.2f; // Movimiento del giroscopio
    private static final float PHONE_TILT_MIN = 20.0f; // Mínima inclinación
    private static final float PHONE_TILT_MAX = 85.0f; // Máxima inclinación
    private boolean isUsingPhone = false;

    private boolean isWalkingAndUsingPhone = false;
    private TextView textViewStatus;

    private BatteryManager batteryManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewGyro = findViewById(R.id.textViewGyro);
        textViewAcc = findViewById(R.id.textViewAcc);
        textViewStatus = findViewById(R.id.textViewStatus);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (gyroscopeSensor != null) {
            sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            textViewGyro.setText("Giroscopio no disponible");
        }

        if (accelerometerSensor != null) {
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            textViewAcc.setText("Acelerómetro no disponible");
        }
    }

    private void sendSensorData() {
        new Thread(() -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);
                conn.setDoInput(true);

                JSONObject json = new JSONObject();
                json.put("accX", accX);
                json.put("accY", accY);
                json.put("accZ", accZ);
                json.put("gyroX", gyroX);
                json.put("gyroY", gyroY);
                json.put("gyroZ", gyroZ);

                // Agregar datos de detección
                json.put("isWalking", isWalking);
                json.put("isUsingPhone", isUsingPhone);
                json.put("isWalkingAndUsingPhone", isWalkingAndUsingPhone);
                json.put("stepCount", stepCount);
                json.put("timestamp", System.currentTimeMillis());

                // Agregar datos del dispositivo
                json.put("batteryLevel", getBatteryLevel());
                json.put("batteryStatus", getBatteryStatus());
                json.put("screenBrightness", getScreenBrightness());
                json.put("screenOn", isScreenOn());
                json.put("deviceModel", getDeviceModel());
                json.put("androidVersion", getAndroidVersion());

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                Log.d("HTTP", "Código de respuesta: " + responseCode);

                conn.disconnect();

            } catch (Exception e) {
                Log.e("HTTP_ERROR", e.toString());
            }
        }).start();
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
        return true; // Si la app esta activa, la pantalla esta encendida
    }

    private String getDeviceModel() {
        return Build.MANUFACTURER + " " + Build.MODEL;
    }

    private String getAndroidVersion() {
        return Build.VERSION.RELEASE;
    }

    private void detectWalking(float accX, float accY, float accZ) {
        // Calcular la magnitud de aceleración (restando gravedad aproximada)
        float magnitude = (float) Math.sqrt(accX * accX + accY * accY + accZ * accZ);

        // Almacenar en el historial
        accMagnitudeHistory[sampleIndex] = magnitude;
        sampleIndex = (sampleIndex + 1) % SAMPLE_SIZE;

        long currentTime = System.currentTimeMillis();

        // Analizar cada 250 ms
        if (currentTime - lastWalkingCheck >= WALKING_CHECK_INTERVAL) {
            lastWalkingCheck = currentTime;

            // Calcular la varianza de las últimas muestras
            float variance = calculateVariance(accMagnitudeHistory);

            // Si hay varianza (movimiento periódico), está caminando
            if (variance > WALKING_VARIANCE_THRESHOLD) {
                isWalking = true;
                stepCount++;
                Log.d("WALKING_DETECTION", "Varianza: " + variance + " - Caminando detectado");
            } else {
                isWalking = false;
                Log.d("WALKING_DETECTION", "Varianza: " + variance + " - Estático");
            }
        }
    }

    private float calculateVariance(float[] samples) {
        // Calcular media
        float sum = 0;
        for (float sample : samples) {
            sum += sample;
        }
        float mean = sum / samples.length;

        // Calcular varianza
        float varianceSum = 0;
        for (float sample : samples) {
            varianceSum += Math.pow(sample - mean, 2);
        }

        return varianceSum / samples.length;
    }

    private void detectPhoneUsage(float gyroX, float gyroY, float gyroZ,
            float accX, float accY, float accZ) {
        // Detectar movimiento del giroscopio (usuario moviendo/manipulando el teléfono)
        float gyroMagnitude = (float) Math.sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ);

        // Calcular ángulos de orientación del teléfono
        float pitch = (float) Math.toDegrees(Math.atan2(accX, Math.sqrt(accY * accY + accZ * accZ)));
        float roll = (float) Math.toDegrees(Math.atan2(accY, accZ));

        // Verificar si el teléfono está en posición de uso (vertical u orientado)
        boolean isPhoneOriented = (Math.abs(pitch) > PHONE_TILT_MIN && Math.abs(pitch) < PHONE_TILT_MAX) ||
                (Math.abs(roll) > PHONE_TILT_MIN && Math.abs(roll) < PHONE_TILT_MAX);

        // El teléfono se considera en uso si:
        // 1. Hay movimiento del giroscopio (usuario interactuando) O
        // 2. Está en posición de uso (orientado hacia el usuario)
        isUsingPhone = (gyroMagnitude > PHONE_USE_GYRO_THRESHOLD) || isPhoneOriented;

        Log.d("PHONE_USAGE", String.format("Gyro: %.3f, Pitch: %.2f, Roll: %.2f, Using: %s",
                gyroMagnitude, pitch, roll, isUsingPhone ? "SI" : "NO"));
    }

    private void detectWalkingAndPhoneUse() {
        boolean previousState = isWalkingAndUsingPhone;
        isWalkingAndUsingPhone = isWalking && isUsingPhone;

        // Actualizar UI con el estado
        runOnUiThread(() -> {
            String status = "";

            if (isWalkingAndUsingPhone) {
                status = "ALERTA: Caminando y usando el teléfono";
            } else if (isWalking) {
                status = "Caminando";
            } else if (isUsingPhone) {
                status = "Usando teléfono";
            } else {
                status = "Estado seguro";
            }

            status += String.format("\n\nPasos: %d\nCaminando: %s\nUsando teléfono: %s",
                    stepCount, isWalking ? "Sí" : "No", isUsingPhone ? "Sí" : "No");

            textViewStatus.setText(status);
        });

        // Si cambió el estado, enviar notificación al servidor
        if (previousState != isWalkingAndUsingPhone) {
            sendAlertToServer();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gyroX = event.values[0];
            gyroY = event.values[1];
            gyroZ = event.values[2];

            runOnUiThread(() -> textViewGyro.setText(
                    String.format("Giroscopio:\nX: %.3f\nY: %.3f\nZ: %.3f",
                            gyroX, gyroY, gyroZ)));
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accX = event.values[0];
            accY = event.values[1];
            accZ = event.values[2];

            runOnUiThread(() -> textViewAcc.setText(
                    String.format("Acelerómetro:\nX: %.3f\nY: %.3f\nZ: %.3f",
                            accX, accY, accZ)));

            // Detectar si está caminando
            detectWalking(accX, accY, accZ);
        }

        // Detectar uso del teléfono (requiere ambos sensores)
        detectPhoneUsage(gyroX, gyroY, gyroZ, accX, accY, accZ);

        // Analizar estado combinado
        detectWalkingAndPhoneUse();

        // Enviar datos con el estado
        sendSensorData();
    }

    // metodo para alertas checar

    private void sendAlertToServer() {
        new Thread(() -> {
            try {
                URL url = new URL(API_URL.replace("/write", "/alert"));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("alert", "WALKING_AND_PHONE_USE");
                json.put("timestamp", System.currentTimeMillis());
                json.put("isActive", isWalkingAndUsingPhone);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.close();

                conn.getResponseCode();
                conn.disconnect();

            } catch (Exception e) {
                Log.e("ALERT_ERROR", e.toString());
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
    }
}