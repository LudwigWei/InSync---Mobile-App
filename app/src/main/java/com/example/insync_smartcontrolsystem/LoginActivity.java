package com.example.insync_smartcontrolsystem;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.button.MaterialButton;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();

        EdgeToEdge.enable(this);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_page), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText loginUsername = findViewById(R.id.login_username_form);
        EditText loginPassword = findViewById(R.id.login_password_form);

        // Set up toggle password visibility button
        ImageView togglePasswordVisibility = findViewById(R.id.togglePasswordVisibility);
        togglePasswordVisibility.setOnClickListener(v -> {
            if (isPasswordVisible) {
                loginPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                togglePasswordVisibility.setImageResource(R.drawable.ic_eye_off);
            } else {
                loginPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                togglePasswordVisibility.setImageResource(R.drawable.ic_eye_on);
            }
            loginPassword.setSelection(loginPassword.getText().length());
            isPasswordVisible = !isPasswordVisible;
        });

        TextView forgotpwButton = findViewById(R.id.forgotpassword);
        forgotpwButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        TextView signupButton = findViewById(R.id.signuptextbutton);
        signupButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });

        MaterialButton loginBtn = findViewById(R.id.login_btn);
        loginBtn.setOnClickListener(v -> {
            String email = loginUsername.getText().toString().trim();
            String password = loginPassword.getText().toString().trim();

            // Validate input fields
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Sign in the user with Firebase
            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();

                            FirebaseUser user = auth.getCurrentUser();

                            // Check if the displayName is set in Firebase Authentication
                            final String[] username = {user != null ? user.getDisplayName() : null};

                            if (username[0] == null) {
                                // If username is not set in Firebase, fetch from Firestore
                                FirebaseFirestore db = FirebaseFirestore.getInstance();
                                assert user != null;
                                db.collection("users").document(user.getUid())
                                        .get()
                                        .addOnSuccessListener(documentSnapshot -> {
                                            // Retrieve username from Firestore
                                            username[0] = documentSnapshot.getString("username");
                                            if (username[0] == null) {
                                                username[0] = "Guest";  // Default username if not found
                                            }

                                            // Pass the username to the DashboardActivity
                                            Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                                            intent.putExtra("USERNAME", username[0]); // Pass username
                                            startActivity(intent);
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(LoginActivity.this, "Error fetching username", Toast.LENGTH_SHORT).show();
                                        });
                            } else {
                                // Pass the username to the DashboardActivity
                                Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                                intent.putExtra("USERNAME", username[0]); // Pass username
                                startActivity(intent);
                                finish();
                            }
                        } else {
                            // Login failed
                            Toast.makeText(LoginActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}
