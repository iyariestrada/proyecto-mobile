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

    // Cambiar contraseña
    public static void changePassword(int userId, String currentPassword, String newPassword,
            String token, ApiCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(API_BASE + "/usuarios/changepassword/" + userId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);
                conn.setDoInput(true);

                JSONObject json = new JSONObject();
                json.put("currentPassword", currentPassword);
                json.put("newPassword", newPassword);

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
}
