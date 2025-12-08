package in.pubbs.pubbsadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import in.pubbs.pubbsadmin.Adapter.CycleDemandAdapter;
import in.pubbs.pubbsadmin.Adapter.RedistributionAdapter;

public class Redistribution extends AppCompatActivity {

    DatabaseReference stationRootRef;
    SharedPreferences sharedPreferences;

    // UI Components
    ProgressBar progressBar;
    TextView noData;
    Button redistributeBtn, doneRedistributionBtn;
    RecyclerView recyclerView;

    // Cycle Demand UI Components
    LinearLayout initialButtonsContainer;
    Button cycleDemandBtn, redistributeBtnInitial;
    RecyclerView cycleDemandRecyclerView;
    Button submitCycleDemandBtn;
    
    // Toolbar Components
    TextView toolbarTitle;
    ImageView backButton;
    
    // Adapters
    RedistributionAdapter adapter;
    CycleDemandAdapter cycleDemandAdapter;
    
    // Data
    List<String> planList = new ArrayList<>();
    List<CycleDemandAdapter.StationDemandItem> cycleDemandStationList = new ArrayList<>();

    // Store calculated redistribution results
    Map<String, StationInfo> stationData = new HashMap<>();
    Map<String, Integer> pickupMap = new HashMap<>();
    Map<String, Integer> dropMap = new HashMap<>();

