package in.pubbs.pubbsadmin;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import in.pubbs.pubbsadmin.View.CustomLoader;

public class BicycleDetailActivity extends AppCompatActivity {
    private static final String TAG = "BicycleDetailActivity";
    
    private TextView tvBicycleId, tvStatus, tvBattery, tvStationName, tvStationId, 
                     tvAreaId, tvBLEAddress, tvType, tvOperation, tvTheft, 
                     tvUserMobile, tvLatitude, tvLongitude;
    private TextView tvBookingId, tvSourceStationName, tvSourceStationId, tvDestinationStationName, 
                     tvDestinationStationId, tvStartTime, tvEndTime, tvRideTime, tvFare, 
                     tvTripStatus, tvTripBattery, tvBicycleNumber, tvTripBLEAddress, tvDeviceName, tvLastUpdated;
    private ImageView ivBack, ivBicycleIcon;
    private CustomLoader customLoader;
    private CardView cardStatus, cardBattery, cardLocation, cardStation, cardTechnical, cardLastTrip;
    private View statusIndicator;
    
    private String bicycleId;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bicycle_detail);
        
        bicycleId = getIntent().getStringExtra("BICYCLE_ID");
        if (bicycleId == null || bicycleId.isEmpty()) {
            Toast.makeText(this, "Bicycle ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        sharedPreferences = getSharedPreferences("pubbs", Context.MODE_PRIVATE);
        initViews();
        setupClickListeners();
        loadBicycleDetails();
    }

    private void initViews() {
        // Toolbar
        ivBack = findViewById(R.id.back_button);
        TextView tvTitle = findViewById(R.id.toolbar_title);
        tvTitle.setText("Bicycle Details");
        
        // Status indicator
        statusIndicator = findViewById(R.id.status_indicator);
        ivBicycleIcon = findViewById(R.id.iv_bicycle_icon);
        
        // Text views
        tvBicycleId = findViewById(R.id.tv_bicycle_id);
        tvStatus = findViewById(R.id.tv_status);
        tvBattery = findViewById(R.id.tv_battery);
        tvStationName = findViewById(R.id.tv_station_name);
        tvStationId = findViewById(R.id.tv_station_id);
        tvAreaId = findViewById(R.id.tv_area_id);
        tvBLEAddress = findViewById(R.id.tv_ble_address);
        tvType = findViewById(R.id.tv_type);
        tvOperation = findViewById(R.id.tv_operation);
        tvTheft = findViewById(R.id.tv_theft);
        tvUserMobile = findViewById(R.id.tv_user_mobile);
        tvLatitude = findViewById(R.id.tv_latitude);
        tvLongitude = findViewById(R.id.tv_longitude);
        
        // Last Trip Details TextViews
        tvBookingId = findViewById(R.id.tv_booking_id);
        tvSourceStationName = findViewById(R.id.tv_source_station_name);
        tvSourceStationId = findViewById(R.id.tv_source_station_id);
        tvDestinationStationName = findViewById(R.id.tv_destination_station_name);
        tvDestinationStationId = findViewById(R.id.tv_destination_station_id);
        tvStartTime = findViewById(R.id.tv_start_time);
        tvEndTime = findViewById(R.id.tv_end_time);
        tvRideTime = findViewById(R.id.tv_ride_time);
        tvFare = findViewById(R.id.tv_fare);
        tvTripStatus = findViewById(R.id.tv_trip_status);
        tvTripBattery = findViewById(R.id.tv_trip_battery);
        tvBicycleNumber = findViewById(R.id.tv_bicycle_number);
        tvTripBLEAddress = findViewById(R.id.tv_trip_ble_address);
        tvDeviceName = findViewById(R.id.tv_device_name);
        tvLastUpdated = findViewById(R.id.tv_last_updated);
        
        // Custom Loader (animated GIF)
        customLoader = new CustomLoader(this, R.style.WideDialog);
        
        // Cards
        cardStatus = findViewById(R.id.card_status);
        cardBattery = findViewById(R.id.card_battery);
        cardLocation = findViewById(R.id.card_location);
        cardStation = findViewById(R.id.card_station);
        cardTechnical = findViewById(R.id.card_technical);
        cardLastTrip = findViewById(R.id.card_last_trip);
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> {
            // Navigate back to BicycleListActivity
            finish();
        });
    }

    private void loadBicycleDetails() {
        customLoader.show();
        
        String organisationName = sharedPreferences.getString("organisationName", "no_data");
        if (organisationName == null || organisationName.isEmpty()) {
            customLoader.dismiss();
            Toast.makeText(this, "Organisation name not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        String path = organisationName.replaceAll(" ", "") + "/Bicycle";
        Log.d(TAG, "Loading bicycle details from path: " + path + "/" + bicycleId);
        
        DatabaseReference bicycleRef = FirebaseDatabase.getInstance()
                .getReference(path)
                .child(bicycleId);
        
        bicycleRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                customLoader.dismiss();
                
                if (!snapshot.exists()) {
                    Toast.makeText(BicycleDetailActivity.this, "Bicycle data not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                
                // Log all available keys in the snapshot for debugging
                Log.d(TAG, "Bicycle snapshot keys:");
                for (DataSnapshot child : snapshot.getChildren()) {
                    Log.d(TAG, "  - " + child.getKey() + ": " + (child.hasChildren() ? "[object]" : child.getValue()));
                }
                
                // Extract all bicycle data
                String id = getStringValue(snapshot, "id");
                String status = getStringValue(snapshot, "status");
                String battery = getStringValue(snapshot, "battery");
                String stationName = getStringValue(snapshot, "inStationName");
                String stationId = getStringValue(snapshot, "inStationId");
                String areaId = getStringValue(snapshot, "inAreaId");
                String bleAddress = getStringValue(snapshot, "BLEAddress");
                String type = getStringValue(snapshot, "type");
                String operation = getStringValue(snapshot, "operation");
                String theft = getStringValue(snapshot, "theft");
                String userMobile = getStringValue(snapshot, "userMobile");
                String latitude = getStringValue(snapshot, "latitude");
                String longitude = getStringValue(snapshot, "longitude");
                
                // Extract LastTripDetail data
                DataSnapshot lastTripSnapshot = snapshot.child("LastTripDetail");
                Log.d(TAG, "LastTripDetail exists: " + lastTripSnapshot.exists());
                Log.d(TAG, "LastTripDetail hasChildren: " + lastTripSnapshot.hasChildren());
                
                // Log all children to see what's available
                if (lastTripSnapshot.exists()) {
                    Log.d(TAG, "LastTripDetail keys: " + lastTripSnapshot.getKey());
                    for (DataSnapshot child : lastTripSnapshot.getChildren()) {
                        Log.d(TAG, "LastTripDetail child: " + child.getKey() + " = " + child.getValue());
                    }
                }
                
                String bookingId = "N/A";
                String sourceStationName = "N/A";
                String sourceStationId = "N/A";
                String destinationStationName = "N/A";
                String destinationStationId = "N/A";
                String startTime = "N/A";
                String endTime = "N/A";
                String rideTime = "N/A";
                String fare = "N/A";
                String tripStatus = "N/A";
                String tripBattery = "N/A";
                String bicycleNumber = "N/A";
                String tripBLEAddress = "N/A";
                String deviceName = "N/A";
                String lastUpdated = "N/A";
                
                if (lastTripSnapshot.exists() && lastTripSnapshot.hasChildren()) {
                    // Try different possible field name variations
                    bookingId = getStringValue(lastTripSnapshot, "bookingId");
                    if (bookingId.equals("N/A")) {
                        bookingId = getStringValue(lastTripSnapshot, "BookingId");
                    }
                    
                    sourceStationName = getStringValue(lastTripSnapshot, "sourceStationName");
                    if (sourceStationName.equals("N/A")) {
                        sourceStationName = getStringValue(lastTripSnapshot, "SourceStationName");
                    }
                    
                    sourceStationId = getStringValue(lastTripSnapshot, "sourceStationId");
                    if (sourceStationId.equals("N/A")) {
                        sourceStationId = getStringValue(lastTripSnapshot, "SourceStationId");
                    }
                    
                    destinationStationName = getStringValue(lastTripSnapshot, "destinationStationName");
                    if (destinationStationName.equals("N/A")) {
                        destinationStationName = getStringValue(lastTripSnapshot, "DestinationStationName");
                    }
                    
                    destinationStationId = getStringValue(lastTripSnapshot, "destinationStationId");
                    if (destinationStationId.equals("N/A")) {
                        destinationStationId = getStringValue(lastTripSnapshot, "DestinationStationId");
                    }
                    
                    startTime = getStringValue(lastTripSnapshot, "startTime");
                    if (startTime.equals("N/A")) {
                        startTime = getStringValue(lastTripSnapshot, "StartTime");
                    }
                    
                    endTime = getStringValue(lastTripSnapshot, "endTime");
                    if (endTime.equals("N/A")) {
                        endTime = getStringValue(lastTripSnapshot, "EndTime");
                    }
                    
                    rideTime = getStringValue(lastTripSnapshot, "rideTime");
                    if (rideTime.equals("N/A")) {
                        rideTime = getStringValue(lastTripSnapshot, "RideTime");
                    }
                    
                    fare = getStringValue(lastTripSnapshot, "fare");
                    if (fare.equals("N/A")) {
                        fare = getStringValue(lastTripSnapshot, "Fare");
                    }
                    
                    tripStatus = getStringValue(lastTripSnapshot, "status");
                    if (tripStatus.equals("N/A")) {
                        tripStatus = getStringValue(lastTripSnapshot, "Status");
                    }
                    
                    tripBattery = getStringValue(lastTripSnapshot, "battery");
                    if (tripBattery.equals("N/A")) {
                        tripBattery = getStringValue(lastTripSnapshot, "Battery");
                    }
                    
                    bicycleNumber = getStringValue(lastTripSnapshot, "bicycleNumber");
                    if (bicycleNumber.equals("N/A")) {
                        bicycleNumber = getStringValue(lastTripSnapshot, "BicycleNumber");
                    }
                    
                    tripBLEAddress = getStringValue(lastTripSnapshot, "bleaddress");
                    if (tripBLEAddress.equals("N/A")) {
                        tripBLEAddress = getStringValue(lastTripSnapshot, "Bleaddress");
                        if (tripBLEAddress.equals("N/A")) {
                            tripBLEAddress = getStringValue(lastTripSnapshot, "BLEAddress");
                        }
                    }
                    
                    deviceName = getStringValue(lastTripSnapshot, "deviceName");
                    if (deviceName.equals("N/A")) {
                        deviceName = getStringValue(lastTripSnapshot, "DeviceName");
                    }
                    
                    lastUpdated = getStringValue(lastTripSnapshot, "lastUpdated");
                    if (lastUpdated.equals("N/A")) {
                        lastUpdated = getStringValue(lastTripSnapshot, "LastUpdated");
                    }
                    
                    Log.d(TAG, "Extracted bookingId: " + bookingId);
                    Log.d(TAG, "Extracted sourceStationName: " + sourceStationName);
                    Log.d(TAG, "Extracted destinationStationName: " + destinationStationName);
                    Log.d(TAG, "Extracted fare: " + fare);
                } else {
                    Log.d(TAG, "LastTripDetail does not exist or is empty");
                    Log.d(TAG, "Snapshot exists: " + lastTripSnapshot.exists());
                    Log.d(TAG, "Snapshot hasChildren: " + lastTripSnapshot.hasChildren());
                }
                
                // Update UI with data
                updateUI(id, status, battery, stationName, stationId, areaId, 
                        bleAddress, type, operation, theft, userMobile, latitude, longitude,
                        bookingId, sourceStationName, sourceStationId, destinationStationName,
                        destinationStationId, startTime, endTime, rideTime, fare, tripStatus,
                        tripBattery, bicycleNumber, tripBLEAddress, deviceName, lastUpdated);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                customLoader.dismiss();
                Log.e(TAG, "Error loading bicycle details: " + error.getMessage());
                Toast.makeText(BicycleDetailActivity.this, "Error loading data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getStringValue(DataSnapshot snapshot, String key) {
        Object value = snapshot.child(key).getValue();
        if (value == null) {
            return "N/A";
        }
        String strValue = value.toString();
        return (strValue.equals("null") || strValue.isEmpty()) ? "N/A" : strValue;
    }

    private void updateUI(String id, String status, String battery, String stationName, 
                         String stationId, String areaId, String bleAddress, String type, 
                         String operation, String theft, String userMobile, 
                         String latitude, String longitude, String bookingId, 
                         String sourceStationName, String sourceStationId, 
                         String destinationStationName, String destinationStationId,
                         String startTime, String endTime, String rideTime, String fare,
                         String tripStatus, String tripBattery, String bicycleNumber,
                         String tripBLEAddress, String deviceName, String lastUpdated) {
        
        // Bicycle ID
        tvBicycleId.setText(id);
        
        // Status
        tvStatus.setText(status);
        updateStatusIndicator(status, theft);
        
        // Battery
        if (!battery.equals("N/A")) {
            tvBattery.setText(battery + "%");
            try {
                int batteryValue = Integer.parseInt(battery);
                if (batteryValue < 20) {
                    tvBattery.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                } else if (batteryValue < 40) {
                    tvBattery.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                } else {
                    tvBattery.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                }
            } catch (NumberFormatException e) {
                tvBattery.setTextColor(getResources().getColor(android.R.color.black));
            }
        } else {
            tvBattery.setText("N/A");
        }
        
        // Station details
        tvStationName.setText(stationName);
        tvStationId.setText(stationId);
        tvAreaId.setText(areaId);
        
        // Technical details
        tvBLEAddress.setText(bleAddress);
        tvType.setText(type);
        tvOperation.setText(operation);
        
        // Theft status
        if (theft != null && theft.equals("1")) {
            tvTheft.setText("Yes");
            tvTheft.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        } else {
            tvTheft.setText("No");
            tvTheft.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        }
        
        // User mobile
        tvUserMobile.setText(userMobile);
        
        // Location
        tvLatitude.setText(latitude);
        tvLongitude.setText(longitude);
        
        // Last Trip Details
        // Show card if we have any trip data (check bookingId or any other field)
        boolean hasTripData = (bookingId != null && !bookingId.equals("N/A") && !bookingId.isEmpty()) ||
                              (sourceStationName != null && !sourceStationName.equals("N/A")) ||
                              (destinationStationName != null && !destinationStationName.equals("N/A"));
        
        Log.d(TAG, "Has trip data: " + hasTripData);
        Log.d(TAG, "bookingId: " + bookingId);
        Log.d(TAG, "sourceStationName: " + sourceStationName);
        
        if (hasTripData) {
            cardLastTrip.setVisibility(View.VISIBLE);
            tvBookingId.setText(bookingId);
            tvSourceStationName.setText(sourceStationName);
            tvSourceStationId.setText(sourceStationId);
            tvDestinationStationName.setText(destinationStationName);
            tvDestinationStationId.setText(destinationStationId);
            tvStartTime.setText(startTime);
            tvEndTime.setText(endTime);
            
            // Ride time - format if it's a number (in seconds, convert to minutes)
            if (!rideTime.equals("N/A")) {
                try {
                    int rideTimeSeconds = Integer.parseInt(rideTime);
                    int minutes = rideTimeSeconds / 60;
                    int seconds = rideTimeSeconds % 60;
                    tvRideTime.setText(minutes + " min " + seconds + " sec");
                } catch (NumberFormatException e) {
                    tvRideTime.setText(rideTime + " sec");
                }
            } else {
                tvRideTime.setText("N/A");
            }
            
            // Fare - format as currency
            if (!fare.equals("N/A")) {
                tvFare.setText("₹" + fare);
            } else {
                tvFare.setText("N/A");
            }
            
            // Trip status with color
            tvTripStatus.setText(tripStatus);
            if (tripStatus != null && tripStatus.equals("completed")) {
                tvTripStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            } else {
                tvTripStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            }
            
            // Trip battery
            if (!tripBattery.equals("N/A")) {
                tvTripBattery.setText(tripBattery + "%");
            } else {
                tvTripBattery.setText("N/A");
            }
            
            tvBicycleNumber.setText(bicycleNumber);
            tvTripBLEAddress.setText(tripBLEAddress);
            tvDeviceName.setText(deviceName);
            tvLastUpdated.setText(lastUpdated);
        } else {
            // Hide last trip card if no trip data available
            cardLastTrip.setVisibility(View.GONE);
            Log.d(TAG, "Hiding last trip card - bookingId: " + bookingId + ", sourceStationName: " + sourceStationName);
        }
    }

    private void updateStatusIndicator(String status, String theft) {
        int statusDrawable;
        int statusIcon;
        
        if (theft != null && theft.equals("1")) {
            // Theft detected - black
            statusDrawable = R.drawable.solid_circle_black;
            statusIcon = R.drawable.ic_cycle_red;
        } else if (status != null && status.equals("active")) {
            // Active - green
            statusDrawable = R.drawable.solid_circle_green;
            statusIcon = R.drawable.ic_cycle_green;
        } else if (status != null && status.equals("busy")) {
            // Busy - yellow/orange
            statusDrawable = R.drawable.solid_circle_yellow;
            statusIcon = R.drawable.ic_cycle_rider;
        } else {
            // Inactive/Other - red
            statusDrawable = R.drawable.solid_circle_red;
            statusIcon = R.drawable.ic_cycle_red;
        }
        
        statusIndicator.setBackgroundResource(statusDrawable);
        if (ivBicycleIcon != null) {
            ivBicycleIcon.setImageResource(statusIcon);
        }
    }
}

