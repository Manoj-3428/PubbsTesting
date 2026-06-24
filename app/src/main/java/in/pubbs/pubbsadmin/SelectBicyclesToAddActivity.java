package in.pubbs.pubbsadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import in.pubbs.pubbsadmin.Adapter.SelectBicyclesToAddAdapter;
import in.pubbs.pubbsadmin.View.CustomLoader;

/**
 * When a station is selected in Manage Bicycle, this screen shows unassigned bicycles
 * (not assigned to any station). User can multi-select and add them to the station at once.
 * Updates: Bicycle (inStationId, inStationName, inAreaId, cyclebattery), Station/cyclesList (id + percentage).
 */
public class SelectBicyclesToAddActivity extends AppCompatActivity {

    private static final String TAG = "SelectBicyclesToAdd";

    private RecyclerView recyclerView;
    private SelectBicyclesToAddAdapter adapter;
    private final ArrayList<SelectBicyclesToAddAdapter.Item> list = new ArrayList<>();
    private TextView tvStationName;
    private View noDataFound;
    private Button btnAddToStation;
    private CustomLoader customLoader;
    private SharedPreferences sharedPreferences;

    private String orgName;
    private String stationId;
    private String stationName;
    private String areaId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_bicycles_to_add);

        sharedPreferences = getSharedPreferences(getResources().getString(R.string.sharedPreferences), Context.MODE_PRIVATE);
        orgName = Objects.requireNonNull(sharedPreferences.getString("organisationName", "no_data")).replaceAll(" ", "");
        stationId = getIntent().getStringExtra("StationId");
        stationName = getIntent().getStringExtra("StationName");
        areaId = getIntent().getStringExtra("AreaId");

        if (stationId == null || stationId.isEmpty()) {
            Toast.makeText(this, "Station not selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (stationName == null) stationName = "";

        customLoader = new CustomLoader(this, R.style.WideDialog);
        recyclerView = findViewById(R.id.recycler_view);
        tvStationName = findViewById(R.id.tv_station_name);
        noDataFound = findViewById(R.id.no_data_found);
        btnAddToStation = findViewById(R.id.btn_add_to_station);

        tvStationName.setText("Station: " + stationName);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        findViewById(R.id.back_button).setOnClickListener(v -> finish());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SelectBicyclesToAddAdapter(list);
        recyclerView.setAdapter(adapter);

        btnAddToStation.setOnClickListener(v -> addSelectedToStation());

        loadUnassignedBicycles();
    }

    /**
     * Load bicycles that are not assigned to any station (inStationId null or empty).
     * For each, get cyclebattery from Bicycle/{id}/cyclebattery or default 100.
     */
    private void loadUnassignedBicycles() {
        customLoader.show();
        list.clear();

        DatabaseReference bicycleRef = FirebaseDatabase.getInstance().getReference(orgName + "/Bicycle");
        bicycleRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                customLoader.dismiss();
                if (!snapshot.exists()) {
                    showNoData();
                    return;
                }

                for (DataSnapshot child : snapshot.getChildren()) {
                    String bicycleId = child.getKey();
                    if (bicycleId == null) continue;

                    Object inStationIdObj = child.child("inStationId").getValue();
                    String inStationId = (inStationIdObj == null) ? "" : inStationIdObj.toString().trim();
                    if (!inStationId.isEmpty()) continue;

                    int percentage = 100;
                    Object cyclebatteryObj = child.child("cyclebattery").getValue();
                    if (cyclebatteryObj != null) {
                        try {
                            double p = Double.parseDouble(cyclebatteryObj.toString());
                            percentage = (int) Math.round(Math.max(0, Math.min(100, p)));
                        } catch (NumberFormatException ignored) { }
                    }
                    list.add(new SelectBicyclesToAddAdapter.Item(bicycleId, percentage));
                }

                refreshList();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                customLoader.dismiss();
                Toast.makeText(SelectBicyclesToAddActivity.this, "Failed to load bicycles", Toast.LENGTH_SHORT).show();
                showNoData();
            }
        });
    }

