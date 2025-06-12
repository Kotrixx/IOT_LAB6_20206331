package com.example.lab6_20206331.models;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ResumenFinanciero {

    private int mes;
    private int año;
    private double totalIngresos;
    private double totalEgresos;
    private double balanceNeto;
    private double porcentajeIngresos;
    private double porcentajeEgresos;
    private List<Ingreso> ingresos;
    private List<Egreso> egresos;
    private String nombreMes;

    // Constructor vacío
    public ResumenFinanciero() {
        this.ingresos = new ArrayList<>();
        this.egresos = new ArrayList<>();
    }

    // Constructor con mes y año
    public ResumenFinanciero(int mes, int año) {
        this.mes = mes;
        this.año = año;
        this.ingresos = new ArrayList<>();
        this.egresos = new ArrayList<>();
        this.nombreMes = obtenerNombreMes(mes);
        calcularTotales();
    }

    // Constructor completo
    public ResumenFinanciero(int mes, int año, List<Ingreso> ingresos, List<Egreso> egresos) {
        this.mes = mes;
        this.año = año;
        this.ingresos = ingresos != null ? ingresos : new ArrayList<>();
        this.egresos = egresos != null ? egresos : new ArrayList<>();
        this.nombreMes = obtenerNombreMes(mes);
        calcularTotales();
    }

    // Getters y Setters
    public int getMes() {
        return mes;
    }

    public void setMes(int mes) {
        this.mes = mes;
        this.nombreMes = obtenerNombreMes(mes);
    }

    public int getAño() {
        return año;
    }

    public void setAño(int año) {
        this.año = año;
    }

    public double getTotalIngresos() {
        return totalIngresos;
    }

    public void setTotalIngresos(double totalIngresos) {
        this.totalIngresos = totalIngresos;
        calcularPorcentajes();
        calcularBalance();
    }

    public double getTotalEgresos() {
        return totalEgresos;
    }

    public void setTotalEgresos(double totalEgresos) {
        this.totalEgresos = totalEgresos;
        calcularPorcentajes();
        calcularBalance();
    }

    public double getBalanceNeto() {
        return balanceNeto;
    }

    public void setBalanceNeto(double balanceNeto) {
        this.balanceNeto = balanceNeto;
    }

    public double getPorcentajeIngresos() {
        return porcentajeIngresos;
    }

    public void setPorcentajeIngresos(double porcentajeIngresos) {
        this.porcentajeIngresos = porcentajeIngresos;
    }

    public double getPorcentajeEgresos() {
        return porcentajeEgresos;
    }

    public void setPorcentajeEgresos(double porcentajeEgresos) {
        this.porcentajeEgresos = porcentajeEgresos;
    }

    public List<Ingreso> getIngresos() {
        return ingresos;
    }

    public void setIngresos(List<Ingreso> ingresos) {
        this.ingresos = ingresos != null ? ingresos : new ArrayList<>();
        calcularTotales();
    }

    public List<Egreso> getEgresos() {
        return egresos;
    }

    public void setEgresos(List<Egreso> egresos) {
        this.egresos = egresos != null ? egresos : new ArrayList<>();
        calcularTotales();
    }

    public String getNombreMes() {
        return nombreMes;
    }

    public void setNombreMes(String nombreMes) {
        this.nombreMes = nombreMes;
    }

    // Métodos de cálculo
    public void calcularTotales() {
        totalIngresos = 0.0;
        totalEgresos = 0.0;

        for (Ingreso ingreso : ingresos) {
            totalIngresos += ingreso.getMonto();
        }

        for (Egreso egreso : egresos) {
            totalEgresos += egreso.getMonto();
        }

        calcularBalance();
        calcularPorcentajes();
    }

    private void calcularBalance() {
        balanceNeto = totalIngresos - totalEgresos;
    }

    private void calcularPorcentajes() {
        double total = totalIngresos + totalEgresos;

        if (total > 0) {
            porcentajeIngresos = (totalIngresos / total) * 100;
            porcentajeEgresos = (totalEgresos / total) * 100;
        } else {
            porcentajeIngresos = 0.0;
            porcentajeEgresos = 0.0;
        }
    }

    // Métodos utilitarios
    public boolean hayDatos() {
        return !ingresos.isEmpty() || !egresos.isEmpty();
    }

    public boolean esSuperavit() {
        return balanceNeto > 0;
    }

    public boolean esDeficit() {
        return balanceNeto < 0;
    }

    public boolean esEquilibrado() {
        return Math.abs(balanceNeto) < 0.01; // Considerar como equilibrado si la diferencia es menor a 1 centavo
    }

    public String getEstadoFinanciero() {
        if (esSuperavit()) {
            return "Superávit";
        } else if (esDeficit()) {
            return "Déficit";
        } else {
            return "Equilibrado";
        }
    }

    public int getCantidadIngresos() {
        return ingresos.size();
    }

    public int getCantidadEgresos() {
        return egresos.size();
    }

    public int getCantidadTotalTransacciones() {
        return getCantidadIngresos() + getCantidadEgresos();
    }

    // Métodos de formateo
    public String getTotalIngresosFormateado() {
        DecimalFormat formatter = new DecimalFormat("#,##0.00");
        return "S/. " + formatter.format(totalIngresos);
    }

    public String getTotalEgresosFormateado() {
        DecimalFormat formatter = new DecimalFormat("#,##0.00");
        return "S/. " + formatter.format(totalEgresos);
    }

    public String getBalanceNetoFormateado() {
        DecimalFormat formatter = new DecimalFormat("#,##0.00");
        String signo = balanceNeto >= 0 ? "+" : "";
        return signo + "S/. " + formatter.format(balanceNeto);
    }

    public String getPorcentajeIngresosFormateado() {
        DecimalFormat formatter = new DecimalFormat("#0.0");
        return formatter.format(porcentajeIngresos) + "%";
    }

    public String getPorcentajeEgresosFormateado() {
        DecimalFormat formatter = new DecimalFormat("#0.0");
        return formatter.format(porcentajeEgresos) + "%";
    }

    // Método para obtener el nombre del mes
    private String obtenerNombreMes(int mes) {
        String[] meses = {
                "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        };

        if (mes >= 1 && mes <= 12) {
            return meses[mes - 1];
        }
        return "Mes desconocido";
    }

    public String getPeriodoFormateado() {
        return nombreMes + " " + año;
    }

    // Método para crear resumen del mes actual
    public static ResumenFinanciero crearResumenMesActual() {
        Calendar calendar = Calendar.getInstance();
        int mesActual = calendar.get(Calendar.MONTH) + 1; // Calendar.MONTH es 0-based
        int añoActual = calendar.get(Calendar.YEAR);
        return new ResumenFinanciero(mesActual, añoActual);
    }

    // Método para agregar ingreso
    public void agregarIngreso(Ingreso ingreso) {
        if (ingreso != null) {
            ingresos.add(ingreso);
            calcularTotales();
        }
    }

    // Método para agregar egreso
    public void agregarEgreso(Egreso egreso) {
        if (egreso != null) {
            egresos.add(egreso);
            calcularTotales();
        }
    }

    // Método para eliminar ingreso
    public boolean eliminarIngreso(String ingresoId) {
        boolean eliminado = ingresos.removeIf(ingreso -> ingreso.getId().equals(ingresoId));
        if (eliminado) {
            calcularTotales();
        }
        return eliminado;
    }

    // Método para eliminar egreso
    public boolean eliminarEgreso(String egresoId) {
        boolean eliminado = egresos.removeIf(egreso -> egreso.getId().equals(egresoId));
        if (eliminado) {
            calcularTotales();
        }
        return eliminado;
    }

    // Método para limpiar datos
    public void limpiar() {
        ingresos.clear();
        egresos.clear();
        calcularTotales();
    }

    @Override
    public String toString() {
        return "ResumenFinanciero{" +
                "mes=" + mes +
                ", año=" + año +
                ", nombreMes='" + nombreMes + '\'' +
                ", totalIngresos=" + totalIngresos +
                ", totalEgresos=" + totalEgresos +
                ", balanceNeto=" + balanceNeto +
                ", cantidadIngresos=" + getCantidadIngresos() +
                ", cantidadEgresos=" + getCantidadEgresos() +
                '}';
    }
}