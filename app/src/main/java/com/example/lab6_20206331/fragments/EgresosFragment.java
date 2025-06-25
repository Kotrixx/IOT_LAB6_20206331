package com.example.lab6_20206331.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lab6_20206331.R;
import com.example.lab6_20206331.ServicioAlmacenamiento;
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
    private static final int PICK_IMAGE_REQUEST = 1002;

    private RecyclerView recyclerView;
    private FloatingActionButton fabAdd;
    private EgresosAdapter adapter;
    private List<Egreso> egresosList;
    private ProgressBar progressBar;

    private EgresoRepository egresoRepository;
    private String selectedDate = "";

    // Variables para manejo de imágenes
    private Uri selectedImageUri;
    private ImageView ivComprobantePreview;
    private View dialogAddEgreso;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_egresos, container, false);

        initViews(view);
        setupRecyclerView();
        initFirebase();
        initServicioAlmacenamiento();

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_view_egresos);
        fabAdd = view.findViewById(R.id.fab_add_egreso);
        progressBar = new ProgressBar(getContext());
        progressBar.setVisibility(View.GONE);

        fabAdd.setOnClickListener(v -> showAddEgresoDialog());
    }

    private void setupRecyclerView() {
        egresosList = new ArrayList<>();
        // Adapter actualizado con descarga
        adapter = new EgresosAdapter(egresosList,
                this::editEgreso,
                this::deleteEgreso,
                this::downloadComprobante); // NUEVO
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void initFirebase() {
        egresoRepository = EgresoRepository.getInstance();
        if (egresoRepository != null) {
            loadFirebaseData();
        } else {
            showError("Error inicializando repository");
        }
    }

    private void initServicioAlmacenamiento() {
        boolean connected = ServicioAlmacenamiento.conectarServicio(getContext());
        if (connected) {
            Log.d(TAG, "✅ Servicio de almacenamiento conectado");
        } else {
            Log.e(TAG, "❌ Error conectando servicio de almacenamiento");
        }
    }

    private void loadFirebaseData() {
        showProgress(true);
        egresoRepository.getAllEgresos(new EgresoRepository.OnEgresosLoadedListener() {
            @Override
            public void onSuccess(List<Egreso> egresos) {
                showProgress(false);
                egresosList.clear();
                egresosList.addAll(egresos);
                adapter.notifyDataSetChanged();

                if (egresos.isEmpty()) {
                    showInfo("No hay egresos aún. Usa el botón +");
                } else {
                    showInfo("Egresos cargados: " + egresos.size());
                }
            }

            @Override
            public void onError(String error) {
                showProgress(false);
                showError("Error cargando egresos: " + error);
            }
        });
    }

    private void showAddEgresoDialog() {
        dialogAddEgreso = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_egreso_with_comprobante, null);
        EditText etTitulo = dialogAddEgreso.findViewById(R.id.et_titulo);
        EditText etMonto = dialogAddEgreso.findViewById(R.id.et_monto);
        EditText etFecha = dialogAddEgreso.findViewById(R.id.et_fecha);
        EditText etDescripcion = dialogAddEgreso.findViewById(R.id.et_descripcion);
        ivComprobantePreview = dialogAddEgreso.findViewById(R.id.iv_comprobante_preview);
        View btnSeleccionarImagen = dialogAddEgreso.findViewById(R.id.btn_seleccionar_imagen);

        // Inicializar con fecha actual
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        selectedDate = sdf.format(calendar.getTime());
        etFecha.setText(selectedDate);

        // Click listener para el campo de fecha
        etFecha.setOnClickListener(v -> showDatePicker(etFecha));

        // Click listener para seleccionar imagen
        btnSeleccionarImagen.setOnClickListener(v -> selectImage());

        // Reset selected image
        selectedImageUri = null;
        ivComprobantePreview.setVisibility(View.GONE);

        new AlertDialog.Builder(getContext())
                .setTitle("Agregar Egreso")
                .setView(dialogAddEgreso)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String titulo = etTitulo.getText().toString().trim();
                    String montoStr = etMonto.getText().toString().trim();
                    String fecha = etFecha.getText().toString().trim();
                    String descripcion = etDescripcion.getText().toString().trim();

                    if (titulo.isEmpty() || montoStr.isEmpty() || fecha.isEmpty()) {
                        showError("Título, monto y fecha son obligatorios");
                        return;
                    }

                    if (selectedImageUri == null) {
                        showError("El comprobante es requerido");
                        return;
                    }

                    try {
                        double monto = Double.parseDouble(montoStr);
                        addEgresoWithComprobante(titulo, monto, descripcion, fecha);
                    } catch (NumberFormatException e) {
                        showError("Monto inválido");
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            if (selectedImageUri != null && ivComprobantePreview != null) {
                ivComprobantePreview.setImageURI(selectedImageUri);
                ivComprobantePreview.setVisibility(View.VISIBLE);
                showInfo("Imagen seleccionada");
            }
        }
    }

    private void addEgresoWithComprobante(String titulo, double monto, String descripcion, String fecha) {
        showProgress(true);

        // Generar nombre único para el archivo
        String fileName = "egreso_" + System.currentTimeMillis() + "_" + FirebaseUtil.getCurrentUserId();

        // Subir imagen primero
        ServicioAlmacenamiento.guardarArchivo(getContext(), selectedImageUri, fileName,
                new ServicioAlmacenamiento.GuardarArchivoCallback() {
                    @Override
                    public void onSuccess(String urlArchivo, String publicId) {
                        // Crear egreso con datos del comprobante
                        Egreso nuevoEgreso = new Egreso(titulo, monto, descripcion, fecha);
                        nuevoEgreso.setUserId(FirebaseUtil.getCurrentUserId() != null ?
                                FirebaseUtil.getCurrentUserId() : "temp_user_dev");
                        nuevoEgreso.setComprobanteUrl(urlArchivo);
                        nuevoEgreso.setComprobantePublicId(publicId);
                        nuevoEgreso.setComprobanteNombre(fileName);

                        // Guardar en Firebase
                        egresoRepository.saveEgreso(nuevoEgreso, new EgresoRepository.OnEgresoSavedListener() {
                            @Override
                            public void onSuccess(String egresoId) {
                                showProgress(false);
                                showInfo("Egreso con comprobante guardado para el " + fecha);
                            }

                            @Override
                            public void onError(String error) {
                                showProgress(false);
                                showError("Error al guardar egreso: " + error);
                            }
                        });
                    }

                    @Override
                    public void onError(String mensajeError) {
                        showProgress(false);
                        showError("Error subiendo comprobante: " + mensajeError);
                    }

                    @Override
                    public void onProgress(String mensaje) {
                        // Actualizar UI con progreso si es necesario
                        Log.d(TAG, "Progreso subida: " + mensaje);
                    }
                });
    }

    private void downloadComprobante(Egreso egreso) {
        if (!egreso.hasComprobante()) {
            showError("Este egreso no tiene comprobante");
            return;
        }

        showProgress(true);
        String fileName = egreso.getComprobanteFileName();

        ServicioAlmacenamiento.descargarArchivo(getContext(), egreso.getComprobanteUrl(), fileName,
                new ServicioAlmacenamiento.DescargarArchivoCallback() {
                    @Override
                    public void onSuccess(String rutaArchivo, java.io.File archivo) {
                        showProgress(false);
                        showInfo("Comprobante descargado: " + archivo.getName());
                        Log.d(TAG, "Archivo descargado en: " + rutaArchivo);
                    }

                    @Override
                    public void onError(String mensajeError) {
                        showProgress(false);
                        showError("Error descargando comprobante: " + mensajeError);
                    }

                    @Override
                    public void onProgress(String mensaje) {
                        Log.d(TAG, "Progreso descarga: " + mensaje);
                    }
                });
    }

    private void showDatePicker(EditText etFecha) {
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

    private void editEgreso(Egreso egreso) {
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
                showProgress(false);
                showInfo("Egreso actualizado");
            }

            @Override
            public void onError(String error) {
                showProgress(false);
                showError("Error actualizando: " + error);
            }
        });
    }

    private void deleteEgreso(Egreso egreso) {
        new AlertDialog.Builder(getContext())
                .setTitle("Eliminar Egreso")
                .setMessage("¿Eliminar " + egreso.getTitulo() + " del " + egreso.getFecha() + "?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    showProgress(true);
                    egresoRepository.deleteEgreso(egreso.getId(), new EgresoRepository.OnEgresoDeletedListener() {
                        @Override
                        public void onSuccess() {
                            showProgress(false);
                            showInfo("Egreso eliminado");
                        }

                        @Override
                        public void onError(String error) {
                            showProgress(false);
                            showError("Error eliminando: " + error);
                        }
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showProgress(boolean show) {
        fabAdd.setEnabled(!show);
        if (show) {
            Toast.makeText(getContext(), "Cargando...", Toast.LENGTH_SHORT).show();
        }
    }

    private void showError(String message) {
        Toast.makeText(getContext(), "❌ " + message, Toast.LENGTH_LONG).show();
    }

    private void showInfo(String message) {
        Toast.makeText(getContext(), "ℹ️ " + message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (egresoRepository != null) {
            egresoRepository.cleanup();
        }
    }
}