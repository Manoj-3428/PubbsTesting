package in.pubbs.pubbsadmin;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import in.pubbs.pubbsadmin.View.CustomLoader;
import in.pubbs.pubbsadmin.View.CustomAlertDialog;

public class UserDetailActivity extends AppCompatActivity {
    private static final String TAG = "UserDetailActivity";
    
    private TextView tvUserId, tvUserName, tvMobile, tvEmail, tvOperator, 
                     tvAddress, tvRegistrationDate, tvStatus, tvTotalTrips, tvTotalSpent;
    private TextView tvGender, tvImei, tvHoldTime, tvRideOnGoingStatus, tvRideTime;
    private TextView btnClearRideId;
    private TextView tvRideId, tvCurrentBookingId, tvCurrentSourceStation, tvCurrentDestinationStation,
                     tvCurrentStartTime, tvCurrentRideStatus, tvCurrentBicycleId;
    private TextView tvLastBookingId, tvLastSourceStation, tvLastDestinationStation,
                     tvLastStartTime, tvLastEndTime, tvLastRideTime, tvLastFare, tvLastRideStatus;
    private ImageView ivBack, ivUserIcon;
    private CustomLoader customLoader;
    private CardView cardBasic, cardContact, cardAccount, cardStatistics, cardRideId, cardCurrentRide, cardLastRide;
    private View statusIndicator;
    
