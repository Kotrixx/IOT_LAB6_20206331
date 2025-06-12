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

    private EgresoRepository() {
        firestore = FirebaseUtil.getFirestore();
        if (firestore == null) {
            throw new IllegalStateException("Firestore no inicializado");
        }

        String userId = FirebaseUtil.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("Usuario no autenticado");
        }

        egresosRef = firestore
                .collection("egresos")
                .whereEqualTo("userId", userId)
                .getFirestore()
                .collection("egresos"); // apunta a /egresos directamente
    }

    public static EgresoRepository getInstance() {
        if (instance == null) {
            instance = new EgresoRepository();
        }
        return instance;
    }

    // Guardar egreso
    public void saveEgreso(Egreso egreso, OnEgresoSavedListener listener) {
        egresosRef.add(egreso)
                .addOnSuccessListener(documentReference -> {
                    String id = documentReference.getId();
                    egreso.setId(id);
                    listener.onSuccess(id);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error guardando egreso", e);
                    listener.onError(e.getMessage());
                });
    }

    // Obtener egresos en tiempo real
    public void getAllEgresos(OnEgresosLoadedListener listener) {
        snapshotListener = egresosRef
                .whereEqualTo("userId", FirebaseUtil.getCurrentUserId())
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error obteniendo egresos", error);
                        listener.onError(error.getMessage());
                        return;
                    }

                    List<Egreso> lista = new ArrayList<>();
                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Egreso egreso = doc.toObject(Egreso.class);
                            egreso.setId(doc.getId());
                            lista.add(egreso);
                        }
                    }

                    listener.onSuccess(lista);
                });
    }

    // Eliminar egreso
    public void deleteEgreso(String egresoId, OnEgresoDeletedListener listener) {
        egresosRef.document(egresoId)
                .delete()
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error eliminando egreso", e);
                    listener.onError(e.getMessage());
                });
    }

    // Detener listener
    public void cleanup() {
        if (snapshotListener != null) {
            snapshotListener.remove();
            snapshotListener = null;
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
