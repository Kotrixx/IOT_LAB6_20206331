package com.example.lab6_20206331.models;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Ingreso {
    private String id;
    private String titulo;
    private double monto;
    private String descripcion;
    private String fecha; // Formato: dd/MM/yyyy
    private long timestamp; // Para ordenamiento y queries
    private String userId; // ID del usuario propietario

    // NUEVOS CAMPOS PARA COMPROBANTE
    private String comprobanteUrl; // URL del comprobante en Cloudinary
    private String comprobantePublicId; // Public ID en Cloudinary
    private String comprobanteNombre; // Nombre original del archivo

    // Constructor vacío (requerido por Firebase)
    public Ingreso() {}

    // Constructor completo
    public Ingreso(String id, String titulo, double monto, String descripcion, String fecha) {
        this.id = id;
        this.titulo = titulo;
        this.monto = monto;
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.timestamp = System.currentTimeMillis();
    }

    // Constructor sin ID (para nuevos ingresos)
    public Ingreso(String titulo, double monto, String descripcion, String fecha) {
        this.titulo = titulo;
        this.monto = monto;
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.timestamp = System.currentTimeMillis();
    }

    // Constructor con comprobante
    public Ingreso(String titulo, double monto, String descripcion, String fecha,
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
    public long getTimestamp() { return timestamp; }
    public String getUserId() { return userId; }

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
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setUserId(String userId) { this.userId = userId; }

    // Nuevos setters para comprobante
    public void setComprobanteUrl(String comprobanteUrl) { this.comprobanteUrl = comprobanteUrl; }
    public void setComprobantePublicId(String comprobantePublicId) { this.comprobantePublicId = comprobantePublicId; }
    public void setComprobanteNombre(String comprobanteNombre) { this.comprobanteNombre = comprobanteNombre; }

    // Métodos de utilidad existentes
    public boolean hasDescription() {
        return descripcion != null && !descripcion.trim().isEmpty();
    }

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

    // Para el mes actual en formato MM/yyyy
    public String getMonthYear() {
        if (fecha != null && fecha.contains("/")) {
            String[] parts = fecha.split("/");
            if (parts.length == 3) {
                return parts[1] + "/" + parts[2]; // MM/yyyy
            }
        }
        return "";
    }

    @Override
    public String toString() {
        return "Ingreso{" +
                "id='" + id + '\'' +
                ", titulo='" + titulo + '\'' +
                ", monto=" + monto +
                ", descripcion='" + descripcion + '\'' +
                ", fecha='" + fecha + '\'' +
                ", timestamp=" + timestamp +
                ", userId='" + userId + '\'' +
                ", comprobanteUrl='" + comprobanteUrl + '\'' +
                ", comprobantePublicId='" + comprobantePublicId + '\'' +
                ", comprobanteNombre='" + comprobanteNombre + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Ingreso ingreso = (Ingreso) obj;
        return id != null ? id.equals(ingreso.id) : ingreso.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}