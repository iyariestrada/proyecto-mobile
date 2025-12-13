package com.example.sensorprojectv1;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.TextView;
import android.util.Log;

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

    private static final String API_URL = "http://192.168.1.80:3001/api/sensor/write";  // IMPORTANTE para emulador

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewGyro = findViewById(R.id.textViewGyro);
        textViewAcc = findViewById(R.id.textViewAcc);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
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

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gyroX = event.values[0];
            gyroY = event.values[1];
            gyroZ = event.values[2];

            runOnUiThread(() -> textViewGyro.setText(
                    String.format("Giroscopio:\nX: %.3f\nY: %.3f\nZ: %.3f",
                            gyroX, gyroY, gyroZ)
            ));
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accX = event.values[0];
            accY = event.values[1];
            accZ = event.values[2];

            runOnUiThread(() -> textViewAcc.setText(
                    String.format("Acelerómetro:\nX: %.3f\nY: %.3f\nZ: %.3f",
                            accX, accY, accZ)
            ));
        }

        // Enviar siempre que haya nueva medición
        sendSensorData();
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

                // Construir JSON
                JSONObject json = new JSONObject();
                json.put("accX", accX);
                json.put("accY", accY);
                json.put("accZ", accZ);
                json.put("gyroX", gyroX);
                json.put("gyroY", gyroY);
                json.put("gyroZ", gyroZ);

                // Enviar datos
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
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
    }
}