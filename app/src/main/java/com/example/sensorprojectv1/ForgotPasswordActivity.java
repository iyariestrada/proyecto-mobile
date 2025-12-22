package com.example.sensorprojectv1;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText etForgotEmail, etVerificationCode, etNewPassword, etConfirmPassword;
    private Button btnSendCode, btnVerifyCode, btnResetPassword;
    private LinearLayout layoutStep1, layoutStep2, layoutStep3;
    private TextView tvStatusMessage, tvBackToLogin;
    private String email;
    private String code;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Recuperar Contraseña");
        }

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        // Step 1
        layoutStep1 = findViewById(R.id.layoutStep1);
        etForgotEmail = findViewById(R.id.etForgotEmail);
        btnSendCode = findViewById(R.id.btnSendCode);

        // Step 2
        layoutStep2 = findViewById(R.id.layoutStep2);
        etVerificationCode = findViewById(R.id.etVerificationCode);
        btnVerifyCode = findViewById(R.id.btnVerifyCode);

        // Step 3
        layoutStep3 = findViewById(R.id.layoutStep3);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnResetPassword = findViewById(R.id.btnResetPassword);

        // Status and navigation
        tvStatusMessage = findViewById(R.id.tvStatusMessage);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);
    }

    private void setupListeners() {
        btnSendCode.setOnClickListener(v -> sendCode());
        btnVerifyCode.setOnClickListener(v -> verifyCode());
        btnResetPassword.setOnClickListener(v -> resetPassword());
        tvBackToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void sendCode() {
        email = etForgotEmail.getText().toString().trim();

        // Validaciones
        if (TextUtils.isEmpty(email)) {
            etForgotEmail.setError("Ingresa tu correo");
            etForgotEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etForgotEmail.setError("Correo inválido");
            etForgotEmail.requestFocus();
            return;
        }

        btnSendCode.setEnabled(false);
        btnSendCode.setText("Enviando...");

        ApiService.forgotPassword(email, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    try {
                        boolean success = response.getBoolean("success");

                        if (success) {
                            showStatusMessage("Código de verificación enviado a tu correo", true);
                            goToStep(2);
                        } else {
                            String errorMsg = response.optString("message", "Error al enviar el código");
                            showStatusMessage(errorMsg, false);
                        }

                    } catch (Exception e) {
                        showStatusMessage("Error al procesar respuesta", false);
                    }

                    btnSendCode.setEnabled(true);
                    btnSendCode.setText("Enviar Código");
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showStatusMessage(error, false);
                    btnSendCode.setEnabled(true);
                    btnSendCode.setText("Enviar Código");
                });
            }
        });
    }

    private void verifyCode() {
        code = etVerificationCode.getText().toString().trim();

        // Validaciones
        if (TextUtils.isEmpty(code)) {
            etVerificationCode.setError("Ingresa el código");
            etVerificationCode.requestFocus();
            return;
        }

        if (code.length() != 6) {
            etVerificationCode.setError("El código debe tener 6 dígitos");
            etVerificationCode.requestFocus();
            return;
        }

        btnVerifyCode.setEnabled(false);
        btnVerifyCode.setText("Verificando...");

        ApiService.verifyCode(email, code, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    try {
                        boolean success = response.getBoolean("success");

                        if (success) {
                            showStatusMessage("Código verificado correctamente", true);
                            goToStep(3);
                        } else {
                            String errorMsg = response.optString("message", "Código inválido");
                            showStatusMessage(errorMsg, false);
                        }

                    } catch (Exception e) {
                        showStatusMessage("Error al procesar respuesta", false);
                    }

                    btnVerifyCode.setEnabled(true);
                    btnVerifyCode.setText("Verificar Código");
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showStatusMessage(error, false);
                    btnVerifyCode.setEnabled(true);
                    btnVerifyCode.setText("Verificar Código");
                });
            }
        });
    }

    private void resetPassword() {
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validaciones
        if (TextUtils.isEmpty(newPassword)) {
            etNewPassword.setError("Ingresa la nueva contraseña");
            etNewPassword.requestFocus();
            return;
        }

        if (!isStrongPassword(newPassword)) {
            etNewPassword.setError(
                    "La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial");
            etNewPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            etConfirmPassword.setError("Confirma la nueva contraseña");
            etConfirmPassword.requestFocus();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            etConfirmPassword.setError("Las contraseñas no coinciden");
            etConfirmPassword.requestFocus();
            return;
        }

        btnResetPassword.setEnabled(false);
        btnResetPassword.setText("Actualizando...");

        ApiService.resetPassword(email, code, newPassword, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    try {
                        boolean success = response.getBoolean("success");

                        if (success) {
                            Toast.makeText(ForgotPasswordActivity.this,
                                    "Contraseña actualizada correctamente",
                                    Toast.LENGTH_LONG).show();

                            // Redirigir al login
                            Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            finish();
                        } else {
                            String errorMsg = response.optString("message", "Error al actualizar la contraseña");
                            showStatusMessage(errorMsg, false);
                        }

                    } catch (Exception e) {
                        showStatusMessage("Error al procesar respuesta", false);
                    }

                    btnResetPassword.setEnabled(true);
                    btnResetPassword.setText("Actualizar Contraseña");
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showStatusMessage(error, false);
                    btnResetPassword.setEnabled(true);
                    btnResetPassword.setText("Actualizar Contraseña");
                });
            }
        });
    }

    private void goToStep(int step) {
        layoutStep1.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        layoutStep2.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        layoutStep3.setVisibility(step == 3 ? View.VISIBLE : View.GONE);
    }

    private void showStatusMessage(String message, boolean isSuccess) {
        tvStatusMessage.setText(message);
        tvStatusMessage.setTextColor(getResources().getColor(
                isSuccess ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
        tvStatusMessage.setVisibility(View.VISIBLE);

        // Ocultar el mensaje después de 5 segundos
        tvStatusMessage.postDelayed(() -> tvStatusMessage.setVisibility(View.GONE), 5000);
    }

    private boolean isStrongPassword(String password) {
        if (password.length() < 8)
            return false;

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c))
                hasUpper = true;
            else if (Character.isLowerCase(c))
                hasLower = true;
            else if (Character.isDigit(c))
                hasDigit = true;
            else if ("!@#$%^&*(),.?\":{}|<>".indexOf(c) >= 0)
                hasSpecial = true;
        }

        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
