package com.example.sensorprojectv1;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ApiService {
    private static final String API_BASE = "http://192.168.1.80:3001/api";

    public interface ApiCallback {
        void onSuccess(JSONObject response);

        void onError(String error);
    }

    // Login de usuario
    public static void login(String email, String password, ApiCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(API_BASE + "/usuarios/login");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);
                conn.setDoInput(true);

                JSONObject json = new JSONObject();
                json.put("email", email);
                json.put("password", password);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    callback.onSuccess(jsonResponse);
                } else {
                    callback.onError("Credenciales incorrectas");
                }

                conn.disconnect();

            } catch (Exception e) {
                Log.e("API_LOGIN", "Error: " + e.toString());
                callback.onError("Error al conectar con el servidor");
            }
        }).start();
    }

    // Registro de nuevo usuario
    public static void registerNewUser(String nombre, String correo, String password,
            String confirmPassword, ApiCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(API_BASE + "/usuarios/register");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);
                conn.setDoInput(true);

                JSONObject json = new JSONObject();
                json.put("nombre", nombre);
                json.put("correo", correo);
                json.put("password", password);
                json.put("confirmPassword", confirmPassword);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();

                BufferedReader in = new BufferedReader(new InputStreamReader(
                        responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                JSONObject jsonResponse = new JSONObject(response.toString());

                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                    callback.onSuccess(jsonResponse);
                } else {
                    String errorMsg = jsonResponse.optString("message", "Error al completar registro");
                    callback.onError(errorMsg);
                }

                conn.disconnect();

            } catch (Exception e) {
                Log.e("API_REGISTER", "Error: " + e.toString());
                callback.onError("Error al conectar con el servidor");
            }
        }).start();
    }

    // Verificar si email está disponible
    public static void checkEmailAvailability(String email, ApiCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(API_BASE + "/usuarios/registervalid/" + email);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Content-Type", "application/json");

                int responseCode = conn.getResponseCode();

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                JSONObject jsonResponse = new JSONObject(response.toString());
                callback.onSuccess(jsonResponse);

                conn.disconnect();

            } catch (Exception e) {
                Log.e("API_CHECK_EMAIL", "Error: " + e.toString());
                callback.onError("Error al verificar email");
            }
        }).start();
    }

    // Actualizar datos del usuario
    public static void updateUser(int userId, String nombre, String correo, String token, ApiCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(API_BASE + "/usuarios/update/" + userId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);
                conn.setDoInput(true);

                JSONObject json = new JSONObject();
                json.put("nombre", nombre);
                json.put("correo", correo);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();

                BufferedReader in = new BufferedReader(new InputStreamReader(
                        responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                JSONObject jsonResponse = new JSONObject(response.toString());

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    callback.onSuccess(jsonResponse);
                } else {
                    String errorMsg = jsonResponse.optString("message", "Error al actualizar datos");
                    callback.onError(errorMsg);
                }

                conn.disconnect();

            } catch (Exception e) {
                Log.e("API_UPDATE_USER", "Error: " + e.toString());
                callback.onError("Error al conectar con el servidor");
            }
        }).start();
    }

    // Registrar o actualizar dispositivo
    public static void registerDevice(long userId, String deviceUUID, String deviceModel,
            String androidVersion, String token, ApiCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(API_BASE + "/dispositivos/register");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                // Solo agregar token si no es usuario anónimo
                if (token != null && !token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                }

                conn.setDoOutput(true);
                conn.setDoInput(true);

                JSONObject json = new JSONObject();
                json.put("id_usuario", userId);
                json.put("device_uuid", deviceUUID);
                json.put("device_model", deviceModel);
                json.put("android_version", androidVersion);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();

                BufferedReader in = new BufferedReader(new InputStreamReader(
                        responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                JSONObject jsonResponse = new JSONObject(response.toString());

                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                    callback.onSuccess(jsonResponse);
                } else {
                    String errorMsg = jsonResponse.optString("message", "Error al registrar dispositivo");
                    callback.onError(errorMsg);
                }

                conn.disconnect();

            } catch (Exception e) {
                Log.e("API_REGISTER_DEVICE", "Error: " + e.toString());
                callback.onError("Error al conectar con el servidor");
            }
        }).start();
    }

    // Iniciar nueva sesión
    public static void startSession(long userId, long deviceId, String contexto,
            String token, ApiCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(API_BASE + "/sesiones/start");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                // Solo agregar token si no es usuario anónimo
                if (token != null && !token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                }

                conn.setDoOutput(true);
                conn.setDoInput(true);

                JSONObject json = new JSONObject();
                json.put("id_usuario", userId);
                json.put("id_dispositivo", deviceId);
                json.put("contexto", contexto);
                // El start_time se asigna automáticamente en el backend

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();

                BufferedReader in = new BufferedReader(new InputStreamReader(
                        responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                JSONObject jsonResponse = new JSONObject(response.toString());

                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                    callback.onSuccess(jsonResponse);
                } else {
                    String errorMsg = jsonResponse.optString("message", "Error al iniciar sesión");
                    callback.onError(errorMsg);
                }

                conn.disconnect();

            } catch (Exception e) {
                Log.e("API_START_SESSION", "Error: " + e.toString());
                callback.onError("Error al conectar con el servidor");
            }
        }).start();
    }

    // Finalizar sesión
    public static void endSession(long sessionId, String token, ApiCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(API_BASE + "/sesiones/" + sessionId + "/finalizar");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);

                int responseCode = conn.getResponseCode();

                BufferedReader in = new BufferedReader(new InputStreamReader(
                        responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                JSONObject jsonResponse = new JSONObject(response.toString());

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    callback.onSuccess(jsonResponse);
                } else {
                    String errorMsg = jsonResponse.optString("message", "Error al finalizar sesión");
                    callback.onError(errorMsg);
                }

                conn.disconnect();

            } catch (Exception e) {
                Log.e("API_END_SESSION", "Error: " + e.toString());
                callback.onError("Error al conectar con el servidor");
            }
        }).start();
    }

    // Cambiar contraseña
    public static void changePassword(String userEmail, String currentPassword, String newPassword,
            String confirmPassword,
            String token, ApiCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(API_BASE + "/usuarios/changepassword/" + userEmail);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);
                conn.setDoInput(true);

                JSONObject json = new JSONObject();
                json.put("correo", userEmail);
                json.put("currentPassword", currentPassword);
                json.put("newPassword", newPassword);
                json.put("confirmPassword", confirmPassword);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();

                BufferedReader in = new BufferedReader(new InputStreamReader(
                        responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                JSONObject jsonResponse = new JSONObject(response.toString());

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    callback.onSuccess(jsonResponse);
                } else {
                    String errorMsg = jsonResponse.optString("message", "Error al cambiar contraseña");
                    callback.onError(errorMsg);
                }

                conn.disconnect();

            } catch (Exception e) {
                Log.e("API_CHANGE_PASS", "Error: " + e.toString());
                callback.onError("Error al conectar con el servidor");
            }
        }).start();
    }

    // Enviar datos de sensores individuales
    public static void sendSensorData(long sessionId, JSONObject sensorData, String token, ApiCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(API_BASE + "/sensordata");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);
                conn.setDoInput(true);

                // Agregar id_sesion a los datos
                sensorData.put("id_sesion", sessionId);

                OutputStream os = conn.getOutputStream();
                os.write(sensorData.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();

                BufferedReader in = new BufferedReader(new InputStreamReader(
                        responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                JSONObject jsonResponse = new JSONObject(response.toString());

                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                    callback.onSuccess(jsonResponse);
                } else {
                    String errorMsg = jsonResponse.optString("message", "Error al enviar datos de sensores");
                    callback.onError(errorMsg);
                }

                conn.disconnect();

            } catch (Exception e) {
                Log.e("API_SENSOR_DATA", "Error: " + e.toString());
                callback.onError("Error al conectar con el servidor");
            }
        }).start();
    }

    // Enviar lote de datos de sensores (batch)
    public static void sendSensorDataBatch(long sessionId, org.json.JSONArray sensorDataArray,
            String token, ApiCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(API_BASE + "/sensordata/batch");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);
                conn.setDoInput(true);

                // Agregar id_sesion a cada elemento del array
                for (int i = 0; i < sensorDataArray.length(); i++) {
                    JSONObject item = sensorDataArray.getJSONObject(i);
                    item.put("id_sesion", sessionId);
                }

                OutputStream os = conn.getOutputStream();
                os.write(sensorDataArray.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();

                BufferedReader in = new BufferedReader(new InputStreamReader(
                        responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                JSONObject jsonResponse = new JSONObject(response.toString());

                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                    callback.onSuccess(jsonResponse);
                } else {
                    String errorMsg = jsonResponse.optString("message", "Error al enviar lote de datos");
                    callback.onError(errorMsg);
                }

                conn.disconnect();

            } catch (Exception e) {
                Log.e("API_SENSOR_BATCH", "Error: " + e.toString());
                callback.onError("Error al conectar con el servidor");
            }
        }).start();
    }

    // Enviar evento/alerta
    public static void sendEvento(long sessionId, String tipoEvento, String descripcion,
            String token, ApiCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(API_BASE + "/eventos");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);
                conn.setDoInput(true);

                JSONObject json = new JSONObject();
                json.put("id_sesion", sessionId);
                json.put("tipo_evento", tipoEvento);
                json.put("descripcion", descripcion);
                json.put("timestamp", System.currentTimeMillis());

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();

                BufferedReader in = new BufferedReader(new InputStreamReader(
                        responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                JSONObject jsonResponse = new JSONObject(response.toString());

                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                    callback.onSuccess(jsonResponse);
                } else {
                    String errorMsg = jsonResponse.optString("message", "Error al enviar evento");
                    callback.onError(errorMsg);
                }

                conn.disconnect();

            } catch (Exception e) {
                Log.e("API_EVENTO", "Error: " + e.toString());
                callback.onError("Error al conectar con el servidor");
            }
        }).start();
    }
}
