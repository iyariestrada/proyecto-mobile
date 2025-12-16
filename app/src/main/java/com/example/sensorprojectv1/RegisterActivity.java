package com.example.sensorprojectv1;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etRegisterName, etRegisterEmail, etRegisterPassword, etRegisterConfirmPassword;
    private Button btnRegister;
    private CheckBox cbParticipateStudy;
    private TextView tvGoToLogin;
    private PreferencesManager preferencesManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        preferencesManager = new PreferencesManager(this);

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        etRegisterName = findViewById(R.id.etRegisterName);
        etRegisterEmail = findViewById(R.id.etRegisterEmail);
        etRegisterPassword = findViewById(R.id.etRegisterPassword);
        etRegisterConfirmPassword = findViewById(R.id.etRegisterConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        cbParticipateStudy = findViewById(R.id.cbParticipateStudy);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);
    }

    private void setupListeners() {
        // Botón Registrar
        btnRegister.setOnClickListener(v -> performRegister());

        // Ir a Login
        tvGoToLogin.setOnClickListener(v -> {
            finish(); // Volver a LoginActivity
        });
    }

    private void performRegister() {
        String name = etRegisterName.getText().toString().trim();
        String email = etRegisterEmail.getText().toString().trim();
        String password = etRegisterPassword.getText().toString().trim();
        String confirmPassword = etRegisterConfirmPassword.getText().toString().trim();

        // Validaciones
        if (TextUtils.isEmpty(name)) {
            etRegisterName.setError("El nombre es requerido");
            etRegisterName.requestFocus();
            return;
        }

        if (name.length() < 3) {
            etRegisterName.setError("El nombre debe tener al menos 3 caracteres");
            etRegisterName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etRegisterEmail.setError("El email es requerido");
            etRegisterEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etRegisterEmail.setError("Email no valido");
            etRegisterEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etRegisterPassword.setError("La contrasena es requerida");
            etRegisterPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etRegisterPassword.setError("La contrasena debe tener al menos 6 caracteres");
            etRegisterPassword.requestFocus();
            return;
        }

        // Validar requisitos de contraseña (similar a la web)
        if (!validatePassword(password)) {
            etRegisterPassword.setError("La contrasena no cumple todos los requisitos");
            etRegisterPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            etRegisterConfirmPassword.setError("Confirma tu contrasena");
            etRegisterConfirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etRegisterConfirmPassword.setError("Las contrasenas no coinciden");
            etRegisterConfirmPassword.requestFocus();
            return;
        }

        // Guardar preferencia de participación
        boolean participateInStudy = cbParticipateStudy.isChecked();

        // Deshabilitar botón mientras se procesa
        btnRegister.setEnabled(false);
        btnRegister.setText("Creando cuenta...");

        // Llamar a la API
        ApiService.registerNewUser(name, email, password, confirmPassword, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    try {
                        boolean success = response.getBoolean("success");

                        if (success) {
                            // Guardar preferencia de participación
                            preferencesManager.setParticipateEnabled(participateInStudy);

                            Toast.makeText(RegisterActivity.this,
                                    "Registro exitoso. Por favor, inicia sesion.",
                                    Toast.LENGTH_LONG).show();

                            // Volver a LoginActivity
                            finish();
                        } else {
                            String errorMsg = response.optString("message", "Error al completar registro");
                            Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                            btnRegister.setEnabled(true);
                            btnRegister.setText("Crear Cuenta");
                        }

                    } catch (Exception e) {
                        Toast.makeText(RegisterActivity.this,
                                "Error al procesar respuesta",
                                Toast.LENGTH_SHORT).show();
                        btnRegister.setEnabled(true);
                        btnRegister.setText("Crear Cuenta");
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(RegisterActivity.this, error, Toast.LENGTH_SHORT).show();
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Crear Cuenta");
                });
            }
        });
    }

    private boolean validatePassword(String password) {
        // Validaciones de contraseña
        // Mínimo 6 caracteres (ya validado antes)
        // Puede incluir más validaciones si son requeridas por el backend
        return password.length() >= 6;
    }
}
