package com.example.lab6_20206331.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class IngresosFragment extends Fragment {

    private static final String TAG = "IngresosFragment";
    private static final int PICK_IMAGE_REQUEST = 1001;
    private static final int CAMERA_REQUEST = 1002;
    private static final int PERMISSION_REQUEST_CAMERA = 100;
    private static final int PERMISSION_REQUEST_STORAGE = 101;

    private RecyclerView recyclerView;
    private FloatingActionButton fabAdd;
    private IngresosAdapter adapter;
    private List<Ingreso> ingresosList;

    private IngresoRepository ingresoRepository;
    private String selectedDate = "";
    private boolean isLoading = false;

    // Variables para manejo de imágenes
    private Uri selectedImageUri;
    private Uri photoUri;
    private ImageView ivComprobantePreview;
    private View dialogAddIngreso;
    private String currentPhotoPath;

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
        fabAdd.setOnClickListener(v -> showAddIngresoDialog());
    }

    private void setupRecyclerView() {
        ingresosList = new ArrayList<>();
        adapter = new IngresosAdapter(ingresosList,
                this::editIngreso,
                this::deleteIngreso,
                this::downloadComprobante);
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
        Log.d(TAG, connected ? "✅ Servicio conectado" : "❌ Error conectando servicio");
    }

    private void loadFirebaseData() {
        if (isLoading) return;

        setLoading(true);
        ingresoRepository.getAllIngresos(new IngresoRepository.OnIngresosLoadedListener() {
            @Override
            public void onSuccess(List<Ingreso> ingresos) {
                setLoading(false);
                ingresosList.clear();
                ingresosList.addAll(ingresos);
                adapter.notifyDataSetChanged();

                if (ingresos.isEmpty()) {
                    showInfo("No hay ingresos registrados");
                }
            }

            @Override
            public void onError(String error) {
                setLoading(false);
                showError("Error cargando ingresos");
                Log.e(TAG, "Error: " + error);
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

        etFecha.setOnClickListener(v -> showDatePicker(etFecha));
        btnSeleccionarImagen.setOnClickListener(v -> showImageSourceDialog());

        // Reset
        selectedImageUri = null;
        photoUri = null;
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

    private void showImageSourceDialog() {
        String[] options = {"📸 Tomar foto", "🖼️ Seleccionar de galería"};

        new AlertDialog.Builder(getContext())
                .setTitle("Seleccionar comprobante")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            if (checkCameraPermission()) {
                                openCamera();
                            } else {
                                requestCameraPermission();
                            }
                            break;
                        case 1:
                            if (checkStoragePermission()) {
                                openGallery();
                            } else {
                                requestStoragePermission();
                            }
                            break;
                    }
                })
                .show();
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(getActivity(),
                new String[]{Manifest.permission.CAMERA},
                PERMISSION_REQUEST_CAMERA);
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(getActivity(),
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                PERMISSION_REQUEST_STORAGE);
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(TAG, "Error creando archivo de imagen", ex);
                showError("Error creando archivo de imagen");
                return;
            }

            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(getContext(),
                        "com.example.lab6_20206331.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST);
            }
        } else {
            showError("No hay aplicación de cámara disponible");
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "INGRESO_" + timeStamp + "_";

        File storageDir = getActivity().getExternalFilesDir("Pictures");
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );

        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSION_REQUEST_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera();
                } else {
                    showError("Permiso de cámara requerido");
                }
                break;

            case PERMISSION_REQUEST_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery();
                } else {
                    showError("Permiso de almacenamiento requerido");
                }
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == getActivity().RESULT_OK) {
            switch (requestCode) {
                case PICK_IMAGE_REQUEST:
                    if (data != null) {
                        selectedImageUri = data.getData();
                        displaySelectedImage();
                    }
                    break;

                case CAMERA_REQUEST:
                    selectedImageUri = photoUri;
                    displaySelectedImage();
                    break;
            }
        }
    }

    private void displaySelectedImage() {
        if (selectedImageUri != null && ivComprobantePreview != null) {
            ivComprobantePreview.setImageURI(selectedImageUri);
            ivComprobantePreview.setVisibility(View.VISIBLE);
            Log.d(TAG, "Imagen seleccionada: " + selectedImageUri.toString());
        }
    }

    private void addIngresoWithComprobante(String titulo, double monto, String descripcion, String fecha) {
        setLoading(true);

        String fileName = "ingreso_" + System.currentTimeMillis() + "_" + FirebaseUtil.getCurrentUserId();

        ServicioAlmacenamiento.guardarArchivo(getContext(), selectedImageUri, fileName,
                new ServicioAlmacenamiento.GuardarArchivoCallback() {
                    @Override
                    public void onSuccess(String urlArchivo, String publicId) {
                        Ingreso nuevoIngreso = new Ingreso(titulo, monto, descripcion, fecha);
                        nuevoIngreso.setUserId(FirebaseUtil.getCurrentUserId() != null ?
                                FirebaseUtil.getCurrentUserId() : "temp_user_dev");
                        nuevoIngreso.setComprobanteUrl(urlArchivo);
                        nuevoIngreso.setComprobantePublicId(publicId);
                        nuevoIngreso.setComprobanteNombre(fileName);

                        ingresoRepository.saveIngreso(nuevoIngreso, new IngresoRepository.OnIngresoSavedListener() {
                            @Override
                            public void onSuccess(String ingresoId) {
                                setLoading(false);
                                showInfo("Ingreso guardado correctamente");
                            }

                            @Override
                            public void onError(String error) {
                                setLoading(false);
                                showError("Error al guardar ingreso");
                                Log.e(TAG, "Error: " + error);
                            }
                        });
                    }

                    @Override
                    public void onError(String mensajeError) {
                        setLoading(false);
                        showError("Error subiendo comprobante");
                        Log.e(TAG, "Error: " + mensajeError);
                    }

                    @Override
                    public void onProgress(String mensaje) {
                        Log.d(TAG, "Progreso: " + mensaje);
                    }
                });
    }

    private void downloadComprobante(Ingreso ingreso) {
        if (!ingreso.hasComprobante()) {
            showError("Este ingreso no tiene comprobante");
            return;
        }

        setLoading(true);
        String fileName = ingreso.getComprobanteFileName();

        ServicioAlmacenamiento.descargarArchivo(getContext(), ingreso.getComprobanteUrl(), fileName,
                new ServicioAlmacenamiento.DescargarArchivoCallback() {
                    @Override
                    public void onSuccess(String rutaArchivo, java.io.File archivo) {
                        setLoading(false);
                        showInfo("Comprobante descargado");
                        Log.d(TAG, "Descargado en: " + rutaArchivo);
                    }

                    @Override
                    public void onError(String mensajeError) {
                        setLoading(false);
                        showError("Error descargando comprobante");
                        Log.e(TAG, "Error: " + mensajeError);
                    }

                    @Override
                    public void onProgress(String mensaje) {
                        Log.d(TAG, "Progreso: " + mensaje);
                    }
                });
    }

    private void showDatePicker(EditText etFecha) {
        Calendar calendar = Calendar.getInstance();

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

        setLoading(true);
        ingresoRepository.saveIngreso(ingreso, new IngresoRepository.OnIngresoSavedListener() {
            @Override
            public void onSuccess(String ingresoId) {
                setLoading(false);
                showInfo("Ingreso actualizado");
            }

            @Override
            public void onError(String error) {
                setLoading(false);
                showError("Error actualizando ingreso");
                Log.e(TAG, "Error: " + error);
            }
        });
    }

    private void deleteIngreso(Ingreso ingreso) {
        new AlertDialog.Builder(getContext())
                .setTitle("Eliminar Ingreso")
                .setMessage("¿Eliminar " + ingreso.getTitulo() + " del " + ingreso.getFecha() + "?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    setLoading(true);
                    ingresoRepository.deleteIngreso(ingreso.getId(), new IngresoRepository.OnIngresoDeletedListener() {
                        @Override
                        public void onSuccess() {
                            setLoading(false);
                            showInfo("Ingreso eliminado");
                        }

                        @Override
                        public void onError(String error) {
                            setLoading(false);
                            showError("Error eliminando ingreso");
                            Log.e(TAG, "Error: " + error);
                        }
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void setLoading(boolean loading) {
        isLoading = loading;
        if (fabAdd != null) {
            fabAdd.setEnabled(!loading);
        }
    }

    private void showError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), "❌ " + message, Toast.LENGTH_SHORT).show();
        }
    }

    private void showInfo(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
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