package com.example.sensorprojectv1;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

public class EditProfileActivity extends AppCompatActivity {

    private TextInputEditText etEditName, etEditEmail;
    private TextInputEditText etCurrentPassword, etNewPassword, etConfirmNewPassword;
    private Button btnUpdateAccount, btnChangePassword;
    private PreferencesManager preferencesManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        preferencesManager = new PreferencesManager(this);

        // Configurar ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Editar Perfil");
        }

        initializeViews();
        loadUserData();
        setupListeners();
    }

    private void initializeViews() {
        etEditName = findViewById(R.id.etEditName);
        etEditEmail = findViewById(R.id.etEditEmail);
        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmNewPassword = findViewById(R.id.etConfirmNewPassword);
        btnUpdateAccount = findViewById(R.id.btnUpdateAccount);
        btnChangePassword = findViewById(R.id.btnChangePassword);
    }

    private void loadUserData() {
        etEditName.setText(preferencesManager.getUserName());
        etEditEmail.setText(preferencesManager.getUserEmail());
    }

    private void setupListeners() {
        btnUpdateAccount.setOnClickListener(v -> updateAccount());
        btnChangePassword.setOnClickListener(v -> changePassword());
    }

    private void updateAccount() {
        String name = etEditName.getText().toString().trim();
        String email = etEditEmail.getText().toString().trim();

        // Validaciones
        if (TextUtils.isEmpty(name)) {
            etEditName.setError("El nombre es requerido");
            etEditName.requestFocus();
            return;
        }

        if (name.length() < 3) {
            etEditName.setError("El nombre debe tener al menos 3 caracteres");
            etEditName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etEditEmail.setError("El email es requerido");
            etEditEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEditEmail.setError("Email no valido");
            etEditEmail.requestFocus();
            return;
        }

        btnUpdateAccount.setEnabled(false);
        btnUpdateAccount.setText("Actualizando...");

        String correoActual = preferencesManager.getUserEmail();
        String token = preferencesManager.getUserToken();

        ApiService.updateUser(correoActual, name, email, token, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    try {
                        boolean success = response.getBoolean("success");

                        if (success) {
                            // Actualizar datos locales
                            preferencesManager.setUserName(name);
                            preferencesManager.setUserEmail(email);

                            Toast.makeText(EditProfileActivity.this,
                                    "Cuenta actualizada exitosamente",
                                    Toast.LENGTH_SHORT).show();

                            finish(); // Volver atrás
                        } else {
                            String errorMsg = response.optString("message", "Error al actualizar");
                            Toast.makeText(EditProfileActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception e) {
                        Toast.makeText(EditProfileActivity.this,
                                "Error al procesar respuesta",
                                Toast.LENGTH_SHORT).show();
                    }

                    btnUpdateAccount.setEnabled(true);
                    btnUpdateAccount.setText("Actualizar Cuenta");
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(EditProfileActivity.this, error, Toast.LENGTH_SHORT).show();
                    btnUpdateAccount.setEnabled(true);
                    btnUpdateAccount.setText("Actualizar Cuenta");
                });
            }
        });
    }

    private void changePassword() {
        String currentPassword = etCurrentPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmNewPassword.getText().toString().trim();
        String email = etEditEmail.getText().toString().trim();

        // Validaciones
        if (TextUtils.isEmpty(currentPassword)) {
            etCurrentPassword.setError("Ingresa tu contrasena actual");
            etCurrentPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(newPassword)) {
            etNewPassword.setError("Ingresa la nueva contrasena");
            etNewPassword.requestFocus();
            return;
        }

        if (!isStrongPassword(newPassword)) {
            etNewPassword.setError(
                    "La contrasena debe tener al menos 8 caracteres, una mayuscula, una minuscula, un numero y un caracter especial");
            etNewPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            etConfirmNewPassword.setError("Confirma la nueva contrasena");
            etConfirmNewPassword.requestFocus();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            etConfirmNewPassword.setError("Las contrasenas no coinciden");
            etConfirmNewPassword.requestFocus();
            return;
        }

        btnChangePassword.setEnabled(false);
        btnChangePassword.setText("Cambiando...");

        int userId = preferencesManager.getUserId();
        String token = preferencesManager.getUserToken();

        ApiService.changePassword(email, currentPassword, newPassword, confirmPassword, token,
                new ApiService.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        runOnUiThread(() -> {
                            try {
                                boolean success = response.getBoolean("success");

                                if (success) {
                                    Toast.makeText(EditProfileActivity.this,
                                            "Contrasena cambiada exitosamente",
                                            Toast.LENGTH_SHORT).show();

                                    // Limpiar campos
                                    etCurrentPassword.setText("");
                                    etNewPassword.setText("");
                                    etConfirmNewPassword.setText("");
                                } else {
                                    String errorMsg = response.optString("message", "Error al cambiar contrasena");
                                    Toast.makeText(EditProfileActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                                }

                            } catch (Exception e) {
                                Toast.makeText(EditProfileActivity.this,
                                        "Error al procesar respuesta",
                                        Toast.LENGTH_SHORT).show();
                            }

                            btnChangePassword.setEnabled(true);
                            btnChangePassword.setText("Cambiar Contrasena");
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(EditProfileActivity.this, error, Toast.LENGTH_SHORT).show();
                            btnChangePassword.setEnabled(true);
                            btnChangePassword.setText("Cambiar Contrasena");
                        });
                    }
                });
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
