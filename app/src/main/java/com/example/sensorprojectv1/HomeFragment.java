package com.example.sensorprojectv1;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {

    private TextView tvWelcome, tvUserStatus, textViewStatus, textViewGyro, textViewAcc;
    private LinearLayout layoutLoginButtons;
    private Button btnLogin, btnRegister;
    private PreferencesManager preferencesManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        preferencesManager = new PreferencesManager(requireContext());

        // Inicializar vistas
        tvWelcome = view.findViewById(R.id.tvWelcome);
        tvUserStatus = view.findViewById(R.id.tvUserStatus);
        layoutLoginButtons = view.findViewById(R.id.layoutLoginButtons);
        btnLogin = view.findViewById(R.id.btnLogin);
        btnRegister = view.findViewById(R.id.btnRegister);
        textViewStatus = view.findViewById(R.id.textViewStatus);
        textViewGyro = view.findViewById(R.id.textViewGyro);
        textViewAcc = view.findViewById(R.id.textViewAcc);

        // Configurar según estado de usuario
        updateUserUI();

        // Botones de login/registro
        btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            startActivity(intent);
        });

        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), RegisterActivity.class);
            startActivity(intent);
        });

        return view;
    }

    private void updateUserUI() {
        if (preferencesManager.isUserLoggedIn()) {
            String userName = preferencesManager.getUserName();
            tvWelcome.setText("Bienvenido, " + userName);
            tvUserStatus.setText("Estas participando en el estudio");
            layoutLoginButtons.setVisibility(View.GONE);
        } else {
            tvWelcome.setText(R.string.welcome_message);
            tvUserStatus.setText(R.string.login_prompt);
            layoutLoginButtons.setVisibility(View.VISIBLE);
        }
    }

    public void updateSensorStatus(String status) {
        if (textViewStatus != null) {
            textViewStatus.setText(status);
        }
    }

    public void updateGyroData(String data) {
        if (textViewGyro != null) {
            textViewGyro.setText(data);
        }
    }

    public void updateAccData(String data) {
        if (textViewAcc != null) {
            textViewAcc.setText(data);
        }
    }
}
