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
    private static final int PICK_IMAGE_REQUEST = 1001;

    private RecyclerView recyclerView;
    private FloatingActionButton fabAdd;
    private IngresosAdapter adapter;
    private List<Ingreso> ingresosList;
    private ProgressBar progressBar;

    private IngresoRepository ingresoRepository;
    private String selectedDate = "";

    // Variables para manejo de imágenes
    private Uri selectedImageUri;
    private ImageView ivComprobantePreview;
    private View dialogAddIngreso;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ingresos, container, false);

        initViews(view);
        setupRecyclerView();
        initFirebase();
        initServicioAlmacenamiento();

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
        // Adapter actualizado con descarga
        adapter = new IngresosAdapter(ingresosList,
                this::editIngreso,
                this::deleteIngreso,
                this::downloadComprobante); // NUEVO
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
        ingresoRepository.getAllIngresos(new IngresoRepository.OnIngresosLoadedListener() {
            @Override
            public void onSuccess(List<Ingreso> ingresos) {
                showProgress(false);
                ingresosList.clear();
                ingresosList.addAll(ingresos);
                adapter.notifyDataSetChanged();

                if (ingresos.isEmpty()) {
                    showInfo("No hay ingresos aún. Usa el botón +");
                } else {
                    showInfo("Ingresos cargados: " + ingresos.size());
                }
            }

            @Override
            public void onError(String error) {
                showProgress(false);
                showError("Error cargando ingresos: " + error);
            }
        });
    }

    private void showAddIngresoDialog() {
        dialogAddIngreso = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_ingreso_with_comprobante, null);
        EditText etTitulo = dialogAddIngreso.findViewById(R.id.et_titulo);
        EditText etMonto = dialogAddIngreso.findViewById(R.id.et_monto);
        EditText etFecha = dialogAddIngreso.findViewById(R.id.et_fecha);
        EditText etDescripcion = dialogAddIngreso.findViewById(R.id.et_descripcion);
        ivComprobantePreview = dialogAddIngreso.findViewById(R.id.iv_comprobante_preview);
        View btnSeleccionarImagen = dialogAddIngreso.findViewById(R.id.btn_seleccionar_imagen);

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
                .setTitle("Agregar Ingreso")
                .setView(dialogAddIngreso)
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
                        addIngresoWithComprobante(titulo, monto, descripcion, fecha);
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

    private void addIngresoWithComprobante(String titulo, double monto, String descripcion, String fecha) {
        showProgress(true);

        // Generar nombre único para el archivo
        String fileName = "ingreso_" + System.currentTimeMillis() + "_" + FirebaseUtil.getCurrentUserId();

        // Subir imagen primero
        ServicioAlmacenamiento.guardarArchivo(getContext(), selectedImageUri, fileName,
                new ServicioAlmacenamiento.GuardarArchivoCallback() {
                    @Override
                    public void onSuccess(String urlArchivo, String publicId) {
                        // Crear ingreso con datos del comprobante
                        Ingreso nuevoIngreso = new Ingreso(titulo, monto, descripcion, fecha);
                        nuevoIngreso.setUserId(FirebaseUtil.getCurrentUserId() != null ?
                                FirebaseUtil.getCurrentUserId() : "temp_user_dev");
                        nuevoIngreso.setComprobanteUrl(urlArchivo);
                        nuevoIngreso.setComprobantePublicId(publicId);
                        nuevoIngreso.setComprobanteNombre(fileName);

                        // Guardar en Firebase
                        ingresoRepository.saveIngreso(nuevoIngreso, new IngresoRepository.OnIngresoSavedListener() {
                            @Override
                            public void onSuccess(String ingresoId) {
                                showProgress(false);
                                showInfo("Ingreso con comprobante guardado para el " + fecha);
                            }

                            @Override
                            public void onError(String error) {
                                showProgress(false);
                                showError("Error al guardar ingreso: " + error);
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

    private void downloadComprobante(Ingreso ingreso) {
        if (!ingreso.hasComprobante()) {
            showError("Este ingreso no tiene comprobante");
            return;
        }

        showProgress(true);
        String fileName = ingreso.getComprobanteFileName();

        ServicioAlmacenamiento.descargarArchivo(getContext(), ingreso.getComprobanteUrl(), fileName,
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

    private void editIngreso(Ingreso ingreso) {
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
                showProgress(false);
                showInfo("Ingreso actualizado");
            }

            @Override
            public void onError(String error) {
                showProgress(false);
                showError("Error actualizando: " + error);
            }
        });
    }

    private void deleteIngreso(Ingreso ingreso) {
        new AlertDialog.Builder(getContext())
                .setTitle("Eliminar Ingreso")
                .setMessage("¿Eliminar " + ingreso.getTitulo() + " del " + ingreso.getFecha() + "?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    showProgress(true);
                    ingresoRepository.deleteIngreso(ingreso.getId(), new IngresoRepository.OnIngresoDeletedListener() {
                        @Override
                        public void onSuccess() {
                            showProgress(false);
                            showInfo("Ingreso eliminado");
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
        if (ingresoRepository != null) {
            ingresoRepository.cleanup();
        }
    }
}