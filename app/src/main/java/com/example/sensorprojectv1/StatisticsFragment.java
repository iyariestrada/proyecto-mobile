package com.example.sensorprojectv1;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

public class StatisticsFragment extends Fragment {

    private TextView tvTotalSteps, tvTotalAlerts, tvSessionTime;
    private TextView tvAlertasLow, tvAlertasMedium, tvAlertasHigh;
    private ProgressBar progressBar;
    private TextView tvNoData;
    private PreferencesManager preferencesManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statistics, container, false);

        preferencesManager = new PreferencesManager(requireContext());

        initializeViews(view);
        loadStatistics();

        return view;
    }

    private void initializeViews(View view) {
        tvTotalSteps = view.findViewById(R.id.tvTotalSteps);
        tvTotalAlerts = view.findViewById(R.id.tvTotalAlerts);
        tvSessionTime = view.findViewById(R.id.tvSessionTime);
        tvAlertasLow = view.findViewById(R.id.tvAlertasLow);
        tvAlertasMedium = view.findViewById(R.id.tvAlertasMedium);
        tvAlertasHigh = view.findViewById(R.id.tvAlertasHigh);
        progressBar = view.findViewById(R.id.progressBar);
        tvNoData = view.findViewById(R.id.tvNoData);
    }

    private void loadStatistics() {
        // Obtener datos de pasos desde MainActivity
        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();
            tvTotalSteps.setText(String.valueOf(activity.getStepCount()));
        }

        // Calcular tiempo de sesión
        long sessionStart = preferencesManager.getSessionStart();
        if (sessionStart > 0) {
            long sessionDuration = System.currentTimeMillis() - sessionStart;
            long minutes = TimeUnit.MILLISECONDS.toMinutes(sessionDuration);
            tvSessionTime.setText(minutes + " min");
        } else {
            tvSessionTime.setText("0 min");
        }

        // Obtener alertas de la sesión desde el servidor
        long sessionId = preferencesManager.getSessionId();

        if (sessionId == -1) {
            showNoData();
            return;
        }

        showLoading();
        String token = preferencesManager.getUserToken();

        ApiService.getAlertasBySession(sessionId, token, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    try {
                        hideLoading();

                        Log.d("STATISTICS", "Respuesta del servidor: " + response.toString());

                        if (response.getBoolean("success")) {
                            // El backend retorna "data" en lugar de "alertas"
                            JSONArray alertas = response.getJSONArray("data");
                            int count = response.getInt("count");

                            Log.i("STATISTICS", "Alertas recibidas: " + count);

                            processAlerts(alertas);
                        } else {
                            Log.w("STATISTICS", "Respuesta sin éxito del servidor");
                            showNoData();
                        }

                    } catch (Exception e) {
                        Log.e("STATISTICS", "Error al procesar respuesta: " + e.toString());
                        e.printStackTrace();
                        showNoData();
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    Log.e("STATISTICS", "Error al obtener alertas: " + error);
                    hideLoading();
                    showNoData();
                });
            }
        });
    }

    private void processAlerts(JSONArray alertas) {
        try {
            int totalAlerts = alertas.length();
            int lowSeverity = 0;
            int mediumSeverity = 0;
            int highSeverity = 0;

            Log.d("STATISTICS", "Procesando " + totalAlerts + " alertas...");

            // Contar alertas por severidad
            for (int i = 0; i < alertas.length(); i++) {
                JSONObject alerta = alertas.getJSONObject(i);
                String severidad = alerta.getString("severidad");

                Log.d("STATISTICS", "Alerta #" + (i + 1) + " - Severidad: " + severidad);

                switch (severidad.toLowerCase()) {
                    case "baja":
                        lowSeverity++;
                        break;
                    case "media":
                        mediumSeverity++;
                        break;
                    case "alta":
                        highSeverity++;
                        break;
                    default:
                        Log.w("STATISTICS", "Severidad desconocida: " + severidad);
                        break;
                }
            }

            // Actualizar UI
            tvTotalAlerts.setText(String.valueOf(totalAlerts));
            tvAlertasLow.setText(String.valueOf(lowSeverity));
            tvAlertasMedium.setText(String.valueOf(mediumSeverity));
            tvAlertasHigh.setText(String.valueOf(highSeverity));

            Log.i("STATISTICS", String.format(
                "Estadísticas actualizadas - Total: %d, Baja: %d, Media: %d, Alta: %d",
                totalAlerts, lowSeverity, mediumSeverity, highSeverity
            ));

            // Si hay alertas, ocultar mensaje de "no hay datos"
            if (totalAlerts > 0) {
                tvNoData.setVisibility(View.GONE);
            }

        } catch (Exception e) {
            Log.e("STATISTICS", "Error al procesar alertas: " + e.toString());
            e.printStackTrace();
        }
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        tvNoData.setVisibility(View.GONE);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
    }

    private void showNoData() {
        progressBar.setVisibility(View.GONE);
        tvNoData.setVisibility(View.VISIBLE);

        // Resetear valores a 0
        tvTotalAlerts.setText("0");
        tvAlertasLow.setText("0");
        tvAlertasMedium.setText("0");
        tvAlertasHigh.setText("0");
    }

    @Override
    public void onResume() {
        super.onResume();
        // Recargar estadísticas al volver al fragment
        loadStatistics();
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
