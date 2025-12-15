package com.example.sensorprojectv1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class StatisticsFragment extends Fragment {

    private TextView tvTotalSteps, tvTotalAlerts;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statistics, container, false);

        tvTotalSteps = view.findViewById(R.id.tvTotalSteps);
        tvTotalAlerts = view.findViewById(R.id.tvTotalAlerts);

        loadStatistics();

        return view;
    }

    private void loadStatistics() {
        // Aquí se cargarían las estadísticas de la base de datos
        // Por ahora mostramos valores de ejemplo
        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();
            tvTotalSteps.setText(String.valueOf(activity.getStepCount()));
        }
    }

    public void updateStatistics(int steps, int alerts) {
        if (tvTotalSteps != null) {
            tvTotalSteps.setText(String.valueOf(steps));
        }
        if (tvTotalAlerts != null) {
            tvTotalAlerts.setText(String.valueOf(alerts));
        }
    }
}
