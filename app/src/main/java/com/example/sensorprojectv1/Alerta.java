package com.example.sensorprojectv1;

import org.json.JSONObject;

public class Alerta {
    private long idAlerta;
    private long idSesion;
    private long idUsuario;
    private String tipoAlerta;
    private String severidad;
    private String descripcion;
    private JSONObject contexto;
    private String detectedAt;
    private String createdAt;

    public Alerta(long idAlerta, long idSesion, long idUsuario, String tipoAlerta,
                  String severidad, String descripcion, JSONObject contexto,
                  String detectedAt, String createdAt) {
        this.idAlerta = idAlerta;
        this.idSesion = idSesion;
        this.idUsuario = idUsuario;
        this.tipoAlerta = tipoAlerta;
        this.severidad = severidad;
        this.descripcion = descripcion;
        this.contexto = contexto;
        this.detectedAt = detectedAt;
        this.createdAt = createdAt;
    }

    // Getters
    public long getIdAlerta() { return idAlerta; }
    public long getIdSesion() { return idSesion; }
    public long getIdUsuario() { return idUsuario; }
    public String getTipoAlerta() { return tipoAlerta; }
    public String getSeveridad() { return severidad; }
    public String getDescripcion() { return descripcion; }
    public JSONObject getContexto() { return contexto; }
    public String getDetectedAt() { return detectedAt; }
    public String getCreatedAt() { return createdAt; }

    // Helper para obtener color según severidad
    public int getSeveridadColor() {
        switch (severidad.toLowerCase()) {
            case "alta":
                return android.graphics.Color.parseColor("#f44336"); // Rojo
            case "media":
                return android.graphics.Color.parseColor("#ff9800"); // Naranja
            case "baja":
                return android.graphics.Color.parseColor("#4caf50"); // Verde
            default:
                return android.graphics.Color.parseColor("#9e9e9e"); // Gris
        }
    }

    // Helper para obtener color de fondo según severidad
    public int getSeveridadBackgroundColor() {
        switch (severidad.toLowerCase()) {
            case "alta":
                return android.graphics.Color.parseColor("#ffebee"); // Rojo claro
            case "media":
                return android.graphics.Color.parseColor("#fff3e0"); // Naranja claro
            case "baja":
                return android.graphics.Color.parseColor("#e8f5e9"); // Verde claro
            default:
                return android.graphics.Color.parseColor("#f5f5f5"); // Gris claro
        }
    }

    // Helper para obtener texto legible del tipo de alerta
    public String getTipoAlertaTexto() {
        switch (tipoAlerta) {
            case "walking_using_phone":
                return "Caminando y usando teléfono";
            case "distraction_detected":
                return "Distracción detectada";
            case "prolonged_usage":
                return "Uso prolongado";
            case "nighttime_usage":
                return "Uso nocturno";
            default:
                return "Otra alerta";
        }
    }
}
