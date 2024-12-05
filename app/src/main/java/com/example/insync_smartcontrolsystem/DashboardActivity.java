package com.example.insync_smartcontrolsystem;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import android.content.res.ColorStateList;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private DatabaseReference databaseRef;

    private TextView currentTimeTextView;

    private BottomNavigationView bottomNavigationView;
    private TextView roomNameTextView;
    private ImageView roomImageView;
    private TextView profileNameTextView;

    private MaterialButton selectedButton = null; // Track the selected button

    // Color values for selected and unselected buttons
    private static final int SELECTED_COLOR = 0xFF202020;   // Active color
    private static final int UNSELECTED_COLOR = 0xFF5A5A5A; // Inactive color
    private static final int COLOR_ON = 0xFF2196F3;         // Blue
    private static final int COLOR_OFF = 0xFFFFFFFF;        // White

    private MaterialCardView device1Card, device2Card, device3Card, device4Card;

    // TextView references for sensor data
    private TextView soundDataValue, temperatureDataValue, humidityDataValue;
    private MaterialCardView sensorDataCard;

    // Firebase Realtime Database references
    private DatabaseReference sensorRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        // Hide the action bar if present
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Set padding for system bars (edge-to-edge effect)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.dashboard), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase components
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        databaseRef = FirebaseDatabase.getInstance("https://insyncweb-default-rtdb.firebaseio.com/").getReference();

        // Initialize views
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        setupBottomNavigation();

        // Initialize sensor views
        sensorDataCard = findViewById(R.id.sensor_data_card);
        soundDataValue = findViewById(R.id.sound_data_value);
        temperatureDataValue = findViewById(R.id.temperature_data_value);
        humidityDataValue = findViewById(R.id.humidity_data_value);

        // Initialize room-related views
        roomNameTextView = findViewById(R.id.room_name);
        roomImageView = findViewById(R.id.room_image);

        // Initialize the device cards
        device1Card = findViewById(R.id.device1_card);
        device2Card = findViewById(R.id.device2_card);
        device3Card = findViewById(R.id.device3_card);
        device4Card = findViewById(R.id.device4_card);

        // Set up the toggle for each device
        setupDeviceToggle(device1Card, R.id.device1_status_light, R.id.on_off_device1_text);
        setupDeviceToggle(device2Card, R.id.device2_status_light, R.id.on_off_device2_text);
        setupDeviceToggle(device3Card, R.id.device3_status_light, R.id.on_off_device3_text);
        setupDeviceToggle(device4Card, R.id.device4_status_light, R.id.on_off_device4_text);

        // Initialize Firebase Realtime Database reference
        sensorRef = FirebaseDatabase.getInstance("https://insyncweb-default-rtdb.firebaseio.com/").getReference("sensors");

        // Set up sensor data listeners
        setupSensorDataListeners();

        // Set up buttons for each room
        MaterialButton bedroomButton = findViewById(R.id.bedroom_btn);
        MaterialButton diningRoomButton = findViewById(R.id.diningroom_btn);
        MaterialButton kitchenButton = findViewById(R.id.kitchen_btn);
        MaterialButton livingRoomButton = findViewById(R.id.livingroom_btn);

        // Set up button click listeners for changing room content
        livingRoomButton.setOnClickListener(v -> {
            selectButton(livingRoomButton);
            showLivingRoomDevices();
        });

        bedroomButton.setOnClickListener(v -> {
            selectButton(bedroomButton);
            roomNameTextView.setText("Bedroom");
            hideDevices();
            roomImageView.setImageResource(R.drawable.bedroom_bg);
        });

        diningRoomButton.setOnClickListener(v -> {
            selectButton(diningRoomButton);
            roomNameTextView.setText("Dining Room");
            hideDevices();
            roomImageView.setImageResource(R.drawable.diningroom_bg);
        });

        kitchenButton.setOnClickListener(v -> {
            selectButton(kitchenButton);
            roomNameTextView.setText("Kitchen");
            hideDevices();
            roomImageView.setImageResource(R.drawable.kitchen_bg);
        });

        // Set Living Room as the default room
        selectButton(livingRoomButton);
        showLivingRoomDevices();

        // Update profile name in the navigation drawer
        updateProfileName();
        // Update the current time every second

    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_statistics) {
                Intent intent = new Intent(DashboardActivity.this, StatisticsActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_learning) {
                Intent intent = new Intent(DashboardActivity.this, LearningActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_profile) {
                Intent intent = new Intent(DashboardActivity.this, ProfileActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }

    // Method to update the profile name
    private void updateProfileName() {
        // Retrieve the username from the Intent passed from SignupActivity if available
        String username = getIntent().getStringExtra("USERNAME");

        if (username != null) {
            // If username is passed via Intent, display it
            if (profileNameTextView != null) {
                profileNameTextView.setText(username);
            }
        } else {
            // Otherwise, fetch the username from Firestore (for logged-in users)
            if (auth.getCurrentUser() != null) {
                String userId = auth.getCurrentUser().getUid();
                DocumentReference userRef = firestore.collection("users").document(userId);

                userRef.get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String profileName = task.getResult().getString("name");
                        if (profileName != null) {
                            if (profileNameTextView != null) {
                                profileNameTextView.setText(profileName); // Update the TextView
                            } else {
                                Log.e("DashboardActivity", "profileNameTextView is null");
                            }
                        } else {
                            Log.e("DashboardActivity", "Profile name is null");
                        }
                    } else {
                        Log.e("DashboardActivity", "Failed to load profile name: " + task.getException());
                        Toast.makeText(DashboardActivity.this, "Failed to load profile name.", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Log.e("DashboardActivity", "User is not logged in");
            }
        }
    }

    // Method for setting up device toggle functionality
    private void setupDeviceToggle(MaterialCardView deviceCard, int circleId, int textId) {
        View statusCircle = deviceCard.findViewById(circleId);
        TextView statusText = deviceCard.findViewById(textId);

        // Determine which device this is
        String deviceType;
        if (deviceCard.getId() == R.id.device1_card) {
            deviceType = "lamp";
        } else if (deviceCard.getId() == R.id.device2_card) {
            deviceType = "fan";
        } else if (deviceCard.getId() == R.id.device3_card) {
            deviceType = "speaker";
        } else if (deviceCard.getId() == R.id.device4_card) {
            deviceType = "thermometer";
        } else {
            return;
        }

        // Set up Firebase listener for this device
        DatabaseReference deviceRef = databaseRef.child("devices").child(deviceType);
        deviceRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    int value = snapshot.getValue(Integer.class);
                    boolean isOn = value == 1;
                    statusCircle.setBackgroundTintList(ColorStateList.valueOf(isOn ? COLOR_ON : COLOR_OFF));
                    statusText.setText(isOn ? "ON" : "OFF");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Error reading device state: " + error.getMessage());
            }
        });

        // Set click listener for toggling
        deviceCard.setOnClickListener(v -> {
            boolean currentlyOn = "ON".contentEquals(statusText.getText());
            boolean newState = !currentlyOn;

            deviceRef.setValue(newState ? 1 : 0)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Firebase", deviceType + " state updated successfully");
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firebase", "Error updating " + deviceType + " state", e);
                        Toast.makeText(DashboardActivity.this,
                                "Failed to update " + deviceType, Toast.LENGTH_SHORT).show();
                    });
        });
    }

    // Method to highlight the selected button and update the UI accordingly
    private void selectButton(MaterialButton selectedButton) {
        if (this.selectedButton != null) {
            this.selectedButton.setTextColor(ColorStateList.valueOf(0xFFF3D19F));
        }
        this.selectedButton = selectedButton;
        this.selectedButton.setTextColor(ColorStateList.valueOf(SELECTED_COLOR));
    }

    // Method to show device cards for the living room
    private void showLivingRoomDevices() {
        // Show all device cards
        device1Card.setVisibility(View.VISIBLE);
        device2Card.setVisibility(View.VISIBLE);
        device3Card.setVisibility(View.VISIBLE);
        device4Card.setVisibility(View.VISIBLE);

        // Hide sensor card in living room
        if (sensorDataCard != null) {
            sensorDataCard.setVisibility(View.GONE);
        }

        // Update room image and name
        roomImageView.setImageResource(R.drawable.livingroom_bg);
        roomNameTextView.setText("Living Room");
    }

    // Method to hide device cards for non-living room sections
    private void hideDevices() {
        device1Card.setVisibility(View.GONE);
        device2Card.setVisibility(View.GONE);
        device3Card.setVisibility(View.GONE);
        device4Card.setVisibility(View.GONE);
    }

    private void setupSensorDataListeners() {
        // Smart Speaker (Sound) Data Listener
        DatabaseReference speakerRef = databaseRef.child("devices").child("speaker");
        DatabaseReference soundRef = sensorRef.child("sound");

        speakerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot deviceSnapshot) {
                if (deviceSnapshot.exists()) {
                    int speakerState = deviceSnapshot.getValue(Integer.class);
                    boolean isSpeakerOn = speakerState == 1;

                    // Add a listener for sound data that updates in real-time
                    soundRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (isSpeakerOn && snapshot.exists()) {
                                String soundLevel = snapshot.getValue(String.class);
                                soundDataValue.setText(soundLevel != null ? soundLevel + " dB" : "N/A");
                                soundDataValue.setVisibility(View.VISIBLE);
                            } else {
                                soundDataValue.setText("Sound: OFF");
                                soundDataValue.setVisibility(View.VISIBLE);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("Firebase", "Error fetching sound data: " + error.getMessage());
                            soundDataValue.setText("Error");
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Error fetching speaker state: " + error.getMessage());
            }
        });

        // Thermometer (Temperature) Data Listener
        DatabaseReference thermometerRef = databaseRef.child("devices").child("thermometer");
        DatabaseReference temperatureRef = sensorRef.child("temperature");

        thermometerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot deviceSnapshot) {
                if (deviceSnapshot.exists()) {
                    int thermometerState = deviceSnapshot.getValue(Integer.class);
                    boolean isThermometerOn = thermometerState == 1;

                    // Add a listener for temperature data that updates in real-time
                    temperatureRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (isThermometerOn && snapshot.exists()) {
                                String temperature = snapshot.getValue(String.class);
                                temperatureDataValue.setText(temperature != null ? temperature + " °C" : "N/A");
                                temperatureDataValue.setVisibility(View.VISIBLE);
                            } else {
                                temperatureDataValue.setText("Temp: OFF");
                                temperatureDataValue.setVisibility(View.VISIBLE);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("Firebase", "Error fetching temperature data: " + error.getMessage());
                            temperatureDataValue.setText("Error");
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Error fetching thermometer state: " + error.getMessage());
            }
        });

        // Humidity Data Listener (unchanged, updates in real-time)
        sensorRef.child("humidity").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String humidity = snapshot.getValue(String.class);
                    humidityDataValue.setText(humidity != null ? humidity + " %" : "N/A");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Error fetching humidity data: " + error.getMessage());
                humidityDataValue.setText("Error");
            }
        });
    }
}
