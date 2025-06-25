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
import com.example.lab6_20206331.adapters.EgresosAdapter;
import com.example.lab6_20206331.FirebaseUtil;
import com.example.lab6_20206331.models.Egreso;
import com.example.lab6_20206331.repository.EgresoRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EgresosFragment extends Fragment {

    private static final String TAG = "EgresosFragment";
    private static final int PICK_IMAGE_REQUEST = 2001;
    private static final int CAMERA_REQUEST = 2002;
    private static final int PERMISSION_REQUEST_CAMERA = 200;
    private static final int PERMISSION_REQUEST_STORAGE = 201;

    private RecyclerView recyclerView;
    private FloatingActionButton fabAdd;
    private EgresosAdapter adapter;
    private List<Egreso> egresosList;

    private EgresoRepository egresoRepository;
    private String selectedDate = "";
    private boolean isLoading = false; // NUEVO: Control de estado de carga

    // Variables para manejo de imágenes
    private Uri selectedImageUri;
    private Uri photoUri;
    private ImageView ivComprobantePreview;
    private View dialogAddEgreso;
    private String currentPhotoPath;

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
        fabAdd.setOnClickListener(v -> showAddEgresoDialog());
    }

    private void setupRecyclerView() {
        egresosList = new ArrayList<>();
        adapter = new EgresosAdapter(egresosList,
                this::editEgreso,
                this::deleteEgreso,
                this::downloadComprobante);
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
        Log.d(TAG, connected ? "✅ Servicio conectado" : "❌ Error conectando servicio");
    }

    private void loadFirebaseData() {
        if (isLoading) return; // NUEVO: Evitar cargas múltiples

        setLoading(true);
        egresoRepository.getAllEgresos(new EgresoRepository.OnEgresosLoadedListener() {
            @Override
            public void onSuccess(List<Egreso> egresos) {
                setLoading(false); // ARREGLADO: Siempre desactivar loading
                egresosList.clear();
                egresosList.addAll(egresos);
                adapter.notifyDataSetChanged();

                // REDUCIDO: Solo mostrar si está vacío
                if (egresos.isEmpty()) {
                    showInfo("No hay egresos registrados");
                }
            }

            @Override
            public void onError(String error) {
                setLoading(false); // ARREGLADO: Siempre desactivar loading
                showError("Error cargando egresos");
                Log.e(TAG, "Error: " + error);
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

        etFecha.setOnClickListener(v -> showDatePicker(etFecha));
        btnSeleccionarImagen.setOnClickListener(v -> showImageSourceDialog());

        // Reset
        selectedImageUri = null;
        photoUri = null;
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
        String imageFileName = "EGRESO_" + timeStamp + "_";

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
            // REMOVIDO: Toast innecesario
            Log.d(TAG, "Imagen seleccionada: " + selectedImageUri.toString());
        }
    }

    private void addEgresoWithComprobante(String titulo, double monto, String descripcion, String fecha) {
        setLoading(true);

        String fileName = "egreso_" + System.currentTimeMillis() + "_" + FirebaseUtil.getCurrentUserId();

        ServicioAlmacenamiento.guardarArchivo(getContext(), selectedImageUri, fileName,
                new ServicioAlmacenamiento.GuardarArchivoCallback() {
                    @Override
                    public void onSuccess(String urlArchivo, String publicId) {
                        Egreso nuevoEgreso = new Egreso(titulo, monto, descripcion, fecha);
                        nuevoEgreso.setUserId(FirebaseUtil.getCurrentUserId() != null ?
                                FirebaseUtil.getCurrentUserId() : "temp_user_dev");
                        nuevoEgreso.setComprobanteUrl(urlArchivo);
                        nuevoEgreso.setComprobantePublicId(publicId);
                        nuevoEgreso.setComprobanteNombre(fileName);

                        egresoRepository.saveEgreso(nuevoEgreso, new EgresoRepository.OnEgresoSavedListener() {
                            @Override
                            public void onSuccess(String egresoId) {
                                setLoading(false);
                                showInfo("Egreso guardado correctamente");
                            }

                            @Override
                            public void onError(String error) {
                                setLoading(false);
                                showError("Error al guardar egreso");
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
                        // REMOVIDO: Toast de progreso
                        Log.d(TAG, "Progreso: " + mensaje);
                    }
                });
    }

    private void downloadComprobante(Egreso egreso) {
        if (!egreso.hasComprobante()) {
            showError("Este egreso no tiene comprobante");
            return;
        }

        setLoading(true);
        String fileName = egreso.getComprobanteFileName();

        ServicioAlmacenamiento.descargarArchivo(getContext(), egreso.getComprobanteUrl(), fileName,
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
                        // REMOVIDO: Toast de progreso
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

    private void editEgreso(Egreso egreso) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_egreso, null);
        EditText etMonto = dialogView.findViewById(R.id.et_monto);
        EditText etDescripcion = dialogView.findViewById(R.id.et_descripcion);

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

        setLoading(true);
        egresoRepository.saveEgreso(egreso, new EgresoRepository.OnEgresoSavedListener() {
            @Override
            public void onSuccess(String egresoId) {
                setLoading(false);
                showInfo("Egreso actualizado");
            }

            @Override
            public void onError(String error) {
                setLoading(false);
                showError("Error actualizando egreso");
                Log.e(TAG, "Error: " + error);
            }
        });
    }

    private void deleteEgreso(Egreso egreso) {
        new AlertDialog.Builder(getContext())
                .setTitle("Eliminar Egreso")
                .setMessage("¿Eliminar " + egreso.getTitulo() + " del " + egreso.getFecha() + "?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    setLoading(true);
                    egresoRepository.deleteEgreso(egreso.getId(), new EgresoRepository.OnEgresoDeletedListener() {
                        @Override
                        public void onSuccess() {
                            setLoading(false);
                            showInfo("Egreso eliminado");
                        }

                        @Override
                        public void onError(String error) {
                            setLoading(false);
                            showError("Error eliminando egreso");
                            Log.e(TAG, "Error: " + error);
                        }
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // NUEVO: Método centralizado para manejar estado de carga
    private void setLoading(boolean loading) {
        isLoading = loading;
        if (fabAdd != null) {
            fabAdd.setEnabled(!loading);
        }
        // REMOVIDO: ProgressBar que causaba problemas
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
        if (egresoRepository != null) {
            egresoRepository.cleanup();
        }
    }
}