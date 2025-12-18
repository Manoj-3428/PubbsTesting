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
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
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
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

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
    private List<StationData> currentRoute = new ArrayList<>(); // Store current route for marker display
    private Marker selectedMarker = null; // Track currently selected marker for highlighting
    
    private Button redistributionFinishedBtn;
    private TextView toolbarTitle;
    private ImageView backButton;
    private TextView tvPickupStopsCount;
    private TextView tvDropStopsCount;
    
    // Loading overlay
    private View loadingOverlay;
    private boolean isRouteLoading = false;
    
    // Distance cache: Store road distances between station pairs to avoid repeated API calls
    private Map<String, Double> roadDistanceCache = new HashMap<>();
    
    // Path cache: Store road paths (polyline points) between station pairs
    private Map<String, List<LatLng>> roadPathCache = new HashMap<>();
    
    private RequestQueue requestQueue; // For Directions API requests
    
    // Vehicle capacity constant
    private static final int MAX_VEHICLE_CAPACITY = 4;
    
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
    
    // Route state class to track vehicle capacity at each step
    private static class RouteState {
        List<StationData> route;
        int cyclesHolding; // Current cycles in vehicle
        int vehicleCapacity; // Remaining space in vehicle (MAX_CAPACITY - cyclesHolding)
        
        RouteState() {
            this.route = new ArrayList<>();
            this.cyclesHolding = 0;
            this.vehicleCapacity = MAX_VEHICLE_CAPACITY;
        }
        
        RouteState(RouteState other) {
            this.route = new ArrayList<>(other.route);
            this.cyclesHolding = other.cyclesHolding;
            this.vehicleCapacity = other.vehicleCapacity;
        }
        
        // Check if we can visit a pickup station (enough space)
        boolean canVisitPickup(StationData station) {
            return station.cyclesToMove <= vehicleCapacity;
        }
        
        // Check if we can visit a drop station (enough cycles to drop)
        // Condition: cyclesHolding >= cyclesToDrop
        boolean canVisitDrop(StationData station) {
            int cyclesToDrop = station.cyclesToMove;
            return cyclesHolding >= cyclesToDrop;
        }
        
        // Visit a station and update vehicle state
        void visitStation(StationData station) {
            route.add(station);
            if ("pickup".equals(station.stationType)) {
                // Pick cycles: increase cyclesHolding, decrease capacity
                cyclesHolding += station.cyclesToMove;
                vehicleCapacity -= station.cyclesToMove;
            } else if ("drop".equals(station.stationType)) {
                // Drop cycles: decrease cyclesHolding, increase capacity
                cyclesHolding -= station.cyclesToMove;
                vehicleCapacity += station.cyclesToMove;
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
        tvPickupStopsCount = findViewById(R.id.tv_pickup_stops_count);
        tvDropStopsCount = findViewById(R.id.tv_drop_stops_count);
        loadingOverlay = findViewById(R.id.loading_overlay);
        
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
        
        // Set custom info window adapter
        googleMap.setInfoWindowAdapter(new StationInfoWindowAdapter());
        
        // Initialize Volley request queue for Directions API
        requestQueue = Volley.newRequestQueue(this);
        Log.d(TAG, "Volley request queue initialized for Directions API");
        
        // Log API key status
        String apiKey = getResources().getString(R.string.google_maps_key);
        Log.d(TAG, "Google Maps/Directions API Key loaded: " + 
              (apiKey != null && !apiKey.isEmpty() ? "YES (length: " + apiKey.length() + ")" : "NO"));
        
        // Marker click listener - animate to marker, highlight it, and show info window
        googleMap.setOnMarkerClickListener(marker -> {
            // Animate camera to marker position with zoom
            LatLng position = marker.getPosition();
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 16f));
            
            // Reset previous marker highlight (if any)
            if (selectedMarker != null && selectedMarker != marker) {
                // Reset previous marker appearance
                Object prevTag = selectedMarker.getTag();
                if (prevTag instanceof StationData) {
                    StationData prevStation = (StationData) prevTag;
                    updateMarkerIcon(selectedMarker, prevStation, false);
                }
            }
            
            // Highlight current marker
            selectedMarker = marker;
            Object tag = marker.getTag();
            if (tag instanceof StationData) {
                StationData station = (StationData) tag;
                updateMarkerIcon(marker, station, true);
            }
            
            // Show info window
            marker.showInfoWindow();
            
            return true; // Prevent default zoom but allow info window to show
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
                
                // After fetching, start pre-calculating road distances, then calculate route
                updateRouteSummaryCards();
                
                // Show loading overlay
                showLoadingOverlay();
                
                // Start pre-calculating road distances and wait for enough to be cached before calculating route
                preCalculateRoadDistancesAndCalculateRoute();
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
        selectedMarker = null; // Reset selected marker
        
        // Create a map to quickly find station order in route
        Map<String, Integer> stationOrderMap = new HashMap<>();
        for (int i = 0; i < currentRoute.size(); i++) {
            stationOrderMap.put(currentRoute.get(i).stationId, i + 1); // 1-based order
        }
        
        // Get the route to identify starting station
        StationData startingStation = currentRoute.isEmpty() ? null : currentRoute.get(0);
        
        // Add markers for all stations
        for (StationData station : allStations) {
            LatLng position = new LatLng(station.latitude, station.longitude);
            
            BitmapDescriptor icon;
            String title;
            int orderNumber = -1; // -1 means not in route
            
            // Find the order of this station in the route
            if (stationOrderMap.containsKey(station.stationId)) {
                orderNumber = stationOrderMap.get(station.stationId);
            }
            
            // Color scheme: Pickup = GREEN, Drop = RED
            if ("pickup".equals(station.stationType)) {
                // Green marker for pickup stations
                if (orderNumber > 0) {
                    icon = createMarkerWithNumber(BitmapDescriptorFactory.HUE_GREEN, orderNumber);
                } else {
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
                }
                title = station.stationName + "\n" + station.cyclesToMove + " Cycles To Pick";
            } else if ("drop".equals(station.stationType)) {
                // Red marker for drop stations
                if (orderNumber > 0) {
                    icon = createMarkerWithNumber(BitmapDescriptorFactory.HUE_RED, orderNumber);
                } else {
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
                }
                title = station.stationName + "\n" + station.cyclesToMove + " Cycles To Drop";
            } else {
                // Skip stations with no redistribution needed
                continue;
            }
            
            // Create marker with proper anchor point (bottom center) so pin tip is at location
            // Markers render above polylines by default in Google Maps
            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(title)
                    .snippet("Station ID: " + station.stationId)
                    .icon(icon)
                    .anchor(0.5f, 1.0f)); // Bottom center anchor - pin tip at location, renders above polyline
            
            if (marker != null) {
                // Store station data in marker tag for info window access
                marker.setTag(station);
                stationMarkers.put(station.stationId, marker);
            }
        }
    }
    
    /**
     * Create a custom marker icon with a number label on top
     * @param hue The color hue for the marker (e.g., HUE_RED, HUE_GREEN)
     * @param number The order number to display on top of the marker
     * @return BitmapDescriptor for the custom marker
     */
    private BitmapDescriptor createMarkerWithNumber(float hue, int number) {
        // Create a larger marker bitmap for better visibility
        int width = 150;
        int height = 150;
        Bitmap markerBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(markerBitmap);
        
        // Get marker color
        int markerColor = Color.HSVToColor(new float[]{hue, 1.0f, 1.0f});
        
        // Draw the marker shape (pin shape)
        Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        markerPaint.setColor(markerColor);
        markerPaint.setStyle(Paint.Style.FILL);
        
        // Draw pin body (circle at top) - larger for better visibility
        float centerX = width / 2f;
        float topCircleY = 45f;
        float radius = 40f; // Increased radius for larger circle
        canvas.drawCircle(centerX, topCircleY, radius, markerPaint);
        
        // Draw pin point (triangle at bottom)
        android.graphics.Path path = new android.graphics.Path();
        path.moveTo(centerX, topCircleY + radius); // Top of triangle
        path.lineTo(centerX - 22f, height - 8f); // Bottom left
        path.lineTo(centerX + 22f, height - 8f); // Bottom right
        path.close();
        canvas.drawPath(path, markerPaint);
        
        // Draw white border around pin circle for better visibility
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(5f);
        canvas.drawCircle(centerX, topCircleY, radius, borderPaint);
        
        // Draw number in the center of the circle with black color
        String numberText = String.valueOf(number);
        
        // Create text paint with black color
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(50f); // Larger text size
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setStyle(Paint.Style.FILL);
        
        // Calculate text position to be perfectly centered in the circle
        // The text baseline should be at the vertical center of the circle
        float textY = topCircleY - ((textPaint.descent() + textPaint.ascent()) / 2);
        canvas.drawText(numberText, centerX, textY, textPaint);
        
        return BitmapDescriptorFactory.fromBitmap(markerBitmap);
    }
    
    /**
     * Update marker icon to highlight or reset it
     */
    private void updateMarkerIcon(Marker marker, StationData station, boolean highlight) {
        BitmapDescriptor icon;
        float alpha = highlight ? 1.0f : 1.0f;
        
        // Find the order of this station in the route
        int orderNumber = -1;
        for (int i = 0; i < currentRoute.size(); i++) {
            if (currentRoute.get(i).stationId.equals(station.stationId)) {
                orderNumber = i + 1; // 1-based order
                break;
            }
        }
        
        // Color scheme: Pickup = GREEN, Drop = RED
        if ("pickup".equals(station.stationType)) {
            if (orderNumber > 0) {
                icon = createMarkerWithNumber(BitmapDescriptorFactory.HUE_GREEN, orderNumber);
            } else {
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
            }
        } else if ("drop".equals(station.stationType)) {
            if (orderNumber > 0) {
                icon = createMarkerWithNumber(BitmapDescriptorFactory.HUE_RED, orderNumber);
            } else {
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
            }
        } else {
            return; // Skip
        }
        
        marker.setIcon(icon);
        marker.setAlpha(alpha);
    }
    
    /**
     * Update the route summary cards with pickup and drop station counts
     */
    private void updateRouteSummaryCards() {
        if (tvPickupStopsCount != null && tvDropStopsCount != null) {
            tvPickupStopsCount.setText(String.valueOf(pickupStations.size()));
            tvDropStopsCount.setText(String.valueOf(dropStations.size()));
        }
    }
    
    /**
     * Show loading overlay with "Preparing route for you" message
     */
    private void showLoadingOverlay() {
        if (loadingOverlay != null) {
            isRouteLoading = true;
            runOnUiThread(() -> {
                loadingOverlay.setVisibility(View.VISIBLE);
                // Hide redistribution button during loading
                if (redistributionFinishedBtn != null) {
                    redistributionFinishedBtn.setVisibility(View.GONE);
                }
            });
        }
    }
    
    /**
     * Hide loading overlay
     */
    private void hideLoadingOverlay() {
        if (loadingOverlay != null && isRouteLoading) {
            isRouteLoading = false;
            runOnUiThread(() -> {
                loadingOverlay.setVisibility(View.GONE);
                // Show redistribution button when loading is done
                if (redistributionFinishedBtn != null) {
                    redistributionFinishedBtn.setVisibility(View.VISIBLE);
                }
            });
        }
    }
    
    /**
     * Recalculate route using road distances (once they're cached)
     * NOTE: This method is currently DISABLED because we want to keep the same station order
     * and only update the path visualization, not change the route order.
     * The user requirement is: "path should change the way not the order of the stations"
     */
    @Deprecated
    private void recalculateRouteWithRoadDistances() {
        // DISABLED: We don't want to recalculate the route and change station order
        // The path visualization will update automatically as road paths are fetched
        Log.d(TAG, "Route recalculation disabled - keeping original route order, only updating path visualization");
        return;
        
        /* DISABLED CODE - keeping for reference
        if (pickupStations.isEmpty() || dropStations.isEmpty()) {
            return;
        }
        
        Log.d(TAG, "Recalculating route with road distances (cached: " + 
              roadDistanceCache.size() + " distances)");
        
        // Recalculate route (will use cached road distances)
        List<StationData> route = calculateNearestNeighborRoute();
        currentRoute = new ArrayList<>(route);
        
        if (route.isEmpty()) {
            return;
        }
        
        // Apply 2-opt optimization
        List<StationData> optimizedRoute = optimizeRouteWith2Opt(route);
        if (!optimizedRoute.equals(route)) {
            route = optimizedRoute;
            currentRoute = new ArrayList<>(route);
        }
        
        // Update markers to reflect new starting station
        displayStationsOnMap();
        
        // Redraw route
        redrawRoute(route);
        
        Log.d(TAG, "Route recalculated with road distances");
        */
    }
    
    /**
     * Redraw the route polyline
     */
    private void redrawRoute(List<StationData> route) {
        // Clear existing polylines
        for (Polyline polyline : routePolylines) {
            polyline.remove();
        }
        routePolylines.clear();
        
        if (route.size() >= 2) {
            Log.d(TAG, "Redrawing route with " + route.size() + " stations");
            
            // Draw route with available cached paths or straight lines (non-blocking)
            List<LatLng> initialPathPoints = new ArrayList<>();
            
            for (int i = 0; i < route.size() - 1; i++) {
                StationData fromStation = route.get(i);
                StationData toStation = route.get(i + 1);
                
                // For first segment, always add the source station coordinate
                if (i == 0) {
                    initialPathPoints.add(new LatLng(fromStation.latitude, fromStation.longitude));
                }
                
                // Use cached path if available, otherwise straight line (non-blocking)
                List<LatLng> segmentPath = getRoadPath(fromStation, toStation);
                
                // Add segment path points
                // For segments after the first, skip the first point to avoid duplicates
                int startIndex = (i == 0) ? 0 : 1;
                for (int j = startIndex; j < segmentPath.size(); j++) {
                    initialPathPoints.add(segmentPath.get(j));
                }
                
                // Always ensure the destination station coordinate is at the end of each segment
                LatLng destStation = new LatLng(toStation.latitude, toStation.longitude);
                if (initialPathPoints.isEmpty()) {
                    initialPathPoints.add(destStation);
                } else {
                    LatLng lastPoint = initialPathPoints.get(initialPathPoints.size() - 1);
                    double distance = calculateDistance(lastPoint.latitude, lastPoint.longitude,
                            destStation.latitude, destStation.longitude);
                    if (distance > 10) {
                        initialPathPoints.add(destStation);
                    }
                }
            }
            
            // Create polyline (will be updated when road paths are fetched)
            PolylineOptions polylineOptions = new PolylineOptions()
                    .addAll(initialPathPoints)
                    .color(Color.parseColor("#1976D2"))
                    .width(12f)
                    .geodesic(false)
                    .zIndex(-10.0f);
            
            Polyline polyline = googleMap.addPolyline(polylineOptions);
            routePolylines.add(polyline);
            
            Log.d(TAG, "Route redrawn with " + route.size() + " stations, " + 
                  initialPathPoints.size() + " path points (includes station coordinates)");
            
            // Log route for debugging
            StringBuilder routeStr = new StringBuilder("Route: ");
            for (int i = 0; i < route.size(); i++) {
                routeStr.append(route.get(i).stationId);
                if (i < route.size() - 1) routeStr.append(" -> ");
            }
            Log.d(TAG, routeStr.toString());
            
            // Asynchronously fetch and update with actual road paths
            fetchRoadPathsAndUpdatePolyline(route, polyline);
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
            hideLoadingOverlay();
            return;
        }
        
        Log.d(TAG, "Calculating route with capacity constraints (Max Capacity: " + MAX_VEHICLE_CAPACITY + ")");
        Log.d(TAG, "Pickup stations: " + pickupStations.size() + ", Drop stations: " + dropStations.size());
        
        // Calculate shortest path using constraint-aware nearest neighbor algorithm
        List<StationData> route = calculateNearestNeighborRoute();
        currentRoute = new ArrayList<>(route); // Store for marker display
        
        if (route.isEmpty()) {
            Log.w(TAG, "No valid route found!");
            hideLoadingOverlay();
            Toast.makeText(this, "No valid route found that satisfies capacity constraints!", 
                    Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Log route and vehicle state
        logRouteWithVehicleState(route);
        
        // Apply 2-opt improvement to optimize the route further (only valid swaps)
        List<StationData> optimizedRoute = optimizeRouteWith2Opt(route);
        
        if (!optimizedRoute.equals(route)) {
            Log.d(TAG, "Route improved by 2-opt optimization");
            logRouteWithVehicleState(optimizedRoute);
            route = optimizedRoute;
            currentRoute = new ArrayList<>(route); // Update stored route
        }
        
        // Don't draw initial polyline - wait for real road paths
        // Only draw when road paths are fetched
        if (route.size() >= 2) {
            Log.d(TAG, "Preparing route with " + route.size() + " stations - fetching road paths...");
            
            // Log route for debugging
            StringBuilder routeStr = new StringBuilder("Route: ");
            for (int i = 0; i < route.size(); i++) {
                routeStr.append(route.get(i).stationId);
                if (i < route.size() - 1) routeStr.append(" -> ");
            }
            Log.d(TAG, routeStr.toString());
            
            // Create empty polyline initially (will be updated when road paths are fetched)
            PolylineOptions polylineOptions = new PolylineOptions()
                    .color(Color.parseColor("#1976D2"))
                    .width(12f)
                    .geodesic(false)
                    .zIndex(-10.0f);
            
            Polyline polyline = googleMap.addPolyline(polylineOptions);
            routePolylines.add(polyline);
            
            // Now asynchronously fetch actual road paths for all segments and update polyline
            // Loading overlay will be hidden when all paths are fetched
            fetchRoadPathsAndUpdatePolyline(route, polyline);
        } else {
            hideLoadingOverlay();
        }
    }
    
    /**
     * Log the route with vehicle state at each step (for debugging)
     */
    private void logRouteWithVehicleState(List<StationData> route) {
        if (route.isEmpty()) return;
        
        Log.d(TAG, "=== Route with Vehicle State ===");
        int cyclesHolding = 0;
        int vehicleCapacity = MAX_VEHICLE_CAPACITY;
        
        for (int i = 0; i < route.size(); i++) {
            StationData station = route.get(i);
            Log.d(TAG, String.format("Step %d: %s (%s)", i + 1, station.stationName, station.stationType));
            Log.d(TAG, String.format("  Before: cyclesHolding=%d, capacity=%d", cyclesHolding, vehicleCapacity));
            
            if ("pickup".equals(station.stationType)) {
                cyclesHolding += station.cyclesToMove;
                vehicleCapacity -= station.cyclesToMove;
                Log.d(TAG, String.format("  Picked: %d cycles", station.cyclesToMove));
            } else if ("drop".equals(station.stationType)) {
                cyclesHolding -= station.cyclesToMove;
                vehicleCapacity += station.cyclesToMove;
                Log.d(TAG, String.format("  Dropped: %d cycles", station.cyclesToMove));
            }
            
            Log.d(TAG, String.format("  After: cyclesHolding=%d, capacity=%d", cyclesHolding, vehicleCapacity));
        }
        
        Log.d(TAG, "=== End of Route ===");
    }
    
    /**
     * Constraint-Aware Nearest Neighbor Algorithm
     * 
     * This algorithm builds a route that respects vehicle capacity constraints:
     * 1. Vehicle can hold maximum 4 cycles
     * 2. Route must start with a pickup station
     * 3. Route should end with a drop station
     * 4. At each step, only visit stations that satisfy capacity constraints:
     *    - Pickup station: cyclesToPick <= remaining vehicle capacity
     *    - Drop station: cyclesToDrop <= cycles currently holding
     * 
     * How it works:
     * Step 1: Start with first pickup station (vehicle starts empty)
     * Step 2: From current position, find all valid stations (pickup or drop) that can be visited
     * Step 3: Among valid stations, choose the nearest one
     * Step 4: Visit the station and update vehicle state (cyclesHolding, vehicleCapacity)
     * Step 5: Repeat steps 2-4 until all stations are visited or no valid stations remain
     * Step 6: If route doesn't end with drop, try to add a valid drop station at the end
     */
    private List<StationData> calculateNearestNeighborRoute() {
        RouteState routeState = new RouteState();
        List<StationData> availablePickups = new ArrayList<>(pickupStations);
        List<StationData> availableDrops = new ArrayList<>(dropStations);
        
        // Step 1: Start with first pickup station (vehicle starts empty, capacity = 4)
        if (availablePickups.isEmpty()) {
            return routeState.route;
        }
        
        StationData startStation = availablePickups.get(0);
        routeState.visitStation(startStation);
        availablePickups.remove(startStation);
        
        // Step 2-5: Build route by finding nearest valid station at each step
        // Continue until all stations are visited or no valid moves remain
        int maxIterations = 200; // Prevent infinite loops, increased to allow more stations
        int iterations = 0;
        boolean progressMade = true;
        
        while (progressMade && (!availablePickups.isEmpty() || !availableDrops.isEmpty()) && iterations < maxIterations) {
            progressMade = false;
            iterations++;
            StationData currentStation = routeState.route.get(routeState.route.size() - 1);
            
            // Find all valid stations (pickup or drop) that can be visited from current position
            List<StationData> validPickups = new ArrayList<>();
            List<StationData> validDrops = new ArrayList<>();
            
            // Check valid pickup stations (must have enough vehicle capacity)
            for (StationData pickup : availablePickups) {
                if (routeState.canVisitPickup(pickup)) {
                    validPickups.add(pickup);
                }
            }
            
            // Check valid drop stations (must have enough cycles to drop)
            for (StationData drop : availableDrops) {
                if (routeState.canVisitDrop(drop)) {
                    validDrops.add(drop);
                }
            }
            
            // Prioritize drop stations if we're near the end (to ensure route ends with drop)
            int remainingStations = availablePickups.size() + availableDrops.size();
            List<StationData> validCandidates = new ArrayList<>();
            
            if (remainingStations == 1) {
                // LAST station - MUST be a drop station if possible
                if (!validDrops.isEmpty()) {
                    validCandidates = validDrops;
                    Log.d(TAG, "Last station - forcing drop station selection");
                } else {
                    // No valid drops available - this should not happen if data is correct
                    // But if it does, use what we have (data constraint issue)
                    validCandidates.addAll(validPickups);
                    validCandidates.addAll(validDrops);
                    Log.w(TAG, "Last station but no valid drops - constraint issue, ending with: " + 
                          (validPickups.isEmpty() ? "none" : "pickup"));
                }
            } else if (remainingStations == 2) {
                // Second to last - CRITICAL: if one pickup and one drop remain, visit pickup first
                // This ensures the last station (drop) can be visited
                if (availablePickups.size() == 1 && availableDrops.size() == 1) {
                    // Exactly one of each - visit pickup first so drop can be last
                    if (!validPickups.isEmpty()) {
                        validCandidates = validPickups;
                        Log.d(TAG, "Second to last - one pickup one drop: visiting pickup first to ensure drop is last");
                    } else {
                        // Pickup not valid (capacity issue), try drop
                        validCandidates = validDrops;
                    }
                } else if (routeState.cyclesHolding > 0 && !validDrops.isEmpty()) {
                    // Multiple stations remain - prefer drop if we have cycles
                    validCandidates = validDrops;
                    Log.d(TAG, "Second to last - prioritizing drop (cycles=" + routeState.cyclesHolding + ")");
                } else {
                    // No cycles or no valid drops - allow both but prefer drops
                    validCandidates.addAll(validDrops);
                    validCandidates.addAll(validPickups);
                }
            } else if (remainingStations == 3) {
                // Third to last - start prioritizing drops more
                if (routeState.cyclesHolding > 0 && !validDrops.isEmpty()) {
                    validCandidates.addAll(validDrops);
                    validCandidates.addAll(validPickups);
                } else {
                    validCandidates.addAll(validPickups);
                    validCandidates.addAll(validDrops);
                }
            } else {
                // Normal operation - prefer drops if we have cycles, otherwise pickups
                if (routeState.cyclesHolding > 0 && !validDrops.isEmpty()) {
                    // We have cycles, prioritize drops
                    validCandidates.addAll(validDrops);
                    validCandidates.addAll(validPickups);
                } else {
                    // No cycles or no valid drops, prioritize pickups
                    validCandidates.addAll(validPickups);
                    validCandidates.addAll(validDrops);
                }
            }
            
            // If we have valid candidates, choose the nearest one
            if (!validCandidates.isEmpty()) {
                StationData nearestStation = findNearestStation(currentStation, validCandidates);
                if (nearestStation != null) {
                    routeState.visitStation(nearestStation);
                    availablePickups.remove(nearestStation);
                    availableDrops.remove(nearestStation);
                    progressMade = true;
                    Log.d(TAG, "Added station to route: " + nearestStation.stationId + 
                          " (Vehicle: " + routeState.cyclesHolding + " cycles, " + 
                          routeState.vehicleCapacity + " capacity)");
                }
            } else {
                // No valid candidates - log remaining stations
                Log.w(TAG, "No valid candidates. Remaining pickups: " + availablePickups.size() + 
                      ", Remaining drops: " + availableDrops.size() + 
                      ", Vehicle state: " + routeState.cyclesHolding + " cycles, " + 
                      routeState.vehicleCapacity + " capacity");
                break;
            }
        }
        
        // Step 6: If route doesn't end with drop, try to add a valid drop station at the end
        if (!routeState.route.isEmpty() && "pickup".equals(routeState.route.get(routeState.route.size() - 1).stationType)) {
            // Try to find a valid drop station to end with
            StationData currentStation = routeState.route.get(routeState.route.size() - 1);
            StationData nearestValidDrop = null;
            double minDistance = Double.MAX_VALUE;
            
            for (StationData drop : availableDrops) {
                if (routeState.canVisitDrop(drop)) {
                    // Use real road distance (from cache) for accurate route calculation
                    double distance = getRoadDistance(currentStation, drop);
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestValidDrop = drop;
                    }
                }
            }
            
            if (nearestValidDrop != null) {
                routeState.visitStation(nearestValidDrop);
            }
        }
        
        Log.d(TAG, "Route calculated with " + routeState.route.size() + " stations. " +
                "Final vehicle state: cyclesHolding=" + routeState.cyclesHolding + 
                ", capacity=" + routeState.vehicleCapacity);
        
        return routeState.route;
    }
    
    private StationData findNearestStation(StationData from, List<StationData> candidates) {
        if (candidates.isEmpty()) return null;
        
        StationData nearest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (StationData candidate : candidates) {
            // Use real road distance (from cache) for accurate route calculation
            // Falls back to straight-line if not cached yet
            double distance = getRoadDistance(from, candidate);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = candidate;
            }
        }
        
        return nearest;
    }
    
    /**
     * Calculate straight-line distance (Haversine formula) - used as fallback
     */
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
    
    /**
     * Get road distance between two stations using Google Directions API
     * Uses cache to avoid repeated API calls
     * NOTE: For now, returns straight-line distance to avoid blocking main thread
     * Road distances can be calculated asynchronously and cached for future use
     */
    private double getRoadDistance(StationData from, StationData to) {
        // Create cache key (always use smaller ID first for consistency)
        String cacheKey = from.stationId.compareTo(to.stationId) < 0 
                ? from.stationId + "_" + to.stationId 
                : to.stationId + "_" + from.stationId;
        
        // Check cache first - use real road distance if available
        if (roadDistanceCache.containsKey(cacheKey)) {
            return roadDistanceCache.get(cacheKey);
        }
        
        // If not cached, use straight-line distance as fallback
        // This should rarely happen if pre-calculation is working properly
        double straightDistance = calculateDistance(from.latitude, from.longitude, 
                to.latitude, to.longitude);
        
        // Start async calculation for future use (non-blocking, don't wait)
        getRoadDistanceAsync(from, to);
        
        return straightDistance;
    }
    
    /**
     * Get road distance asynchronously and cache it (non-blocking)
     */
    private void getRoadDistanceAsync(StationData from, StationData to) {
        String cacheKey = from.stationId.compareTo(to.stationId) < 0 
                ? from.stationId + "_" + to.stationId 
                : to.stationId + "_" + from.stationId;
        
        if (roadDistanceCache.containsKey(cacheKey)) {
            return; // Already cached
        }
        
        String url = buildDirectionsUrl(from.latitude, from.longitude, to.latitude, to.longitude);
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        Log.d(TAG, "Async Directions API response received for: " + from.stationId + " -> " + to.stationId);
                        String status = response.optString("status", "UNKNOWN");
                        
                        if (!"OK".equals(status)) {
                            String errorMessage = response.optString("error_message", "No error message");
                            Log.e(TAG, "=== Directions API ERROR ===");
                            Log.e(TAG, "Status: " + status);
                            Log.e(TAG, "Error message: " + errorMessage);
                            Log.e(TAG, "This usually means:");
                            Log.e(TAG, "  1. Directions API is not enabled for this API key");
                            Log.e(TAG, "  2. API key has restrictions that block this usage");
                            Log.e(TAG, "  3. API key is invalid or expired");
                            Log.e(TAG, "Please check your Google Cloud Console API key settings");
                        }
                        
                        if ("OK".equals(status)) {
                            JSONArray routes = response.getJSONArray("routes");
                            if (routes.length() > 0) {
                                JSONObject route = routes.getJSONObject(0);
                                JSONArray legs = route.getJSONArray("legs");
                                if (legs.length() > 0) {
                                    JSONObject leg = legs.getJSONObject(0);
                                    JSONObject distance = leg.getJSONObject("distance");
                                    double distanceInMeters = distance.getDouble("value");
                                    roadDistanceCache.put(cacheKey, distanceInMeters);
                                    
                                    int cached = cachedRoadDistancesCount.incrementAndGet();
                                    Log.d(TAG, "Cached road distance: " + from.stationId + " -> " + to.stationId + 
                                          " = " + distanceInMeters + " meters (" + cached + "/" + expectedRoadDistancesCount + " cached)");
                                    
                                    // Check if we have enough cached distances to calculate route
                                    // Start route calculation when 60% of distances are cached
                                    final double requiredCachePercentage = 0.6;
                                    final int requiredCachedCount = (int) (expectedRoadDistancesCount * requiredCachePercentage);
                                    
                                    if (cached >= requiredCachedCount && roadDistanceCallback != null) {
                                        Log.d(TAG, "Reached " + (int)(requiredCachePercentage * 100) + 
                                              "% cached distances (" + cached + "/" + expectedRoadDistancesCount + 
                                              "). Calculating route with real road distances...");
                                        runOnUiThread(() -> {
                                            if (roadDistanceCallback != null) {
                                                roadDistanceCallback.onDistanceCached();
                                            }
                                        });
                                        roadDistanceCallback = null; // Only call once
                                    }
                                }
                            }
                        } else {
                            String errorMessage = response.optString("error_message", "No error message");
                            Log.e(TAG, "Directions API returned status: " + status);
                            Log.e(TAG, "Error message: " + errorMessage);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing async Directions API response", e);
                    }
                },
                error -> {
                    Log.e(TAG, "Async Directions API error for " + from.stationId + " -> " + to.stationId + ": " + error.getMessage());
                });
        
        requestQueue.add(request);
    }
    
    /**
     * Callback interface for when road distance is cached
     */
    private interface RoadDistanceCallback {
        void onDistanceCached();
    }
    
    private RoadDistanceCallback roadDistanceCallback;
    private int expectedRoadDistancesCount = 0;
    private AtomicInteger cachedRoadDistancesCount = new AtomicInteger(0);
    
    /**
     * Get road distance synchronously (blocks until API response)
     * DEPRECATED: This blocks the main thread and causes ANR. Use getRoadDistance() instead.
     * Kept for reference only - do not use.
     */
    @Deprecated
    private double getRoadDistanceSync(StationData from, StationData to) {
        String cacheKey = from.stationId.compareTo(to.stationId) < 0 
                ? from.stationId + "_" + to.stationId 
                : to.stationId + "_" + from.stationId;
        
        if (roadDistanceCache.containsKey(cacheKey)) {
            Log.d(TAG, "Using cached road distance: " + from.stationId + " -> " + to.stationId + 
                  " = " + roadDistanceCache.get(cacheKey) + " meters");
            return roadDistanceCache.get(cacheKey);
        }
        
        Log.d(TAG, "=== Directions API: Getting road distance ===");
        Log.d(TAG, "From: " + from.stationName + " (" + from.stationId + ") at " + from.latitude + "," + from.longitude);
        Log.d(TAG, "To: " + to.stationName + " (" + to.stationId + ") at " + to.latitude + "," + to.longitude);
        
        // Make synchronous API call using CountDownLatch
        AtomicReference<Double> result = new AtomicReference<>(null);
        CountDownLatch latch = new CountDownLatch(1);
        
        String url = buildDirectionsUrl(from.latitude, from.longitude, to.latitude, to.longitude);
        Log.d(TAG, "Directions API URL: " + url);
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        Log.d(TAG, "Directions API response received successfully");
                        Log.d(TAG, "Response: " + response.toString());
                        
                        String status = response.optString("status", "UNKNOWN");
                        Log.d(TAG, "API Status: " + status);
                        
                        if (!"OK".equals(status)) {
                            Log.e(TAG, "Directions API returned non-OK status: " + status);
                            String errorMessage = response.optString("error_message", "No error message");
                            Log.e(TAG, "Error message: " + errorMessage);
                        }
                        
                        JSONArray routes = response.getJSONArray("routes");
                        Log.d(TAG, "Number of routes returned: " + routes.length());
                        
                        if (routes.length() > 0) {
                            JSONObject route = routes.getJSONObject(0);
                            JSONArray legs = route.getJSONArray("legs");
                            if (legs.length() > 0) {
                                JSONObject leg = legs.getJSONObject(0);
                                JSONObject distance = leg.getJSONObject("distance");
                                double distanceInMeters = distance.getDouble("value");
                                String distanceText = distance.getString("text");
                                
                                Log.d(TAG, "Road distance: " + distanceInMeters + " meters (" + distanceText + ")");
                                result.set(distanceInMeters);
                                roadDistanceCache.put(cacheKey, distanceInMeters);
                                
                                // Also log duration for reference
                                JSONObject duration = leg.getJSONObject("duration");
                                String durationText = duration.getString("text");
                                Log.d(TAG, "Estimated duration: " + durationText);
                            } else {
                                Log.w(TAG, "No legs found in route");
                            }
                        } else {
                            Log.w(TAG, "No routes found in API response");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing Directions API response", e);
                        e.printStackTrace();
                        // Fallback to straight-line distance
                        double fallback = calculateDistance(from.latitude, from.longitude, 
                                to.latitude, to.longitude);
                        Log.w(TAG, "Using fallback straight-line distance: " + fallback + " meters");
                        result.set(fallback);
                    }
                    latch.countDown();
                },
                error -> {
                    Log.e(TAG, "=== Directions API ERROR ===");
                    Log.e(TAG, "Error message: " + error.getMessage());
                    Log.e(TAG, "Error class: " + error.getClass().getSimpleName());
                    if (error.networkResponse != null) {
                        Log.e(TAG, "Network response status code: " + error.networkResponse.statusCode);
                        Log.e(TAG, "Network response data: " + new String(error.networkResponse.data));
                    }
                    error.printStackTrace();
                    
                    // Fallback to straight-line distance
                    double fallback = calculateDistance(from.latitude, from.longitude, 
                            to.latitude, to.longitude);
                    Log.w(TAG, "Using fallback straight-line distance: " + fallback + " meters");
                    result.set(fallback);
                    roadDistanceCache.put(cacheKey, fallback); // Cache fallback too
                    latch.countDown();
                });
        
        Log.d(TAG, "Adding Directions API request to queue...");
        requestQueue.add(request);
        
        try {
            Log.d(TAG, "Waiting for Directions API response...");
            latch.await(); // Wait for response (blocks current thread)
            Log.d(TAG, "Directions API response received");
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted while waiting for Directions API response", e);
            e.printStackTrace();
            // Return straight-line distance as fallback
            double fallback = calculateDistance(from.latitude, from.longitude, 
                    to.latitude, to.longitude);
            roadDistanceCache.put(cacheKey, fallback);
            return fallback;
        }
        
        double finalDistance = result.get() != null ? result.get() : calculateDistance(from.latitude, from.longitude, 
                to.latitude, to.longitude);
        Log.d(TAG, "Final road distance: " + finalDistance + " meters");
        Log.d(TAG, "=== End Directions API call ===\n");
        
        return finalDistance;
    }
    
    /**
     * Get road path (polyline points) between two stations - non-blocking version
     * Returns cached path if available, otherwise returns straight line and starts async fetch
     */
    private List<LatLng> getRoadPath(StationData from, StationData to) {
        String cacheKey = from.stationId.compareTo(to.stationId) < 0 
                ? from.stationId + "_" + to.stationId 
                : to.stationId + "_" + from.stationId;
        
        if (roadPathCache.containsKey(cacheKey)) {
            Log.d(TAG, "Using cached road path: " + from.stationId + " -> " + to.stationId + 
                  " (" + roadPathCache.get(cacheKey).size() + " points)");
            return new ArrayList<>(roadPathCache.get(cacheKey));
        }
        
        // For now, use straight-line path to avoid blocking main thread
        Log.d(TAG, "Using straight-line path (road path not cached): " + from.stationId + " -> " + to.stationId);
        List<LatLng> straightPath = createStraightLinePath(from, to);
        
        // Start async fetch for future use (non-blocking)
        getRoadPathAsync(from, to);
        
        return straightPath;
    }
    
    /**
     * Asynchronously fetch road paths for all route segments and update the polyline when complete
     * This method does NOT block the UI thread
     */
    private void fetchRoadPathsAndUpdatePolyline(List<StationData> route, Polyline polyline) {
        int totalSegments = route.size() - 1;
        if (totalSegments <= 0) {
            Log.w(TAG, "Route has " + route.size() + " stations, cannot fetch paths");
            return;
        }
        
        Log.d(TAG, "Asynchronously fetching road paths for " + totalSegments + " route segments (" + 
              route.size() + " stations total)...");
        
        // Log all stations in route
        StringBuilder routeStr = new StringBuilder("Fetching paths for route: ");
        for (int i = 0; i < route.size(); i++) {
            routeStr.append(route.get(i).stationId);
            if (i < route.size() - 1) routeStr.append(" -> ");
        }
        Log.d(TAG, routeStr.toString());
        
        // Store paths in an array indexed by segment position
        List<LatLng>[] segmentPaths = new List[totalSegments];
        AtomicInteger completedSegments = new AtomicInteger(0);
        
        Log.d(TAG, "Starting to fetch " + totalSegments + " segments for route with " + route.size() + " stations");
        
        // Fetch all segments asynchronously
        for (int i = 0; i < totalSegments; i++) {
            final int segmentIndex = i;
            final StationData fromStation = route.get(i);  // Make final for lambda
            final StationData toStation = route.get(i + 1);  // Make final for lambda
            
            String cacheKey = fromStation.stationId.compareTo(toStation.stationId) < 0 
                    ? fromStation.stationId + "_" + toStation.stationId 
                    : toStation.stationId + "_" + fromStation.stationId;
            
            // Check cache first
            if (roadPathCache.containsKey(cacheKey)) {
                segmentPaths[segmentIndex] = new ArrayList<>(roadPathCache.get(cacheKey));
                int completed = completedSegments.incrementAndGet();
                Log.d(TAG, "Segment " + (segmentIndex + 1) + "/" + totalSegments + " (cached): " + 
                      fromStation.stationId + " -> " + toStation.stationId + 
                      " (" + segmentPaths[segmentIndex].size() + " points)");
                
                        if (completed == totalSegments) {
                            // All segments complete, update polyline on UI thread
                            Log.d(TAG, "All " + totalSegments + " segments completed (from cache)! Updating polyline...");
                            runOnUiThread(() -> {
                                updatePolylineWithRoadPaths(route, polyline, segmentPaths);
                                hideLoadingOverlay();
                            });
                        }
                continue;
            }
            
            // Fetch from API asynchronously
            String url = buildDirectionsUrl(fromStation.latitude, fromStation.longitude, 
                    toStation.latitude, toStation.longitude);
            
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                    response -> {
                        try {
                            String status = response.optString("status", "UNKNOWN");
                            
                            if ("OK".equals(status)) {
                                JSONArray routes = response.getJSONArray("routes");
                                if (routes.length() > 0) {
                                    // Use only the first route (best route) - alternatives=false ensures this
                                    JSONObject routeObj = routes.getJSONObject(0);
                                    JSONObject overviewPolyline = routeObj.getJSONObject("overview_polyline");
                                    String encodedPolyline = overviewPolyline.getString("points");
                                    List<LatLng> path = decodePolyline(encodedPolyline);
                                    
                                    // Ensure path includes exact station coordinates
                                    // Add source station at the beginning if not already there
                                    LatLng sourceStation = new LatLng(fromStation.latitude, fromStation.longitude);
                                    if (path.isEmpty() || 
                                        calculateDistance(path.get(0).latitude, path.get(0).longitude,
                                                sourceStation.latitude, sourceStation.longitude) > 10) {
                                        path.add(0, sourceStation);
                                    }
                                    
                                    // Add destination station at the end if not already there
                                    LatLng destStation = new LatLng(toStation.latitude, toStation.longitude);
                                    if (path.isEmpty() || 
                                        calculateDistance(path.get(path.size() - 1).latitude, 
                                                path.get(path.size() - 1).longitude,
                                                destStation.latitude, destStation.longitude) > 10) {
                                        path.add(destStation);
                                    }
                                    
                                    roadPathCache.put(cacheKey, path); // Cache it
                                    segmentPaths[segmentIndex] = path;
                                    
                                    Log.d(TAG, "Segment " + (segmentIndex + 1) + "/" + totalSegments + 
                                          " (API): " + fromStation.stationId + " -> " + toStation.stationId + 
                                          " (" + path.size() + " points, includes station coordinates)");
                                } else {
                                    Log.w(TAG, "No routes found for segment " + (segmentIndex + 1));
                                    segmentPaths[segmentIndex] = createStraightLinePath(fromStation, toStation);
                                }
                            } else {
                                String errorMessage = response.optString("error_message", "No error message");
                                Log.e(TAG, "API status " + status + " for segment " + (segmentIndex + 1));
                                Log.e(TAG, "Error message: " + errorMessage);
                                segmentPaths[segmentIndex] = createStraightLinePath(fromStation, toStation);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing API response for segment " + (segmentIndex + 1), e);
                            segmentPaths[segmentIndex] = createStraightLinePath(fromStation, toStation);
                        }
                        
                        // Check if all segments are complete
                        int completed = completedSegments.incrementAndGet();
                        Log.d(TAG, "Segment " + (segmentIndex + 1) + "/" + totalSegments + " completed. " + 
                              completed + "/" + totalSegments + " total segments done");
                        if (completed == totalSegments) {
                            // All segments complete, update polyline on main thread
                            Log.d(TAG, "All " + totalSegments + " segments completed! Updating polyline...");
                            runOnUiThread(() -> {
                                updatePolylineWithRoadPaths(route, polyline, segmentPaths);
                                hideLoadingOverlay();
                            });
                        }
                    },
                    error -> {
                        Log.e(TAG, "API error for segment " + (segmentIndex + 1) + "/" + totalSegments + 
                              ": " + error.getMessage() + " (" + fromStation.stationId + " -> " + toStation.stationId + ")");
                        segmentPaths[segmentIndex] = createStraightLinePath(fromStation, toStation);
                        
                        int completed = completedSegments.incrementAndGet();
                        Log.d(TAG, "Segment " + (segmentIndex + 1) + "/" + totalSegments + " completed (error fallback). " + 
                              completed + "/" + totalSegments + " total segments done");
                        if (completed == totalSegments) {
                            Log.d(TAG, "All " + totalSegments + " segments completed (with errors)! Updating polyline...");
                            runOnUiThread(() -> {
                                updatePolylineWithRoadPaths(route, polyline, segmentPaths);
                                hideLoadingOverlay();
                            });
                        }
                    });
            
            requestQueue.add(request);
        }
    }
    
    /**
     * Update the polyline with actual road paths (called on UI thread)
     * Ensures the path includes exact station coordinates so it touches all stations
     */
    private void updatePolylineWithRoadPaths(List<StationData> route, Polyline polyline, List<LatLng>[] segmentPaths) {
        List<LatLng> allPathPoints = new ArrayList<>();
        
        if (route.size() < 2) {
            Log.w(TAG, "Route has less than 2 stations, cannot draw path");
            return;
        }
        
        if (segmentPaths.length != route.size() - 1) {
            Log.e(TAG, "Mismatch: route has " + route.size() + " stations (" + (route.size() - 1) + 
                  " segments) but segmentPaths has " + segmentPaths.length + " segments");
            return;
        }
        
        Log.d(TAG, "Updating polyline with " + route.size() + " stations, " + segmentPaths.length + " segments");
        
        // Verify all segments are present
        int missingSegments = 0;
        for (int i = 0; i < segmentPaths.length; i++) {
            if (segmentPaths[i] == null) {
                missingSegments++;
                Log.w(TAG, "Segment " + i + " is null: " + route.get(i).stationId + " -> " + route.get(i + 1).stationId);
            }
        }
        if (missingSegments > 0) {
            Log.w(TAG, "Warning: " + missingSegments + " segments are null, will use fallback paths");
        }
        
        for (int i = 0; i < segmentPaths.length; i++) {
            StationData fromStation = route.get(i);
            StationData toStation = route.get(i + 1);
            
            List<LatLng> segmentPath = segmentPaths[i];
            if (segmentPath == null || segmentPath.isEmpty()) {
                // Fallback to straight line if segment failed
                Log.w(TAG, "Segment " + (i + 1) + "/" + segmentPaths.length + " is null or empty, using straight line: " + 
                      fromStation.stationId + " -> " + toStation.stationId);
                segmentPath = createStraightLinePath(fromStation, toStation);
            }
            
            // For first segment, always add the source station coordinate
            if (i == 0) {
                LatLng sourceStation = new LatLng(fromStation.latitude, fromStation.longitude);
                allPathPoints.add(sourceStation);
            }
            
            // Add the road path points
            // For segments after the first, skip the first point to avoid duplicates
            int startIndex = (i == 0) ? 0 : 1;
            for (int j = startIndex; j < segmentPath.size(); j++) {
                LatLng point = segmentPath.get(j);
                allPathPoints.add(point);
            }
            
            // Always ensure the destination station coordinate is at the end of each segment
            LatLng destStation = new LatLng(toStation.latitude, toStation.longitude);
            if (allPathPoints.isEmpty()) {
                allPathPoints.add(destStation);
            } else {
                // Check if last point is the destination station (within 10 meters)
                LatLng lastPoint = allPathPoints.get(allPathPoints.size() - 1);
                double distance = calculateDistance(lastPoint.latitude, lastPoint.longitude,
                        destStation.latitude, destStation.longitude);
                if (distance > 10) {
                    // Add destination station if last point is more than 10 meters away
                    allPathPoints.add(destStation);
                }
            }
        }
        
        // Update polyline points
        if (allPathPoints.size() > 0) {
            polyline.setPoints(allPathPoints);
            Log.d(TAG, "Polyline updated with road paths: " + allPathPoints.size() + " total points, " + 
                  route.size() + " stations in route");
        } else {
            Log.e(TAG, "ERROR: No path points generated! Route has " + route.size() + " stations");
        }
        
        // Log all stations in route for debugging
        StringBuilder routeStr = new StringBuilder("Route stations: ");
        for (int i = 0; i < route.size(); i++) {
            routeStr.append(route.get(i).stationId);
            if (i < route.size() - 1) routeStr.append(" -> ");
        }
        Log.d(TAG, routeStr.toString());
    }
    
    /**
     * Get road path asynchronously and cache it (non-blocking)
     */
    private void getRoadPathAsync(StationData from, StationData to) {
        String cacheKey = from.stationId.compareTo(to.stationId) < 0 
                ? from.stationId + "_" + to.stationId 
                : to.stationId + "_" + from.stationId;
        
        if (roadPathCache.containsKey(cacheKey)) {
            return; // Already cached
        }
        
        String url = buildDirectionsUrl(from.latitude, from.longitude, to.latitude, to.longitude);
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        Log.d(TAG, "Async Directions API path response for: " + from.stationId + " -> " + to.stationId);
                        String status = response.optString("status", "UNKNOWN");
                        
                        if ("OK".equals(status)) {
                            JSONArray routes = response.getJSONArray("routes");
                            if (routes.length() > 0) {
                                // Use only the first route (best route)
                                JSONObject route = routes.getJSONObject(0);
                                JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                                String encodedPolyline = overviewPolyline.getString("points");
                                List<LatLng> path = decodePolyline(encodedPolyline);
                                
                                // Ensure path includes exact station coordinates
                                LatLng sourceStation = new LatLng(from.latitude, from.longitude);
                                if (path.isEmpty() || 
                                    calculateDistance(path.get(0).latitude, path.get(0).longitude,
                                            sourceStation.latitude, sourceStation.longitude) > 10) {
                                    path.add(0, sourceStation);
                                }
                                
                                LatLng destStation = new LatLng(to.latitude, to.longitude);
                                if (path.isEmpty() || 
                                    calculateDistance(path.get(path.size() - 1).latitude, 
                                            path.get(path.size() - 1).longitude,
                                            destStation.latitude, destStation.longitude) > 10) {
                                    path.add(destStation);
                                }
                                
                                roadPathCache.put(cacheKey, path);
                                Log.d(TAG, "Cached road path: " + from.stationId + " -> " + to.stationId + 
                                      " (" + path.size() + " points, includes station coordinates)");
                            }
                        } else {
                            String errorMessage = response.optString("error_message", "No error message");
                            Log.e(TAG, "Directions API path returned status: " + status);
                            Log.e(TAG, "Error message: " + errorMessage);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing async Directions API path response", e);
                    }
                },
                error -> {
                    Log.e(TAG, "Async Directions API path error: " + error.getMessage());
                });
        
        requestQueue.add(request);
    }
    
    /**
     * Build Google Directions API URL
     * Uses the provided API key and ensures only one best route is returned
     */
    private String buildDirectionsUrl(double lat1, double lng1, double lat2, double lng2) {
        // Use the provided API key directly
        String apiKey = "AIzaSyCv2efqVWc7ju_Tdn01pnLSCiPoLSH00yQ";
        String origin = lat1 + "," + lng1;
        String destination = lat2 + "," + lng2;
        // Use driving mode (suitable for vans/vehicles)
        // Add alternatives=false to get only the best route (not alternate routes)
        String url = "https://maps.googleapis.com/maps/api/directions/json" +
                "?origin=" + origin +
                "&destination=" + destination +
                "&mode=driving" +
                "&alternatives=false" +
                "&key=" + apiKey;
        
        Log.d(TAG, "Built Directions API URL with origin: " + origin + ", destination: " + destination);
        return url;
    }
    
    /**
     * Decode Google's encoded polyline string to List of LatLng points
     */
    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0;
        int len = encoded.length();
        int lat = 0;
        int lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0) ? ~(result >> 1) : (result >> 1);
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0) ? ~(result >> 1) : (result >> 1);
            lng += dlng;

            LatLng p = new LatLng((double) lat / 1E5, (double) lng / 1E5);
            poly.add(p);
        }
        return poly;
    }
    
    /**
     * Pre-calculate road distances for all station pairs asynchronously
     * This will populate the cache so subsequent route calculations can use actual road distances
     */
    private void preCalculateRoadDistancesAndCalculateRoute() {
        Log.d(TAG, "Pre-calculating road distances for all station pairs...");
        List<StationData> allRelevantStations = new ArrayList<>();
        allRelevantStations.addAll(pickupStations);
        allRelevantStations.addAll(dropStations);
        
        // Calculate expected number of distances (n*(n-1)/2 pairs)
        expectedRoadDistancesCount = allRelevantStations.size() * (allRelevantStations.size() - 1) / 2;
        cachedRoadDistancesCount.set(0);
        
        // Set callback to calculate route when enough distances are cached
        // We'll start route calculation when 60% of distances are cached (good balance)
        final double requiredCachePercentage = 0.6;
        final int requiredCachedCount = (int) (expectedRoadDistancesCount * requiredCachePercentage);
        
        roadDistanceCallback = () -> {
            // Calculate route when enough distances are cached
            runOnUiThread(() -> {
                calculateAndDrawRoute();
                displayStationsOnMap();
            });
        };
        
        // Calculate distances between all pairs asynchronously
        for (int i = 0; i < allRelevantStations.size(); i++) {
            for (int j = i + 1; j < allRelevantStations.size(); j++) {
                getRoadDistanceAsync(allRelevantStations.get(i), allRelevantStations.get(j));
                getRoadPathAsync(allRelevantStations.get(i), allRelevantStations.get(j));
            }
        }
        Log.d(TAG, "Started pre-calculation for " + expectedRoadDistancesCount + " station pairs. " +
              "Will calculate route when " + requiredCachedCount + " (" + 
              (int)(requiredCachePercentage * 100) + "%) distances are cached.");
    }
    
    /**
     * Create a straight line path (fallback)
     */
    private List<LatLng> createStraightLinePath(StationData from, StationData to) {
        List<LatLng> path = new ArrayList<>();
        path.add(new LatLng(from.latitude, from.longitude));
        path.add(new LatLng(to.latitude, to.longitude));
        return path;
    }
    
    // Calculate total distance of a route using straight-line distances (fast for optimization)
    // Road distances are used only for final visualization
    private double calculateRouteDistance(List<StationData> route) {
        if (route.size() < 2) return 0;
        
        double totalDistance = 0;
        for (int i = 0; i < route.size() - 1; i++) {
            // Use straight-line distance for route optimization (much faster)
            // Road distances are calculated separately for visualization
            totalDistance += calculateDistance(
                route.get(i).latitude, route.get(i).longitude,
                route.get(i + 1).latitude, route.get(i + 1).longitude
            );
        }
        return totalDistance;
    }
    
    /**
     * 2-opt Improvement Algorithm with Capacity Constraints
     * 
     * This algorithm tries to improve the route by swapping route segments,
     * but only accepts swaps that maintain valid vehicle capacity constraints.
     * 
     * How it works:
     * 1. Try swapping segments between positions i and j (reverse the segment)
     * 2. For each swapped route, validate that capacity constraints are satisfied
     * 3. Only accept the swap if:
     *    - The route is valid (capacity constraints satisfied at each step)
     *    - The route is shorter than the current best route
     * 4. Repeat until no improvements can be made
     */
    private List<StationData> optimizeRouteWith2Opt(List<StationData> route) {
        if (route.size() < 4) return route; // Need at least 4 stations for 2-opt
        
        List<StationData> improvedRoute = new ArrayList<>(route);
        boolean improved = true;
        int maxIterations = 20; // Reduced from 100 to speed up (2-opt is expensive)
        int iterations = 0;
        
        while (improved && iterations < maxIterations) {
            improved = false;
            iterations++;
            
            double bestDistance = calculateRouteDistance(improvedRoute);
            
            // Try 2-opt swaps (reduced search space for performance)
            // Only try swaps that are likely to improve the route
            for (int i = 1; i < improvedRoute.size() - 2; i++) {
                for (int j = i + 2; j < improvedRoute.size() && j < i + 5; j++) { // Limit j to nearby stations
                    // Create new route with swapped segment
                    List<StationData> newRoute = twoOptSwap(improvedRoute, i, j);
                    
                    // Check if the swapped route maintains capacity constraints
                    if (isValidRoute(newRoute)) {
                        double newDistance = calculateRouteDistance(newRoute);
                        
                        // If new route is shorter and valid, use it
                        if (newDistance < bestDistance * 0.99) { // Only accept if at least 1% improvement
                            improvedRoute = newRoute;
                            bestDistance = newDistance;
                            improved = true;
                            break; // Restart from beginning after improvement
                        }
                    }
                }
                if (improved) break;
            }
        }
        
        return improvedRoute;
    }
    
    /**
     * Validate if a route satisfies vehicle capacity constraints at each step
     * 
     * @param route The route to validate
     * @return true if route is valid, false otherwise
     */
    private boolean isValidRoute(List<StationData> route) {
        if (route.isEmpty()) return false;
        
        // Route must start with a pickup station
        if (!"pickup".equals(route.get(0).stationType)) {
            return false;
        }
        
        // Simulate the route and check capacity constraints
        int cyclesHolding = 0;
        int vehicleCapacity = MAX_VEHICLE_CAPACITY;
        
        for (StationData station : route) {
            if ("pickup".equals(station.stationType)) {
                // Check if we have enough capacity to pick cycles
                if (station.cyclesToMove > vehicleCapacity) {
                    return false; // Not enough capacity
                }
                // Update vehicle state after picking
                cyclesHolding += station.cyclesToMove;
                vehicleCapacity -= station.cyclesToMove;
            } else if ("drop".equals(station.stationType)) {
                // Check if we have enough cycles to drop
                // Condition: cyclesHolding >= cyclesToDrop
                int cyclesToDrop = station.cyclesToMove;
                if (cyclesHolding < cyclesToDrop) {
                    return false; // Not enough cycles to drop
                }
                // Update vehicle state after dropping
                cyclesHolding -= cyclesToDrop;
                vehicleCapacity += cyclesToDrop;
            }
        }
        
        return true; // All constraints satisfied
    }
    
    /**
     * Perform 2-opt swap: reverse the segment between positions i and j
     * 
     * Example:
     * Original: A → B → C → D → E → F
     * Swap(1, 3): A → D → C → B → E → F
     * (segment B-C-D is reversed)
     */
    private List<StationData> twoOptSwap(List<StationData> route, int i, int j) {
        List<StationData> newRoute = new ArrayList<>();
        
        // Add route[0] to route[i-1] in order
        for (int k = 0; k < i; k++) {
            newRoute.add(route.get(k));
        }
        
        // Add route[i] to route[j] in reverse order
        for (int k = j; k >= i; k--) {
            newRoute.add(route.get(k));
        }
        
        // Add route[j+1] to route[end] in order
        for (int k = j + 1; k < route.size(); k++) {
            newRoute.add(route.get(k));
        }
        
        return newRoute;
    }
    
    /**
     * Custom InfoWindowAdapter for displaying station information on map markers
     * Shows different layouts for pickup and drop stations matching the screenshot design
     */
    private class StationInfoWindowAdapter implements InfoWindowAdapter {
        
        @Override
        public View getInfoWindow(Marker marker) {
            // Get station data from marker tag
            Object tag = marker.getTag();
            if (!(tag instanceof StationData)) {
                return null;
            }
            
            StationData station = (StationData) tag;
            View view;
            
            if ("pickup".equals(station.stationType)) {
                // Inflate pickup station info window layout
                view = LayoutInflater.from(RedistributionMapActivity.this)
                        .inflate(R.layout.info_window_pickup_station, null);
                
                TextView stationNameLabel = view.findViewById(R.id.tv_station_name_label);
                TextView stationName = view.findViewById(R.id.tv_station_name);
                TextView stationIdLabel = view.findViewById(R.id.tv_station_id_label);
                TextView stationId = view.findViewById(R.id.tv_station_id);
                TextView pickupBtnText = view.findViewById(R.id.tv_pickup_button_text);
                
                stationNameLabel.setText("Station Name");
                stationName.setText(station.stationName);
                stationIdLabel.setText("Station ID");
                stationId.setText(station.stationId);
                // Show cycles to move (cycles to pick) in brackets
                pickupBtnText.setText("Pick Up Station (" + station.cyclesToMove + ")");
                
            } else if ("drop".equals(station.stationType)) {
                // Inflate drop station info window layout
                view = LayoutInflater.from(RedistributionMapActivity.this)
                        .inflate(R.layout.info_window_drop_station, null);
                
                TextView stationNameLabel = view.findViewById(R.id.tv_station_name_label);
                TextView stationName = view.findViewById(R.id.tv_station_name);
                TextView stationIdLabel = view.findViewById(R.id.tv_station_id_label);
                TextView stationId = view.findViewById(R.id.tv_station_id);
                TextView dropBtnText = view.findViewById(R.id.tv_drop_button_text);
                
                stationNameLabel.setText("Station Name");
                stationName.setText(station.stationName);
                stationIdLabel.setText("Station ID");
                stationId.setText(station.stationId);
                // Show cycles to move (cycles to drop) in brackets
                dropBtnText.setText("Drop Off Station (" + station.cyclesToMove + ")");
                
            } else {
                return null;
            }
            
            return view;
        }
        
        @Override
        public View getInfoContents(Marker marker) {
            // Return null to use getInfoWindow instead
            return null;
        }
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

