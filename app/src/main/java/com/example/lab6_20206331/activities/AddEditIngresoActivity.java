package com.example.lab6_20206331.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.example.lab6_20206331.R;
import com.example.lab6_20206331.models.Ingreso;

public class AddEditIngresoActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private TextInputEditText etTitulo, etMonto, etDescripcion;
    private EditText etFecha;
    private MaterialButton btnGuardar, btnCancelar;
    private View progressBar;

    private Calendar calendar;
    private SimpleDateFormat dateFormat;

    private boolean isEditMode = false;
    private String ingresoId = null;
    private Ingreso currentIngreso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_ingreso);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        initViews();

        // Setup toolbar
        setupToolbar();

        // Setup date picker
        setupDatePicker();

        // Check if editing existing ingreso
        checkEditMode();

        // Set click listeners
        setClickListeners();
    }

    private void initViews() {
        etTitulo = findViewById(R.id.et_titulo);
        etMonto = findViewById(R.id.et_monto);
        etDescripcion = findViewById(R.id.et_descripcion);
        etFecha = findViewById(R.id.et_fecha);
        btnGuardar = findViewById(R.id.btn_guardar);
        btnCancelar = findViewById(R.id.btn_cancelar);
        progressBar = findViewById(R.id.progress_bar);

        calendar = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        // Set current date as default
        etFecha.setText(dateFormat.format(calendar.getTime()));
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    private void setupDatePicker() {
        etFecha.setOnClickListener(v -> showDatePickerDialog());
    }

    private void checkEditMode() {
        // Check if we're editing an existing ingreso
        ingresoId = getIntent().getStringExtra("ingreso_id");
        if (ingresoId != null) {
            isEditMode = true;
            setTitle("Editar Ingreso");
            loadIngresoData();
        } else {
            setTitle("Nuevo Ingreso");
        }
    }

    private void loadIngresoData() {
        showProgress(true);

        db.collection("ingresos").document(ingresoId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    showProgress(false);
                    if (documentSnapshot.exists()) {
                        currentIngreso = documentSnapshot.toObject(Ingreso.class);
                        if (currentIngreso != null) {
                            populateFields();
                        }
                    } else {
                        Toast.makeText(this, "Ingreso no encontrado", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    showProgress(false);
                    Toast.makeText(this, "Error al cargar datos: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void populateFields() {
        etTitulo.setText(currentIngreso.getTitulo());
        etMonto.setText(String.valueOf(currentIngreso.getMonto()));
        etDescripcion.setText(currentIngreso.getDescripcion());
        etFecha.setText(currentIngreso.getFecha());

        // In edit mode, only allow editing amount and description
        etTitulo.setEnabled(false);
        etFecha.setEnabled(false);
    }

    private void setClickListeners() {
        btnGuardar.setOnClickListener(v -> saveIngreso());
        btnCancelar.setOnClickListener(v -> finish());
    }

    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    etFecha.setText(dateFormat.format(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void saveIngreso() {
        if (!validateFields()) {
            return;
        }

        String titulo = etTitulo.getText().toString().trim();
        String montoStr = etMonto.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();
        String fecha = etFecha.getText().toString().trim();

        double monto;
        try {
            monto = Double.parseDouble(montoStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Monto inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();

        Map<String, Object> ingresoData = new HashMap<>();
        ingresoData.put("titulo", titulo);
        ingresoData.put("monto", monto);
        ingresoData.put("descripcion", descripcion);
        ingresoData.put("fecha", fecha);
        ingresoData.put("userId", userId);
        ingresoData.put("timestamp", System.currentTimeMillis());

        showProgress(true);

        if (isEditMode) {
            // Only update monto and descripcion in edit mode
            Map<String, Object> updates = new HashMap<>();
            updates.put("monto", monto);
            updates.put("descripcion", descripcion);

            db.collection("ingresos").document(ingresoId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        showProgress(false);
                        Toast.makeText(this, "Ingreso actualizado correctamente", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        showProgress(false);
                        Toast.makeText(this, "Error al actualizar: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Create new ingreso
            db.collection("ingresos")
                    .add(ingresoData)
                    .addOnSuccessListener(documentReference -> {
                        showProgress(false);
                        Toast.makeText(this, "Ingreso guardado correctamente", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        showProgress(false);
                        Toast.makeText(this, "Error al guardar: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private boolean validateFields() {
        String titulo = etTitulo.getText().toString().trim();
        String monto = etMonto.getText().toString().trim();
        String fecha = etFecha.getText().toString().trim();

        if (titulo.isEmpty()) {
            etTitulo.setError("El título es requerido");
            etTitulo.requestFocus();
            return false;
        }

        if (monto.isEmpty()) {
            etMonto.setError("El monto es requerido");
            etMonto.requestFocus();
            return false;
        }

        try {
            double montoValue = Double.parseDouble(monto);
            if (montoValue <= 0) {
                etMonto.setError("El monto debe ser mayor a 0");
                etMonto.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            etMonto.setError("Monto inválido");
            etMonto.requestFocus();
            return false;
        }

        if (fecha.isEmpty()) {
            Toast.makeText(this, "Seleccione una fecha", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnGuardar.setEnabled(!show);
        btnCancelar.setEnabled(!show);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}