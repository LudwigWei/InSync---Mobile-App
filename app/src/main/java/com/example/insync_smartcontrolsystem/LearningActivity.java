package com.example.insync_smartcontrolsystem;

import android.content.Intent;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class LearningActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_learning);

        // Hide the action bar if present
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Initialize views
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Set up bottom navigation
        setupBottomNavigation();

        // Set window insets padding for edge-to-edge effect
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.learning), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_learning);
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
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }
}
