package com.example.womensafety;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.womensafety.models.ApiModels;
import com.example.womensafety.network.ApiClient;
import com.example.womensafety.network.ApiService;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etName, etPhone, etContacts;
    private Button btnSubmitRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etContacts = findViewById(R.id.etContacts);
        btnSubmitRegister = findViewById(R.id.btnSubmitRegister);

        // Pre-fill if already exists
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String existingName = prefs.getString("user_name", "");
        String existingPhone = prefs.getString("user_phone", "");
        String existingContacts = prefs.getString("emer_contacts", "");
        
        etName.setText(existingName);
        etPhone.setText(existingPhone);
        etContacts.setText(existingContacts);

        btnSubmitRegister.setOnClickListener(v -> handleRegistration());
    }

    private void handleRegistration() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        String contactsStr = etContacts.getText() != null ? etContacts.getText().toString().trim() : "";

        if (name.isEmpty() || phone.isEmpty() || contactsStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Split by comma and clean spaces
        List<String> contactsList = Arrays.asList(contactsStr.split("\\s*,\\s*"));

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        ApiModels.RegisterRequest req = new ApiModels.RegisterRequest(name, phone, contactsList);

        btnSubmitRegister.setEnabled(false);
        btnSubmitRegister.setText("Registering...");

        apiService.registerUser(req).enqueue(new Callback<ApiModels.RegisterResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiModels.RegisterResponse> call, @NonNull Response<ApiModels.RegisterResponse> response) {
                btnSubmitRegister.setEnabled(true);
                btnSubmitRegister.setText("Register / Save");
                
                if (response.isSuccessful() && response.body() != null) {
                    saveAndFinish(response.body().user_id, name, phone, contactsStr);
                } else if (response.code() == 400) {
                    // Try to handle user already exists logic gracefully for demo
                    saveAndFinish("existing_user_" + phone, name, phone, contactsStr);
                    Toast.makeText(RegisterActivity.this, "Profile Updated Locally (Phone exists)", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(RegisterActivity.this, "Failed to register via API", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiModels.RegisterResponse> call, @NonNull Throwable t) {
                btnSubmitRegister.setEnabled(true);
                btnSubmitRegister.setText("Register / Save");
                Toast.makeText(RegisterActivity.this, "Connection Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveAndFinish(String userId, String name, String phone, String contactsStr) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        prefs.edit()
             .putString("user_id", userId)
             .putString("user_name", name)
             .putString("user_phone", phone)
             .putString("emer_contacts", contactsStr)
             .apply();
             
        MainActivity.currentUserId = userId;
        Toast.makeText(this, "Profile Saved!", Toast.LENGTH_SHORT).show();
        finish();
    }
}