    int surplusCycles = 0; // extra cycles if supply > demand

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_redistribution);

        initView();
        initFirebase();
        setupInitialButtons();

        // Existing redistribution button click handlers
        redistributeBtn.setOnClickListener(v -> redistributeCycles());
        doneRedistributionBtn.setOnClickListener(v -> applyRedistribution());
        
        // Cycle Demand submit button
        submitCycleDemandBtn.setOnClickListener(v -> showConfirmationDialog());
    }
    
    @Override
    public void onBackPressed() {
        // If initial buttons are hidden, show them again
        if (initialButtonsContainer.getVisibility() != View.VISIBLE) {
            // Reset to initial state
            initialButtonsContainer.setVisibility(View.VISIBLE);
            cycleDemandRecyclerView.setVisibility(View.GONE);
            submitCycleDemandBtn.setVisibility(View.GONE);
            redistributeBtn.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
            doneRedistributionBtn.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            noData.setVisibility(View.GONE);
            toolbarTitle.setText("Redistribution");
        } else {
            // Otherwise, use default back button behavior
            super.onBackPressed();
        }
    }

    private void initView() {
        // Initial buttons container
        initialButtonsContainer = findViewById(R.id.initial_buttons_container);
        cycleDemandBtn = findViewById(R.id.cycle_demand_btn);
        redistributeBtnInitial = findViewById(R.id.redistribute_btn_initial);
        
        // Existing redistribution UI
        redistributeBtn = findViewById(R.id.redistribute_btn);
        doneRedistributionBtn = findViewById(R.id.done_redistribution_btn);
        progressBar = findViewById(R.id.progress_bar);
        recyclerView = findViewById(R.id.redistribution_list);
        noData = findViewById(R.id.no_data_found);

        // Cycle Demand UI
        cycleDemandRecyclerView = findViewById(R.id.cycle_demand_list);
        submitCycleDemandBtn = findViewById(R.id.submit_cycle_demand_btn);
        
        // Toolbar
        toolbarTitle = findViewById(R.id.toolbar_title);
        backButton = findViewById(R.id.back_button);
        
        // Setup toolbar
        toolbarTitle.setText("Redistribution");
        backButton.setOnClickListener(v -> onBackPressed());

        // Setup RecyclerViews
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RedistributionAdapter(planList);
        recyclerView.setAdapter(adapter);
        
        cycleDemandRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        cycleDemandAdapter = new CycleDemandAdapter(cycleDemandStationList);
        cycleDemandRecyclerView.setAdapter(cycleDemandAdapter);
    }
    
    private void setupInitialButtons() {
        // Cycle Demand button - show station list with input fields
        cycleDemandBtn.setOnClickListener(v -> {
            showCycleDemandScreen();
        });
        
        // Redistribute button (initial) - show existing redistribution flow
        redistributeBtnInitial.setOnClickListener(v -> {
            showRedistributionScreen();
        });
    }
    
    private void showCycleDemandScreen() {
        // Hide initial buttons
        initialButtonsContainer.setVisibility(View.GONE);
        
        // Update toolbar title
        toolbarTitle.setText("Station List");
        
        // Show Cycle Demand UI
        progressBar.setVisibility(View.VISIBLE);
        cycleDemandRecyclerView.setVisibility(View.GONE);
        submitCycleDemandBtn.setVisibility(View.GONE);
        noData.setVisibility(View.GONE);
        
        // Fetch stations from Firebase
        fetchStationsForCycleDemand();
    }
    
    private void showRedistributionScreen() {
        // Launch RedistributionMapActivity when Redistribute button is clicked
        Intent intent = new Intent(Redistribution.this, RedistributionMapActivity.class);
        startActivity(intent);
        
        // Commented out existing redistribution UI - now using map screen instead
        // Hide initial buttons
        // initialButtonsContainer.setVisibility(View.GONE);
        // Show existing redistribution UI
        // redistributeBtn.setVisibility(View.VISIBLE);
    }
    
    private void fetchStationsForCycleDemand() {
        if (stationRootRef == null) {
            progressBar.setVisibility(View.GONE);
            noData.setVisibility(View.VISIBLE);
            noData.setText("Organisation name missing!");
            return;
        }
        
        // Log the Firebase path being fetched
        Log.d("Redistribution", "Fetching stations from Firebase path: " + stationRootRef.toString());
        
        cycleDemandStationList.clear();
        
        stationRootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Fetching all stations from Firebase path: /<organisation>/Station/
                // Example: /RM/Station/ or /IITPubbs/Station/
                for (DataSnapshot stationSnap : snapshot.getChildren()) {
                    String stationId = stationSnap.getKey(); // e.g., "Station_0", "Station_1"
                    String stationName = stationSnap.child("stationName").getValue(String.class);
                    
                    // EXTRACTING DEMAND VALUES:
                    // Path being checked: /<organisation>/Station/{stationId}/stationCycleDemand
                    // Example: /RM/Station/Station_0/stationCycleDemand
                    // 
                    // IMPORTANT: This field may NOT exist yet in Firebase!
                    // - If field exists: Returns the stored Integer value
                    // - If field doesn't exist: Returns null (will show empty input field)
                    // - Field will be CREATED when user saves for the first time
                    Long currentDemand = stationSnap.child("stationCycleDemand").getValue(Long.class);
                    
                    if (stationId != null && stationName != null) {
                        // Convert to Integer (null if field doesn't exist in Firebase)
                        Integer demandValue = (currentDemand != null) ? currentDemand.intValue() : null;
                        cycleDemandStationList.add(
                            new CycleDemandAdapter.StationDemandItem(stationId, stationName, demandValue)
                        );
                    }
                }
                
                progressBar.setVisibility(View.GONE);
                
                if (cycleDemandStationList.isEmpty()) {
                    noData.setVisibility(View.VISIBLE);
                    noData.setText("No stations found!");
                    cycleDemandRecyclerView.setVisibility(View.GONE);
                    submitCycleDemandBtn.setVisibility(View.GONE);
                } else {
                    noData.setVisibility(View.GONE);
                    cycleDemandRecyclerView.setVisibility(View.VISIBLE);
                    submitCycleDemandBtn.setVisibility(View.VISIBLE);
                    cycleDemandAdapter.notifyDataSetChanged();
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Redistribution", "Error fetching stations: " + error.getMessage());
                progressBar.setVisibility(View.GONE);
                noData.setVisibility(View.VISIBLE);
                noData.setText("Error loading stations!");
            }
        });
    }
    
    private void showConfirmationDialog() {
        Map<String, Integer> demandMap = cycleDemandAdapter.getDemandMap();
        
        if (demandMap.isEmpty()) {
            Toast.makeText(this, "Please enter at least one cycle demand", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Simple confirmation dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm")
                .setMessage("Do you want to save the cycle demand?")
                .setPositiveButton("Confirm", (dialog, which) -> {
                    saveCycleDemandToFirebase(demandMap);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }
    
    private void saveCycleDemandToFirebase(Map<String, Integer> demandMap) {
        progressBar.setVisibility(View.VISIBLE);
        
        // SAVING DEMAND VALUES TO FIREBASE:
        // Path: /<organisation>/Station/{stationId}/stationCycleDemand
        // Example: /RM/Station/Station_0/stationCycleDemand = 5
        // 
        // This will CREATE the field if it doesn't exist, or UPDATE if it already exists
        for (Map.Entry<String, Integer> entry : demandMap.entrySet()) {
            String stationId = entry.getKey(); // e.g., "Station_0"
            int demand = entry.getValue(); // User entered value
            
            // Full path: /<organisation>/Station/{stationId}/stationCycleDemand
            DatabaseReference stationRef = stationRootRef.child(stationId);
            stationRef.child("stationCycleDemand").setValue(demand);
        }
        
        // For stations not in the map, set demand to 0 (optional - uncomment if needed)
        // for (CycleDemandAdapter.StationDemandItem item : cycleDemandStationList) {
        //     if (!demandMap.containsKey(item.getStationId())) {
        //         stationRootRef.child(item.getStationId()).child("stationCycleDemand").setValue(0);
        //     }
        // }
        
        progressBar.setVisibility(View.GONE);
        
        Toast.makeText(this, "Cycle demand saved successfully!", Toast.LENGTH_SHORT).show();
        
        // Reset UI
        cycleDemandStationList.clear();
        cycleDemandAdapter.notifyDataSetChanged();
        initialButtonsContainer.setVisibility(View.VISIBLE);
        cycleDemandRecyclerView.setVisibility(View.GONE);
        submitCycleDemandBtn.setVisibility(View.GONE);
        noData.setVisibility(View.GONE);
        toolbarTitle.setText("Redistribution");
    }

    private void initFirebase() {
        sharedPreferences = getSharedPreferences("pubbs", MODE_PRIVATE);
        String organisation = sharedPreferences.getString("organisationName", "").replaceAll(" ", "");
        if (organisation.isEmpty()) {
            Log.e("Redistribution", "Organisation name missing!");
            return;
        }
        stationRootRef = FirebaseDatabase.getInstance()
                .getReference()
                .child(organisation)
                .child("Station");
        
        // Log the Firebase path for debugging
        Log.d("Redistribution", "Firebase Station path: " + organisation + "/Station");
        Log.d("Redistribution", "Full Firebase URL: " + stationRootRef.toString());
    }

    private void redistributeCycles() {
        if (stationRootRef == null) {
            noData.setVisibility(View.VISIBLE);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        planList.clear();
        pickupMap.clear();
        dropMap.clear();
        surplusCycles = 0;
        adapter.notifyDataSetChanged();

        stationRootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int totalPickup = 0;
                int totalDrop = 0;
                stationData.clear();

                for (DataSnapshot stationSnap : snapshot.getChildren()) {
                    Long count = stationSnap.child("stationCycleCount").getValue(Long.class);
                    Long demand = stationSnap.child("stationCycleDemand").getValue(Long.class);
                    String name = stationSnap.child("stationName").getValue(String.class);

                    if (count == null || demand == null || name == null) continue;

                    stationData.put(stationSnap.getKey(), new StationInfo(name, count, demand));

                    if (count > demand) {
                        // 🚲 Pickup station
                        int pickup = (int) (count - demand);
                        pickupMap.put(stationSnap.getKey(), pickup);
                        totalPickup += pickup;
                        planList.add("🚲 Pickup from " + name + ": " + pickup + " cycles");
                    } else if (count < demand) {
                        // 📍 Drop station
                        int drop = (int) (demand - count);
                        dropMap.put(stationSnap.getKey(), drop);
                        totalDrop += drop;
                        planList.add("📍 Drop at " + name + ": " + drop + " cycles");
                    }
                }

                // ⚖️ Balance check
                if (totalPickup > totalDrop) {
                    surplusCycles = totalPickup - totalDrop;
                    planList.add("⚠️ Extra surplus cycles: " + surplusCycles);
                } else if (totalDrop > totalPickup) {
                    planList.add("⚠️ Not enough cycles! Shortage: " + (totalDrop - totalPickup));
                }

                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                noData.setVisibility(planList.isEmpty() ? View.VISIBLE : View.GONE);
                recyclerView.setVisibility(planList.isEmpty() ? View.GONE : View.VISIBLE);
                doneRedistributionBtn.setVisibility(planList.isEmpty() ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Redistribution", "Error: " + error.getMessage());
                progressBar.setVisibility(View.GONE);
                noData.setVisibility(View.VISIBLE);
            }
        });
    }


    private void applyRedistribution() {
        progressBar.setVisibility(View.VISIBLE);

        for (Map.Entry<String, StationInfo> entry : stationData.entrySet()) {
            String stationId = entry.getKey();
            StationInfo info = entry.getValue();

            int finalCount = info.count.intValue();

            // Apply pickups (subtract cycles)
            if (pickupMap.containsKey(stationId)) {
                finalCount -= pickupMap.get(stationId);
            }

            // Apply drops (add cycles)
            if (dropMap.containsKey(stationId)) {
                finalCount += dropMap.get(stationId);
            }

            if (finalCount < 0) finalCount = 0;

            DatabaseReference ref = stationRootRef.child(stationId);
            ref.child("stationCycleCount").setValue(finalCount);
            ref.child("stationCycleDemand").setValue(0); // reset demand
        }

        progressBar.setVisibility(View.GONE);
        doneRedistributionBtn.setVisibility(View.GONE);
        planList.clear();
        adapter.notifyDataSetChanged();
        noData.setVisibility(View.VISIBLE);
        noData.setText("✅ Redistribution completed successfully.");
        Toast.makeText(this, "Redistribution applied!", Toast.LENGTH_SHORT).show();
    }

    // Helper class
    private static class StationInfo {
        String name;
        Long count;
        Long demand;

        StationInfo(String name, Long count, Long demand) {
            this.name = name;
            this.count = count;
            this.demand = demand;
        }
    }
}
