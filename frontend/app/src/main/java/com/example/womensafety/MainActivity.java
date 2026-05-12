package com.example.womensafety;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.net.Uri;
import com.example.womensafety.models.ApiModels;
import com.example.womensafety.network.ApiClient;
import com.example.womensafety.network.ApiService;
import com.example.womensafety.services.SafetyForegroundService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.Arrays;

import android.telephony.SmsManager;
import android.content.SharedPreferences;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import android.location.Location;
import com.google.android.gms.tasks.OnSuccessListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQ_CODE = 100;

    private TextView tvStatusValue, tvGreeting, tvLocationStatus;
    private com.google.android.material.button.MaterialButton btnToggleService, btnSOS;
    private com.google.android.material.imageview.ShapeableImageView ivProfile;
    private com.google.android.material.card.MaterialCardView cardPolice, cardHelpline, cardSafeRoutes, cardGuardians;
    private com.google.android.material.switchmaterial.SwitchMaterial switchAI;
    private boolean isServiceRunning = false;



    public static String currentUserId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatusValue = findViewById(R.id.tvStatusValue);
        tvGreeting = findViewById(R.id.tvGreeting);
        tvLocationStatus = findViewById(R.id.tvLocationStatus);
        btnToggleService = findViewById(R.id.btnToggleService);
        ivProfile = findViewById(R.id.ivProfile);
        btnSOS = findViewById(R.id.btnSOS);
        cardPolice = findViewById(R.id.cardPolice);
        cardHelpline = findViewById(R.id.cardHelpline);
        cardSafeRoutes = findViewById(R.id.cardSafeRoutes);
        cardGuardians = findViewById(R.id.cardGuardians);
        switchAI = findViewById(R.id.switchAI);

        checkPermissions();

        // Check shared preferences to load user ID if exists
        android.content.SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        currentUserId = prefs.getString("user_id", null);
        String userName = prefs.getString("user_name", "User");
        tvGreeting.setText("Hi, " + userName);

        updateLocationDisplay();

        ivProfile.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        btnToggleService.setOnClickListener(v -> {
            if (isServiceRunning) {
                stopSafetyService();
            } else {
                startSafetyService();
            }
        });

        btnSOS.setOnClickListener(v -> {
            triggerSOS();
        });

        cardPolice.setOnClickListener(v -> makeCall("100"));
        cardHelpline.setOnClickListener(v -> makeCall("181"));

        cardSafeRoutes.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SafeRouteActivity.class);
            startActivity(intent);
        });

        cardGuardians.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GuardiansActivity.class);
            startActivity(intent);
        });

        switchAI.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences prefs1 = getSharedPreferences("AppPrefs", MODE_PRIVATE);
            prefs1.edit().putBoolean("ai_enabled", isChecked).apply();
            if (isServiceRunning) {
                // Restart service to apply change
                stopSafetyService();
                startSafetyService();
            }
        });
    }

    private void makeCall(String number) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + number));
            startActivity(intent);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, PERMISSION_REQ_CODE);
        }
    }

    private void checkPermissions() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.CALL_PHONE
            };
        } else {
            permissions = new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.CALL_PHONE
            };
        }

        boolean allGranted = true;
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQ_CODE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void updateLocationDisplay() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    tvLocationStatus.setText("● Location active");
                    tvLocationStatus.setTextColor(ContextCompat.getColor(this, R.color.status_green));
                } else {
                    tvLocationStatus.setText("● Location unavailable");
                    tvLocationStatus.setTextColor(ContextCompat.getColor(this, R.color.sos_red));
                }
            });
        }
    }

    private void startSafetyService() {
        Intent serviceIntent = new Intent(this, SafetyForegroundService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
        isServiceRunning = true;
        tvStatusValue.setText("Active");
        btnToggleService.setText("Disable");
    }

    private void stopSafetyService() {
        Intent serviceIntent = new Intent(this, SafetyForegroundService.class);
        stopService(serviceIntent);
        isServiceRunning = false;
        tvStatusValue.setText("Inactive");
        btnToggleService.setText("Enable");
    }

    private void triggerSOS() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String contactsStr = prefs.getString("emer_contacts", "");
        
        if (currentUserId == null || contactsStr.isEmpty()) {
            Toast.makeText(this, "Please Register Profile with Emergency Contacts first.", Toast.LENGTH_LONG).show();
            return;
        }

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                double lat = 0.0, lng = 0.0;
                String locationLink = "Location not available.";
                if (location != null) {
                    lat = location.getLatitude();
                    lng = location.getLongitude();
                    locationLink = "http://maps.google.com/maps?q=loc:" + lat + "," + lng;
                }

                // Send SMS
                String message = "SOS! I need help. My location: " + locationLink;
                SmsManager smsManager = SmsManager.getDefault();
                String[] contacts = contactsStr.split("\\s*,\\s*");
                for (String contact : contacts) {
                    if (!contact.isEmpty()) {
                        try {
                            smsManager.sendTextMessage(contact, null, message, null, null);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                
                Toast.makeText(MainActivity.this, "SOS Activated! SMS sent to contacts.", Toast.LENGTH_LONG).show();

                // Trigger backend API
                ApiService apiService = ApiClient.getClient().create(ApiService.class);
                ApiModels.AlertRequest req = new ApiModels.AlertRequest(currentUserId, lat, lng);
                apiService.triggerAlert(req).enqueue(new Callback<ApiModels.AlertResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiModels.AlertResponse> call, @NonNull Response<ApiModels.AlertResponse> response) {}
                    @Override
                    public void onFailure(@NonNull Call<ApiModels.AlertResponse> call, @NonNull Throwable t) {}
                });
            });
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}
