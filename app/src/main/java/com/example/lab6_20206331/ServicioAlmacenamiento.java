package com.example.lab6_20206331;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Servicio de almacenamiento en la nube para MoneyTracker
 * Gestiona la conexión, guardado, obtención y descarga de archivos usando Cloudinary
 */
public class ServicioAlmacenamiento {
    private static final String TAG = "ServicioAlmacenamiento";

    // ✅ CONFIGURACIÓN COMPLETA DE CLOUDINARY
    private static final String CLOUD_NAME = "dwqmdnqrx";
    private static final String API_KEY = "347514942719473";
    private static final String API_SECRET = "vHfSf56CTgMGXrcK1AeoKNXZgEk";
    private static final String UPLOAD_PRESET = "telehotel_unsigned"; // Preset unsigned

    private static boolean isConnected = false;
    private static ExecutorService downloadExecutor = Executors.newFixedThreadPool(3);

    /**
     * CONEXIÓN AL SERVICIO
     */
    public static boolean conectarServicio(Context context) {
        if (isConnected) {
            Log.d(TAG, "✅ Ya conectado al servicio de almacenamiento");
            return true;
        }

        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", CLOUD_NAME);
            config.put("api_key", API_KEY);
            config.put("api_secret", API_SECRET);
            config.put("secure", "true");

            MediaManager.init(context, config);
            isConnected = true;

            Log.d(TAG, "✅ Conexión establecida con el servicio de almacenamiento");
            Log.d(TAG, "Cloud Name: " + CLOUD_NAME);
            Log.d(TAG, "API Key: " + API_KEY.substring(0, 4) + "***");

            return true;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error estableciendo conexión con el servicio", e);
            isConnected = false;
            return false;
        }
    }

    /**
     * GUARDAR ARCHIVO
     */
    public static void guardarArchivo(Context context, Uri archivoUri, String nombreArchivo,
                                      GuardarArchivoCallback callback) {

        if (!isConnected) {
            Log.e(TAG, "❌ No hay conexión con el servicio de almacenamiento");
            if (callback != null) {
                callback.onError("No hay conexión con el servicio");
            }
            return;
        }

        try {
            // Configurar opciones de subida SIN preset (para evitar errores)
            Map<String, Object> options = new HashMap<>();
            options.put("public_id", nombreArchivo);
            options.put("folder", "moneytracker");
            options.put("resource_type", "auto");
            // NO usar upload_preset para evitar errores

            Log.d(TAG, "📤 Iniciando subida de archivo: " + nombreArchivo);

            MediaManager.get().upload(archivoUri)
                    .options(options)
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {
                            Log.d(TAG, "🚀 Subida iniciada - ID: " + requestId);
                            if (callback != null) {
                                callback.onProgress("Subida iniciada");
                            }
                        }

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {
                            int progress = (int) ((bytes * 100) / totalBytes);
                            Log.d(TAG, "📊 Progreso de subida: " + progress + "%");
                            if (callback != null) {
                                callback.onProgress("Subiendo: " + progress + "%");
                            }
                        }

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String urlArchivo = (String) resultData.get("secure_url");
                            String publicId = (String) resultData.get("public_id");

                            Log.d(TAG, "✅ Archivo subido exitosamente");
                            Log.d(TAG, "URL: " + urlArchivo);
                            Log.d(TAG, "Public ID: " + publicId);

                            if (callback != null) {
                                callback.onSuccess(urlArchivo, publicId);
                            }
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            String mensajeError = "Error subiendo archivo: " + error.getDescription();
                            Log.e(TAG, "❌ " + mensajeError);

                            if (callback != null) {
                                callback.onError(mensajeError);
                            }
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {
                            Log.w(TAG, "⏰ Subida reprogramada: " + error.getDescription());
                            if (callback != null) {
                                callback.onProgress("Reintentando subida...");
                            }
                        }
                    })
                    .dispatch();

        } catch (Exception e) {
            String mensajeError = "Error iniciando subida: " + e.getMessage();
            Log.e(TAG, "❌ " + mensajeError, e);

            if (callback != null) {
                callback.onError(mensajeError);
            }
        }
    }

    /**
     * FUNCIÓN ACTUALIZADA: DESCARGAR ARCHIVO A CARPETA PÚBLICA RECONOCIBLE
     */
    public static void descargarArchivo(Context context, String urlCloudinary, String nombreArchivo,
                                        DescargarArchivoCallback callback) {

        if (urlCloudinary == null || urlCloudinary.trim().isEmpty()) {
            Log.e(TAG, "❌ URL de archivo no válida");
            if (callback != null) {
                callback.onError("URL de archivo no válida");
            }
            return;
        }

        // Ejecutar descarga en hilo separado
        downloadExecutor.execute(() -> {
            try {
                Log.d(TAG, "📥 Iniciando descarga de: " + urlCloudinary);

                // Notificar inicio
                if (callback != null) {
                    callback.onProgress("Iniciando descarga...");
                }

                // Crear conexión HTTP
                URL url = new URL(urlCloudinary);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();

                // Verificar respuesta
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new IOException("Error HTTP: " + connection.getResponseCode());
                }

                // Obtener tamaño del archivo
                int fileLength = connection.getContentLength();
                InputStream input = connection.getInputStream();

                // NUEVA LÓGICA: Determinar donde guardar según la versión de Android
                String fileName = nombreArchivo + ".jpg";
                String rutaFinal = "";
                File outputFile = null;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+ (API 29+) - Usar MediaStore para acceso a Downloads públicos
                    rutaFinal = descargarConMediaStore(context, input, fileName, fileLength, callback);
                } else {
                    // Android 9 y anteriores - Usar carpeta Downloads tradicional
                    rutaFinal = descargarEnCarpetaTradicional(input, fileName, fileLength, callback);

                }

                // Cerrar conexión
                input.close();
                connection.disconnect();

                if (rutaFinal != null && !rutaFinal.isEmpty()) {
                    Log.d(TAG, "✅ Archivo descargado exitosamente en: " + rutaFinal);

                    // Notificar éxito en el hilo principal
                    if (callback != null) {
                        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                        String finalRutaFinal = rutaFinal;
                        String finalRutaFinal1 = rutaFinal;
                        mainHandler.post(() -> {
                            callback.onSuccess(finalRutaFinal, new File(finalRutaFinal1));
                        });
                    }
                } else {
                    throw new IOException("No se pudo determinar la ruta de descarga");
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ Error descargando archivo", e);

                // Notificar error en el hilo principal
                if (callback != null) {
                    android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                    mainHandler.post(() -> {
                        callback.onError("Error descargando: " + e.getMessage());
                    });
                }
            }
        });
    }

    /**
     * DESCARGA USANDO MEDIASTORE (Android 10+)
     */
    private static String descargarConMediaStore(Context context, InputStream input, String fileName,
                                                 int fileLength, DescargarArchivoCallback callback) {
        try {
            ContentResolver resolver = context.getContentResolver();

            // Configurar valores para MediaStore
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            contentValues.put(MediaStore.Downloads.MIME_TYPE, "image/jpeg");
            contentValues.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MoneyTracker/");

            // Insertar en MediaStore
            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);

            if (uri != null) {
                // Escribir archivo
                OutputStream output = resolver.openOutputStream(uri);
                if (output != null) {
                    copiarConProgreso(input, output, fileLength, callback);
                    output.close();

                    // Retornar ruta legible
                    return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            + "/MoneyTracker/" + fileName;
                }
            }

            return null;

        } catch (Exception e) {
            Log.e(TAG, "Error con MediaStore", e);
            return null;
        }
    }

    /**
     * DESCARGA EN CARPETA TRADICIONAL (Android 9 y anteriores)
     */
    private static String descargarEnCarpetaTradicional(InputStream input, String fileName,
                                                        int fileLength, DescargarArchivoCallback callback) {
        try {
            // Crear directorio MoneyTracker en Downloads
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File moneyTrackerDir = new File(downloadsDir, "MoneyTracker");

            if (!moneyTrackerDir.exists()) {
                boolean created = moneyTrackerDir.mkdirs();
                Log.d(TAG, "📁 Directorio MoneyTracker creado: " + created);
            }

            // Crear archivo de destino
            File outputFile = new File(moneyTrackerDir, fileName);
            FileOutputStream output = new FileOutputStream(outputFile);

            // Copiar con progreso
            copiarConProgreso(input, output, fileLength, callback);

            output.close();

            Log.d(TAG, "📁 Archivo guardado en: " + outputFile.getAbsolutePath());
            return outputFile.getAbsolutePath();

        } catch (Exception e) {
            Log.e(TAG, "Error en descarga tradicional", e);
            return null;
        }
    }

    /**
     * MÉTODO AUXILIAR: COPIAR CON PROGRESO
     */
    private static void copiarConProgreso(InputStream input, OutputStream output, int fileLength,
                                          DescargarArchivoCallback callback) throws IOException {
        byte[] buffer = new byte[4096];
        long total = 0;
        int count;

        // Descargar archivo con progreso
        while ((count = input.read(buffer)) != -1) {
            total += count;
            output.write(buffer, 0, count);

            // Calcular y notificar progreso
            if (fileLength > 0) {
                int progress = (int) ((total * 100) / fileLength);
                if (callback != null) {
                    callback.onProgress("Descargando: " + progress + "%");
                }
                Log.d(TAG, "📊 Progreso descarga: " + progress + "%");
            }
        }

        output.flush();
    }

    /**
     * OBTENER ARCHIVO (versión existente)
     */
    public static String obtenerArchivo(String publicId, Map<String, Object> transformaciones) {
        if (!isConnected) {
            Log.e(TAG, "❌ No hay conexión con el servicio de almacenamiento");
            return null;
        }

        if (publicId == null || publicId.trim().isEmpty()) {
            Log.e(TAG, "❌ Public ID no puede estar vacío");
            return null;
        }

        try {
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append("https://res.cloudinary.com/")
                    .append(CLOUD_NAME)
                    .append("/image/upload/");

            if (transformaciones != null && !transformaciones.isEmpty()) {
                StringBuilder transformParams = new StringBuilder();

                for (Map.Entry<String, Object> entry : transformaciones.entrySet()) {
                    if (transformParams.length() > 0) {
                        transformParams.append(",");
                    }
                    transformParams.append(entry.getKey()).append("_").append(entry.getValue());
                }

                urlBuilder.append(transformParams.toString()).append("/");
            }

            urlBuilder.append(publicId);

            String urlArchivo = urlBuilder.toString();
            Log.d(TAG, "📥 URL generada para archivo: " + urlArchivo);

            return urlArchivo;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error generando URL del archivo", e);
            return null;
        }
    }

    public static String obtenerArchivo(String publicId) {
        return obtenerArchivo(publicId, null);
    }

    // ========== MÉTODOS UTILITARIOS ==========

    public static boolean estaConectado() {
        return isConnected;
    }

    public static String getEstadoConexion() {
        if (isConnected) {
            return "Conectado al servicio de almacenamiento";
        } else {
            return "Sin conexión al servicio";
        }
    }

    public static void desconectar() {
        isConnected = false;
        if (downloadExecutor != null && !downloadExecutor.isShutdown()) {
            downloadExecutor.shutdown();
        }
        Log.d(TAG, "🔌 Desconectado del servicio de almacenamiento");
    }

    // ========== INTERFACES DE CALLBACK ==========

    /**
     * Interface para callbacks de guardado de archivos
     */
    public interface GuardarArchivoCallback {
        void onSuccess(String urlArchivo, String publicId);
        void onError(String mensajeError);
        void onProgress(String mensaje);
    }

    /**
     * Interface para callbacks de descarga de archivos
     */
    public interface DescargarArchivoCallback {
        void onSuccess(String rutaArchivo, File archivo);
        void onError(String mensajeError);
        void onProgress(String mensaje);
    }
}