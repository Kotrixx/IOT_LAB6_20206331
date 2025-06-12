package com.example.lab6_20206331;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

public class FirebaseUtil {

    private static final String TAG = "FirebaseUtil";

    private static FirebaseFirestore firestoreInstance;
    private static FirebaseAuth authInstance;
    private static FirebaseStorage storageInstance;

    private FirebaseUtil() {}

    // Firestore
    public static FirebaseFirestore getFirestore() {
        try {
            if (firestoreInstance == null) {
                firestoreInstance = FirebaseFirestore.getInstance();
                Log.d(TAG, "Firestore instance created");
            }
            return firestoreInstance;
        } catch (Exception e) {
            Log.e(TAG, "Error getting Firestore instance", e);
            return null;
        }
    }

    // Auth
    public static FirebaseAuth getAuth() {
        try {
            if (authInstance == null) {
                authInstance = FirebaseAuth.getInstance();
                Log.d(TAG, "Auth instance created");
            }
            return authInstance;
        } catch (Exception e) {
            Log.e(TAG, "Error getting Auth instance", e);
            return null;
        }
    }

    // Storage
    public static FirebaseStorage getStorage() {
        try {
            if (storageInstance == null) {
                storageInstance = FirebaseStorage.getInstance();
                Log.d(TAG, "Storage instance created");
            }
            return storageInstance;
        } catch (Exception e) {
            Log.e(TAG, "Error getting Storage instance", e);
            return null;
        }
    }

    // ID del usuario actual
    public static String getCurrentUserId() {
        FirebaseAuth auth = getAuth();
        if (auth != null && auth.getCurrentUser() != null) {
            return auth.getCurrentUser().getUid();
        }
        return null;
    }

    public static boolean isUserLoggedIn() {
        FirebaseAuth auth = getAuth();
        return auth != null && auth.getCurrentUser() != null;
    }

    // Rutas de base de datos
    public static class DatabasePaths {
        public static final String USERS = "users";
        public static final String INGRESOS = "ingresos";
        public static final String EGRESOS = "egresos";

        public static String getUserIngresosPath(String userId) {
            return USERS + "/" + userId + "/" + INGRESOS;
        }

        public static String getUserEgresosPath(String userId) {
            return USERS + "/" + userId + "/" + EGRESOS;
        }
    }
}
