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

        // Verificar si ya está logueado
        if (preferencesManager.isUserLoggedIn()) {
            goToMainActivity();
            return;
        }

        initializeViews();
        setupListeners();
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
        // Botón Login
        btnLogin.setOnClickListener(v -> performLogin());

        // Ir a Registro
        tvGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // Olvidé contraseña
        tvForgotPassword.setOnClickListener(v -> {
            Toast.makeText(this, "Funcionalidad de recuperacion en desarrollo", Toast.LENGTH_SHORT).show();
        });

        // Continuar sin cuenta (modo anónimo)
        tvContinueAnonymous.setOnClickListener(v -> {
            Toast.makeText(this, "Modo anonimo: No tendras acceso al historial de alertas", Toast.LENGTH_LONG).show();
            goToMainActivity();
        });
    }

    private void performLogin() {
        String email = etLoginEmail.getText().toString().trim();
        String password = etLoginPassword.getText().toString().trim();

        // Validaciones
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

        // Deshabilitar botón mientras se procesa
        btnLogin.setEnabled(false);
        btnLogin.setText("Iniciando sesion...");

        // Llamar a la API
        ApiService.login(email, password, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    try {
                        boolean success = response.getBoolean("success");

                        if (success) {
                            JSONObject user = response.getJSONObject("user");
                            String token = response.getString("token");

                            // Guardar datos del usuario
                            preferencesManager.setUserLoggedIn(true);
                            preferencesManager.setUserId(user.getInt("id"));
                            preferencesManager.setUserName(user.getString("nombre"));
                            preferencesManager.setUserEmail(email);
                            preferencesManager.setUserToken(token);
                            preferencesManager.setUserType(user.optString("tipo", ""));

                            Toast.makeText(LoginActivity.this,
                                    "Bienvenido " + user.getString("nombre"),
                                    Toast.LENGTH_SHORT).show();

                            // Ir a MainActivity
                            goToMainActivity();
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

    private void goToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
