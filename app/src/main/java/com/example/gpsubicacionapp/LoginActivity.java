package com.example.gpsubicacionapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {
    private EditText editTextEmail, editTextPassword;
    private Button btnLogin, btnGoToRegister;
    private SignInButton googleSignInButton;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        firebaseAuthWithGoogle(account);
                    } catch (ApiException e) {
                        Toast.makeText(this, "Error Google Sign-In: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("GoogleSignIn", "ApiException", e);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Inicializar vistas
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoToRegister = findViewById(R.id.btnGoToRegister);
        googleSignInButton = findViewById(R.id.googleSignInButton);

        // Inicializar FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        // Configurar Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Login con email y contraseña
        btnLogin.setOnClickListener(view -> loginWithEmail());

        // Ir a la pantalla de registro
        btnGoToRegister.setOnClickListener(view ->
                startActivity(new Intent(LoginActivity.this,RegistroActivity.class))
        );

        // Login con Google
        googleSignInButton.setOnClickListener(view -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });


    }

    private void loginWithEmail() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Ingrese email y contraseña", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Email inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult ->
                        goToWelcomeScreen(mAuth.getCurrentUser()))
                .addOnFailureListener(e -> {
                    String errorMessage = getFirebaseErrorMessage(e.getMessage());
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                });
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult ->
                        goToWelcomeScreen(mAuth.getCurrentUser()))
                .addOnFailureListener(e -> {
                    String errorMessage = getFirebaseErrorMessage(e.getMessage());
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                });
    }

    private void goToWelcomeScreen(FirebaseUser user) {
        if (user != null) {
            String email = user.getEmail();
            Toast.makeText(this, "Bienvenido " + email, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(LoginActivity.this,MainActivity.class);
            intent.putExtra("user_email", email);
            startActivity(intent);
            finish();
        }
    }

    // Método para traducir mensajes de error de Firebase al español
    private String getFirebaseErrorMessage(String error) {
        if (error == null) return "Error desconocido";

        if (error.contains("The password is invalid") || error.contains("There is no user record")) {
            return "Correo o contraseña incorrectos";
        } else if (error.contains("A network error")) {
            return "Error de red. Por favor, revisa tu conexión.";
        } else if (error.contains("The email address is badly formatted")) {
            return "Formato de correo inválido";
        } else if (error.contains("An internal error")) {
            return "Error interno. Inténtalo más tarde.";
        } else if (error.contains("User disabled")) {
            return "La cuenta ha sido deshabilitada";
        } else if (error.contains("User not found")) {
            return "Usuario no encontrado";
        } else {
            return "Error: " + error; // Mensaje original si no coincide ninguno
        }
    }
}



