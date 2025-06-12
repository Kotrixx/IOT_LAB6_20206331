package com.example.lab6_20206331.models;

import java.util.Date;

public abstract class TransaccionFinanciera {

    protected String id;
    protected String titulo;
    protected double monto;
    protected String descripcion;
    protected Date fecha;
    protected String userId;
    protected Date fechaCreacion;
    protected Date fechaModificacion;

    // Constructor vacío
    public TransaccionFinanciera() {
    }

    // Constructor básico
    public TransaccionFinanciera(String titulo, double monto, String descripcion, Date fecha) {
        this.titulo = titulo;
        this.monto = monto;
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.fechaCreacion = new Date();
        this.fechaModificacion = new Date();
    }

    // Getters y Setters comunes
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
        this.fechaModificacion = new Date();
    }

    public double getMonto() {
        return monto;
    }

    public void setMonto(double monto) {
        this.monto = monto;
        this.fechaModificacion = new Date();
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
        this.fechaModificacion = new Date();
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
        this.fechaModificacion = new Date();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Date getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Date fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Date getFechaModificacion() {
        return fechaModificacion;
    }

    public void setFechaModificacion(Date fechaModificacion) {
        this.fechaModificacion = fechaModificacion;
    }

    // Método abstracto para validación
    public abstract boolean isValid();

    // Método abstracto para obtener el tipo de transacción
    public abstract String getTipoTransaccion();

    // Métodos comunes
    public boolean hasDescripcion() {
        return descripcion != null && !descripcion.trim().isEmpty();
    }

    public boolean isTituloValido() {
        return titulo != null && !titulo.trim().isEmpty();
    }

    public boolean isMontoValido() {
        return monto > 0;
    }

    public boolean isFechaValida() {
        return fecha != null;
    }
}