    private String userMobile;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_detail);
        
        userMobile = getIntent().getStringExtra("USER_MOBILE");
        if (userMobile == null || userMobile.isEmpty()) {
            Toast.makeText(this, "User mobile not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        sharedPreferences = getSharedPreferences("pubbs", Context.MODE_PRIVATE);
        initViews();
        setupClickListeners();
        loadUserDetails();
    }

    private void initViews() {
        // Toolbar
        ivBack = findViewById(R.id.back_button);
        TextView tvTitle = findViewById(R.id.toolbar_title);
        tvTitle.setText("User Details");
        
        // Status indicator
        statusIndicator = findViewById(R.id.status_indicator);
        ivUserIcon = findViewById(R.id.iv_user_icon);
        
        // Text views
        tvUserId = findViewById(R.id.tv_user_id);
        tvUserName = findViewById(R.id.tv_user_name);
        tvMobile = findViewById(R.id.tv_mobile);
        tvEmail = findViewById(R.id.tv_email);
        tvOperator = findViewById(R.id.tv_operator);
        tvAddress = findViewById(R.id.tv_address);
        tvRegistrationDate = findViewById(R.id.tv_registration_date);
        tvStatus = findViewById(R.id.tv_status);
        tvTotalTrips = findViewById(R.id.tv_total_trips);
        tvTotalSpent = findViewById(R.id.tv_total_spent);
        
        // Additional fields
        tvGender = findViewById(R.id.tv_gender);
        tvImei = findViewById(R.id.tv_imei);
        tvHoldTime = findViewById(R.id.tv_hold_time);
        tvRideOnGoingStatus = findViewById(R.id.tv_ride_on_going_status);
        tvRideTime = findViewById(R.id.tv_ride_time);
        
        // Custom Loader (animated GIF)
        customLoader = new CustomLoader(this, R.style.WideDialog);
        
        // Cards
        cardBasic = findViewById(R.id.card_basic);
        cardContact = findViewById(R.id.card_contact);
        cardAccount = findViewById(R.id.card_account);
        cardStatistics = findViewById(R.id.card_statistics);
        cardRideId = findViewById(R.id.card_ride_id);
        cardCurrentRide = findViewById(R.id.card_current_ride);
        cardLastRide = findViewById(R.id.card_last_ride);
        
        // Ride ID
        tvRideId = findViewById(R.id.tv_ride_id);
        
        // Current Ride TextViews
        tvCurrentBookingId = findViewById(R.id.tv_current_booking_id);
        tvCurrentSourceStation = findViewById(R.id.tv_current_source_station);
        tvCurrentDestinationStation = findViewById(R.id.tv_current_destination_station);
        tvCurrentStartTime = findViewById(R.id.tv_current_start_time);
        tvCurrentRideStatus = findViewById(R.id.tv_current_ride_status);
        tvCurrentBicycleId = findViewById(R.id.tv_current_bicycle_id);
        
        // Last Ride TextViews
        tvLastBookingId = findViewById(R.id.tv_last_booking_id);
        tvLastSourceStation = findViewById(R.id.tv_last_source_station);
        tvLastDestinationStation = findViewById(R.id.tv_last_destination_station);
        tvLastStartTime = findViewById(R.id.tv_last_start_time);
        tvLastEndTime = findViewById(R.id.tv_last_end_time);
        tvLastRideTime = findViewById(R.id.tv_last_ride_time);
        tvLastFare = findViewById(R.id.tv_last_fare);
        tvLastRideStatus = findViewById(R.id.tv_last_ride_status);
        
        // Button
        btnClearRideId = findViewById(R.id.btn_clear_ride_id);
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> {
            // Navigate back to ManageUser
            finish();
        });
        
        btnClearRideId.setOnClickListener(v -> {
            showClearRideIdConfirmation();
        });
    }
    
    private void showClearRideIdConfirmation() {
        CustomAlertDialog dialog = new CustomAlertDialog(
            this,
            R.style.WideDialog,
            "Clear Ride ID",
            "Are you sure you want to clear the Ride ID? This action cannot be undone."
        );
        dialog.show();
        
        dialog.onPositiveButton(v -> {
            dialog.dismiss();
            clearRideId();
        });
        
        dialog.onNegativeButton(v -> {
            dialog.dismiss();
        });
    }
    
    private void clearRideId() {
        customLoader.show();
        
        String path = "Users/" + userMobile + "/rideId";
        Log.d(TAG, "Clearing rideId at path: " + path);
        
        DatabaseReference rideIdRef = FirebaseDatabase.getInstance()
                .getReference(path);
        
        rideIdRef.setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                customLoader.dismiss();
                if (task.isSuccessful()) {
                    Toast.makeText(UserDetailActivity.this, "Ride ID cleared successfully", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Ride ID cleared successfully");
                    // Update UI
                    tvRideId.setText("No active ride");
                    tvRideId.setTextColor(getResources().getColor(android.R.color.darker_gray));
                    cardCurrentRide.setVisibility(View.GONE);
                } else {
                    Toast.makeText(UserDetailActivity.this, "Failed to clear Ride ID", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error clearing rideId: " + task.getException());
                }
            }
        });
    }

    private void loadUserDetails() {
        customLoader.show();
        
        String path = "Users/" + userMobile;
        Log.d(TAG, "Loading user details from path: " + path);
        
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference(path);
        
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                customLoader.dismiss();
                
                if (!snapshot.exists()) {
                    Toast.makeText(UserDetailActivity.this, "User data not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                
                // Log all available keys in the snapshot for debugging
                Log.d(TAG, "User snapshot keys:");
                for (DataSnapshot child : snapshot.getChildren()) {
                    Log.d(TAG, "  - " + child.getKey() + ": " + (child.hasChildren() ? "[object]" : child.getValue()));
                }
                
                // Extract all user data
                String userId = getStringValue(snapshot, "user_id");
                if (userId.equals("N/A")) {
                    userId = getStringValue(snapshot, "id");
                }
                String name = getStringValue(snapshot, "name");
                String mobile = getStringValue(snapshot, "mobile");
                String email = getStringValue(snapshot, "email");
                String operator = getStringValue(snapshot, "operator");
                String address = getStringValue(snapshot, "address");
                String registrationDate = getStringValue(snapshot, "registrationDate");
                String status = getStringValue(snapshot, "status");
                String totalTrips = getStringValue(snapshot, "totalTrips");
                String totalSpent = getStringValue(snapshot, "totalSpent");
                
                // Additional fields from Firebase
                String gender = getStringValue(snapshot, "gender");
                String imei = getStringValue(snapshot, "imei");
                String holdTime = getStringValue(snapshot, "holdTime");
                String rideOnGoingStatus = getStringValue(snapshot, "rideOnGoingStatus");
                String rideTime = getStringValue(snapshot, "rideTime");
                
                // Try alternative field names
                if (email.equals("N/A")) {
                    email = getStringValue(snapshot, "Email");
                }
                if (address.equals("N/A")) {
                    address = getStringValue(snapshot, "Address");
                }
                if (registrationDate.equals("N/A")) {
                    registrationDate = getStringValue(snapshot, "RegistrationDate");
                    if (registrationDate.equals("N/A")) {
                        registrationDate = getStringValue(snapshot, "createdAt");
                    }
                }
                if (status.equals("N/A")) {
                    status = getStringValue(snapshot, "Status");
                    if (status.equals("N/A")) {
                        status = getStringValue(snapshot, "isActive");
                        if (!status.equals("N/A")) {
                            status = status.equals("true") ? "Active" : "Inactive";
                        }
                    }
                }
                
                // Extract rideId
                String rideId = getStringValue(snapshot, "rideId");
                // Handle null string
                if (rideId.equals("null")) {
                    rideId = "N/A";
                }
                
                // Update UI with data
                updateUI(userId, name, mobile, email, operator, address, 
                        registrationDate, status, totalTrips, totalSpent, rideId,
                        gender, imei, holdTime, rideOnGoingStatus, rideTime);
                
                // Fetch ride details if rideId exists
                if (!rideId.equals("N/A") && !rideId.isEmpty() && !rideId.equals("null")) {
                    loadCurrentRideDetails(rideId);
                } else {
                    cardCurrentRide.setVisibility(View.GONE);
                }
                
                // Fetch last ride details from Trips node
                loadLastRideDetails(userMobile);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                customLoader.dismiss();
                Log.e(TAG, "Error loading user details: " + error.getMessage());
                Toast.makeText(UserDetailActivity.this, "Error loading data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void loadCurrentRideDetails(String rideId) {
        Log.d(TAG, "Loading current ride details for rideId: " + rideId);
        
        // Try different possible paths for ride data
        String orgName = sharedPreferences.getString("organisationName", "").replace(" ", "");
        String path = orgName + "/Booking/" + rideId;
        
        DatabaseReference rideRef = FirebaseDatabase.getInstance().getReference(path);
        rideRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Log.d(TAG, "Found current ride at path: " + path);
                    
                    String bookingId = getStringValue(snapshot, "bookingId");
                    if (bookingId.equals("N/A")) {
                        bookingId = getStringValue(snapshot, "BookingId");
                        if (bookingId.equals("N/A")) {
                            bookingId = rideId; // Use rideId as bookingId if not found
                        }
                    }
                    
                    String sourceStation = getStringValue(snapshot, "sourceStationName");
                    if (sourceStation.equals("N/A")) {
                        sourceStation = getStringValue(snapshot, "SourceStationName");
                    }
                    
                    String destinationStation = getStringValue(snapshot, "destinationStationName");
                    if (destinationStation.equals("N/A")) {
                        destinationStation = getStringValue(snapshot, "DestinationStationName");
                    }
                    
                    String startTime = getStringValue(snapshot, "startTime");
                    if (startTime.equals("N/A")) {
                        startTime = getStringValue(snapshot, "StartTime");
                    }
                    
                    String rideStatus = getStringValue(snapshot, "status");
                    if (rideStatus.equals("N/A")) {
                        rideStatus = getStringValue(snapshot, "Status");
                    }
                    
                    String bicycleId = getStringValue(snapshot, "bicycleId");
                    if (bicycleId.equals("N/A")) {
                        bicycleId = getStringValue(snapshot, "BicycleId");
                        if (bicycleId.equals("N/A")) {
                            bicycleId = getStringValue(snapshot, "bicycleNumber");
                        }
                    }
                    
                    updateCurrentRideUI(bookingId, sourceStation, destinationStation, startTime, rideStatus, bicycleId);
                    cardCurrentRide.setVisibility(View.VISIBLE);
                } else {
                    Log.d(TAG, "Current ride not found at path: " + path);
                    cardCurrentRide.setVisibility(View.GONE);
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading current ride: " + error.getMessage());
                cardCurrentRide.setVisibility(View.GONE);
            }
        });
    }
    
    private void loadLastRideDetails(String userMobile) {
        Log.d(TAG, "Loading last ride details from Users/" + userMobile + "/Trips");
        
        // Fetch from Users/{userMobile}/Trips
        String path = "Users/" + userMobile + "/Trips";
        DatabaseReference tripsRef = FirebaseDatabase.getInstance().getReference(path);
        
        tripsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.hasChildren()) {
                    // Find the latest trip (by trackLocationTime or last child)
                    DataSnapshot lastTripSnapshot = null;
                    String latestTime = "";
                    
                    for (DataSnapshot trip : snapshot.getChildren()) {
                        String trackTime = getStringValue(trip, "trackLocationTime");
                        if (!trackTime.equals("N/A") && trackTime.compareTo(latestTime) > 0) {
                            latestTime = trackTime;
                            lastTripSnapshot = trip;
                        }
                    }
                    
                    // If no trackLocationTime found, use the last child
                    if (lastTripSnapshot == null) {
                        for (DataSnapshot trip : snapshot.getChildren()) {
                            lastTripSnapshot = trip;
                        }
                    }
                    
                    if (lastTripSnapshot != null && lastTripSnapshot.exists()) {
                        Log.d(TAG, "Last trip found: " + lastTripSnapshot.getKey());
                        
                        String bookingId = lastTripSnapshot.getKey(); // Use trip key as bookingId
                        String sourceStation = getStringValue(lastTripSnapshot, "sourceStationName");
                        String destinationStation = getStringValue(lastTripSnapshot, "destinationStationName");
                        String fare = getStringValue(lastTripSnapshot, "fare");
                        String rideTimer = getStringValue(lastTripSnapshot, "rideTimer");
                        String holdTimer = getStringValue(lastTripSnapshot, "holdTimer");
                        String totalTripTime = getStringValue(lastTripSnapshot, "totalTripTime");
                        String trackLocationTime = getStringValue(lastTripSnapshot, "trackLocationTime");
                        
                        // Format ride time (rideTimer is in seconds)
                        String rideTime = rideTimer;
                        if (!rideTime.equals("N/A")) {
                            try {
                                int rideTimeSeconds = Integer.parseInt(rideTime);
                                int minutes = rideTimeSeconds / 60;
                                int seconds = rideTimeSeconds % 60;
                                rideTime = minutes + " min " + seconds + " sec";
                            } catch (NumberFormatException e) {
                                rideTime = rideTime + " sec";
                            }
                        }
                        
                        // Format fare
                        if (!fare.equals("N/A")) {
                            fare = "₹" + fare;
                        }
                        
                        // Use trackLocationTime as start time, and calculate end time if possible
                        String startTime = trackLocationTime;
                        String endTime = "N/A";
                        if (!totalTripTime.equals("N/A") && !startTime.equals("N/A")) {
                            // Could calculate end time if needed
                            endTime = "N/A";
                        }
                        
                        String rideStatus = "completed"; // Trips are usually completed
                        
                        boolean hasTripData = (bookingId != null && !bookingId.equals("N/A") && !bookingId.isEmpty()) ||
                                              (sourceStation != null && !sourceStation.equals("N/A")) ||
                                              (destinationStation != null && !destinationStation.equals("N/A"));
                        
                        if (hasTripData) {
                            updateLastRideUI(bookingId, sourceStation, destinationStation, startTime, endTime, rideTime, fare, rideStatus);
                            cardLastRide.setVisibility(View.VISIBLE);
                        } else {
                            cardLastRide.setVisibility(View.GONE);
                        }
                    } else {
                        Log.d(TAG, "No trips found");
                        cardLastRide.setVisibility(View.GONE);
                    }
                } else {
                    Log.d(TAG, "Trips node does not exist or is empty");
                    cardLastRide.setVisibility(View.GONE);
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading last ride details: " + error.getMessage());
                cardLastRide.setVisibility(View.GONE);
            }
        });
    }
    
    private void updateCurrentRideUI(String bookingId, String sourceStation, String destinationStation,
                                     String startTime, String status, String bicycleId) {
        tvCurrentBookingId.setText(bookingId);
        tvCurrentSourceStation.setText(sourceStation);
        tvCurrentDestinationStation.setText(destinationStation);
        tvCurrentStartTime.setText(startTime);
        tvCurrentRideStatus.setText(status);
        tvCurrentBicycleId.setText(bicycleId);
        
        // Set status color
        if (status != null && status.equalsIgnoreCase("active") || status.equalsIgnoreCase("ongoing")) {
            tvCurrentRideStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            tvCurrentRideStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
        }
    }
    
    private void updateLastRideUI(String bookingId, String sourceStation, String destinationStation,
                                 String startTime, String endTime, String rideTime, String fare, String status) {
        tvLastBookingId.setText(bookingId);
        tvLastSourceStation.setText(sourceStation);
        tvLastDestinationStation.setText(destinationStation);
        tvLastStartTime.setText(startTime);
        tvLastEndTime.setText(endTime);
        tvLastRideTime.setText(rideTime);
        tvLastFare.setText(fare);
        tvLastRideStatus.setText(status);
        
        // Set status color
        if (status != null && status.equalsIgnoreCase("completed")) {
            tvLastRideStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            tvLastRideStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
        }
    }

    private String getStringValue(DataSnapshot snapshot, String key) {
        Object value = snapshot.child(key).getValue();
        if (value == null) {
            return "N/A";
        }
        String strValue = value.toString();
        return (strValue.equals("null") || strValue.isEmpty()) ? "N/A" : strValue;
    }

    private void updateUI(String userId, String name, String mobile, String email, 
                         String operator, String address, String registrationDate,
                         String status, String totalTrips, String totalSpent, String rideId,
                         String gender, String imei, String holdTime, String rideOnGoingStatus, String rideTime) {
        
        // Update Ride ID
        if (rideId.equals("N/A") || rideId.isEmpty()) {
            tvRideId.setText("No active ride");
            tvRideId.setTextColor(getResources().getColor(android.R.color.darker_gray));
        } else {
            tvRideId.setText(rideId);
            tvRideId.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        }
        
        // User ID
        tvUserId.setText(userId);
        
        // User Name
        tvUserName.setText(name);
        
        // Mobile
        tvMobile.setText(mobile);
        
        // Email
        tvEmail.setText(email);
        
        // Operator
        tvOperator.setText(operator);
        
        // Address
        tvAddress.setText(address);
        
        // Registration Date
        tvRegistrationDate.setText(registrationDate);
        
        // Status
        tvStatus.setText(status);
        if (status != null && (status.equals("Active") || status.equals("active"))) {
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            statusIndicator.setBackgroundResource(R.drawable.solid_circle_green);
        } else {
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            statusIndicator.setBackgroundResource(R.drawable.solid_circle_red);
        }
        
        // Statistics
        if (!totalTrips.equals("N/A")) {
            tvTotalTrips.setText(totalTrips);
        } else {
            tvTotalTrips.setText("0");
        }
        
        if (!totalSpent.equals("N/A")) {
            tvTotalSpent.setText("₹" + totalSpent);
        } else {
            tvTotalSpent.setText("₹0");
        }
        
        // Additional fields
        if (tvGender != null) {
            tvGender.setText(gender);
        }
        if (tvImei != null) {
            tvImei.setText(imei);
        }
        if (tvHoldTime != null) {
            tvHoldTime.setText(holdTime + " sec");
        }
        if (tvRideOnGoingStatus != null) {
            tvRideOnGoingStatus.setText(rideOnGoingStatus.equals("true") ? "Yes" : "No");
            tvRideOnGoingStatus.setTextColor(rideOnGoingStatus.equals("true") ? 
                getResources().getColor(android.R.color.holo_green_dark) : 
                getResources().getColor(android.R.color.darker_gray));
        }
        if (tvRideTime != null) {
            if (!rideTime.equals("N/A") && !rideTime.equals("0")) {
                try {
                    int rideTimeSeconds = Integer.parseInt(rideTime);
                    int minutes = rideTimeSeconds / 60;
                    int seconds = rideTimeSeconds % 60;
                    tvRideTime.setText(minutes + " min " + seconds + " sec");
                } catch (NumberFormatException e) {
                    tvRideTime.setText(rideTime + " sec");
                }
            } else {
                tvRideTime.setText("0 sec");
            }
        }
    }
}

