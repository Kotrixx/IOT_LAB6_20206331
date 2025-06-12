package com.example.lab6_20206331.utils;

import android.text.TextUtils;
import android.util.Patterns;

import com.example.lab6_20206331.models.Egreso;
import com.example.lab6_20206331.models.Ingreso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

public class ValidationHelper {

    // Patrones de validación
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[+]?[0-9]{10,13}$");

    // ================= VALIDACIONES DE AUTENTICACIÓN =================

    /**
     * Valida formato de email
     */
    public static boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * Valida contraseña básica (mínimo 6 caracteres)
     */
    public static boolean isValidPassword(String password) {
        return !TextUtils.isEmpty(password) && password.length() >= 6;
    }

    /**
     * Valida contraseña fuerte (mayúscula, minúscula, número, símbolo, mín 8 caracteres)
     */
    public static boolean isStrongPassword(String password) {
        return !TextUtils.isEmpty(password) && PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * Valida número de teléfono
     */
    public static boolean isValidPhoneNumber(String phoneNumber) {
        return !TextUtils.isEmpty(phoneNumber) && PHONE_PATTERN.matcher(phoneNumber).matches();
    }

    // ================= VALIDACIONES DE TRANSACCIONES =================

    /**
     * Valida título de transacción
     */
    public static boolean isValidTitulo(String titulo) {
        return !TextUtils.isEmpty(titulo) &&
                titulo.trim().length() >= 3 &&
                titulo.trim().length() <= 100;
    }

    /**
     * Valida monto de transacción
     */
    public static boolean isValidMonto(double monto) {
        return monto > 0 && monto <= 999999999.99; // Máximo casi mil millones
    }

    /**
     * Valida monto desde String
     */
    public static boolean isValidMonto(String montoStr) {
        if (TextUtils.isEmpty(montoStr)) {
            return false;
        }

        try {
            double monto = Double.parseDouble(montoStr);
            return isValidMonto(monto);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Valida descripción (opcional pero si existe debe tener formato válido)
     */
    public static boolean isValidDescripcion(String descripcion) {
        // La descripción es opcional, pero si existe no debe ser solo espacios
        // y no debe exceder 500 caracteres
        if (descripcion == null) {
            return true; // null es válido (descripción opcional)
        }

        String trimmed = descripcion.trim();
        return trimmed.length() <= 500;
    }

    /**
     * Valida fecha de transacción
     */
    public static boolean isValidFecha(Date fecha) {
        if (fecha == null) {
            return false;
        }

        Date ahora = new Date();
        Date hace10Anos = new Date(ahora.getTime() - (10L * 365 * 24 * 60 * 60 * 1000));
        Date dentroDeUnAno = new Date(ahora.getTime() + (365L * 24 * 60 * 60 * 1000));

        // La fecha debe estar entre hace 10 años y dentro de 1 año
        return fecha.after(hace10Anos) && fecha.before(dentroDeUnAno);
    }

    // ================= VALIDACIONES DE MODELOS COMPLETOS =================

    /**
     * Valida objeto Ingreso completo
     */
    public static ValidationResult validateIngreso(Ingreso ingreso) {
        ValidationResult result = new ValidationResult();

        if (ingreso == null) {
            result.addError("El ingreso no puede ser nulo");
            return result;
        }

        // Validar título
        if (!isValidTitulo(ingreso.getTitulo())) {
            result.addError("El título debe tener entre 3 y 100 caracteres");
        }

        // Validar monto
        if (!isValidMonto(ingreso.getMonto())) {
            result.addError("El monto debe ser mayor a 0 y menor a 1,000,000,000");
        }

        // Validar descripción
        if (!isValidDescripcion(ingreso.getDescripcion())) {
            result.addError("La descripción no debe exceder 500 caracteres");
        }

        // Validar fecha
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); // Usa el formato real de tu string
            Date fecha = sdf.parse(ingreso.getFecha());
            if (!isValidFecha(fecha)) {
                result.addError("La fecha debe estar entre hace 10 años y dentro de 1 año");
            }
        } catch (ParseException e) {
            result.addError("La fecha tiene un formato inválido");
        }

        return result;
    }

    /**
     * Valida objeto Egreso completo
     */
    public static ValidationResult validateEgreso(Egreso egreso) {
        ValidationResult result = new ValidationResult();

        if (egreso == null) {
            result.addError("El egreso no puede ser nulo");
            return result;
        }

        // Validar título
        if (!isValidTitulo(egreso.getTitulo())) {
            result.addError("El título debe tener entre 3 y 100 caracteres");
        }

        // Validar monto
        if (!isValidMonto(egreso.getMonto())) {
            result.addError("El monto debe ser mayor a 0 y menor a 1,000,000,000");
        }

        // Validar descripción
        if (!isValidDescripcion(egreso.getDescripcion())) {
            result.addError("La descripción no debe exceder 500 caracteres");
        }

        // Validar fecha
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); // Usa el formato real de tu string
            Date fecha = sdf.parse(egreso.getFecha());
            if (!isValidFecha(fecha)) {
                result.addError("La fecha debe estar entre hace 10 años y dentro de 1 año");
            }
        } catch (ParseException e) {
            result.addError("La fecha tiene un formato inválido");
        }

