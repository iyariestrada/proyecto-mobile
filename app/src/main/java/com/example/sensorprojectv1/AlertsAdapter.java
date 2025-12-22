package com.example.sensorprojectv1;

import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AlertsAdapter extends RecyclerView.Adapter<AlertsAdapter.AlertViewHolder> {

    private List<Alerta> alertas;
    private OnAlertClickListener listener;

    public interface OnAlertClickListener {
        void onAlertClick(Alerta alerta);
    }

    public AlertsAdapter(List<Alerta> alertas, OnAlertClickListener listener) {
        this.alertas = alertas;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AlertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.alert_item, parent, false);
        return new AlertViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlertViewHolder holder, int position) {
        Alerta alerta = alertas.get(position);
        holder.bind(alerta, listener);
    }

    @Override
    public int getItemCount() {
        return alertas.size();
    }

    public void updateAlertas(List<Alerta> newAlertas) {
        this.alertas = newAlertas;
        notifyDataSetChanged();
    }

    static class AlertViewHolder extends RecyclerView.ViewHolder {
        private TextView tvSeveridadBadge;
        private TextView tvSessionId;
        private TextView tvTipoAlerta;
        private TextView tvDescripcion;
        private TextView tvFecha;
        private TextView tvVelocidad;
        private TextView tvPasos;
        private LinearLayout layoutVelocidad;
        private LinearLayout layoutPasos;
        private Button btnVerDetalles;

        public AlertViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSeveridadBadge = itemView.findViewById(R.id.tvSeveridadBadge);
            tvSessionId = itemView.findViewById(R.id.tvSessionId);
            tvTipoAlerta = itemView.findViewById(R.id.tvTipoAlerta);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcion);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            tvVelocidad = itemView.findViewById(R.id.tvVelocidad);
            tvPasos = itemView.findViewById(R.id.tvPasos);
            layoutVelocidad = itemView.findViewById(R.id.layoutVelocidad);
            layoutPasos = itemView.findViewById(R.id.layoutPasos);
            btnVerDetalles = itemView.findViewById(R.id.btnVerDetalles);
        }

        public void bind(Alerta alerta, OnAlertClickListener listener) {
            // Badge de severidad
            tvSeveridadBadge.setText(alerta.getSeveridad().toUpperCase());
            GradientDrawable badgeBackground = (GradientDrawable) tvSeveridadBadge.getBackground();
            badgeBackground.setColor(alerta.getSeveridadColor());

            tvSessionId.setText("Sesión #" + alerta.getIdSesion());

            tvTipoAlerta.setText(alerta.getTipoAlertaTexto());
            tvDescripcion.setText(alerta.getDescripcion());

            tvFecha.setText(formatDate(alerta.getDetectedAt()));

            if (alerta.getContexto() != null) {
                try {
                    // Velocidad de caminata
                    if (alerta.getContexto().has("walking_speed")) {
                        String velocidad = alerta.getContexto().getString("walking_speed");
                        tvVelocidad.setText(velocidad);
                        layoutVelocidad.setVisibility(View.VISIBLE);
                    } else {
                        layoutVelocidad.setVisibility(View.GONE);
                    }

                    if (alerta.getContexto().has("step_count")) {
                        int pasos = alerta.getContexto().getInt("step_count");
                        tvPasos.setText(String.valueOf(pasos));
                        layoutPasos.setVisibility(View.VISIBLE);
                    } else {
                        layoutPasos.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                    layoutVelocidad.setVisibility(View.GONE);
                    layoutPasos.setVisibility(View.GONE);
                }
            } else {
                layoutVelocidad.setVisibility(View.GONE);
                layoutPasos.setVisibility(View.GONE);
            }

            btnVerDetalles.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAlertClick(alerta);
                }
            });
        }

        private String formatDate(String isoDate) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                        Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                Date date = inputFormat.parse(isoDate);
                return outputFormat.format(date);
            } catch (Exception e) {
                // Si falla el parsing, intentar formato sin milisegundos
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",
                            Locale.getDefault());
                    SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                    Date date = inputFormat.parse(isoDate);
                    return outputFormat.format(date);
                } catch (Exception e2) {
                    return isoDate;
                }
            }
        }
    }
}
