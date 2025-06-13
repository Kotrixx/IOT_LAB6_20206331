package com.example.lab6_20206331.repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.lab6_20206331.FirebaseUtil;
import com.example.lab6_20206331.models.Ingreso;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class IngresoRepository {

    private static final String TAG = "IngresoRepository";
    private static IngresoRepository instance;

    private final FirebaseFirestore firestore;
    private final CollectionReference ingresosRef;
    private ListenerRegistration snapshotListener;

    private IngresoRepository() {
        firestore = FirebaseUtil.getFirestore();
        if (firestore == null) {
            throw new IllegalStateException("Firestore no inicializado");
        }

        String userId = FirebaseUtil.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("Usuario no autenticado");
        }

        ingresosRef = firestore
                .collection("users")
                .document(userId)
                .collection("ingresos");
    }

    public static IngresoRepository getInstance() {
        if (instance == null) {
            instance = new IngresoRepository();
        }
        return instance;
    }

    // Agregar un ingreso
    public void saveIngreso(Ingreso ingreso, OnIngresoSavedListener listener) {
        ingresosRef.add(ingreso)
                .addOnSuccessListener(documentReference -> {
                    String id = documentReference.getId();
                    ingreso.setId(id);
                    listener.onSuccess(id);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error guardando ingreso", e);
                    listener.onError(e.getMessage());
                });
    }

    // Obtener todos los ingresos
    public void getAllIngresos(OnIngresosLoadedListener listener) {
        snapshotListener = ingresosRef
                // TEMPORALMENTE SIN orderBy HASTA QUE SE CREE EL ÍNDICE
                // .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error obteniendo ingresos", error);

                        // Mensaje de error más específico
                        String errorMsg = error.getMessage();
                        if (errorMsg != null && errorMsg.contains("requires an index")) {
                            listener.onError("Creando índice de Firebase. Inténtalo en 2-3 minutos.");
                        } else {
                            listener.onError(error.getMessage());
                        }
                        return;
                    }

                    List<Ingreso> lista = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Ingreso ingreso = doc.toObject(Ingreso.class);
                            ingreso.setId(doc.getId());
                            lista.add(ingreso);
                        }
                    }

                    // ORDENAR MANUALMENTE POR TIMESTAMP (TEMPORALMENTE)
                    lista.sort((i1, i2) -> Long.compare(i2.getTimestamp(), i1.getTimestamp()));

                    listener.onSuccess(lista);
                });
    }

    // Eliminar ingreso
    public void deleteIngreso(String ingresoId, OnIngresoDeletedListener listener) {
        ingresosRef.document(ingresoId)
                .delete()
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error eliminando ingreso", e);
                    listener.onError(e.getMessage());
                });
    }

    // Detener escuchas activas
    public void cleanup() {
        if (snapshotListener != null) {
            snapshotListener.remove();
        }
    }

    // Interfaces de callback
    public interface OnIngresoSavedListener {
        void onSuccess(String ingresoId);
        void onError(String error);
    }

    public interface OnIngresosLoadedListener {
        void onSuccess(List<Ingreso> ingresos);
        void onError(String error);
    }

    public interface OnIngresoDeletedListener {
        void onSuccess();
        void onError(String error);
    }
}