        return result;
    }

    // ================= VALIDACIONES DE FORMULARIOS =================

    /**
     * Valida campos de formulario de login
     */
    public static ValidationResult validateLoginForm(String email, String password) {
        ValidationResult result = new ValidationResult();

        if (!isValidEmail(email)) {
            result.addError("Ingresa un email válido");
        }

        if (!isValidPassword(password)) {
            result.addError("La contraseña debe tener al menos 6 caracteres");
        }

        return result;
    }

    /**
     * Valida campos de formulario de registro
     */
    public static ValidationResult validateRegisterForm(String email, String password, String confirmPassword) {
        ValidationResult result = new ValidationResult();

        if (!isValidEmail(email)) {
            result.addError("Ingresa un email válido");
        }

        if (!isValidPassword(password)) {
            result.addError("La contraseña debe tener al menos 6 caracteres");
        }

        if (!password.equals(confirmPassword)) {
            result.addError("Las contraseñas no coinciden");
        }

        return result;
    }

    // ================= CLASE AUXILIAR PARA RESULTADOS =================

    public static class ValidationResult {
        private final java.util.List<String> errors;
        private boolean isValid;

        public ValidationResult() {
            this.errors = new java.util.ArrayList<>();
            this.isValid = true;
        }

        public void addError(String error) {
            errors.add(error);
            isValid = false;
        }

        public boolean isValid() {
            return isValid;
        }

        public java.util.List<String> getErrors() {
            return errors;
        }

        public String getFirstError() {
            return errors.isEmpty() ? null : errors.get(0);
        }

        public String getAllErrorsAsString() {
            if (errors.isEmpty()) {
                return "";
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < errors.size(); i++) {
                sb.append("• ").append(errors.get(i));
                if (i < errors.size() - 1) {
                    sb.append("\n");
                }
            }
            return sb.toString();
        }

        public int getErrorCount() {
            return errors.size();
        }
    }

    // ================= MÉTODOS UTILITARIOS =================

    /**
     * Limpia y normaliza texto de entrada
     */
    public static String cleanInput(String input) {
        if (input == null) {
            return null;
        }
        return input.trim();
    }

    /**
     * Convierte String a Double de forma segura
     */
    public static Double parseDoubleOrNull(String value) {
        if (TextUtils.isEmpty(value)) {
            return null;
        }

        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Verifica si una cadena contiene solo números
     */
    public static boolean isNumeric(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }

        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Verifica si una cadena es un entero válido
     */
    public static boolean isInteger(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }

        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}