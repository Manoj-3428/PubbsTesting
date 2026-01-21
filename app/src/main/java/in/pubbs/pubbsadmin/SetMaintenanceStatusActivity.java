package in.pubbs.pubbsadmin;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import in.pubbs.pubbsadmin.View.CustomLoader;

public class SetMaintenanceStatusActivity extends AppCompatActivity {

    private SharedPreferences sp;
    private String org;
    private String bicycleId;

    private CustomLoader loader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_maintenance_status);

        sp = getSharedPreferences("pubbs", Context.MODE_PRIVATE);
        org = Objects.requireNonNull(sp.getString("organisationName", "")).replaceAll(" ", "");
        bicycleId = getIntent().getStringExtra("bicycleId");

        ((ImageView) findViewById(R.id.back_button)).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.toolbar_title)).setText("Set maintenance status");

        TextView tvCycle = findViewById(R.id.tv_bicycle_id);
        tvCycle.setText(bicycleId == null ? "" : bicycleId);

        RadioButton rbInRepair = findViewById(R.id.rb_in_repair);
        rbInRepair.setChecked(true);

        Button save = findViewById(R.id.btn_save);
        save.setOnClickListener(v -> showConfirmAndSave());

        loader = new CustomLoader(this, R.style.WideDialog);
    }

    private void showConfirmAndSave() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm")
                .setMessage("Set this bicycle to In Repair?")
                .setPositiveButton("Yes", (d, w) -> setInRepair())
                .setNegativeButton("No", null)
                .show();
    }

    private void setInRepair() {
        if (bicycleId == null || bicycleId.isEmpty()) return;
        loader.show();

        DatabaseReference orgRef = FirebaseDatabase.getInstance().getReference(org);

        // 1) Update bicycle node (best-effort)
        Map<String, Object> bikeUpdates = new HashMap<>();
        bikeUpdates.put("cycleStatus", "InRepair");
        bikeUpdates.put("maintenanceStatus", "InRepair");
        bikeUpdates.put("operation", "0"); // Set operation to 0 as requested

        orgRef.child("Bicycle").child(bicycleId).updateChildren(bikeUpdates);

        // 2) Update any matching report(s) to InRepair
        orgRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                DataSnapshot reportSnap = snapshot.child("ReportCycleCollection");
                if (!reportSnap.exists()) reportSnap = snapshot.child("ReportCycle");

                Map<String, Object> updates = new HashMap<>();
                for (DataSnapshot child : reportSnap.getChildren()) {
                    String bid = String.valueOf(child.child("bicycleId").getValue());
                    if ("null".equalsIgnoreCase(bid)) bid = "";
                    String cid = String.valueOf(child.child("CycleId").getValue());
                    if ("null".equalsIgnoreCase(cid)) cid = "";

                    if (bicycleId.equalsIgnoreCase(bid) || bicycleId.equalsIgnoreCase(cid)) {
                        updates.put(child.getKey() + "/CycleStatus", "InRepair");
                        updates.put(child.getKey() + "/cycleStatus", "InRepair");
                    }
                }

                if (!updates.isEmpty()) {
                    reportSnap.getRef().updateChildren(updates)
                            .addOnSuccessListener(a -> {
                                loader.dismiss();
                                Toast.makeText(SetMaintenanceStatusActivity.this, "Status updated", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                loader.dismiss();
                                Toast.makeText(SetMaintenanceStatusActivity.this, "Failed to update status", Toast.LENGTH_SHORT).show();
                            });
                } else {
                    loader.dismiss();
                    Toast.makeText(SetMaintenanceStatusActivity.this, "Status updated", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loader.dismiss();
                Toast.makeText(SetMaintenanceStatusActivity.this, "Failed to update status", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

