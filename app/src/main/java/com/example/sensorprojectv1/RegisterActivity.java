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

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etRegisterUsername, etRegisterEmail, etRegisterPassword, etRegisterConfirmPassword;
    private Button btnRegister, btnGoogleSignUp;
    private CheckBox cbTermsAndConditions, cbParticipateStudy;
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
        etRegisterUsername = findViewById(R.id.etRegisterUsername);
        etRegisterEmail = findViewById(R.id.etRegisterEmail);
        etRegisterPassword = findViewById(R.id.etRegisterPassword);
        etRegisterConfirmPassword = findViewById(R.id.etRegisterConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnGoogleSignUp = findViewById(R.id.btnGoogleSignUp);
        cbTermsAndConditions = findViewById(R.id.cbTermsAndConditions);
        cbParticipateStudy = findViewById(R.id.cbParticipateStudy);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);
    }

    private void setupListeners() {
        // Botón Registrar
        btnRegister.setOnClickListener(v -> performRegister());

        // Google Sign-Up
        btnGoogleSignUp.setOnClickListener(v -> {
            performGoogleSignUp();
        });

        // Ir a Login
        tvGoToLogin.setOnClickListener(v -> {
            finish(); // Volver a LoginActivity
        });
    }

    private void performRegister() {
        String username = etRegisterUsername.getText().toString().trim();
        String email = etRegisterEmail.getText().toString().trim();
        String password = etRegisterPassword.getText().toString().trim();
        String confirmPassword = etRegisterConfirmPassword.getText().toString().trim();

        // Validaciones
        if (TextUtils.isEmpty(username)) {
            etRegisterUsername.setError("Ingresa un nombre de usuario");
            etRegisterUsername.requestFocus();
            return;
        }

        if (username.length() < 3) {
            etRegisterUsername.setError("El nombre debe tener al menos 3 caracteres");
            etRegisterUsername.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etRegisterEmail.setError("Ingresa tu correo");
            etRegisterEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etRegisterEmail.setError("Correo invalido");
            etRegisterEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etRegisterPassword.setError("Ingresa una contrasena");
            etRegisterPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etRegisterPassword.setError("La contrasena debe tener al menos 6 caracteres");
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

        if (!cbTermsAndConditions.isChecked()) {
            Toast.makeText(this, "Debes aceptar los terminos y condiciones", Toast.LENGTH_SHORT).show();
            return;
        }

        // Guardar preferencia de participación
        boolean participateInStudy = cbParticipateStudy.isChecked();

        // Aquí iría el registro en el servidor
        // Por ahora, simulamos un registro exitoso
        registerUser(email, username, participateInStudy);
    }

    private void performGoogleSignUp() {
        // Aquí iría la implementación de Google Sign-Up
        Toast.makeText(this, "Google Sign-Up en desarrollo", Toast.LENGTH_SHORT).show();

        // Simulación de registro exitoso con Google
        // registerUser("usuario@gmail.com", "Usuario Google", true);
    }

    private void registerUser(String email, String username, boolean participate) {
        // Guardar datos del usuario
        preferencesManager.setUserLoggedIn(true);
        preferencesManager.setUserEmail(email);
        preferencesManager.setUserName(username);
        preferencesManager.setParticipateEnabled(participate);

        Toast.makeText(this, "Cuenta creada exitosamente", Toast.LENGTH_SHORT).show();

        // Ir a MainActivity
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
