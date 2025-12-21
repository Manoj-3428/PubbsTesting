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
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.graphics.Color;

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
        // Show dialog to select algorithm
        AlertDialog.Builder builder = new AlertDialog.Builder(Redistribution.this);
        builder.setTitle("Select Route Algorithm");
        
        String[] algorithms = {
            "Nearest Neighbor + 2-Opt\n(Fast Algorithm)",
            "Ant Colony Optimization\n(Optimal Algorithm)"
        };
        
        // When user clicks an option, it immediately launches with that algorithm
        builder.setItems(algorithms, (dialog, which) -> {
            String selectedAlgorithm = which == 0 ? "nearest_neighbor" : "ant_colony";
            String algorithmName = which == 0 ? "Nearest Neighbor + 2-Opt" : "Ant Colony Optimization";
            
            // Launch RedistributionMapActivity with selected algorithm
            Intent intent = new Intent(Redistribution.this, RedistributionMapActivity.class);
            intent.putExtra("algorithm", selectedAlgorithm);
            intent.putExtra("algorithm_name", algorithmName);
            startActivity(intent);
            dialog.dismiss();
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        
        AlertDialog dialog = builder.create();
        dialog.show();
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
        
        // Optimize: Only fetch required fields - use orderBy to minimize data transfer
        // Fetch only stationName, stationCycleCount, and stationCycleDemand
        stationRootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Fetching only required fields from Firebase path: /<organisation>/Station/
                // Optimized: Only accessing stationName, stationCycleCount, stationCycleDemand
                for (DataSnapshot stationSnap : snapshot.getChildren()) {
                    String stationId = stationSnap.getKey();
                    if (stationId == null) continue;
                    
                    // Only fetch required fields
                    String stationName = stationSnap.child("stationName").getValue(String.class);
                    if (stationName == null) continue;
                    
                    // Get cycle count - handle String, Long, Integer types efficiently
                    DataSnapshot cycleCountSnap = stationSnap.child("stationCycleCount");
                    int cycleCount = 0;
                    if (cycleCountSnap.exists()) {
                        Object cycleCountValue = cycleCountSnap.getValue();
                        if (cycleCountValue instanceof Number) {
                            cycleCount = ((Number) cycleCountValue).intValue();
                        } else if (cycleCountValue instanceof String) {
                            try {
                                cycleCount = Integer.parseInt((String) cycleCountValue);
                            } catch (NumberFormatException e) {
                                cycleCount = 0;
                            }
                        }
                    }
                    
                    // Get demand value (may not exist)
                    Integer demandValue = null;
                    DataSnapshot demandSnap = stationSnap.child("stationCycleDemand");
                    if (demandSnap.exists()) {
                        Object demandObj = demandSnap.getValue();
                        if (demandObj instanceof Number) {
                            demandValue = ((Number) demandObj).intValue();
                        }
                    }
                    
                    cycleDemandStationList.add(
                            new CycleDemandAdapter.StationDemandItem(stationId, stationName, demandValue, cycleCount)
                    );
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
        
        // PRE-VALIDATION 1: Check if any demand value exceeds MAX_DEMAND_VALUE (15)
        List<String> maxDemandViolationStations = new ArrayList<>();
        for (CycleDemandAdapter.StationDemandItem item : cycleDemandStationList) {
            String stationId = item.getStationId();
            Integer demand;
            
            if (demandMap.containsKey(stationId)) {
                demand = demandMap.get(stationId);
            } else {
                demand = item.getCurrentDemand();
                if (demand == null) {
                    demand = 0;
                }
            }
            
            // Check constraint: demand value must be <= MAX_DEMAND_VALUE (15)
            if (demand > CycleDemandAdapter.MAX_DEMAND_VALUE) {
                String stationName = item.getStationName();
                maxDemandViolationStations.add(stationName + " (" + stationId + "): " + demand);
            }
        }
        
        // If max demand validation failed, show error dialog
        if (!maxDemandViolationStations.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder();
            errorMessage.append("The following stations have demand values greater than ")
                    .append(CycleDemandAdapter.MAX_DEMAND_VALUE).append(":\n\n");
            for (String station : maxDemandViolationStations) {
                errorMessage.append("• ").append(station).append("\n");
            }
            errorMessage.append("\nMaximum demand allowed is ").append(CycleDemandAdapter.MAX_DEMAND_VALUE)
                    .append(" cycles per station.");
            
            new AlertDialog.Builder(Redistribution.this)
                    .setTitle("Maximum Demand Violation")
                    .setMessage(errorMessage.toString())
                    .setPositiveButton("OK", (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .show();
            return;
        }
        
        // PRE-VALIDATION 2: Check if any station violates the <= 4 constraint for pickup/drop cycles
        List<String> violationStations = new ArrayList<>();
        for (CycleDemandAdapter.StationDemandItem item : cycleDemandStationList) {
            String stationId = item.getStationId();
            Integer cycleCount = item.getCycleCount();
            
            if (cycleCount != null) {
                // Use entered demand if available, otherwise use current demand (or 0 if null)
                Integer demand;
                if (demandMap.containsKey(stationId)) {
                    demand = demandMap.get(stationId);
                } else {
                    demand = item.getCurrentDemand();
                    if (demand == null) {
                        demand = 0;
                    }
                }
                
                // Calculate cycles to move (pickup or drop)
                int cyclesToMove = Math.abs(cycleCount - demand);
                
                // Check constraint: cycles to move must be <= 4
                if (cyclesToMove > 4) {
                    String stationName = item.getStationName();
                    violationStations.add(stationName + " (" + stationId + ")");
                }
            }
        }
        
        // If validation failed, show error dialog
        if (!violationStations.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder();
            if (violationStations.size() == 1) {
                errorMessage.append("Station ").append(violationStations.get(0))
                        .append(" violates the constraint.\n\n");
            } else {
                errorMessage.append("The following stations violate the constraint:\n");
                for (String station : violationStations) {
                    errorMessage.append("• ").append(station).append("\n");
                }
                errorMessage.append("\n");
            }
            errorMessage.append("Pickup or drop number cannot exceed 4 cycles per station.");
            
            new AlertDialog.Builder(Redistribution.this)
                    .setTitle("Constraint Violation")
                    .setMessage(errorMessage.toString())
                    .setPositiveButton("OK", (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .show();
            return;
        }
        
        // Calculate difference between cycle count and demand for each station
        // Use entered demand if available, otherwise use current demand
        int totalDifference = 0;
        for (CycleDemandAdapter.StationDemandItem item : cycleDemandStationList) {
            String stationId = item.getStationId();
            Integer cycleCount = item.getCycleCount();
            
            if (cycleCount != null) {
                // Use entered demand if available, otherwise use current demand (or 0 if null)
                Integer demand;
                if (demandMap.containsKey(stationId)) {
                    // User entered a new demand value
                    demand = demandMap.get(stationId);
                } else {
                    // No new demand entered, use current demand from Firebase (or 0)
                    demand = item.getCurrentDemand();
                    if (demand == null) {
                        demand = 0;
                    }
                }
                
                // Calculate (cycleCount - demand) for this station
                int difference = cycleCount - demand;
                totalDifference += difference;
            }
        }
        
        // Validate based on total difference
        if (totalDifference > 0) {
            // Positive: More cycles available than demanded
            String messageText = "You have " + totalDifference + " extra cycles remaining.\n\nPlease increase the demand values by " + totalDifference + " cycles across the stations to balance the redistribution.";
            SpannableString spannableMessage = new SpannableString(messageText);
            int increaseStart = messageText.indexOf("increase");
            int increaseEnd = increaseStart + "increase".length();
            spannableMessage.setSpan(new ForegroundColorSpan(Color.parseColor("#4CAF50")), increaseStart, increaseEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            new AlertDialog.Builder(Redistribution.this)
                    .setTitle("Demand Values Not Balanced")
                    .setMessage(spannableMessage)
                    .setPositiveButton("OK", (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .show();
            return;
        } else if (totalDifference < 0) {
            // Negative: More cycles demanded than available
            int absoluteDifference = Math.abs(totalDifference);
            String messageText = "You have exceeded available cycles by " + absoluteDifference + " cycles.\n\nPlease decrease the demand values by " + absoluteDifference + " cycles across the stations to balance the redistribution.";
            SpannableString spannableMessage = new SpannableString(messageText);
            int decreaseStart = messageText.indexOf("decrease");
            int decreaseEnd = decreaseStart + "decrease".length();
            spannableMessage.setSpan(new ForegroundColorSpan(Color.parseColor("#F44336")), decreaseStart, decreaseEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            new AlertDialog.Builder(Redistribution.this)
                    .setTitle("Demand Values Not Balanced")
                    .setMessage(spannableMessage)
                    .setPositiveButton("OK", (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .show();
            return;
        }
        
        // totalDifference == 0: Valid inputs, show confirmation dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(Redistribution.this);
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
    
    /**
     * Validates the demand values entered by admin
     * Rules:
     * - For each station: calculate (cycleCount - demand)
     * - Sum all these values
     * - If sum > 0: Demand is less (have more cycles than demanded) - return positive value
     * - If sum < 0: Demand is more (need more cycles than available) - return negative value
     * - If sum == 0: Valid (can submit) - return 0
     * 
     * @param demandMap Map of stationId -> demand value
     * @return Difference (0 if valid, positive if demand is less, negative if demand is more)
     */
    private int validateDemandValues(Map<String, Integer> demandMap) {
        int sum = 0;
        
        // Calculate sum of (cycleCount - demand) for all stations with entered demand
        for (CycleDemandAdapter.StationDemandItem item : cycleDemandStationList) {
            String stationId = item.getStationId();
            
            // Only consider stations where demand was entered
            if (demandMap.containsKey(stationId)) {
                Integer cycleCount = item.getCycleCount();
                Integer demand = demandMap.get(stationId);
                
                if (cycleCount != null && demand != null) {
                    // Calculate (cycleCount - demand)
                    int difference = cycleCount - demand;
                    sum += difference;
                }
            }
        }
        
        return sum;
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
    
    /**
     * Helper method to safely get integer value from Firebase DataSnapshot
     * Handles String, Long, and Integer types
     */
    private int getIntValue(DataSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            return 0;
        }
        
        Object value = snapshot.getValue();
        if (value == null) {
            return 0;
        }
        
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Long) {
            return ((Long) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                Log.e("Redistribution", "Error parsing value as integer: " + value, e);
                return 0;
            }
        } else {
            Log.w("Redistribution", "Unexpected type for value: " + value.getClass().getName());
            return 0;
        }
    }
}
