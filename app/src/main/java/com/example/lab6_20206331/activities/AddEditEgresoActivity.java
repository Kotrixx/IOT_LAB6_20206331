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
import com.example.lab6_20206331.models.Egreso;

public class AddEditEgresoActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private TextInputEditText etTitulo, etMonto, etDescripcion;
    private EditText etFecha;
    private MaterialButton btnGuardar, btnCancelar;
    private View progressBar;

    private Calendar calendar;
    private SimpleDateFormat dateFormat;

    private boolean isEditMode = false;
    private String egresoId = null;
    private Egreso currentEgreso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_egreso);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        initViews();

        // Setup toolbar
        setupToolbar();

        // Setup date picker
        setupDatePicker();

        // Check if editing existing egreso
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
        // Check if we're editing an existing egreso
        egresoId = getIntent().getStringExtra("egreso_id");
        if (egresoId != null) {
            isEditMode = true;
            setTitle("Editar Egreso");
            loadEgresoData();
        } else {
            setTitle("Nuevo Egreso");
        }
    }

    private void loadEgresoData() {
        showProgress(true);

        db.collection("egresos").document(egresoId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    showProgress(false);
                    if (documentSnapshot.exists()) {
                        currentEgreso = documentSnapshot.toObject(Egreso.class);
                        if (currentEgreso != null) {
                            populateFields();
                        }
                    } else {
                        Toast.makeText(this, "Egreso no encontrado", Toast.LENGTH_SHORT).show();
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
        etTitulo.setText(currentEgreso.getTitulo());
        etMonto.setText(String.valueOf(currentEgreso.getMonto()));
        etDescripcion.setText(currentEgreso.getDescripcion());
        etFecha.setText(currentEgreso.getFecha());

        // In edit mode, only allow editing amount and description
        etTitulo.setEnabled(false);
        etFecha.setEnabled(false);
    }

    private void setClickListeners() {
        btnGuardar.setOnClickListener(v -> saveEgreso());
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

    private void saveEgreso() {
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

        Map<String, Object> egresoData = new HashMap<>();
        egresoData.put("titulo", titulo);
        egresoData.put("monto", monto);
        egresoData.put("descripcion", descripcion);
        egresoData.put("fecha", fecha);
        egresoData.put("userId", userId);
        egresoData.put("timestamp", System.currentTimeMillis());

        showProgress(true);

        if (isEditMode) {
            // Only update monto and descripcion in edit mode
            Map<String, Object> updates = new HashMap<>();
            updates.put("monto", monto);
            updates.put("descripcion", descripcion);

            db.collection("egresos").document(egresoId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        showProgress(false);
                        Toast.makeText(this, "Egreso actualizado correctamente", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        showProgress(false);
                        Toast.makeText(this, "Error al actualizar: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Create new egreso
            db.collection("egresos")
                    .add(egresoData)
                    .addOnSuccessListener(documentReference -> {
                        showProgress(false);
                        Toast.makeText(this, "Egreso guardado correctamente", Toast.LENGTH_SHORT).show();
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