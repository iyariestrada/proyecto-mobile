package com.example.sensorprojectv1;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etLoginEmail, etLoginPassword;
    private Button btnLogin, tvContinueAnonymous;
    private TextView tvGoToRegister, tvForgotPassword;
    private ProgressBar progressBar;
    private PreferencesManager preferencesManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        preferencesManager = new PreferencesManager(this);

        // Verificar si hay sesión activa y válida
        if (preferencesManager.isUserLoggedIn()) {
            // Verificar si el token es válido antes de saltar a MainActivity
            String token = preferencesManager.getUserToken();
            long userId = preferencesManager.getUserId();

            if (token != null && !token.isEmpty() && userId != -1) {
                validateSessionAndProceed(token, userId);
                return;
            } else {
                // Token inválido o faltante - limpiar sesión corrupta
                android.util.Log.w("LOGIN", "Sesión corrupta detectada - limpiando datos");
                preferencesManager.logout();
            }
        }

        initializeViews();
        setupListeners();
    }

    /**
     * Valida la sesión existente con el backend antes de saltar a MainActivity
     * Previene acceso con sesiones expiradas o datos de instalaciones anteriores
     */
    private void validateSessionAndProceed(String token, long userId) {
        // TODO: Implementar endpoint de validación de token en el backend
        // Por ahora, asumir que si hay token, la sesión es válida
        // En producción, deberías hacer una llamada al backend para verificar

        // TEMPORAL: Ir directamente a MainActivity
        // RECOMENDACIÓN: Agregar endpoint /api/auth/validate en el backend
        goToMainActivity();

        /*
         * IMPLEMENTACIÓN RECOMENDADA (comentada para no romper el flujo actual):
         * ApiService.validateToken(token, new ApiService.ApiCallback() {
         * 
         * @Override
         * public void onSuccess(JSONObject response) {
         * runOnUiThread(() -> {
         * try {
         * if (response.getBoolean("valid")) {
         * // Token válido → ir a MainActivity
         * goToMainActivity();
         * } else {
         * // Token expirado → limpiar y mostrar login
         * preferencesManager.logout();
         * initializeViews();
         * setupListeners();
         * Toast.makeText(LoginActivity.this,
         * "Sesión expirada - Por favor inicia sesión nuevamente",
         * Toast.LENGTH_LONG).show();
         * }
         * } catch (Exception e) {
         * // Error al validar → limpiar y mostrar login
         * preferencesManager.logout();
         * initializeViews();
         * setupListeners();
         * }
         * });
         * }
         * 
         * @Override
         * public void onError(String error) {
         * runOnUiThread(() -> {
         * // Error de red o backend → limpiar y mostrar login
         * preferencesManager.logout();
         * initializeViews();
         * setupListeners();
         * Toast.makeText(LoginActivity.this,
         * "No se pudo verificar la sesión",
         * Toast.LENGTH_SHORT).show();
         * });
         * }
         * });
         */
    }

    private void initializeViews() {
        etLoginEmail = findViewById(R.id.etLoginEmail);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvContinueAnonymous = findViewById(R.id.tvContinueAnonymous);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> performLogin());

        tvGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        tvContinueAnonymous.setOnClickListener(v -> {
            Toast.makeText(this, "Modo anonimo: No tendras acceso al historial de alertas", Toast.LENGTH_LONG).show();
            goToMainActivity();
        });
    }

    private void performLogin() {
        String email = etLoginEmail.getText().toString().trim();
        String password = etLoginPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etLoginEmail.setError("Ingresa tu correo");
            etLoginEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etLoginEmail.setError("Correo invalido");
            etLoginEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etLoginPassword.setError("Ingresa tu contrasena");
            etLoginPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etLoginPassword.setError("La contrasena debe tener al menos 6 caracteres");
            etLoginPassword.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Iniciando sesion...");

        ApiService.login(email, password, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    try {
                        boolean success = response.getBoolean("success");

                        if (success) {
                            JSONObject user = response.getJSONObject("user");
                            String token = response.getString("token");

                            preferencesManager.setUserLoggedIn(true);
                            preferencesManager.setUserId(user.getInt("id"));
                            preferencesManager.setUserName(user.getString("nombre"));
                            preferencesManager.setUserEmail(email);
                            preferencesManager.setUserToken(token);
                            preferencesManager.setUserType(user.optString("tipo", ""));

                            Toast.makeText(LoginActivity.this,
                                    "Bienvenido " + user.getString("nombre"),
                                    Toast.LENGTH_SHORT).show();

                            registerDeviceAndGoToMain(token);
                        } else {
                            Toast.makeText(LoginActivity.this,
                                    "Credenciales incorrectas",
                                    Toast.LENGTH_SHORT).show();
                            btnLogin.setEnabled(true);
                            btnLogin.setText("Iniciar Sesion");
                        }

                    } catch (Exception e) {
                        Toast.makeText(LoginActivity.this,
                                "Error al procesar respuesta",
                                Toast.LENGTH_SHORT).show();
                        btnLogin.setEnabled(true);
                        btnLogin.setText("Iniciar Sesion");
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, error, Toast.LENGTH_SHORT).show();
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Iniciar Sesion");
                });
            }
        });
    }

    private void registerDeviceAndGoToMain(String token) {
        String deviceUUID = preferencesManager.getDeviceUUID();
        if (deviceUUID.isEmpty()) {
            deviceUUID = android.provider.Settings.Secure.getString(
                    getContentResolver(),
                    android.provider.Settings.Secure.ANDROID_ID);
            preferencesManager.setDeviceUUID(deviceUUID);
        }

        String deviceModel = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;
        String androidVersion = android.os.Build.VERSION.RELEASE;
        long userId = preferencesManager.getUserId();

        ApiService.registerDevice(userId, deviceUUID, deviceModel, androidVersion, token,
                new ApiService.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        runOnUiThread(() -> {
                            try {
                                if (response.getBoolean("success")) {
                                    JSONObject deviceData = response.getJSONObject("data");
                                    long deviceId = deviceData.getLong("id_dispositivo");

                                    preferencesManager.setDeviceId(deviceId);

                                    goToMainActivity();
                                } else {
                                    Toast.makeText(LoginActivity.this,
                                            "Error al registrar dispositivo",
                                            Toast.LENGTH_SHORT).show();
                                    goToMainActivity();
                                }
                            } catch (Exception e) {
                                Toast.makeText(LoginActivity.this,
                                        "Error al procesar dispositivo",
                                        Toast.LENGTH_SHORT).show();
                                goToMainActivity();
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(LoginActivity.this,
                                    "Advertencia: " + error,
                                    Toast.LENGTH_SHORT).show();
                            goToMainActivity();
                        });
                    }
                });
    }

    private void goToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
