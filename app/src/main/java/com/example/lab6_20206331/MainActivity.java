package com.example.lab6_20206331;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.lab6_20206331.fragments.EgresosFragment;
import com.example.lab6_20206331.fragments.IngresosFragment;
import com.example.lab6_20206331.fragments.ResumenFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // User data
    private String currentUserId;
    private String currentUserEmail;
    private String currentUserName;

    // Firebase
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "=== INICIANDO MAIN ACTIVITY ===");

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();

        // Get user data from intent
        getUserDataFromIntent();

        // Setup navigation
        setupBottomNavigation();

        // Load initial fragment
        if (savedInstanceState == null) {
            loadFragment(new IngresosFragment());
        }

        // Welcome message
        showWelcomeMessage();
    }

    private void getUserDataFromIntent() {
        Intent intent = getIntent();
        currentUserId = intent.getStringExtra("user_id");
        currentUserEmail = intent.getStringExtra("user_email");
        currentUserName = intent.getStringExtra("user_name");

        // If no data from intent, try to get from current Firebase user
        if (currentUserId == null) {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                currentUserId = user.getUid();
                currentUserEmail = user.getEmail();
                currentUserName = user.getDisplayName();
            }
        }

        Log.d(TAG, "Usuario: " + currentUserEmail);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;

                int itemId = item.getItemId();
                if (itemId == R.id.nav_ingresos) {
                    selectedFragment = new IngresosFragment();
                    Log.d(TAG, "Navegando a Ingresos");
                } else if (itemId == R.id.nav_egresos) {
                    selectedFragment = new EgresosFragment();
                    Log.d(TAG, "Navegando a Egresos");
                } else if (itemId == R.id.nav_resumen) {
                    selectedFragment = new ResumenFragment();
                    Log.d(TAG, "Navegando a Resumen");
                } else if (itemId == R.id.nav_logout) {
                    showLogoutConfirmation();
                    return true;
                }

                if (selectedFragment != null) {
                    return loadFragment(selectedFragment);
                }
                return false;
            }
        });
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            Log.d(TAG, "Cargando fragment: " + fragment.getClass().getSimpleName());
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }

    private void showWelcomeMessage() {
        String welcomeMessage = "¡Bienvenido!";

        if (currentUserEmail != null && !currentUserEmail.equals("anon@test.com")) {
            welcomeMessage = "¡Bienvenido, " +
                    (currentUserName != null ? currentUserName : currentUserEmail) + "!";
        }

        Toast.makeText(this, welcomeMessage, Toast.LENGTH_SHORT).show();
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Cerrar Sesión")
                .setMessage("¿Estás seguro de que quieres cerrar sesión?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Cerrar Sesión", (dialog, which) -> logout())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void logout() {
        Log.d(TAG, "Cerrando sesión...");

        try {
            // Sign out from Firebase
            if (mAuth != null) {
                mAuth.signOut();
            }

            // Clear user data
            currentUserId = null;
            currentUserEmail = null;
            currentUserName = null;

            // Show message
            Toast.makeText(this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show();

            // Return to login
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();

        } catch (Exception e) {
            Log.e(TAG, "Error al cerrar sesión", e);
            Toast.makeText(this, "Error al cerrar sesión", Toast.LENGTH_SHORT).show();
        }
    }

    // Getter methods for fragments to access user data
    public String getCurrentUserId() {
        return currentUserId;
    }

    public String getCurrentUserEmail() {
        return currentUserEmail;
    }

    public String getCurrentUserName() {
        return currentUserName;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "MainActivity onResume");

        // Verify user is still authenticated
        if (mAuth != null && mAuth.getCurrentUser() == null && currentUserId != null) {
            // User was logged out externally, return to login
            Log.w(TAG, "Usuario desautenticado, regresando al login");
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        // Show confirmation before exiting the app
        new AlertDialog.Builder(this)
                .setTitle("Salir de la aplicación")
                .setMessage("¿Quieres salir de MoneyTracker?")
                .setPositiveButton("Salir", (dialog, which) -> super.onBackPressed())
                .setNegativeButton("Cancelar", null)
                .show();
    }
}