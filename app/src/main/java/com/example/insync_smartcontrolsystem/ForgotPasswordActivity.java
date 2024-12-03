package com.example.insync_smartcontrolsystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private FirebaseAuth mAuth; // FirebaseAuth instance
    private EditText emailEditText; // EditText for the email input

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_forgotpassword);

        // Initialize FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        // Set up the layout with padding for system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.forgotpw), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize the email input field and button
        emailEditText = findViewById(R.id.login_username_form);
        MaterialButton resetpwBtn = findViewById(R.id.forgotpw_btn);

        // Set up the button click listener to send the reset email
        resetpwBtn.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();

            // Check if the email field is not empty
            if (!email.isEmpty()) {
                sendPasswordResetEmail(email);
            } else {
                Toast.makeText(ForgotPasswordActivity.this, "Please enter your email address.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Method to send a password reset email
    private void sendPasswordResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Show success message
                        Toast.makeText(ForgotPasswordActivity.this, "Password reset email sent!", Toast.LENGTH_SHORT).show();
                        // Redirect to login page
                        Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        // Show error message
                        Toast.makeText(ForgotPasswordActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
