package in.pubbs.pubbsadmin;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import in.pubbs.pubbsadmin.View.CustomLoader;

public class BicycleMaintenanceActivity extends AppCompatActivity {

    private static final int REQ_SCAN_IN_REPAIR = 2001;
    private static final int REQ_SELECT_STATION = 2002;

    private SharedPreferences sp;
    private String org;

    private RecyclerView recyclerView;
    private View noData;
    private CustomLoader loader;
    private CoordinatorLayout coordinatorLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bicycle_maintenance);

        sp = getSharedPreferences("pubbs", Context.MODE_PRIVATE);
        org = Objects.requireNonNull(sp.getString("organisationName", "")).replaceAll(" ", "");

        ImageView back = findViewById(R.id.back_button);
        back.setOnClickListener(v -> finish());
        TextView title = findViewById(R.id.toolbar_title);
        title.setText("Bicycle Maintenance");

        Button btnScan = findViewById(R.id.btn_scan_qr);
        Button btnReassign = findViewById(R.id.btn_reassign);

        btnScan.setOnClickListener(v -> {
            Intent i = new Intent(this, MaintenanceQRScannerActivity.class);
            i.putExtra("mode", "IN_REPAIR");
            startActivityForResult(i, REQ_SCAN_IN_REPAIR);
        });

        btnReassign.setOnClickListener(v -> startActivityForResult(
                new Intent(this, SelectStationActivity.class),
                REQ_SELECT_STATION
        ));

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        noData = findViewById(R.id.no_data_found);
        coordinatorLayout = findViewById(R.id.coordinator_layout);

        loader = new CustomLoader(this, R.style.WideDialog);
        loader.show();
        loadReports();
    }

    private void loadReports() {
        DatabaseReference orgRef = FirebaseDatabase.getInstance().getReference(org);
        orgRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                DataSnapshot reportSnap = snapshot.child("ReportCycleCollection");
                if (!reportSnap.exists()) reportSnap = snapshot.child("ReportCycle");

                List<ReportNotificationsActivity.ReportItem> items = new ArrayList<>();
                for (DataSnapshot child : reportSnap.getChildren()) {
                    ReportNotificationsActivity.ReportItem item = ReportNotificationsActivity.ReportItem.fromSnapshot(child);
                    if (item != null) items.add(item);
                }

                items.sort((a, b) -> Long.compare(b.timestampMs, a.timestampMs));

                BicycleMaintenanceReportAdapter adapter = new BicycleMaintenanceReportAdapter(items, reportItem -> {
                    Intent intent = new Intent(BicycleMaintenanceActivity.this, ReportMapActivity.class);
                    intent.putExtra("reportKey", reportItem.reportKey);
                    intent.putExtra("bicycleId", reportItem.bicycleId);
                    intent.putExtra("issue", reportItem.issue);
                    intent.putExtra("dateTime", reportItem.dateTimeRaw);
                    intent.putExtra("lat", reportItem.lat);
                    intent.putExtra("lng", reportItem.lng);
                    startActivity(intent);
                });
                recyclerView.setAdapter(adapter);

                loader.dismiss();
                noData.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loader.dismiss();
                noData.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (loader != null) loader.show();
        loadReports();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK || data == null) return;

        if (requestCode == REQ_SCAN_IN_REPAIR) {
            String bicycleId = data.getStringExtra("bicycleId");
            if (bicycleId == null || bicycleId.isEmpty()) return;
            Intent i = new Intent(this, SetMaintenanceStatusActivity.class);
            i.putExtra("bicycleId", bicycleId);
            startActivity(i);
        } else if (requestCode == REQ_SELECT_STATION) {
            String stationId = data.getStringExtra("stationId");
            if (stationId == null || stationId.isEmpty()) return;
            Intent i = new Intent(this, MaintenanceQRScannerActivity.class);
            i.putExtra("mode", "REASSIGN");
            i.putExtra("stationId", stationId);
            startActivityForResult(i, MaintenanceQRScannerActivity.REQ_REASSIGN_SCAN);
        } else if (requestCode == MaintenanceQRScannerActivity.REQ_REASSIGN_SCAN) {
            String bicycleId = data.getStringExtra("bicycleId");
            String stationId = data.getStringExtra("stationId");
            if (bicycleId == null || stationId == null) return;
            
            // Check cycleStatus before proceeding
            checkCycleStatusAndProceed(bicycleId, stationId);
        } else if (requestCode == 2004) {
            // Result from ReassignConfirmActivity
            if (data != null && data.getBooleanExtra("reassigned", false)) {
                showSnackbar("Bicycle re-assigned successfully", true);
                // Reload reports to reflect changes
                loadReports();
            }
        }
    }
    
    private void checkCycleStatusAndProceed(String bicycleId, String stationId) {
        DatabaseReference bikeRef = FirebaseDatabase.getInstance()
                .getReference(org)
                .child("Bicycle")
                .child(bicycleId)
                .child("cycleStatus");

        bikeRef.get().addOnSuccessListener(snapshot -> {
            String cycleStatus = null;
            if (snapshot.exists()) {
                Object value = snapshot.getValue();
                if (value != null) {
                    cycleStatus = String.valueOf(value);
                }
            }

            if (cycleStatus != null && "InRepair".equalsIgnoreCase(cycleStatus.trim())) {
                // Status is InRepair, proceed to confirmation screen
                Intent confirmIntent = new Intent(this, ReassignConfirmActivity.class);
                confirmIntent.putExtra("bicycleId", bicycleId);
                confirmIntent.putExtra("stationId", stationId);
                confirmIntent.putExtra("org", org);
                startActivityForResult(confirmIntent, 2004);
            } else {
                // Status is not InRepair, show error
                showSnackbar("Cannot reassign: Bicycle status must be 'InRepair'", false);
            }
        }).addOnFailureListener(e -> {
            showSnackbar("Failed to check bicycle status", false);
        });
    }
    
    private void showSnackbar(String message, boolean isSuccess) {
        if (coordinatorLayout == null) {
            View rootView = findViewById(android.R.id.content);
            if (rootView instanceof CoordinatorLayout) {
                coordinatorLayout = (CoordinatorLayout) rootView;
            } else {
                coordinatorLayout = new CoordinatorLayout(this);
            }
        }
        
        Snackbar snackbar = Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_LONG);
        snackbar.setBackgroundTint(isSuccess ? 0xFF38C852 : 0xFFFF4444);
        snackbar.setTextColor(0xFFFFFFFF);
        snackbar.show();
    }
}

