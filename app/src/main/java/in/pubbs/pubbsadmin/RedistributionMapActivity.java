package in.pubbs.pubbsadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RedistributionMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "RedistributionMap";
    
    private MapView mapView;
    private GoogleMap googleMap;
    private SharedPreferences sharedPreferences;
    private String organisation;
    private DatabaseReference stationRootRef;
    
    // Data structures
    private List<StationData> allStations = new ArrayList<>();
    private List<StationData> pickupStations = new ArrayList<>();
    private List<StationData> dropStations = new ArrayList<>();
    private Map<String, Marker> stationMarkers = new HashMap<>();
    private List<Polyline> routePolylines = new ArrayList<>();
    
    private Button redistributionFinishedBtn;
    private TextView toolbarTitle;
    private ImageView backButton;
    
    // Station data class
    private static class StationData {
        String stationId;
        String stationName;
        double latitude;
        double longitude;
        int cycleCount;
        int cycleDemand;
        String stationType; // "pickup", "drop", or "none"
        int cyclesToMove; // positive for pickup, negative for drop
        
        StationData(String stationId, String stationName, double latitude, double longitude, 
                   int cycleCount, int cycleDemand) {
            this.stationId = stationId;
            this.stationName = stationName;
            this.latitude = latitude;
            this.longitude = longitude;
            this.cycleCount = cycleCount;
            this.cycleDemand = cycleDemand;
            
            // Classify station type
            if (cycleCount > cycleDemand) {
                this.stationType = "pickup";
                this.cyclesToMove = cycleCount - cycleDemand;
            } else if (cycleCount < cycleDemand) {
                this.stationType = "drop";
                this.cyclesToMove = cycleDemand - cycleCount;
            } else {
                this.stationType = "none";
                this.cyclesToMove = 0;
            }
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_redistribution_map);
        
        // Read organisation from SharedPreferences
        sharedPreferences = getSharedPreferences("pubbs", MODE_PRIVATE);
        organisation = sharedPreferences.getString("organisationName", "").replaceAll(" ", "");
        
        // Initialize Firebase
        if (organisation != null && !organisation.isEmpty()) {
            stationRootRef = FirebaseDatabase.getInstance().getReference(organisation + "/Station");
        }
        
        // Initialize map
        mapView = findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        
        // Initialize UI
        redistributionFinishedBtn = findViewById(R.id.redistribution_finished_btn);
        toolbarTitle = findViewById(R.id.tv_title);
        backButton = findViewById(R.id.iv_back);
        
        toolbarTitle.setText("Area Manager");
        backButton.setOnClickListener(v -> onBackPressed());
        
        // Redistribution Finished button
        redistributionFinishedBtn.setOnClickListener(v -> handleRedistributionFinished());
        
        ImageView ivMenu = findViewById(R.id.iv_menu);
        ivMenu.setVisibility(View.GONE);
    }
    
    @Override
    public void onMapReady(GoogleMap map) {
        this.googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        
        // Default location
        LatLng defaultLocation = new LatLng(22.3178509, 87.3106518);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 13f));
        
        // Marker click listener
        googleMap.setOnMarkerClickListener(marker -> {
            showStationDetails(marker);
            return true; // Prevent default zoom
        });
        
        // Fetch stations and classify them
        fetchStationsAndClassify();
    }
    
    private void fetchStationsAndClassify() {
        if (stationRootRef == null) {
            Toast.makeText(this, "Organisation name missing!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Log.d(TAG, "Fetching stations from: " + stationRootRef.getPath());
        
        allStations.clear();
        pickupStations.clear();
        dropStations.clear();
        
        stationRootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot stationSnap : snapshot.getChildren()) {
                    String stationId = stationSnap.getKey();
                    String stationName = stationSnap.child("stationName").getValue(String.class);
                    
                    // Get coordinates
                    String latStr = stationSnap.child("stationLatitude").getValue(String.class);
                    String lngStr = stationSnap.child("stationLongitude").getValue(String.class);
                    
                    if (stationName == null || latStr == null || lngStr == null) {
                        continue;
                    }
                    
                    try {
                        double latitude = Double.parseDouble(latStr);
                        double longitude = Double.parseDouble(lngStr);
                        
                        // Get cycle count and demand - handle String, Long, or Integer types
                        int cycleCount = getIntValue(stationSnap.child("stationCycleCount"));
                        int cycleDemand = getIntValue(stationSnap.child("stationCycleDemand"));
                        
                        StationData station = new StationData(stationId, stationName, 
                                latitude, longitude, cycleCount, cycleDemand);
                        
                        allStations.add(station);
                        
                        if ("pickup".equals(station.stationType)) {
                            pickupStations.add(station);
                        } else if ("drop".equals(station.stationType)) {
                            dropStations.add(station);
                        }
                        
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Error parsing coordinates for station " + stationId, e);
                    }
                }
                
                // After fetching, display markers and route
                displayStationsOnMap();
                calculateAndDrawRoute();
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to fetch stations", error.toException());
                Toast.makeText(RedistributionMapActivity.this, 
                        "Failed to fetch stations: " + error.getMessage(), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void displayStationsOnMap() {
        if (googleMap == null) return;
        
        // Clear existing markers
        for (Marker marker : stationMarkers.values()) {
            marker.remove();
        }
        stationMarkers.clear();
        
        // Add markers for all stations
        for (StationData station : allStations) {
            LatLng position = new LatLng(station.latitude, station.longitude);
            
            BitmapDescriptor icon;
            String title;
            
            if ("pickup".equals(station.stationType)) {
                // Orange default map marker for pickup stations
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE);
                title = station.stationName + "\n" + station.cyclesToMove + " Cycles To Pick";
            } else if ("drop".equals(station.stationType)) {
                // Green default map marker for drop stations
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
                title = station.stationName + "\n" + station.cyclesToMove + " Cycles To Drop";
            } else {
                // Skip stations with no redistribution needed
                continue;
            }
            
            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(title)
                    .snippet("Station ID: " + station.stationId)
                    .icon(icon));
            
            if (marker != null) {
                stationMarkers.put(station.stationId, marker);
            }
        }
        
        // Fit camera to show all markers
        if (!stationMarkers.isEmpty()) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (StationData station : allStations) {
                if (!"none".equals(station.stationType)) {
                    builder.include(new LatLng(station.latitude, station.longitude));
                }
            }
            LatLngBounds bounds = builder.build();
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        }
    }
    
    private void calculateAndDrawRoute() {
        // Clear existing polylines
        for (Polyline polyline : routePolylines) {
            polyline.remove();
        }
        routePolylines.clear();
        
        if (pickupStations.isEmpty() || dropStations.isEmpty()) {
            Log.d(TAG, "No pickup or drop stations to connect");
            return;
        }
        
        // Calculate shortest path using nearest neighbor algorithm
        // Start from a pickup station, connect to nearest drop station, then to next pickup, etc.
        List<StationData> route = calculateNearestNeighborRoute();
        
        // Draw polyline connecting the route
        if (route.size() >= 2) {
            List<LatLng> pathPoints = new ArrayList<>();
            for (StationData station : route) {
                pathPoints.add(new LatLng(station.latitude, station.longitude));
            }
            
            // Create blue polyline matching screenshot style
            PolylineOptions polylineOptions = new PolylineOptions()
                    .addAll(pathPoints)
                    .color(Color.parseColor("#2196F3")) // Blue color matching screenshot
                    .width(6f)
                    .geodesic(true)
                    .zIndex(1);
            
            Polyline polyline = googleMap.addPolyline(polylineOptions);
            routePolylines.add(polyline);
        }
    }
    
    private List<StationData> calculateNearestNeighborRoute() {
        List<StationData> route = new ArrayList<>();
        List<StationData> availablePickups = new ArrayList<>(pickupStations);
        List<StationData> availableDrops = new ArrayList<>(dropStations);
        
        // Start with the first pickup station
        if (availablePickups.isEmpty()) {
            return route;
        }
        
        StationData currentStation = availablePickups.get(0);
        route.add(currentStation);
        availablePickups.remove(currentStation);
        
        // Build route: pickup -> drop -> pickup -> drop ... (shortest distance between each)
        // Route should start from pickup and end with drop
        while (!availablePickups.isEmpty() || !availableDrops.isEmpty()) {
            if (!availableDrops.isEmpty()) {
                // Find nearest drop station from current position
                StationData nearestDrop = findNearestStation(currentStation, availableDrops);
                if (nearestDrop != null) {
                    route.add(nearestDrop);
                    availableDrops.remove(nearestDrop);
                    currentStation = nearestDrop;
                } else {
                    break;
                }
            }
            
            if (!availablePickups.isEmpty()) {
                // Find nearest pickup station from current position
                StationData nearestPickup = findNearestStation(currentStation, availablePickups);
                if (nearestPickup != null) {
                    route.add(nearestPickup);
                    availablePickups.remove(nearestPickup);
                    currentStation = nearestPickup;
                } else {
                    break;
                }
            }
        }
        
        // Add any remaining stations in nearest order (prioritize drop stations to end with drop)
        while (!availableDrops.isEmpty() || !availablePickups.isEmpty()) {
            StationData nearest = null;
            List<StationData> candidates = new ArrayList<>();
            // Prioritize drop stations first to ensure route ends with drop
            if (!availableDrops.isEmpty()) {
                candidates.addAll(availableDrops);
            }
            if (!availablePickups.isEmpty()) {
                candidates.addAll(availablePickups);
            }
            
            nearest = findNearestStation(currentStation, candidates);
            if (nearest != null) {
                route.add(nearest);
                availableDrops.remove(nearest);
                availablePickups.remove(nearest);
                currentStation = nearest;
            } else {
                break;
            }
        }
        
        // Ensure route ends with a drop station if possible
        if (!route.isEmpty() && "pickup".equals(route.get(route.size() - 1).stationType)) {
            // If last station is pickup, try to find nearest drop and add it
            for (StationData drop : dropStations) {
                if (!route.contains(drop)) {
                    route.add(drop);
                    break;
                }
            }
        }
        
        return route;
    }
    
    private StationData findNearestStation(StationData from, List<StationData> candidates) {
        if (candidates.isEmpty()) return null;
        
        StationData nearest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (StationData candidate : candidates) {
            double distance = calculateDistance(from.latitude, from.longitude,
                    candidate.latitude, candidate.longitude);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = candidate;
            }
        }
        
        return nearest;
    }
    
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371000; // meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }
    
    private void showStationDetails(Marker marker) {
        // Find station data from marker using title or snippet
        StationData station = null;
        String markerSnippet = marker.getSnippet();
        
        if (markerSnippet != null && markerSnippet.startsWith("Station ID: ")) {
            String stationId = markerSnippet.replace("Station ID: ", "");
            for (StationData s : allStations) {
                if (s.stationId.equals(stationId)) {
                    station = s;
                    break;
                }
            }
        }
        
        if (station == null) {
            Toast.makeText(this, "Station data not found", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create details dialog matching the screenshot format
        String details = "Station Name: " + station.stationName +
                "\nStation ID: " + station.stationId +
                "\nCycle Count: " + station.cycleCount +
                "\nCycle Demand: " + station.cycleDemand;
        
        if ("pickup".equals(station.stationType)) {
            details += "\nCycles To Pick: " + station.cyclesToMove;
        } else if ("drop".equals(station.stationType)) {
            details += "\nCycles To Drop: " + station.cyclesToMove;
        }
        
        new AlertDialog.Builder(this)
                .setTitle("Station Details")
                .setMessage(details)
                .setPositiveButton("OK", null)
                .show();
    }
    
    private void handleRedistributionFinished() {
        // Show confirmation dialog before setting demand to 0
        new AlertDialog.Builder(this)
                .setTitle("Confirm")
                .setMessage("Do you want to mark redistribution as finished? This will set cycle demand to 0 for all stations.")
                .setPositiveButton("Confirm", (dialog, which) -> {
                    // User confirmed - proceed to set demand to 0
                    setAllDemandToZero();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // User cancelled - just dismiss the dialog
                    dialog.dismiss();
                })
                .setCancelable(true) // Allow dismissing by tapping outside
                .show();
    }
    
    private void setAllDemandToZero() {
        if (stationRootRef == null) {
            Toast.makeText(this, "Organisation name missing!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Update all stations' cycle demand to 0
        for (StationData station : allStations) {
            stationRootRef.child(station.stationId).child("stationCycleDemand")
                    .setValue(0);
        }
        
        Toast.makeText(this, "Redistribution finished! All demands set to 0.", 
                Toast.LENGTH_SHORT).show();
        
        // Refresh the map
        fetchStationsAndClassify();
    }
    
    // MapView lifecycle methods
    @Override
    protected void onStart() {
        super.onStart();
        if (mapView != null) mapView.onStart();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }
    
    @Override
    protected void onPause() {
        if (mapView != null) mapView.onPause();
        super.onPause();
    }
    
    @Override
    protected void onStop() {
        if (mapView != null) mapView.onStop();
        super.onStop();
    }
    
    @Override
    protected void onDestroy() {
        if (mapView != null) mapView.onDestroy();
        super.onDestroy();
    }
    
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }
    
    // Commented out helper methods - using default map markers instead
    // Helper methods for custom icons are not needed as we're using default map markers
    /*
    private BitmapDescriptor getBitmapIcon(int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(this, drawableId);
        if (drawable instanceof BitmapDrawable) {
            Bitmap original = ((BitmapDrawable) drawable).getBitmap();
            Bitmap scaled = Bitmap.createScaledBitmap(original, 80, 80, false);
            return BitmapDescriptorFactory.fromBitmap(scaled);
        }
        return BitmapDescriptorFactory.defaultMarker();
    }
    
    private BitmapDescriptor getColoredBicycleIcon(int drawableId, int color) {
        Drawable drawable = ContextCompat.getDrawable(this, drawableId);
        if (drawable != null) {
            Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), 
                    drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            
            ColorFilter filter = new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN);
            drawable.setColorFilter(filter);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 80, 80, false);
            return BitmapDescriptorFactory.fromBitmap(scaled);
        }
        return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE);
    }
    */
    
    // Helper method to safely get integer value from Firebase DataSnapshot
    // Handles String, Long, Integer, or null values
    private int getIntValue(DataSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            return 0;
        }
        
        Object value = snapshot.getValue();
        if (value == null) {
            return 0;
        }
        
        try {
            if (value instanceof String) {
                return Integer.parseInt((String) value);
            } else if (value instanceof Long) {
                return ((Long) value).intValue();
            } else if (value instanceof Integer) {
                return (Integer) value;
            } else if (value instanceof Number) {
                return ((Number) value).intValue();
            } else {
                // Try to parse as string
                return Integer.parseInt(value.toString());
            }
        } catch (NumberFormatException | ClassCastException e) {
            Log.e(TAG, "Error parsing value: " + value, e);
            return 0;
        }
    }
}

