package com.example.lab6_20206331.models;

public class Egreso {
    private String id;
    private String titulo;
    private double monto;
    private String descripcion;
    private String fecha;
    private String userId;
    private long timestamp;

    // NUEVOS CAMPOS PARA COMPROBANTE
    private String comprobanteUrl; // URL del comprobante en Cloudinary
    private String comprobantePublicId; // Public ID en Cloudinary
    private String comprobanteNombre; // Nombre original del archivo

    // Constructor vacío requerido para Firestore
    public Egreso() {}

    public Egreso(String titulo, double monto, String descripcion, String fecha, String userId) {
        this.titulo = titulo;
        this.monto = monto;
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.userId = userId;
        this.timestamp = System.currentTimeMillis();
    }

    public Egreso(String titulo, double monto, String descripcion, String fecha) {
        this.titulo = titulo;
        this.monto = monto;
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.timestamp = System.currentTimeMillis();
    }

    // Constructor con comprobante
    public Egreso(String titulo, double monto, String descripcion, String fecha,
                  String comprobanteUrl, String comprobantePublicId, String comprobanteNombre) {
        this.titulo = titulo;
        this.monto = monto;
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.comprobanteUrl = comprobanteUrl;
        this.comprobantePublicId = comprobantePublicId;
        this.comprobanteNombre = comprobanteNombre;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters existentes
    public String getId() { return id; }
    public String getTitulo() { return titulo; }
    public double getMonto() { return monto; }
    public String getDescripcion() { return descripcion; }
    public String getFecha() { return fecha; }
    public String getUserId() { return userId; }
    public long getTimestamp() { return timestamp; }

    // Nuevos getters para comprobante
    public String getComprobanteUrl() { return comprobanteUrl; }
    public String getComprobantePublicId() { return comprobantePublicId; }
    public String getComprobanteNombre() { return comprobanteNombre; }

    // Setters existentes
    public void setId(String id) { this.id = id; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public void setMonto(double monto) { this.monto = monto; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    // Nuevos setters para comprobante
    public void setComprobanteUrl(String comprobanteUrl) { this.comprobanteUrl = comprobanteUrl; }
    public void setComprobantePublicId(String comprobantePublicId) { this.comprobantePublicId = comprobantePublicId; }
    public void setComprobanteNombre(String comprobanteNombre) { this.comprobanteNombre = comprobanteNombre; }

    // Nuevos métodos de utilidad para comprobante
    public boolean hasComprobante() {
        return comprobanteUrl != null && !comprobanteUrl.trim().isEmpty();
    }

    public boolean hasComprobanteFile() {
        return hasComprobante() && comprobantePublicId != null && !comprobantePublicId.trim().isEmpty();
    }

    public String getComprobanteFileName() {
        if (comprobanteNombre != null && !comprobanteNombre.trim().isEmpty()) {
            return comprobanteNombre;
        }
        if (comprobantePublicId != null) {
            return comprobantePublicId + ".jpg"; // Default extension
        }
        return "comprobante_" + id + ".jpg";
    }

    @Override
    public String toString() {
        return "Egreso{" +
                "id='" + id + '\'' +
                ", titulo='" + titulo + '\'' +
                ", monto=" + monto +
                ", descripcion='" + descripcion + '\'' +
                ", fecha='" + fecha + '\'' +
                ", userId='" + userId + '\'' +
                ", timestamp=" + timestamp +
                ", comprobanteUrl='" + comprobanteUrl + '\'' +
                ", comprobantePublicId='" + comprobantePublicId + '\'' +
                ", comprobanteNombre='" + comprobanteNombre + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Egreso egreso = (Egreso) obj;
        return id != null ? id.equals(egreso.id) : egreso.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}