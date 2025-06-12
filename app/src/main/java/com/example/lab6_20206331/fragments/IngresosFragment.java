// IngresosFragment.java - Corrección completa
package com.example.lab6_20206331.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lab6_20206331.R;
import com.example.lab6_20206331.adapters.IngresosAdapter;
import com.example.lab6_20206331.FirebaseUtil;
import com.example.lab6_20206331.models.Ingreso;
import com.example.lab6_20206331.repository.IngresoRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class IngresosFragment extends Fragment {

    private static final String TAG = "IngresosFragment";

    private RecyclerView recyclerView;
    private FloatingActionButton fabAdd;
    private IngresosAdapter adapter;
    private List<Ingreso> ingresosList;
    private ProgressBar progressBar;

    private IngresoRepository ingresoRepository;
    private String selectedDate = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ingresos, container, false);

        initViews(view);
        setupRecyclerView();
        initFirebase();

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_ingresos);
        fabAdd = view.findViewById(R.id.fab_add_ingreso);
        progressBar = new ProgressBar(getContext());
        progressBar.setVisibility(View.GONE);

        fabAdd.setOnClickListener(v -> showAddIngresoDialog());
    }

    private void setupRecyclerView() {
        ingresosList = new ArrayList<>();
        adapter = new IngresosAdapter(ingresosList, this::editIngreso, this::deleteIngreso);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void initFirebase() {
        ingresoRepository = IngresoRepository.getInstance();
        if (ingresoRepository != null) {
            loadFirebaseData();
        } else {
            showError("Error inicializando repository");
        }
    }

    private void loadFirebaseData() {
        showProgress(true);
        ingresoRepository.getAllIngresos(new IngresoRepository.OnIngresosLoadedListener() {
            @Override
            public void onSuccess(List<Ingreso> ingresos) {
                // VERIFICAR QUE EL FRAGMENT SIGUE ADJUNTO
                if (!isAdded() || getContext() == null) {
                    Log.w(TAG, "Fragment no está adjunto, ignorando callback");
                    return;
                }

                showProgress(false);
                ingresosList.clear();
                ingresosList.addAll(ingresos);

                // Verificar que el adapter existe antes de notificar
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }

                if (ingresos.isEmpty()) {
                    showInfo("No hay ingresos aún. Usa el botón +");
                } else {
                    showInfo("Ingresos cargados: " + ingresos.size());
                }
            }

            @Override
            public void onError(String error) {
                // VERIFICAR QUE EL FRAGMENT SIGUE ADJUNTO
                if (!isAdded() || getContext() == null) {
                    Log.w(TAG, "Fragment no está adjunto, ignorando callback de error");
                    return;
                }

                showProgress(false);
                showError("Error cargando ingresos: " + error);
            }
        });
    }

    private void showAddIngresoDialog() {
        // VERIFICAR CONTEXTO ANTES DE CREAR DIÁLOGO
        if (getContext() == null) {
            Log.w(TAG, "Contexto nulo, no se puede mostrar diálogo");
            return;
        }

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_ingreso, null);
        EditText etTitulo = dialogView.findViewById(R.id.et_titulo);
        EditText etMonto = dialogView.findViewById(R.id.et_monto);
        EditText etFecha = dialogView.findViewById(R.id.et_fecha);
        EditText etDescripcion = dialogView.findViewById(R.id.et_descripcion);

        // Inicializar con fecha actual
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        selectedDate = sdf.format(calendar.getTime());
        etFecha.setText(selectedDate);

        // Click listener para el campo de fecha
        etFecha.setOnClickListener(v -> showDatePicker(etFecha));

        new AlertDialog.Builder(getContext())
                .setTitle("Agregar Ingreso")
                .setView(dialogView)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String titulo = etTitulo.getText().toString().trim();
                    String montoStr = etMonto.getText().toString().trim();
                    String fecha = etFecha.getText().toString().trim();
                    String descripcion = etDescripcion.getText().toString().trim();

                    if (titulo.isEmpty() || montoStr.isEmpty() || fecha.isEmpty()) {
                        showError("Título, monto y fecha son obligatorios");
                        return;
                    }

                    try {
                        double monto = Double.parseDouble(montoStr);
                        addIngresoToFirebase(titulo, monto, descripcion, fecha);
                    } catch (NumberFormatException e) {
                        showError("Monto inválido");
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showDatePicker(EditText etFecha) {
        // VERIFICAR CONTEXTO ANTES DE CREAR DATEPICKER
        if (getContext() == null) {
            Log.w(TAG, "Contexto nulo, no se puede mostrar DatePicker");
            return;
        }

        Calendar calendar = Calendar.getInstance();

        // Si ya hay una fecha seleccionada, usarla como inicial
        if (!selectedDate.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date date = sdf.parse(selectedDate);
                if (date != null) {
                    calendar.setTime(date);
                }
            } catch (Exception e) {
                // Si hay error, usar fecha actual
            }
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(year, month, dayOfMonth);

                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    selectedDate = sdf.format(selectedCalendar.getTime());
                    etFecha.setText(selectedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private void addIngresoToFirebase(String titulo, double monto, String descripcion, String fecha) {
        Ingreso nuevoIngreso = new Ingreso(titulo, monto, descripcion, fecha);
        nuevoIngreso.setUserId(FirebaseUtil.getCurrentUserId() != null ?
                FirebaseUtil.getCurrentUserId() : "temp_user_dev");

        showProgress(true);
        ingresoRepository.saveIngreso(nuevoIngreso, new IngresoRepository.OnIngresoSavedListener() {
            @Override
            public void onSuccess(String ingresoId) {
                // VERIFICAR QUE EL FRAGMENT SIGUE ADJUNTO
                if (!isAdded() || getContext() == null) {
                    Log.w(TAG, "Fragment no está adjunto, ignorando callback de guardado");
                    return;
                }

                showProgress(false);
                showInfo("Ingreso guardado para el " + fecha);
            }

            @Override
            public void onError(String error) {
                // VERIFICAR QUE EL FRAGMENT SIGUE ADJUNTO
                if (!isAdded() || getContext() == null) {
                    Log.w(TAG, "Fragment no está adjunto, ignorando callback de error");
                    return;
                }

                showProgress(false);
                showError("Error al guardar: " + error);
            }
        });
    }

    private void editIngreso(Ingreso ingreso) {
        // VERIFICAR CONTEXTO ANTES DE CREAR DIÁLOGO
        if (getContext() == null) {
            Log.w(TAG, "Contexto nulo, no se puede editar ingreso");
            return;
        }

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_ingreso, null);
        EditText etMonto = dialogView.findViewById(R.id.et_monto);
        EditText etDescripcion = dialogView.findViewById(R.id.et_descripcion);

        // Precargar datos actuales
        etMonto.setText(String.valueOf(ingreso.getMonto()));
        etDescripcion.setText(ingreso.getDescripcion());

        new AlertDialog.Builder(getContext())
                .setTitle("Editar Ingreso")
                .setMessage("Solo puedes editar monto y descripción")
                .setView(dialogView)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String montoStr = etMonto.getText().toString().trim();
                    String descripcion = etDescripcion.getText().toString().trim();

                    if (montoStr.isEmpty()) {
                        showError("El monto es obligatorio");
                        return;
                    }

                    try {
                        double monto = Double.parseDouble(montoStr);
                        updateIngreso(ingreso, monto, descripcion);
                    } catch (NumberFormatException e) {
                        showError("Monto inválido");
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void updateIngreso(Ingreso ingreso, double monto, String descripcion) {
        ingreso.setMonto(monto);
        ingreso.setDescripcion(descripcion);

        showProgress(true);
        ingresoRepository.saveIngreso(ingreso, new IngresoRepository.OnIngresoSavedListener() {
            @Override
            public void onSuccess(String ingresoId) {
                // VERIFICAR QUE EL FRAGMENT SIGUE ADJUNTO
                if (!isAdded() || getContext() == null) {
                    return;
                }

                showProgress(false);
                showInfo("Ingreso actualizado");
            }

            @Override
            public void onError(String error) {
                // VERIFICAR QUE EL FRAGMENT SIGUE ADJUNTO
                if (!isAdded() || getContext() == null) {
                    return;
                }

                showProgress(false);
                showError("Error actualizando: " + error);
            }
        });
    }

    private void deleteIngreso(Ingreso ingreso) {
        // VERIFICAR CONTEXTO ANTES DE CREAR DIÁLOGO
        if (getContext() == null) {
            Log.w(TAG, "Contexto nulo, no se puede eliminar ingreso");
            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Eliminar Ingreso")
                .setMessage("¿Eliminar " + ingreso.getTitulo() + " del " + ingreso.getFecha() + "?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    showProgress(true);
                    ingresoRepository.deleteIngreso(ingreso.getId(), new IngresoRepository.OnIngresoDeletedListener() {
                        @Override
                        public void onSuccess() {
                            // VERIFICAR QUE EL FRAGMENT SIGUE ADJUNTO
                            if (!isAdded() || getContext() == null) {
                                return;
                            }

                            showProgress(false);
                            showInfo("Ingreso eliminado");
                        }

                        @Override
                        public void onError(String error) {
                            // VERIFICAR QUE EL FRAGMENT SIGUE ADJUNTO
                            if (!isAdded() || getContext() == null) {
                                return;
                            }

                            showProgress(false);
                            showError("Error eliminando: " + error);
                        }
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showProgress(boolean show) {
        if (fabAdd != null) {
            fabAdd.setEnabled(!show);
        }
        if (show && isAdded() && getContext() != null) {
            Toast.makeText(getContext(), "Cargando...", Toast.LENGTH_SHORT).show();
        }
    }

    private void showError(String message) {
        // VERIFICACIÓN SEGURA PARA TOAST
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), "❌ " + message, Toast.LENGTH_LONG).show();
        } else {
            Log.e(TAG, "Error (fragment no adjunto): " + message);
        }
    }

    private void showInfo(String message) {
        // VERIFICACIÓN SEGURA PARA TOAST
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), "ℹ️ " + message, Toast.LENGTH_SHORT).show();
        } else {
            Log.i(TAG, "Info (fragment no adjunto): " + message);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (ingresoRepository != null) {
            ingresoRepository.cleanup();
        }
    }
}