package com.example.sensorprojectv1;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {

    private TextView tvProfileName, tvProfileEmail, tvParticipationStatus;
    private Button btnLogout, btnProfileLogin, btnEditProfile;
    private CardView cardUserInfo, cardNotLoggedIn;
    private PreferencesManager preferencesManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        preferencesManager = new PreferencesManager(requireContext());

        // Inicializar vistas
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        tvParticipationStatus = view.findViewById(R.id.tvParticipationStatus);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnProfileLogin = view.findViewById(R.id.btnProfileLogin);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        cardUserInfo = view.findViewById(R.id.cardUserInfo);
        cardNotLoggedIn = view.findViewById(R.id.cardNotLoggedIn);

        // Cargar información del usuario
        loadUserInfo();

        // Listener del botón logout
        btnLogout.setOnClickListener(v -> performLogout());

        // Listener del botón login desde perfil
        btnProfileLogin.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            startActivity(intent);
            requireActivity().finish();
        });

        // Listener del botón editar perfil
        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), EditProfileActivity.class);
            startActivity(intent);
        });

        return view;
    }

    private void loadUserInfo() {
        if (preferencesManager.isUserLoggedIn()) {
            // Usuario logueado - Mostrar información
            cardUserInfo.setVisibility(View.VISIBLE);
            cardNotLoggedIn.setVisibility(View.GONE);
            btnEditProfile.setVisibility(View.VISIBLE);
            btnLogout.setVisibility(View.VISIBLE);

            String userName = preferencesManager.getUserName();
            String userEmail = preferencesManager.getUserEmail();
            boolean isParticipating = preferencesManager.isParticipateEnabled();

            tvProfileName.setText(userName);
            tvProfileEmail.setText(userEmail);

            if (isParticipating) {
                tvParticipationStatus.setText("Participando en el estudio");
                tvParticipationStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            } else {
                tvParticipationStatus.setText("Modo anonimo");
                tvParticipationStatus.setTextColor(getResources().getColor(android.R.color.darker_gray));
            }

        } else {
            // Usuario no logueado
            cardUserInfo.setVisibility(View.GONE);
            cardNotLoggedIn.setVisibility(View.VISIBLE);
            btnEditProfile.setVisibility(View.GONE);
            btnLogout.setVisibility(View.GONE);
        }
    }

    private void performLogout() {
        // Finalizar sesión en el servidor antes de limpiar datos locales
        long sessionId = preferencesManager.getSessionId();
        String token = preferencesManager.getUserToken();

        if (sessionId != -1) {
            // Finalizar sesión en el servidor
            ApiService.endSession(sessionId, token, new ApiService.ApiCallback() {
                @Override
                public void onSuccess(org.json.JSONObject response) {
                    // Sesión finalizada exitosamente en el servidor
                    android.util.Log.i("LOGOUT", "Sesión finalizada en el servidor");
                    completeLogout();
                }

                @Override
                public void onError(String error) {
                    // Aunque falle, continuar con el logout local
                    android.util.Log.e("LOGOUT", "Error al finalizar sesión: " + error);
                    completeLogout();
                }
            });
        } else {
            // No hay sesión activa, proceder con logout local
            completeLogout();
        }
    }

    private void completeLogout() {
        // Limpiar datos del usuario localmente
        preferencesManager.logout();

        Toast.makeText(requireContext(), "Sesion cerrada", Toast.LENGTH_SHORT).show();

        // Ir a LoginActivity
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Actualizar información al volver al fragment
        loadUserInfo();
    }
}
