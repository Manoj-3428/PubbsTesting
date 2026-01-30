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
import in.pubbs.pubbsadmin.Api.ApiClient;
import in.pubbs.pubbsadmin.Api.CycleDemandApiService;
import in.pubbs.pubbsadmin.Api.AllStationsDemandResponse;
import in.pubbs.pubbsadmin.View.CustomLoader;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
    
    // Track changes and fetch source
    private boolean hasUnsavedChanges = false;
    private boolean isFetchingFromApi = false;
    private CycleDemandApiService apiService;
    private CustomLoader loader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_redistribution);

        initView();
        initFirebase();
        setupInitialButtons();
        
        // Initialize CustomLoader
        loader = new CustomLoader(this, R.style.WideDialog);

        // Existing redistribution button click handlers
        redistributeBtn.setOnClickListener(v -> redistributeCycles());
        doneRedistributionBtn.setOnClickListener(v -> applyRedistribution());
        
        // Cycle Demand submit button
        submitCycleDemandBtn.setOnClickListener(v -> showConfirmationDialog());
    }
    
    @Override
    public void onBackPressed() {
        // If initial buttons are hidden, check for unsaved changes
        if (initialButtonsContainer.getVisibility() != View.VISIBLE) {
            // Check if we're on cycle demand screen and have unsaved changes
            if (cycleDemandRecyclerView.getVisibility() == View.VISIBLE && hasUnsavedChanges) {
                // Show save changes dialog
                showSaveChangesDialog();
            } else {
                // Reset to initial state
                resetToInitialState();
            }
        } else {
            // Otherwise, use default back button behavior
            super.onBackPressed();
        }
    }
    
    /**
     * Show dialog asking user if they want to save changes
     */
    private void showSaveChangesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(Redistribution.this);
        builder.setTitle("Unsaved Changes");
        builder.setMessage("You have unsaved changes. Do you want to save them?");
        
        builder.setPositiveButton("Save", (dialog, which) -> {
            // Save changes and then go back
            Map<String, Integer> demandMap = cycleDemandAdapter.getDemandMap();
            if (!demandMap.isEmpty()) {
                saveCycleDemandToFirebase(demandMap);
            }
            resetToInitialState();
            dialog.dismiss();
        });
        
        builder.setNegativeButton("Don't Save", (dialog, which) -> {
            // Discard changes and go back
            resetToInitialState();
            dialog.dismiss();
        });
        
        builder.setNeutralButton("Cancel", (dialog, which) -> {
            // Stay on current screen
            dialog.dismiss();
        });
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    /**
     * Reset UI to initial state
     */
    private void resetToInitialState() {
        initialButtonsContainer.setVisibility(View.VISIBLE);
        cycleDemandRecyclerView.setVisibility(View.GONE);
        submitCycleDemandBtn.setVisibility(View.GONE);
        redistributeBtn.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        doneRedistributionBtn.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        noData.setVisibility(View.GONE);
        toolbarTitle.setText("Redistribution");
        hasUnsavedChanges = false;
        isFetchingFromApi = false;
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
        // Set listener to track changes
        cycleDemandAdapter.setOnDemandChangeListener(() -> {
            hasUnsavedChanges = true;
        });
        cycleDemandRecyclerView.setAdapter(cycleDemandAdapter);
    }
    
    private void setupInitialButtons() {
        // Cycle Demand button - show dialog with 2 options
        cycleDemandBtn.setOnClickListener(v -> {
            showCycleDemandOptionsDialog();
        });
        
        // Redistribute button (initial) - show existing redistribution flow
        redistributeBtnInitial.setOnClickListener(v -> {
            showRedistributionScreen();
        });
    }
    
    /**
     * Show dialog with options to fetch from database or API
     */
    private void showCycleDemandOptionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(Redistribution.this);
        builder.setTitle("Fetch Cycle Demand");
        
        // Create options array
        String[] options = new String[]{
            "Fetch from Database",
            "Fetch from API"
        };
        
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                // Fetch from Database (existing behavior)
                isFetchingFromApi = false;
                showCycleDemandScreen();
            } else if (which == 1) {
                // Fetch from API
                isFetchingFromApi = true;
                showCycleDemandScreen();
                // API fetch will be called after stations are loaded (in fetchStationsForCycleDemand)
            }
            dialog.dismiss();
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        
        AlertDialog dialog = builder.create();
        dialog.show();
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
        
        // Reset unsaved changes flag
        hasUnsavedChanges = false;
        
        // Fetch stations from Firebase (for both database and API flows)
        // For API flow, we'll update the demand values after API call completes
        fetchStationsForCycleDemand();
    }
    
    private void showRedistributionScreen() {
        // Show dialog to select vehicle view
        AlertDialog.Builder builder = new AlertDialog.Builder(Redistribution.this);
        builder.setTitle("Select Vehicle Route");
        
        String[] vehicleOptions = {
            "Vehicle 1",
            "Vehicle 2",
            "Total Path"
        };
        
        // When user clicks an option, launch with selected vehicle
        builder.setItems(vehicleOptions, (dialog, which) -> {
            String selectedVehicle = null;
            if (which == 0) {
                selectedVehicle = "vehicle1";
            } else if (which == 1) {
                selectedVehicle = "vehicle2";
            } else if (which == 2) {
                selectedVehicle = "total";
            }
            
            // Launch RedistributionMapActivity with selected vehicle
            Intent intent = new Intent(Redistribution.this, RedistributionMapActivity.class);
            intent.putExtra("selected_vehicle", selectedVehicle);
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
                    // If fetching from API, don't load demand from database - set it to null
                    Integer demandValue = null;
                    if (!isFetchingFromApi) {
                        // Only load demand from database if NOT fetching from API
                        DataSnapshot demandSnap = stationSnap.child("stationCycleDemand");
                        if (demandSnap.exists()) {
                            Object demandObj = demandSnap.getValue();
                            if (demandObj instanceof Number) {
                                demandValue = ((Number) demandObj).intValue();
                            }
                        }
                    }
                    // If isFetchingFromApi is true, demandValue remains null
                    
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
                    
                    // If fetching from API, call API now that stations are loaded
                    if (isFetchingFromApi) {
                        // Show CustomLoader while fetching from API
                        if (loader != null && !loader.isShowing()) {
                            loader.show();
                        }
                        fetchCycleDemandFromApi();
                    } else {
                        progressBar.setVisibility(View.GONE);
                    }
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
    
    /**
     * Fetch cycle demand from API for all stations
     * This will fetch demand for all stations at once and update the UI
     */
    private void fetchCycleDemandFromApi() {
        if (apiService == null) {
            apiService = ApiClient.getApiService();
        }
        
        if (apiService == null) {
            Log.e("Redistribution", "API Service not initialized");
            Toast.makeText(this, "API service not available. Please check API configuration.", Toast.LENGTH_LONG).show();
            progressBar.setVisibility(View.GONE);
            return;
        }
        
        // Hide progress bar (CustomLoader is showing)
        progressBar.setVisibility(View.GONE);
        noData.setVisibility(View.GONE);
        
        // Call API to get all stations' demand at once
        Call<AllStationsDemandResponse> call = apiService.getAllStationsDemand();
        
        call.enqueue(new Callback<AllStationsDemandResponse>() {
            @Override
            public void onResponse(Call<AllStationsDemandResponse> call, Response<AllStationsDemandResponse> response) {
                // Dismiss CustomLoader
                if (loader != null && loader.isShowing()) {
                    loader.dismiss();
                }
                progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    AllStationsDemandResponse apiResponse = response.body();
                    
                    if (apiResponse.isSuccess() && apiResponse.getDemands() != null) {
                        Map<String, Integer> apiDemands = apiResponse.getDemands();
                        
                        // Update the station list items with API values
                        // If API doesn't have a station, keep demand as null (user can enter manually)
                        for (int i = 0; i < cycleDemandStationList.size(); i++) {
                            CycleDemandAdapter.StationDemandItem item = cycleDemandStationList.get(i);
                            String stationId = item.getStationId();
                            Integer apiDemand = apiDemands.get(stationId); // Will be null if not in API response
                            
                            // Create new item with API demand value (or null if not provided)
                            CycleDemandAdapter.StationDemandItem updatedItem = 
                                new CycleDemandAdapter.StationDemandItem(
                                    item.getStationId(),
                                    item.getStationName(),
                                    apiDemand, // Can be null if API didn't return this station
                                    item.getCycleCount()
                                );
                            cycleDemandStationList.set(i, updatedItem);
                        }
                        
                        // Notify adapter to refresh the UI with API values
                        cycleDemandAdapter.notifyDataSetChanged();
                        
                        // Immediately update database with API values (only for stations that API returned)
                        updateDatabaseWithApiDemands(apiDemands);
                        
                        // Reset unsaved changes flag since we just fetched from API
                        hasUnsavedChanges = false;
                        
                        // Show UI
                        if (cycleDemandStationList.isEmpty()) {
                            noData.setVisibility(View.VISIBLE);
                            noData.setText("No stations found!");
                            cycleDemandRecyclerView.setVisibility(View.GONE);
                            submitCycleDemandBtn.setVisibility(View.GONE);
                        } else {
                            noData.setVisibility(View.GONE);
                            cycleDemandRecyclerView.setVisibility(View.VISIBLE);
                            submitCycleDemandBtn.setVisibility(View.VISIBLE);
                        }
                        
                        Toast.makeText(Redistribution.this, "Cycle demand fetched from API successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        String errorMsg = apiResponse != null ? apiResponse.getMessage() : "Unknown error";
                        Log.e("Redistribution", "API returned error: " + errorMsg);
                        Toast.makeText(Redistribution.this, "API Error: " + errorMsg, Toast.LENGTH_LONG).show();
                        noData.setVisibility(View.VISIBLE);
                        noData.setText("Error fetching from API: " + errorMsg);
                        // Still show the stations even if API failed, so user can enter manually
                        if (!cycleDemandStationList.isEmpty()) {
                            cycleDemandRecyclerView.setVisibility(View.VISIBLE);
                            submitCycleDemandBtn.setVisibility(View.VISIBLE);
                        }
                    }
                } else {
                    String errorMsg = "Failed to fetch from API. Response code: " + response.code();
                    Log.e("Redistribution", errorMsg);
                    Toast.makeText(Redistribution.this, errorMsg, Toast.LENGTH_LONG).show();
                    noData.setVisibility(View.VISIBLE);
                    noData.setText("Error fetching from API");
                }
            }
            
            @Override
            public void onFailure(Call<AllStationsDemandResponse> call, Throwable t) {
                // Dismiss CustomLoader
                if (loader != null && loader.isShowing()) {
                    loader.dismiss();
                }
                progressBar.setVisibility(View.GONE);
                String errorMsg = "Network error: " + (t.getMessage() != null ? t.getMessage() : "Unknown error");
                Log.e("Redistribution", "API call failed: " + errorMsg, t);
                Toast.makeText(Redistribution.this, errorMsg, Toast.LENGTH_LONG).show();
                noData.setVisibility(View.VISIBLE);
                noData.setText("Failed to connect to API");
            }
        });
    }
    
    /**
     * Update database immediately with API demand values
     * This is called right after fetching from API
     */
    private void updateDatabaseWithApiDemands(Map<String, Integer> apiDemands) {
        if (stationRootRef == null || apiDemands == null || apiDemands.isEmpty()) {
            return;
        }
        
        Log.d("Redistribution", "Updating database with API demands for " + apiDemands.size() + " stations");
        
        // Update each station's demand in Firebase
        for (Map.Entry<String, Integer> entry : apiDemands.entrySet()) {
            String stationId = entry.getKey();
            int demand = entry.getValue();
            
            DatabaseReference stationRef = stationRootRef.child(stationId);
            stationRef.child("stationCycleDemand").setValue(demand)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Redistribution", "Updated station " + stationId + " demand to " + demand);
                })
                .addOnFailureListener(e -> {
                    Log.e("Redistribution", "Failed to update station " + stationId + " demand: " + e.getMessage());
                });
        }
        
        Log.d("Redistribution", "Database update initiated for all stations");
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
        hasUnsavedChanges = false;
        resetToInitialState();
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
        
        // Initialize API service
        apiService = ApiClient.getApiService();
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
