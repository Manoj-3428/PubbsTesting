package in.pubbs.pubbsadmin;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class ReassignConfirmActivity extends AppCompatActivity {

    private String bicycleId;
    private String stationId;
    private String org;
    private CoordinatorLayout coordinatorLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reassign_confirm);

        bicycleId = getIntent().getStringExtra("bicycleId");
        stationId = getIntent().getStringExtra("stationId");
        org = getIntent().getStringExtra("org");

        if (bicycleId == null || stationId == null || org == null) {
            finish();
            return;
        }

        coordinatorLayout = findViewById(R.id.coordinator_layout);

        ImageView backButton = findViewById(R.id.back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        TextView tvBicycleId = findViewById(R.id.tv_bicycle_id);
        if (tvBicycleId != null && bicycleId != null) {
            tvBicycleId.setText(bicycleId);
        }

        TextView tvStationName = findViewById(R.id.tv_station_name);
        loadStationName(stationId, tvStationName);

        Button btnReassign = findViewById(R.id.btn_reassign_confirm);
        if (btnReassign != null) {
            btnReassign.setOnClickListener(v -> showConfirmationDialog());
        }
    }

    private void loadStationName(String stationId, TextView tvStationName) {
        if (stationId == null || org == null || tvStationName == null) return;

        DatabaseReference stationRef = FirebaseDatabase.getInstance()
                .getReference(org)
                .child("Station")
                .child(stationId);

        stationRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String stationName = snapshot.child("stationName").getValue(String.class);
                    if (stationName != null && !stationName.isEmpty()) {
                        tvStationName.setText(stationName);
                    } else {
                        tvStationName.setText("Station ID: " + stationId);
                    }
                } else {
                    tvStationName.setText("Station ID: " + stationId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvStationName.setText("Station ID: " + stationId);
            }
        });
    }

    private void showConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Re-assignment")
                .setMessage("Are you sure you want to re-assign bicycle " + bicycleId + " to this station?")
                .setPositiveButton("Yes", (dialog, which) -> performReassign())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performReassign() {
        ReassignHelper.reassignBicycleToStation(
                this,
                org,
                bicycleId,
                stationId,
                () -> {
                    showSnackbar("Bicycle re-assigned successfully", true);
                    // Return result to parent activity
                    Intent result = new Intent();
                    result.putExtra("reassigned", true);
                    setResult(RESULT_OK, result);
                    finish();
                },
                () -> {
                    showSnackbar("Failed to re-assign bicycle", false);
                }
        );
    }

    private void showSnackbar(String message, boolean isSuccess) {
        if (coordinatorLayout == null) {
            coordinatorLayout = findViewById(R.id.coordinator_layout);
        }
        if (coordinatorLayout == null) {
            coordinatorLayout = findViewById(android.R.id.content);
        }

        Snackbar snackbar = Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_LONG);
        snackbar.setBackgroundTint(isSuccess ? 0xFF38C852 : 0xFFFF4444);
        snackbar.setTextColor(0xFFFFFFFF);
        snackbar.show();
    }
}
