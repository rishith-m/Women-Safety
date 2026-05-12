package com.example.womensafety;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.womensafety.models.ApiModels;
import com.example.womensafety.network.ApiClient;
import com.example.womensafety.network.ApiService;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SafeRouteActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private TextView tvSafetyStatus;
    private TextInputEditText etStart, etDest;
    private ApiService apiService;
    private Geocoder geocoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_safe_route);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        tvSafetyStatus = findViewById(R.id.tvSafetyStatus);
        etStart = findViewById(R.id.etStartAddress);
        etDest = findViewById(R.id.etDestAddress);
        MaterialButton btnAnalyze = findViewById(R.id.btnAnalyze);

        apiService = ApiClient.getClient().create(ApiService.class);
        geocoder = new Geocoder(this);

        btnAnalyze.setOnClickListener(v -> fetchSafeRoutes());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        // Default center (Hyderabad)
        LatLng hyderabad = new LatLng(17.3850, 78.4867);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(hyderabad, 12));
    }

    private void fetchSafeRoutes() {
        String startStr = etStart.getText().toString().trim();
        String destStr = etDest.getText().toString().trim();

        if (startStr.isEmpty() || destStr.isEmpty()) {
            Toast.makeText(this, "Please enter both addresses", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            List<Address> startAddresses = geocoder.getFromLocationName(startStr, 1);
            List<Address> destAddresses = geocoder.getFromLocationName(destStr, 1);

            if (startAddresses == null || startAddresses.isEmpty() || destAddresses == null || destAddresses.isEmpty()) {
                Toast.makeText(this, "Could not find one or both locations", Toast.LENGTH_SHORT).show();
                return;
            }

            Address start = startAddresses.get(0);
            Address dest = destAddresses.get(0);

            LatLng startLatLng = new LatLng(start.getLatitude(), start.getLongitude());
            LatLng destLatLng = new LatLng(dest.getLatitude(), dest.getLongitude());

            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(startLatLng).title("Start: " + startStr));
            mMap.addMarker(new MarkerOptions().position(destLatLng).title("Destination: " + destStr)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

            tvSafetyStatus.setText("Analyzing routes for safety...");

            ApiModels.RouteRequest request = new ApiModels.RouteRequest(
                    start.getLatitude(), start.getLongitude(),
                    dest.getLatitude(), dest.getLongitude());

            apiService.getSafeRoutes(request).enqueue(new Callback<ApiModels.RouteSafetyResponse>() {
                @Override
                public void onResponse(@NonNull Call<ApiModels.RouteSafetyResponse> call, @NonNull Response<ApiModels.RouteSafetyResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        displayRoutes(response.body().routes);
                    } else {
                        tvSafetyStatus.setText("Analysis failed.");
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ApiModels.RouteSafetyResponse> call, @NonNull Throwable t) {
                    tvSafetyStatus.setText("Network error.");
                }
            });

        } catch (IOException e) {
            Toast.makeText(this, "Geocoding error", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayRoutes(List<ApiModels.SafeRoute> routes) {
        if (routes == null || routes.isEmpty()) return;

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        ApiModels.SafeRoute safest = routes.get(0);
        tvSafetyStatus.setText("Safest path found! Score: " + (int)(safest.safetyScore * 100) + "%\n" + safest.reason);

        for (ApiModels.SafeRoute route : routes) {
            // Green for safe, Red for risky
            int color = route.safetyScore > 0.7 ? 0xFF4CAF50 : 0xFFF44336;
            
            PolylineOptions options = new PolylineOptions()
                    .width(18)
                    .color(color)
                    .geodesic(true);

            for (ApiModels.LatLng pt : route.waypoints) {
                LatLng latLng = new LatLng(pt.lat, pt.lng);
                options.add(latLng);
                boundsBuilder.include(latLng);
            }
            mMap.addPolyline(options);
        }
        
        try {
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 150));
        } catch (Exception ignored) {}
    }
}
