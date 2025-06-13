// EgresoRepository.java - CORRECCIÓN COMPLETA

package com.example.lab6_20206331.repository;

import android.util.Log;

import com.example.lab6_20206331.FirebaseUtil;
import com.example.lab6_20206331.models.Egreso;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class EgresoRepository {

    private static final String TAG = "EgresoRepository";
    private static EgresoRepository instance;

    private final FirebaseFirestore firestore;
    private final CollectionReference egresosRef;
    private ListenerRegistration snapshotListener;
    private final String userId;

    private EgresoRepository() {
        firestore = FirebaseUtil.getFirestore();
        if (firestore == null) {
            throw new IllegalStateException("Firestore no inicializado");
        }

        userId = FirebaseUtil.getCurrentUserId();
        if (userId == null) {
            Log.w(TAG, "Usuario no autenticado, usando ID temporal");
        }

        // CORRECCIÓN: Usar la estructura correcta de Firestore
        // OPCIÓN A: Colección global con filtro por userId
        egresosRef = firestore.collection("egresos");

        // OPCIÓN B: Subcolección por usuario (comentada)
        // egresosRef = firestore.collection("users").document(userId).collection("egresos");

        Log.d(TAG, "EgresoRepository inicializado para usuario: " + userId);
    }

    public static EgresoRepository getInstance() {
        if (instance == null) {
            try {
                instance = new EgresoRepository();
            } catch (Exception e) {
                Log.e(TAG, "Error creando EgresoRepository", e);
                return null;
            }
        }
        return instance;
    }

    // Guardar egreso
    public void saveEgreso(Egreso egreso, OnEgresoSavedListener listener) {
        try {
            // Asegurar que el egreso tenga userId
            if (egreso.getUserId() == null || egreso.getUserId().isEmpty()) {
                egreso.setUserId(userId != null ? userId : "temp_user");
            }

            Log.d(TAG, "Guardando egreso para usuario: " + egreso.getUserId());

            egresosRef.add(egreso)
                    .addOnSuccessListener(documentReference -> {
                        String id = documentReference.getId();
                        egreso.setId(id);
                        Log.d(TAG, "Egreso guardado con ID: " + id);
                        listener.onSuccess(id);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error guardando egreso", e);
                        listener.onError("Error al guardar: " + e.getMessage());
                    });

        } catch (Exception e) {
            Log.e(TAG, "Excepción al guardar egreso", e);
            listener.onError("Error interno: " + e.getMessage());
        }
    }

    // Obtener egresos en tiempo real
    public void getAllEgresos(OnEgresosLoadedListener listener) {
        try {
            String currentUserId = userId != null ? userId : "temp_user";
            Log.d(TAG, "Cargando egresos para usuario: " + currentUserId);

            snapshotListener = egresosRef
                    .whereEqualTo("userId", currentUserId)
                    // TEMPORALMENTE SIN orderBy HASTA QUE SE CREE EL ÍNDICE
                    // .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .addSnapshotListener((snapshots, error) -> {
                        if (error != null) {
                            Log.e(TAG, "Error obteniendo egresos", error);

                            // Mensaje de error más específico
                            String errorMsg = error.getMessage();
                            if (errorMsg != null && errorMsg.contains("requires an index")) {
                                listener.onError("Creando índice de Firebase. Inténtalo en 2-3 minutos.");
                            } else if (errorMsg != null && errorMsg.contains("failed-precondition")) {
                                listener.onError("Error de configuración de Firebase. Verifica las reglas de Firestore.");
                            } else if (errorMsg != null && errorMsg.contains("permission-denied")) {
                                listener.onError("Sin permisos. Usuario debe estar autenticado.");
                            } else {
                                listener.onError("Error de conexión: " + errorMsg);
                            }
                            return;
                        }

                        List<Egreso> lista = new ArrayList<>();
                        if (snapshots != null) {
                            for (QueryDocumentSnapshot doc : snapshots) {
                                try {
                                    Egreso egreso = doc.toObject(Egreso.class);
                                    egreso.setId(doc.getId());
                                    lista.add(egreso);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parseando egreso: " + doc.getId(), e);
                                }
                            }
                        }

                        // ORDENAR MANUALMENTE POR TIMESTAMP (TEMPORALMENTE)
                        lista.sort((e1, e2) -> Long.compare(e2.getTimestamp(), e1.getTimestamp()));

                        Log.d(TAG, "Egresos cargados: " + lista.size());
                        listener.onSuccess(lista);
                    });

        } catch (Exception e) {
            Log.e(TAG, "Excepción al cargar egresos", e);
            listener.onError("Error interno al cargar egresos: " + e.getMessage());
        }
    }

    // Eliminar egreso
    public void deleteEgreso(String egresoId, OnEgresoDeletedListener listener) {
        try {
            if (egresoId == null || egresoId.isEmpty()) {
                listener.onError("ID de egreso inválido");
                return;
            }

            Log.d(TAG, "Eliminando egreso: " + egresoId);

            egresosRef.document(egresoId)
                    .delete()
                    .addOnSuccessListener(unused -> {
                        Log.d(TAG, "Egreso eliminado: " + egresoId);
                        listener.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error eliminando egreso", e);
                        listener.onError("Error al eliminar: " + e.getMessage());
                    });

        } catch (Exception e) {
            Log.e(TAG, "Excepción al eliminar egreso", e);
            listener.onError("Error interno al eliminar: " + e.getMessage());
        }
    }

    // Detener listener
    public void cleanup() {
        if (snapshotListener != null) {
            snapshotListener.remove();
            snapshotListener = null;
            Log.d(TAG, "Snapshot listener removido");
        }
    }

    // Callbacks
    public interface OnEgresoSavedListener {
        void onSuccess(String egresoId);
        void onError(String error);
    }

    public interface OnEgresosLoadedListener {
        void onSuccess(List<Egreso> egresos);
        void onError(String error);
    }

    public interface OnEgresoDeletedListener {
        void onSuccess();
        void onError(String error);
    }
}