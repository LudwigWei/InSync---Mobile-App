package com.example.insync_smartcontrolsystem;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ProfileActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    
    private ImageView profileImage;
    private TextView profileName;
    private TextView profileEmail;
    private MaterialButton editProfileButton;
    private MaterialButton logoutButton;
    private BottomNavigationView bottomNavigationView;
    
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;
    private StorageReference storageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        EdgeToEdge.enable(this);

        // Hide the action bar if present
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Set padding for system bars (edge-to-edge effect)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profile), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase components
        mAuth = FirebaseAuth.getInstance();
        userRef = FirebaseDatabase.getInstance("https://insyncweb-default-rtdb.firebaseio.com/").getReference().child("users");
        storageRef = FirebaseStorage.getInstance().getReference();  // Initialize at root level

        // Initialize views
        profileImage = findViewById(R.id.profileImage);
        profileName = findViewById(R.id.profileName);
        profileEmail = findViewById(R.id.profileEmail);
        editProfileButton = findViewById(R.id.editprofile);
        logoutButton = findViewById(R.id.logout);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Set click listeners
        profileImage.setOnClickListener(v -> openImagePicker());
        profileName.setOnClickListener(v -> showChangeUsernameDialog());
        editProfileButton.setOnClickListener(v -> showChangeUsernameDialog());
        logoutButton.setOnClickListener(v -> showLogoutConfirmationDialog());

        // Load user data and setup navigation
        loadUserData();
        setupBottomNavigation();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            
            // Show loading state
            Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show();
            
            // Load image preview immediately
            Glide.with(this)
                .load(imageUri)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(profileImage);
            
            uploadImageToFirebase(imageUri);
        }
    }

    private void uploadImageToFirebase(Uri imageUri) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // Create a reference to "profile_images/[USER_ID].jpg"
            StorageReference fileRef = storageRef.child("profile_images").child(user.getUid() + ".jpg");
            
            fileRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();
                            userRef.child(user.getUid()).child("profileImage").setValue(imageUrl)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(ProfileActivity.this, "Profile image updated successfully", Toast.LENGTH_SHORT).show();
                                        loadProfileImage(imageUrl);
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Failed to update profile image URL: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        });
                    })
                    .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(ProfileActivity.this, "Error: User not logged in", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadProfileImage(String imageUrl) {
        Glide.with(this)
            .load(imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.ic_profile)
            .error(R.drawable.ic_profile)
            .into(profileImage);
    }

    private void showChangeUsernameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_change_username, null);
        EditText usernameInput = view.findViewById(R.id.username_input);
        usernameInput.setText(profileName.getText());

        builder.setView(view)
                .setTitle("Change Username")
                .setPositiveButton("Save", (dialog, which) -> {
                    String newUsername = usernameInput.getText().toString().trim();
                    if (!newUsername.isEmpty()) {
                        updateUsername(newUsername);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateUsername(String newUsername) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            userRef.child(user.getUid()).child("username").setValue(newUsername)
                    .addOnSuccessListener(aVoid -> {
                        profileName.setText(newUsername);
                        Toast.makeText(ProfileActivity.this, "Username updated successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Failed to update username", Toast.LENGTH_SHORT).show());
        }
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Set email
            profileEmail.setText(currentUser.getEmail());

            // Load username and profile image
            userRef.child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String username = snapshot.child("username").getValue(String.class);
                        String profileImageUrl = snapshot.child("profileImage").getValue(String.class);

                        if (username != null && !username.isEmpty()) {
                            profileName.setText(username);
                        }

                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            loadProfileImage(profileImageUrl);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(ProfileActivity.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    mAuth.signOut();
                    Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_profile);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_statistics) {
                startActivity(new Intent(this, StatisticsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_learning) {
                startActivity(new Intent(this, LearningActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                return true;
            }
            return false;
        });
    }
}
