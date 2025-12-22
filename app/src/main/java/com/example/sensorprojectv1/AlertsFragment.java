package com.example.sensorprojectv1;

import android.os.Bundle;
import android.util.Log;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AlertsFragment extends Fragment {

    private RecyclerView recyclerViewAlerts;
    private LinearLayout layoutContent, layoutEmpty, layoutLoading, layoutError;
    private TextView tvAlertCount, tvErrorMessage;
    private Button btnRetry;

    private AlertsAdapter adapter;
    private List<Alerta> alertas;
    private PreferencesManager preferencesManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alerts, container, false);

        preferencesManager = new PreferencesManager(requireContext());

        recyclerViewAlerts = view.findViewById(R.id.recyclerViewAlerts);
        layoutContent = view.findViewById(R.id.layoutContent);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        layoutLoading = view.findViewById(R.id.layoutLoading);
        layoutError = view.findViewById(R.id.layoutError);
        tvAlertCount = view.findViewById(R.id.tvAlertCount);
        tvErrorMessage = view.findViewById(R.id.tvErrorMessage);
        btnRetry = view.findViewById(R.id.btnRetry);

        alertas = new ArrayList<>();
        adapter = new AlertsAdapter(alertas, this::onAlertClick);
        recyclerViewAlerts.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewAlerts.setAdapter(adapter);

        btnRetry.setOnClickListener(v -> loadAlertas());

        loadAlertas();

        return view;
    }

    private void loadAlertas() {
        showLoadingState();

        // Determinar si el usuario está en modo anónimo o participante
        boolean isLoggedIn = preferencesManager.isUserLoggedIn();
        boolean isParticipating = preferencesManager.isParticipateEnabled();

        // Si NO está logueado o NO está participando, buscar por UUID de dispositivo
        if (!isLoggedIn || !isParticipating) {
            String deviceUUID = preferencesManager.getDeviceUUID();

            if (deviceUUID == null || deviceUUID.isEmpty()) {
                showEmptyState("No se pudo identificar el dispositivo");
                return;
            }

            // Buscar alertas por UUID de dispositivo (modo anónimo)
            ApiService.getAlertasByDeviceUUID(deviceUUID, new ApiService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    processAlertasResponse(response, true);
                }

                @Override
                public void onError(String error) {
                    handleAlertasError(error);
                }
            });
        } else {
            // Usuario registrado y participante: buscar por userId
            long userId = preferencesManager.getUserId();
            String token = preferencesManager.getUserToken();

            if (userId == -1) {
                showEmptyState("Usuario no identificado");
                return;
            }

            ApiService.getAlertasByUsuario(userId, token, new ApiService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    processAlertasResponse(response, false);
                }

                @Override
                public void onError(String error) {
                    handleAlertasError(error);
                }
            });
        }
    }

    /**
     * Procesa la respuesta de alertas del servidor
     * @param response Respuesta JSON del servidor
     * @param isAnonymous Si las alertas son del modo anónimo
     */
    private void processAlertasResponse(JSONObject response, boolean isAnonymous) {
        if (getActivity() == null)
            return;

        getActivity().runOnUiThread(() -> {
            try {
                if (response.getBoolean("success")) {
                    JSONArray dataArray = response.getJSONArray("data");
                    alertas.clear();

                    for (int i = 0; i < dataArray.length(); i++) {
                        JSONObject alertaJson = dataArray.getJSONObject(i);

                        long idAlerta = alertaJson.getLong("id_alerta");
                        long idSesion = alertaJson.getLong("id_sesion");
                        long idUsuario = alertaJson.getLong("id_usuario");
                        String tipoAlerta = alertaJson.getString("tipo_alerta");
                        String severidad = alertaJson.getString("severidad");
                        String descripcion = alertaJson.optString("descripcion", "");
                        JSONObject contexto = alertaJson.optJSONObject("contexto");
                        String detectedAt = alertaJson.getString("detected_at");
                        String createdAt = alertaJson.optString("created_at", "");

                        Alerta alerta = new Alerta(idAlerta, idSesion, idUsuario,
                                tipoAlerta, severidad, descripcion, contexto,
                                detectedAt, createdAt);

                        alertas.add(alerta);
                    }

                    adapter.updateAlertas(alertas);

                    if (alertas.isEmpty()) {
                        if (isAnonymous) {
                            showEmptyState("No hay alertas registradas en modo anónimo.\n\nNota: Las alertas en modo anónimo no aparecen en tu historial web completo.");
                        } else {
                            showEmptyState("No hay alertas registradas");
                        }
                    } else {
                        showContentState();
                        // Mostrar notificación sobre modo anónimo si aplica
                        if (isAnonymous) {
                            showAnonymousModeNotice();
                        }
                    }

                    Log.d("ALERTS_FRAGMENT", "Alertas cargadas: " + alertas.size() + " (Anónimo: " + isAnonymous + ")");
                } else {
                    showErrorState("Error al cargar alertas");
                }
            } catch (Exception e) {
                Log.e("ALERTS_FRAGMENT", "Error al procesar alertas: " + e.toString());
                showErrorState("Error al procesar datos");
            }
        });
    }

    /**
     * Maneja errores al cargar alertas
     */
    private void handleAlertasError(String error) {
        if (getActivity() == null)
            return;

        getActivity().runOnUiThread(() -> {
            Log.e("ALERTS_FRAGMENT", "Error al cargar alertas: " + error);
            showErrorState(error);
        });
    }

    /**
     * Muestra un aviso sobre el modo anónimo
     */
    private void showAnonymousModeNotice() {
        Toast.makeText(requireContext(),
            "Mostrando alertas del dispositivo (modo anónimo). Estas no aparecen en tu historial web completo.",
            Toast.LENGTH_LONG).show();
    }

    private void onAlertClick(Alerta alerta) {
        StringBuilder details = new StringBuilder();
        details.append("Alerta #").append(alerta.getIdAlerta()).append("\n\n");
        details.append("Tipo: ").append(alerta.getTipoAlertaTexto()).append("\n");
        details.append("Severidad: ").append(alerta.getSeveridad()).append("\n");
        details.append("Sesión: #").append(alerta.getIdSesion()).append("\n\n");

        if (alerta.getContexto() != null) {
            try {
                JSONObject ctx = alerta.getContexto();
                details.append("--- Detalles ---\n");

                if (ctx.has("walking_speed")) {
                    details.append("Velocidad: ").append(ctx.getString("walking_speed")).append("\n");
                }
                if (ctx.has("variance")) {
                    details.append("Varianza: ").append(String.format("%.3f", ctx.getDouble("variance"))).append("\n");
                }
                if (ctx.has("step_count")) {
                    details.append("Pasos: ").append(ctx.getInt("step_count")).append("\n");
                }
                if (ctx.has("battery_level")) {
                    details.append("Batería: ").append(ctx.getInt("battery_level")).append("%\n");
                }
                if (ctx.has("screen_brightness")) {
                    details.append("Brillo: ").append(ctx.getInt("screen_brightness")).append("\n");
                }
            } catch (Exception e) {
                Log.e("ALERTS_FRAGMENT", "Error al leer contexto: " + e.toString());
            }
        }

        details.append("\n").append(alerta.getDescripcion());

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Detalles de la Alerta")
                .setMessage(details.toString())
                .setPositiveButton("Cerrar", null)
                .show();
    }

    private void showLoadingState() {
        layoutLoading.setVisibility(View.VISIBLE);
        layoutContent.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
    }

    private void showContentState() {
        layoutLoading.setVisibility(View.GONE);
        layoutContent.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);

        recyclerViewAlerts.setVisibility(View.VISIBLE);
        tvAlertCount.setVisibility(View.VISIBLE);
        tvAlertCount.setText(String.valueOf(alertas.size()));
    }

    private void showEmptyState(String message) {
        layoutLoading.setVisibility(View.GONE);
        layoutContent.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
        layoutError.setVisibility(View.GONE);

        TextView tvEmptyMessage = layoutEmpty.findViewById(R.id.tvEmptyMessage);
        if (tvEmptyMessage != null) {
            tvEmptyMessage.setText(message);
        }
    }

    private void showErrorState(String error) {
        layoutLoading.setVisibility(View.GONE);
        layoutContent.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
        layoutError.setVisibility(View.VISIBLE);

        tvErrorMessage.setText(error);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Recargar alertas cuando el fragment vuelve a ser visible
        // Funciona tanto en modo anónimo como en modo participante
        loadAlertas();
    }
}
