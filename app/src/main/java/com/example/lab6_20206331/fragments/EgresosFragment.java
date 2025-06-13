// EgresosFragment.java - CORRECCIÓN COMPLETA

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
import com.example.lab6_20206331.adapters.EgresosAdapter;
import com.example.lab6_20206331.FirebaseUtil;
import com.example.lab6_20206331.models.Egreso;
import com.example.lab6_20206331.repository.EgresoRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EgresosFragment extends Fragment {

    private static final String TAG = "EgresosFragment";

    private RecyclerView recyclerView;
    private FloatingActionButton fabAdd;
    private EgresosAdapter adapter;
    private List<Egreso> egresosList;
    private ProgressBar progressBar;

    private EgresoRepository egresoRepository;
    private String selectedDate = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_egresos, container, false);

        initViews(view);
        setupRecyclerView();
        initFirebase();

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_view_egresos);
        fabAdd = view.findViewById(R.id.fab_add_egreso);
        progressBar = new ProgressBar(getContext());
        progressBar.setVisibility(View.GONE);

        if (fabAdd != null) {
            fabAdd.setOnClickListener(v -> showAddEgresoDialog());
        }
    }

    private void setupRecyclerView() {
        egresosList = new ArrayList<>();
        adapter = new EgresosAdapter(egresosList, this::editEgreso, this::deleteEgreso);

        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerView.setAdapter(adapter);
        }
    }

    private void initFirebase() {
        egresoRepository = EgresoRepository.getInstance();
        if (egresoRepository != null) {
            loadFirebaseData();
        } else {
            showError("Error inicializando repository");
        }
    }

    private void loadFirebaseData() {
        showProgress(true);
        egresoRepository.getAllEgresos(new EgresoRepository.OnEgresosLoadedListener() {
            @Override
            public void onSuccess(List<Egreso> egresos) {
                // VERIFICAR QUE EL FRAGMENT SIGUE ADJUNTO
                if (!isAdded() || getContext() == null) {
                    Log.w(TAG, "Fragment no está adjunto, ignorando callback");
                    return;
                }

                Log.d(TAG, "Egresos recibidos: " + egresos.size());

                showProgress(false);
                egresosList.clear();
                egresosList.addAll(egresos);

                // VERIFICAR QUE EL ADAPTER EXISTE ANTES DE NOTIFICAR
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                    Log.d(TAG, "Adapter actualizado con " + egresosList.size() + " items");
                } else {
                    Log.w(TAG, "Adapter es null, no se puede actualizar");
                }

                if (egresos.isEmpty()) {
                    showInfo("No hay egresos aún. Usa el botón +");
                } else {
                    showInfo("Egresos cargados: " + egresos.size());
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
                showError("Error cargando egresos: " + error);
            }
        });
    }

    private void showAddEgresoDialog() {
        // VERIFICAR CONTEXTO ANTES DE CREAR DIÁLOGO
        if (getContext() == null) {
            Log.w(TAG, "Contexto nulo, no se puede mostrar diálogo");
            return;
        }

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_egreso, null);
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
                .setTitle("Agregar Egreso")
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
                        addEgresoToFirebase(titulo, monto, descripcion, fecha);
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

    private void addEgresoToFirebase(String titulo, double monto, String descripcion, String fecha) {
        Egreso nuevoEgreso = new Egreso(titulo, monto, descripcion, fecha);
        nuevoEgreso.setUserId(FirebaseUtil.getCurrentUserId() != null ?
                FirebaseUtil.getCurrentUserId() : "temp_user_dev");

        showProgress(true);
        egresoRepository.saveEgreso(nuevoEgreso, new EgresoRepository.OnEgresoSavedListener() {
            @Override
            public void onSuccess(String egresoId) {
                // VERIFICAR QUE EL FRAGMENT SIGUE ADJUNTO
                if (!isAdded() || getContext() == null) {
                    Log.w(TAG, "Fragment no está adjunto, ignorando callback de guardado");
                    return;
                }

                showProgress(false);
                showInfo("Egreso guardado para el " + fecha);
                // NO NECESITAS RECARGAR DATOS - Firebase Realtime los actualiza automáticamente
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

    private void editEgreso(Egreso egreso) {
        // VERIFICAR CONTEXTO ANTES DE CREAR DIÁLOGO
        if (getContext() == null) {
            Log.w(TAG, "Contexto nulo, no se puede editar egreso");
            return;
        }

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_egreso, null);
        EditText etMonto = dialogView.findViewById(R.id.et_monto);
        EditText etDescripcion = dialogView.findViewById(R.id.et_descripcion);

        // Precargar datos actuales
        etMonto.setText(String.valueOf(egreso.getMonto()));
        etDescripcion.setText(egreso.getDescripcion());

        new AlertDialog.Builder(getContext())
                .setTitle("Editar Egreso")
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
                        updateEgreso(egreso, monto, descripcion);
                    } catch (NumberFormatException e) {
                        showError("Monto inválido");
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void updateEgreso(Egreso egreso, double monto, String descripcion) {
        egreso.setMonto(monto);
        egreso.setDescripcion(descripcion);

        showProgress(true);
        egresoRepository.saveEgreso(egreso, new EgresoRepository.OnEgresoSavedListener() {
            @Override
            public void onSuccess(String egresoId) {
                // VERIFICAR QUE EL FRAGMENT SIGUE ADJUNTO
                if (!isAdded() || getContext() == null) {
                    return;
                }

                showProgress(false);
                showInfo("Egreso actualizado");
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

    private void deleteEgreso(Egreso egreso) {
        // VERIFICAR CONTEXTO ANTES DE CREAR DIÁLOGO
        if (getContext() == null) {
            Log.w(TAG, "Contexto nulo, no se puede eliminar egreso");
            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Eliminar Egreso")
                .setMessage("¿Eliminar " + egreso.getTitulo() + " del " + egreso.getFecha() + "?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    showProgress(true);
                    egresoRepository.deleteEgreso(egreso.getId(), new EgresoRepository.OnEgresoDeletedListener() {
                        @Override
                        public void onSuccess() {
                            // VERIFICAR QUE EL FRAGMENT SIGUE ADJUNTO
                            if (!isAdded() || getContext() == null) {
                                return;
                            }

                            showProgress(false);
                            showInfo("Egreso eliminado");
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
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume - Fragment visible");

        // RECARGAR DATOS CUANDO EL FRAGMENT SE HACE VISIBLE
        if (egresoRepository != null) {
            loadFirebaseData();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (egresoRepository != null) {
            egresoRepository.cleanup();
        }
    }
}