//    /**
//     * Load bicycles that are not assigned to any station.
//     * Assignment is determined only by station cyclesList collections: a bicycle is assigned
//     * iff its ID appears under any Station/{stationId}/cyclesList. Bicycle node fields (e.g. inStationId)
//     * are not used for this check.
//     * For each unassigned bicycle, get cyclebattery from Bicycle/{id}/cyclebattery or default 100.
//     */
//    private void loadUnassignedBicycles() {
//        customLoader.show();
//        list.clear();
//
//        DatabaseReference stationRef = FirebaseDatabase.getInstance().getReference(orgName + "/Station");
//        stationRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot stationSnapshot) {
//                Set<String> assignedBicycleIds = new HashSet<>();
//                for (DataSnapshot stationChild : stationSnapshot.getChildren()) {
//                    DataSnapshot cyclesListSnap = stationChild.child("cyclesList");
//                    if (cyclesListSnap.exists()) {
//                        for (DataSnapshot cycleEntry : cyclesListSnap.getChildren()) {
//                            String bicycleId = cycleEntry.getKey();
//                            if (bicycleId != null && !bicycleId.isEmpty()) {
//                                assignedBicycleIds.add(bicycleId);
//                            }
//                        }
//                    }
//                }
//                Log.d(TAG, "Bicycle IDs found in station cyclesList(s): " + assignedBicycleIds.size());
//
//                DatabaseReference bicycleRef = FirebaseDatabase.getInstance().getReference(orgName + "/Bicycle");
//                bicycleRef.addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot bicycleSnapshot) {
//                        customLoader.dismiss();
//                        if (!bicycleSnapshot.exists()) {
//                            showNoData();
//                            return;
//                        }
//                        for (DataSnapshot child : bicycleSnapshot.getChildren()) {
//                            String bicycleId = child.getKey();
//                            if (bicycleId == null) continue;
//                            if (assignedBicycleIds.contains(bicycleId)) continue;
//
//                            int percentage = 100;
//                            Object cyclebatteryObj = child.child("cyclebattery").getValue();
//                            if (cyclebatteryObj != null) {
//                                try {
//                                    double p = Double.parseDouble(cyclebatteryObj.toString());
//                                    percentage = (int) Math.round(Math.max(0, Math.min(100, p)));
//                                } catch (NumberFormatException ignored) { }
//                            }
//                            list.add(new SelectBicyclesToAddAdapter.Item(bicycleId, percentage));
//                        }
//                        refreshList();
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {
//                        customLoader.dismiss();
//                        Toast.makeText(SelectBicyclesToAddActivity.this, "Failed to load bicycles", Toast.LENGTH_SHORT).show();
//                        showNoData();
//                    }
//                });
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                customLoader.dismiss();
//                Toast.makeText(SelectBicyclesToAddActivity.this, "Failed to load station lists", Toast.LENGTH_SHORT).show();
//                showNoData();
//            }
//        });
//    }

    private void refreshList() {
        adapter.notifyDataSetChanged();
        if (list.isEmpty()) {
            showNoData();
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            noDataFound.setVisibility(View.GONE);
        }
    }

    private void showNoData() {
        recyclerView.setVisibility(View.GONE);
        noDataFound.setVisibility(View.VISIBLE);
    }

    /**
     * Update Bicycle with inStationId, inStationName, inAreaId, cyclebattery.
     * Add to Station/{stationId}/cyclesList/{bicycleId} with { id, percentage }.
     * Increment stationCycleCount.
     */
    private void addSelectedToStation() {
        ArrayList<SelectBicyclesToAddAdapter.Item> selected = adapter.getSelectedItems();
        if (selected.isEmpty()) {
            Toast.makeText(this, "Select at least one bicycle", Toast.LENGTH_SHORT).show();
            return;
        }

        customLoader.show();
        DatabaseReference stationRef = FirebaseDatabase.getInstance().getReference(orgName + "/Station").child(stationId);
        DatabaseReference bicycleRootRef = FirebaseDatabase.getInstance().getReference(orgName + "/Bicycle");
        DatabaseReference cyclesListRef = stationRef.child("cyclesList");

        final int[] done = {0};
        final int count = selected.size();

        for (SelectBicyclesToAddAdapter.Item item : selected) {
            Map<String, Object> bikeUpdates = new HashMap<>();
            bikeUpdates.put("inStationId", stationId);
            bikeUpdates.put("inStationName", stationName);
            bikeUpdates.put("inAreaId", areaId != null ? areaId : "");
            bikeUpdates.put("cyclebattery", item.percentage);
            bikeUpdates.put("cycleState", "STATION");
            bikeUpdates.put("cycleStatus", "STATION");

            bicycleRootRef.child(item.bicycleId).updateChildren(bikeUpdates).addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.e(TAG, "Failed to update bicycle " + item.bicycleId);
                }
                cyclesListRef.child(item.bicycleId).setValue(new CycleEntry(item.bicycleId, item.percentage)).addOnCompleteListener(t -> {
                    if (!t.isSuccessful()) {
                        Log.e(TAG, "Failed to add to cyclesList " + item.bicycleId);
                    }
                    synchronized (done) {
                        done[0]++;
                        if (done[0] >= count) {
                            incrementStationCycleCount(stationRef, count);
                        }
                    }
                });
            });
        }
    }

    private void incrementStationCycleCount(DatabaseReference stationRef, int addCount) {
        DatabaseReference countRef = stationRef.child("stationCycleCount");
        countRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Integer current = currentData.getValue(Integer.class);
                if (current == null) current = 0;
                currentData.setValue(current + addCount);
                return Transaction.success(currentData);
            }
            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
                customLoader.dismiss();
                if (error == null && committed) {
                    Toast.makeText(SelectBicyclesToAddActivity.this, "Added " + addCount + " bicycle(s) to station", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(SelectBicyclesToAddActivity.this, "Added bicycles but station count may be wrong", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                }
            }
        });
    }

    /** For Firebase: { id, percentage, cycleState } under cyclesList/{bicycleId} */
    @SuppressWarnings("unused")
    public static class CycleEntry {
        public String id;
        public int percentage;
        public String cycleState;
        public CycleEntry(String id, int percentage) {
            this.id = id;
            this.percentage = percentage;
            this.cycleState = "STATION";
        }
    }
}
