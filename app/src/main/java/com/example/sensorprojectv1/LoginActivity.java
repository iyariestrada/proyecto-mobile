package com.example.sensorprojectv1;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etLoginEmail, etLoginPassword;
    private Button btnLogin, btnGoogleSignIn;
    private TextView tvGoToRegister, tvForgotPassword, tvContinueAnonymous;
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
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
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

        // Google Sign-In
        btnGoogleSignIn.setOnClickListener(v -> {
            performGoogleSignIn();
        });

        // Olvidé contraseña
        tvForgotPassword.setOnClickListener(v -> {
            Toast.makeText(this, "Funcionalidad de recuperacion en desarrollo", Toast.LENGTH_SHORT).show();
        });

        // Continuar sin cuenta
        tvContinueAnonymous.setOnClickListener(v -> {
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

        // Aquí iría la autenticación con el servidor
        // Por ahora, simulamos un login exitoso
        loginUser(email, "Usuario", "email");
    }

    private void performGoogleSignIn() {
        // Aquí iría la implementación de Google Sign-In
        // Por ahora mostramos un mensaje
        Toast.makeText(this, "Google Sign-In en desarrollo", Toast.LENGTH_SHORT).show();

        // Simulación de login exitoso con Google
        // loginUser("usuario@gmail.com", "Usuario Google", "google");
    }

    private void loginUser(String email, String username, String loginMethod) {
        // Guardar datos del usuario
        preferencesManager.setUserLoggedIn(true);
        preferencesManager.setUserEmail(email);
        preferencesManager.setUserName(username);

        Toast.makeText(this, "Bienvenido " + username, Toast.LENGTH_SHORT).show();

        // Ir a MainActivity
        goToMainActivity();
    }

    private void goToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
