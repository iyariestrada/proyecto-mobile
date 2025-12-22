package com.example.sensorprojectv1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {

    private SwitchCompat switchSoundAlert, switchVibrationAlert, switchParticipate;
    private TextView tvParticipateDescription;
    private PreferencesManager preferencesManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        preferencesManager = new PreferencesManager(requireContext());

        // Inicializar vistas
        switchSoundAlert = view.findViewById(R.id.switchSoundAlert);
        switchVibrationAlert = view.findViewById(R.id.switchVibrationAlert);
        switchParticipate = view.findViewById(R.id.switchParticipate);
        tvParticipateDescription = view.findViewById(R.id.tvParticipateDescription);

        loadSettings();

        // Listeners
        switchSoundAlert.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesManager.setSoundAlertEnabled(isChecked);
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).updateAlertSettings();
            }
        });

        switchVibrationAlert.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesManager.setVibrationAlertEnabled(isChecked);
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).updateAlertSettings();
            }
        });

        switchParticipate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            boolean wasParticipating = preferencesManager.isParticipateEnabled();
            preferencesManager.setParticipateEnabled(isChecked);
            updateParticipateDescription(isChecked);

            // Si el usuario estaba participando y ahora deja de participar,
            // finalizar la sesión actual e iniciar una nueva sesión anónima
            if (wasParticipating && !isChecked && getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToAnonymousSession();
            }
            // Si el usuario no estaba participando y ahora decide participar,
            // finalizar la sesión anónima e iniciar una sesión con su cuenta
            else if (!wasParticipating && isChecked && getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToParticipatingSession();
            }
        });

        return view;
    }

    private void loadSettings() {
        switchSoundAlert.setChecked(preferencesManager.isSoundAlertEnabled());
        switchVibrationAlert.setChecked(preferencesManager.isVibrationAlertEnabled());
        switchParticipate.setChecked(preferencesManager.isParticipateEnabled());
        updateParticipateDescription(preferencesManager.isParticipateEnabled());
    }

    private void updateParticipateDescription(boolean isParticipating) {
        if (isParticipating) {
            tvParticipateDescription.setText(R.string.settings_participate_desc);
        } else {
            tvParticipateDescription.setText(R.string.settings_anonymous_desc);
        }
    }
}
