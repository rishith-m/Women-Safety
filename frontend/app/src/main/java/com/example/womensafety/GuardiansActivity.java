package com.example.womensafety;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GuardiansActivity extends AppCompatActivity {

    private RecyclerView rvGuardians;
    private GuardiansAdapter adapter;
    private List<String> guardianList;
    private TextInputEditText etGuardianPhone;
    private MaterialButton btnAddGuardian;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guardians);

        rvGuardians = findViewById(R.id.rvGuardians);
        etGuardianPhone = findViewById(R.id.etGuardianPhone);
        btnAddGuardian = findViewById(R.id.btnAddGuardian);

        loadGuardians();

        adapter = new GuardiansAdapter(guardianList);
        rvGuardians.setLayoutManager(new LinearLayoutManager(this));
        rvGuardians.setAdapter(adapter);

        btnAddGuardian.setOnClickListener(v -> {
            String phone = etGuardianPhone.getText().toString().trim();
            if (!phone.isEmpty()) {
                if (!guardianList.contains(phone)) {
                    guardianList.add(phone);
                    saveGuardians();
                    adapter.notifyDataSetChanged();
                    etGuardianPhone.setText("");
                    Toast.makeText(this, "Guardian added", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Already in list", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadGuardians() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String contactsStr = prefs.getString("emer_contacts", "");
        if (contactsStr.isEmpty()) {
            guardianList = new ArrayList<>();
        } else {
            guardianList = new ArrayList<>(Arrays.asList(contactsStr.split("\\s*,\\s*")));
        }
    }

    private void saveGuardians() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < guardianList.size(); i++) {
            sb.append(guardianList.get(i));
            if (i < guardianList.size() - 1) sb.append(",");
        }
        prefs.edit().putString("emer_contacts", sb.toString()).apply();
    }

    class GuardiansAdapter extends RecyclerView.Adapter<GuardiansAdapter.ViewHolder> {
        private List<String> list;

        GuardiansAdapter(List<String> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_guardian, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String phone = list.get(position);
            holder.tvPhone.setText(phone);
            holder.btnDelete.setOnClickListener(v -> {
                list.remove(position);
                saveGuardians();
                notifyDataSetChanged();
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvPhone;
            ImageButton btnDelete;

            ViewHolder(View itemView) {
                super(itemView);
                tvPhone = itemView.findViewById(R.id.tvGuardianPhone);
                btnDelete = itemView.findViewById(R.id.btnDeleteGuardian);
            }
        }
    }
}
