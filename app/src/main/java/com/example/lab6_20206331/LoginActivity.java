package com.example.lab6_20206331;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.lab6_20206331.FirebaseUtil;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final int RC_SIGN_IN = 9001;

    // FLAG PARA DESARROLLO - CAMBIAR A false PARA LOGIN REAL
    private static final boolean BYPASS_LOGIN = false; // Cambiado a false para usar login real

    // Firebase
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private CallbackManager mCallbackManager;

    // Views
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnRegister, btnGoogleSignin;
    private LoginButton btnFacebookLogin;
    private TextView tvTitle, tvSwitchMode;
    private ProgressBar progressBar;

    // States
    private boolean isLoginMode = true; // true = login, false = register

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (BYPASS_LOGIN) {
            setupBypassMode();
        } else {
            initializeFirebase();
            initViews();
            setupClickListeners();
            configureGoogleSignIn();
            configureFacebookSignIn();
        }
    }

    private void initializeFirebase() {
        try {
            mAuth = FirebaseAuth.getInstance();
            Log.d(TAG, "Firebase Auth inicializado correctamente");
        } catch (Exception e) {
            Log.e(TAG, "Error inicializando Firebase Auth", e);
            showError("Error inicializando Firebase");
        }
    }

    private void initViews() {
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnRegister = findViewById(R.id.btn_register);
        btnGoogleSignin = findViewById(R.id.btn_google_signin);
        btnFacebookLogin = findViewById(R.id.btn_facebook_login);
        tvTitle = findViewById(R.id.tv_title);
        tvSwitchMode = findViewById(R.id.tv_switch_mode);
        progressBar = findViewById(R.id.progress_bar);

        updateUIForMode();
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> {
            if (isLoginMode) {
                signInWithEmail();
            } else {
                registerWithEmail();
            }
        });

        btnRegister.setOnClickListener(v -> toggleMode());
        tvSwitchMode.setOnClickListener(v -> toggleMode());
        btnGoogleSignin.setOnClickListener(v -> signInWithGoogle());
    }

    private void toggleMode() {
        isLoginMode = !isLoginMode;
        updateUIForMode();
    }

    private void updateUIForMode() {
        if (isLoginMode) {
            // Modo Login
            tvTitle.setText("Iniciar Sesión");
            btnLogin.setText("Iniciar Sesión");
            btnRegister.setText("¿No tienes cuenta? Regístrate");
            tvSwitchMode.setText("¿Olvidaste tu contraseña?");
        } else {
            // Modo Registro
            tvTitle.setText("Crear Cuenta");
            btnLogin.setText("Registrarse");
            btnRegister.setText("¿Ya tienes cuenta? Inicia sesión");
            tvSwitchMode.setText("Al registrarte aceptas nuestros términos");
        }
    }

    private void configureGoogleSignIn() {
        try {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();

            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
            Log.d(TAG, "Google Sign In configurado correctamente");
        } catch (Exception e) {
            Log.e(TAG, "Error configurando Google Sign In", e);
            showError("Error configurando Google Sign In");
        }
    }

    private void configureFacebookSignIn() {
        try {
            mCallbackManager = CallbackManager.Factory.create();

            btnFacebookLogin.setReadPermissions("email", "public_profile");
            btnFacebookLogin.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
                @Override
                public void onSuccess(LoginResult loginResult) {
                    Log.d(TAG, "Facebook login exitoso");
                    handleFacebookAccessToken(loginResult.getAccessToken());
                }

                @Override
                public void onCancel() {
                    Log.d(TAG, "Facebook login cancelado");
                    showInfo("Inicio de sesión con Facebook cancelado");
                }

                @Override
                public void onError(FacebookException error) {
                    Log.e(TAG, "Error en Facebook login", error);
                    showError("Error en Facebook: " + error.getMessage());
                }
            });
            Log.d(TAG, "Facebook Sign In configurado correctamente");
        } catch (Exception e) {
            Log.e(TAG, "Error configurando Facebook Sign In", e);
        }
    }

    private void signInWithEmail() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!validateInput(email, password)) return;

        showProgress(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        showProgress(false);
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Inicio de sesión exitoso");
                            FirebaseUser user = mAuth.getCurrentUser();
                            showSuccess("¡Bienvenido de vuelta!");
                            updateUI(user);
                        } else {
                            Log.w(TAG, "Error en inicio de sesión", task.getException());
                            showError("Error: " + getErrorMessage(task.getException()));
                        }
                    }
                });
    }

    private void registerWithEmail() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!validateInput(email, password)) return;

        if (password.length() < 6) {
            showError("La contraseña debe tener al menos 6 caracteres");
            return;
        }

        showProgress(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        showProgress(false);
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Registro exitoso");
                            FirebaseUser user = mAuth.getCurrentUser();
                            showSuccess("¡Cuenta creada exitosamente!");
                            updateUI(user);
                        } else {
                            Log.w(TAG, "Error en registro", task.getException());
                            showError("Error en registro: " + getErrorMessage(task.getException()));
                        }
                    }
                });
    }

    private void signInWithGoogle() {
        showProgress(true);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "Google Sign In exitoso: " + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.w(TAG, "Google Sign In falló", e);
                showProgress(false);
                showError("Error en Google Sign In");
            }
        }

        // Facebook callback
        if (mCallbackManager != null) {
            mCallbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        showProgress(false);
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Autenticación con Google exitosa");
                            FirebaseUser user = mAuth.getCurrentUser();
                            showSuccess("¡Bienvenido!");
                            updateUI(user);
                        } else {
                            Log.w(TAG, "Error en autenticación con Google", task.getException());
                            showError("Error de autenticación con Google");
                        }
                    }
                });
    }

    private void handleFacebookAccessToken(AccessToken token) {
        Log.d(TAG, "Manejando token de Facebook");

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        showProgress(false);
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Autenticación con Facebook exitosa");
                            FirebaseUser user = mAuth.getCurrentUser();
                            showSuccess("¡Bienvenido!");
                            updateUI(user);
                        } else {
                            Log.w(TAG, "Error en autenticación con Facebook", task.getException());
                            showError("Error de autenticación con Facebook");
                        }
                    }
                });
    }

    public void onFacebookButtonClick(View view) {
        // Método llamado desde el layout para el botón personalizado de Facebook
        showProgress(true);
        btnFacebookLogin.performClick();
    }

    private boolean validateInput(String email, String password) {
        if (email.isEmpty()) {
            etEmail.setError("El correo es obligatorio");
            etEmail.requestFocus();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Ingresa un correo válido");
            etEmail.requestFocus();
            return false;
        }

        if (password.isEmpty()) {
            etPassword.setError("La contraseña es obligatoria");
            etPassword.requestFocus();
            return false;
        }

        return true;
    }

    private String getErrorMessage(Exception exception) {
        if (exception == null) return "Error desconocido";

        String message = exception.getMessage();
        if (message == null) return "Error desconocido";

        // Traducir mensajes comunes de Firebase
        if (message.contains("user-not-found")) {
            return "No existe una cuenta con este correo";
        } else if (message.contains("wrong-password")) {
            return "Contraseña incorrecta";
        } else if (message.contains("email-already-in-use")) {
            return "Este correo ya está registrado";
        } else if (message.contains("weak-password")) {
            return "La contraseña es muy débil";
        } else if (message.contains("invalid-email")) {
            return "Correo electrónico inválido";
        } else if (message.contains("network-request-failed")) {
            return "Error de conexión. Verifica tu internet";
        }

        return message;
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
        btnRegister.setEnabled(!show);
        btnGoogleSignin.setEnabled(!show);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showInfo(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.putExtra("user_id", user.getUid());
            intent.putExtra("user_email", user.getEmail());
            intent.putExtra("user_name", user.getDisplayName());
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!BYPASS_LOGIN) {
            // Verificar si el usuario ya está logueado
            FirebaseUser currentUser = mAuth != null ? mAuth.getCurrentUser() : null;
            if (currentUser != null) {
                Log.d(TAG, "Usuario ya logueado: " + currentUser.getEmail());
                updateUI(currentUser);
            }
        }
    }

    // ================ MODO BYPASS PARA DESARROLLO ================

    private void setupBypassMode() {
        hideLoginElements();

        MaterialButton btnBypass = findViewById(R.id.btn_login);
        btnBypass.setText("BYPASS LOGIN (DESARROLLO)");
        btnBypass.setOnClickListener(v -> bypassToMainActivity());

        Toast.makeText(this, "MODO DESARROLLO: Login bypassed", Toast.LENGTH_SHORT).show();
    }

    private void hideLoginElements() {
        View etEmail = findViewById(R.id.et_email);
        View etPassword = findViewById(R.id.et_password);
        View btnRegister = findViewById(R.id.btn_register);
        View btnGoogleSignin = findViewById(R.id.btn_google_signin);
        View btnFacebookLogin = findViewById(R.id.btn_facebook_login);

        if (etEmail != null) etEmail.setVisibility(View.GONE);
        if (etPassword != null) etPassword.setVisibility(View.GONE);
        if (btnRegister != null) btnRegister.setVisibility(View.GONE);
        if (btnGoogleSignin != null) btnGoogleSignin.setVisibility(View.GONE);
        if (btnFacebookLogin != null) btnFacebookLogin.setVisibility(View.GONE);
    }

    private void bypassToMainActivity() {
        if (mAuth == null) {
            mAuth = FirebaseAuth.getInstance();
        }

        mAuth.signInAnonymously()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra("user_id", mAuth.getCurrentUser().getUid());
                        intent.putExtra("user_email", "anon@test.com");
                        intent.putExtra("user_name", "Usuario de Desarrollo");
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "Error al simular login", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}