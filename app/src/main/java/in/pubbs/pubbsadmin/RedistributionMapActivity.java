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
import android.os.Handler;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import in.pubbs.pubbsadmin.Model.StationData;
import in.pubbs.pubbsadmin.Model.RouteState;

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
    private Polyline highlightedSegment = null; // Track highlighted path segment
    private List<LatLng> currentPathPoints = new ArrayList<>(); // Store current path points for highlighting
    private Handler animationHandler = new Handler(); // Handler for path animation
    private Runnable animationRunnable = null; // Runnable for animated path highlighting
    
    private Button redistributionFinishedBtn;
    private TextView toolbarTitle;
    private ImageView backButton;
    private TextView tvPickupStopsCount;
    private TextView tvDropStopsCount;
    private TextView tvRouteDistance; // Display total distance in route title
    
    // Loading overlay
    private View loadingOverlay;
    private boolean isRouteLoading = false;
    
    // Distance cache: Store road distances between station pairs to avoid repeated API calls
    private Map<String, Double> roadDistanceCache = new HashMap<>();
    
    // Path cache: Store road paths (polyline points) between station pairs
    private Map<String, List<LatLng>> roadPathCache = new HashMap<>();
    
    private RequestQueue requestQueue; // For Directions API requests
    
    // Vehicle capacity constant (now using RouteValidator)
    private static final int MAX_VEHICLE_CAPACITY = RouteValidator.getMaxVehicleCapacity();
    
    // Algorithm selection
    private String selectedAlgorithm = "nearest_neighbor"; // Default to nearest neighbor
    private String selectedAlgorithmName = "Nearest Neighbor + 2-Opt"; // Display name
    private double finalRouteDistance = 0; // Store final route distance for toast
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_redistribution_map);
        
        // Read algorithm selection from Intent
        String algorithm = getIntent().getStringExtra("algorithm");
        String algorithmName = getIntent().getStringExtra("algorithm_name");
        if (algorithm != null && (algorithm.equals("nearest_neighbor") || algorithm.equals("ant_colony"))) {
            selectedAlgorithm = algorithm;
            selectedAlgorithmName = algorithmName != null ? algorithmName : 
                (algorithm.equals("ant_colony") ? "Ant Colony Optimization" : "Nearest Neighbor + 2-Opt");
            Log.d(TAG, "Selected algorithm: " + selectedAlgorithm + " (" + selectedAlgorithmName + ")");
        }
        
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
        tvRouteDistance = findViewById(R.id.tv_route_distance);
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
        
        // Marker click listener - animate to marker, highlight it, show info window, and highlight path segment
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
                
                // Highlight path segment from this station to next station
                highlightPathSegment(station);
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
                // Store station data in marker tag for click handling and info window
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
                
                // Show toast with algorithm and distance
                if (finalRouteDistance > 0) {
                    String distanceKm = String.format("%.2f", finalRouteDistance / 1000.0);
                    String message = selectedAlgorithmName + "\nTotal Distance: " + distanceKm + " km";
                    Toast.makeText(RedistributionMapActivity.this, message, Toast.LENGTH_LONG).show();
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
        // Clear ALL existing polylines - ensure only ONE polyline exists
        for (Polyline polyline : routePolylines) {
            polyline.remove();
        }
        routePolylines.clear();
        
        if (route.size() >= 2) {
            Log.d(TAG, "Redrawing route with " + route.size() + " stations - creating SINGLE polyline");
            
            // Create ONE empty polyline - will be updated with real road paths only
            PolylineOptions polylineOptions = new PolylineOptions()
                    .color(Color.parseColor("#1976D2"))
                    .width(12f)
                    .geodesic(false)
                    .zIndex(-10.0f);
            
            Polyline polyline = googleMap.addPolyline(polylineOptions);
            routePolylines.add(polyline);
            
            // Log route for debugging
            StringBuilder routeStr = new StringBuilder("Route: ");
            for (int i = 0; i < route.size(); i++) {
                routeStr.append(route.get(i).stationId);
                if (i < route.size() - 1) routeStr.append(" -> ");
            }
            Log.d(TAG, routeStr.toString());
            
            // Asynchronously fetch and update with actual road paths only
            fetchRoadPathsAndUpdatePolyline(route, polyline);
        }
    }
    
    private void calculateAndDrawRoute() {
        // Clear ALL existing polylines - ensure only ONE polyline exists
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
        Log.d(TAG, "Using algorithm: " + selectedAlgorithm);
        
        // Calculate route using selected algorithm
        List<StationData> route;
        if ("ant_colony".equals(selectedAlgorithm)) {
            route = calculateAntColonyRoute();
        } else {
            route = calculateNearestNeighborRoute();
        }
        currentRoute = new ArrayList<>(route); // Store for marker display
        
        if (route.isEmpty()) {
            Log.w(TAG, "No valid route found!");
            hideLoadingOverlay();
            Toast.makeText(this, "No valid route found that satisfies capacity constraints!", 
                    Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Log route and vehicle state
        double initialDistance = calculateRouteDistance(route);
        String algorithmName = "ant_colony".equals(selectedAlgorithm) ? "Ant Colony Optimization" : "Nearest Neighbor";
        Log.d(TAG, "Initial " + algorithmName + " route distance: " + String.format("%.2f", initialDistance / 1000.0) + " km (" + String.format("%.0f", initialDistance) + " meters)");
        logRouteWithVehicleState(route);
        
        // Apply 2-opt improvement only for Nearest Neighbor (ACO already optimizes)
        List<StationData> optimizedRoute = route;
        double finalDistance = initialDistance;
        if ("nearest_neighbor".equals(selectedAlgorithm)) {
            optimizedRoute = optimizeRouteWith2Opt(route);
            
            if (!optimizedRoute.equals(route)) {
                finalDistance = calculateRouteDistance(optimizedRoute);
                double improvement = ((initialDistance - finalDistance) / initialDistance) * 100;
                Log.d(TAG, "Route improved by 2-opt optimization");
                Log.d(TAG, "Optimized route distance: " + String.format("%.2f", finalDistance / 1000.0) + " km (" + String.format("%.0f", finalDistance) + " meters)");
                Log.d(TAG, "Distance improvement: " + String.format("%.2f", improvement) + "% (" + String.format("%.0f", initialDistance - finalDistance) + " meters saved)");
                logRouteWithVehicleState(optimizedRoute);
                route = optimizedRoute;
                currentRoute = new ArrayList<>(route); // Update stored route
            } else {
                Log.d(TAG, "2-opt optimization: No improvement found (route is already locally optimal)");
            }
        } else {
            Log.d(TAG, "Ant Colony Optimization: Route already optimized, skipping 2-opt");
        }
        
        // Store final distance for toast display
        finalRouteDistance = finalDistance;
        
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
            
            // Create ONE empty polyline - will be updated with real road paths only (NO straight lines)
            PolylineOptions polylineOptions = new PolylineOptions()
                    .color(Color.parseColor("#1976D2"))
                    .width(12f)
                    .geodesic(false)
                    .zIndex(-10.0f);
            
            Polyline polyline = googleMap.addPolyline(polylineOptions);
            routePolylines.add(polyline); // Only ONE polyline in the list
            
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
            // Use road distance (from cache) for accurate route calculation
            double distance = getRoadDistance(from, candidate);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = candidate;
            }
        }
        
        return nearest;
    }
    
    /**
     * Calculate straight-line distance (Haversine formula) - ONLY for proximity checking
     * NOT used for route distance calculations - road distances are used instead
     */
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        return RouteUtils.calculateDistance(lat1, lng1, lat2, lng2);
    }
    
    /**
     * Get road distance between two stations using Google Directions API
     * Uses cache to avoid repeated API calls
     * Returns road distance from cache only - no straight-line fallback
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
        
        // If not cached, log error and return large value (should not happen if pre-calculation works)
        Log.e(TAG, "Road distance not cached for: " + from.stationId + " -> " + to.stationId + 
              ". This should not happen if pre-calculation is working. Returning large distance value.");
        
        // Start async calculation for future use (non-blocking, don't wait)
        getRoadDistanceAsync(from, to);
        
        // Return very large value to indicate this distance is not available
        // This will make this route segment undesirable in optimization
        return Double.MAX_VALUE;
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
                                    // Start route calculation when 100% of distances are cached (required since we don't use straight-line fallback)
                                    final double requiredCachePercentage = 1.0;
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
                        // NO straight-line fallback - return error value
                        result.set(Double.MAX_VALUE);
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
                    
                    // NO straight-line fallback - return error value
                    result.set(Double.MAX_VALUE);
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
            // NO straight-line fallback - return error value
            return Double.MAX_VALUE;
        }
        
        double finalDistance = result.get() != null ? result.get() : Double.MAX_VALUE;
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
            List<LatLng> cachedPath = roadPathCache.get(cacheKey);
            List<LatLng> pathToUse = new ArrayList<>(cachedPath);
            
            // Check if we need to reverse the path (if cached direction is opposite)
            boolean needReverse = from.stationId.compareTo(to.stationId) > 0;
            if (needReverse && pathToUse.size() > 1) {
                // Reverse the path to match our travel direction
                Collections.reverse(pathToUse);
                Log.d(TAG, "Reversed cached road path for: " + from.stationId + " -> " + to.stationId);
            }
            
            Log.d(TAG, "Using cached road path: " + from.stationId + " -> " + to.stationId + 
                  " (" + pathToUse.size() + " points)");
            return pathToUse;
        }
        
        // Don't return straight line - return empty list if not cached
        // Paths will be fetched asynchronously via fetchRoadPathsAndUpdatePolyline
        Log.d(TAG, "Road path not cached for: " + from.stationId + " -> " + to.stationId + " - will be fetched asynchronously");
        
        // Start async fetch for future use (non-blocking)
        getRoadPathAsync(from, to);
        
        return new ArrayList<>(); // Return empty list, not straight line
    }
    
    /**
     * Fetch complete route path using ONE Directions API call with waypoints
     * Makes a single API call: origin -> waypoints -> destination (optimize=false)
     * Extracts and merges all step polylines into ONE continuous path
     */
    private void fetchRoadPathsAndUpdatePolyline(List<StationData> route, Polyline polyline) {
        if (route.size() < 2) {
            Log.w(TAG, "Route has less than 2 stations, cannot fetch path");
            hideLoadingOverlay();
            return;
        }
        
        Log.d(TAG, "Fetching complete route path with ONE API call for " + route.size() + " stations");
        
        // Build URL with waypoints (ONE API call for entire route)
        String url = buildDirectionsUrlWithWaypoints(route);
        if (url == null) {
            Log.e(TAG, "Failed to build Directions API URL");
            hideLoadingOverlay();
            return;
        }
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        String status = response.optString("status", "UNKNOWN");
                        
                        if ("OK".equals(status)) {
                            JSONArray routes = response.getJSONArray("routes");
                            if (routes.length() > 0) {
                                JSONObject routeObj = routes.getJSONObject(0);
                                
                                // Extract and merge all step polylines into ONE continuous path
                                List<LatLng> mergedPath = extractAndMergeAllStepPolylines(routeObj, route);
                                
                                if (!mergedPath.isEmpty()) {
                                    // Calculate total distance including straight line segments
                                    double totalDistance = calculateTotalPathDistance(mergedPath);
                                    
                                    runOnUiThread(() -> {
                                        polyline.setPoints(mergedPath);
                                        // Store path points for highlighting segments
                                        currentPathPoints = new ArrayList<>(mergedPath);
                                        
                                        Log.d(TAG, "SUCCESS: Single continuous polyline created with " + 
                                              mergedPath.size() + " points covering " + route.size() + " stations");
                                        
                                        // Update final route distance to include straight line segments
                                        finalRouteDistance = totalDistance;
                                        
                                        // Update distance display in route title
                                        updateRouteDistanceDisplay(totalDistance);
                                        
                                        // Show toast with algorithm name and total distance
                                        String algorithmName = "ant_colony".equals(selectedAlgorithm) ? 
                                                "Ant Colony Optimization" : "Nearest Neighbor + 2-Opt";
                                        String distanceText = String.format("%.2f", totalDistance / 1000.0) + " km";
                                        Toast.makeText(RedistributionMapActivity.this, 
                                                algorithmName + "\nTotal Distance: " + distanceText, 
                                                Toast.LENGTH_LONG).show();
                                        
                                        hideLoadingOverlay();
                                    });
                                } else {
                                    Log.e(TAG, "Failed to extract path from Directions API response");
                                    runOnUiThread(() -> hideLoadingOverlay());
                                }
                            } else {
                                Log.e(TAG, "No routes found in Directions API response");
                                runOnUiThread(() -> hideLoadingOverlay());
                            }
                        } else {
                            String errorMessage = response.optString("error_message", "No error message");
                            Log.e(TAG, "Directions API returned status: " + status);
                            Log.e(TAG, "Error message: " + errorMessage);
                            runOnUiThread(() -> hideLoadingOverlay());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing Directions API response", e);
                        runOnUiThread(() -> hideLoadingOverlay());
                    }
                },
                error -> {
                    Log.e(TAG, "Directions API error: " + error.getMessage());
                    runOnUiThread(() -> hideLoadingOverlay());
                });
        
        requestQueue.add(request);
    }
    
    /**
     * Extract and merge all step polylines from Directions API response into ONE continuous path
     * Removes duplicate consecutive points to avoid overlapping lines
     * ALWAYS adds straight line extensions to ensure path reaches each station exactly
     */
    private List<LatLng> extractAndMergeAllStepPolylines(JSONObject routeObj, List<StationData> route) {
        List<LatLng> allPoints = new ArrayList<>();
        
        try {
            JSONArray legs = routeObj.getJSONArray("legs");
            
            // Process each leg
            for (int legIndex = 0; legIndex < legs.length(); legIndex++) {
                JSONObject leg = legs.getJSONObject(legIndex);
                JSONArray steps = leg.getJSONArray("steps");
                
                // Process each step in the leg
                for (int stepIndex = 0; stepIndex < steps.length(); stepIndex++) {
                    JSONObject step = steps.getJSONObject(stepIndex);
                    JSONObject polyline = step.getJSONObject("polyline");
                    String encodedPolyline = polyline.getString("points");
                    List<LatLng> stepPoints = decodePolyline(encodedPolyline);
                    
                    // Merge step points, removing duplicates at connection points
                    if (!stepPoints.isEmpty()) {
                        if (allPoints.isEmpty()) {
                            // First step: add all points
                            allPoints.addAll(stepPoints);
                        } else {
                            // Subsequent steps: skip first point if it's duplicate (within 5m)
                            LatLng lastPoint = allPoints.get(allPoints.size() - 1);
                            LatLng firstStepPoint = stepPoints.get(0);
                            double dist = calculateDistance(
                                lastPoint.latitude, lastPoint.longitude,
                                firstStepPoint.latitude, firstStepPoint.longitude
                            );
                            
                            int startIdx = (dist < 5.0) ? 1 : 0; // Skip duplicate connection point
                            for (int k = startIdx; k < stepPoints.size(); k++) {
                                allPoints.add(stepPoints.get(k));
                            }
                        }
                    }
                }
                
                // After processing each leg, ALWAYS ensure path reaches the destination station exactly
                // Always add station coordinate to ensure pin is on top of path (no gaps)
                if (legIndex < route.size() - 1 && !allPoints.isEmpty()) {
                    StationData destinationStation = route.get(legIndex + 1); // Destination of this leg
                    LatLng pathEndPoint = allPoints.get(allPoints.size() - 1);
                    LatLng stationPoint = new LatLng(destinationStation.latitude, destinationStation.longitude);
                    
                    double distanceToStation = calculateDistance(
                        pathEndPoint.latitude, pathEndPoint.longitude,
                        stationPoint.latitude, stationPoint.longitude
                    );
                    
                    // Always add station coordinate to ensure path reaches station exactly (even if very close)
                    if (distanceToStation > 0.1) { // Only skip if essentially the same point (0.1m)
                        Log.d(TAG, "Ensuring path reaches station " + destinationStation.stationId + 
                              " (distance: " + String.format("%.2f", distanceToStation) + "m) - Adding station coordinate");
                        allPoints.add(stationPoint); // Add station coordinate to ensure pin is on path
                    }
                }
            }
            
            // ALWAYS ensure final path reaches the last station exactly
            if (!allPoints.isEmpty() && !route.isEmpty()) {
                StationData lastStation = route.get(route.size() - 1);
                LatLng pathEndPoint = allPoints.get(allPoints.size() - 1);
                LatLng lastStationPoint = new LatLng(lastStation.latitude, lastStation.longitude);
                
                double distanceToLastStation = calculateDistance(
                    pathEndPoint.latitude, pathEndPoint.longitude,
                    lastStationPoint.latitude, lastStationPoint.longitude
                );
                
                // Always add last station coordinate to ensure path reaches it exactly
                if (distanceToLastStation > 0.1) {
                    Log.d(TAG, "Ensuring path reaches final station " + lastStation.stationId + 
                          " (distance: " + String.format("%.2f", distanceToLastStation) + "m) - Adding station coordinate");
                    allPoints.add(lastStationPoint);
                }
            }
            
            // ALWAYS ensure first station is at the start of the path exactly
            if (!allPoints.isEmpty() && !route.isEmpty()) {
                StationData firstStation = route.get(0);
                LatLng pathStartPoint = allPoints.get(0);
                LatLng firstStationPoint = new LatLng(firstStation.latitude, firstStation.longitude);
                
                double distanceFromFirstStation = calculateDistance(
                    pathStartPoint.latitude, pathStartPoint.longitude,
                    firstStationPoint.latitude, firstStationPoint.longitude
                );
                
                // Always ensure path starts at first station coordinate exactly
                if (distanceFromFirstStation > 0.1) {
                    Log.d(TAG, "Ensuring path starts at first station " + firstStation.stationId + 
                          " (distance: " + String.format("%.2f", distanceFromFirstStation) + "m) - Adding station coordinate");
                    allPoints.set(0, firstStationPoint); // Replace first point with exact station coordinate
                }
            }
            
            // Remove duplicate consecutive points (within 3 meters) to avoid overlapping lines
            List<LatLng> deduplicatedPoints = new ArrayList<>();
            for (LatLng point : allPoints) {
                if (deduplicatedPoints.isEmpty()) {
                    deduplicatedPoints.add(point);
                } else {
                    LatLng lastPoint = deduplicatedPoints.get(deduplicatedPoints.size() - 1);
                    double dist = calculateDistance(
                        lastPoint.latitude, lastPoint.longitude,
                        point.latitude, point.longitude
                    );
                    // Only add if point is significantly different (more than 3 meters)
                    if (dist > 3.0) {
                        deduplicatedPoints.add(point);
                    }
                }
            }
            
            // CRITICAL: Ensure path points at station locations are EXACTLY at station coordinates
            // This ensures pins are on top of the path with no gaps
            if (!deduplicatedPoints.isEmpty() && !route.isEmpty()) {
                // Ensure first point is exactly at first station
                StationData firstStation = route.get(0);
                LatLng firstStationPoint = new LatLng(firstStation.latitude, firstStation.longitude);
                deduplicatedPoints.set(0, firstStationPoint);
                
                // Ensure last point is exactly at last station
                StationData lastStation = route.get(route.size() - 1);
                LatLng lastStationPoint = new LatLng(lastStation.latitude, lastStation.longitude);
                deduplicatedPoints.set(deduplicatedPoints.size() - 1, lastStationPoint);
                
                // For intermediate stations, find closest point and replace it with exact station coordinate
                for (int i = 1; i < route.size() - 1; i++) {
                    StationData station = route.get(i);
                    LatLng stationPoint = new LatLng(station.latitude, station.longitude);
                    
                    // Find closest point in deduplicated path
                    int closestIdx = -1;
                    double minDist = Double.MAX_VALUE;
                    for (int j = 0; j < deduplicatedPoints.size(); j++) {
                        LatLng pathPoint = deduplicatedPoints.get(j);
                        double dist = calculateDistance(
                            stationPoint.latitude, stationPoint.longitude,
                            pathPoint.latitude, pathPoint.longitude
                        );
                        if (dist < minDist && dist < 50.0) { // Only consider points within 50m of station
                            minDist = dist;
                            closestIdx = j;
                        }
                    }
                    
                    // Replace closest point with exact station coordinate if found
                    if (closestIdx >= 0 && minDist > 0.1) {
                        deduplicatedPoints.set(closestIdx, stationPoint);
                        Log.d(TAG, "Replaced path point " + closestIdx + " with exact coordinate of station " + station.stationId);
                    }
                }
            }
            
            Log.d(TAG, "Extracted and merged " + allPoints.size() + " points, deduplicated to " + 
                  deduplicatedPoints.size() + " points, with exact station coordinates");
            
            return deduplicatedPoints;
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting step polylines from route", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * LEGACY METHOD - Asynchronously fetch road paths for all route segments (OLD APPROACH)
     * This method is kept for reference but should not be used
     * Use fetchRoadPathsAndUpdatePolyline instead (which makes ONE API call)
     */
    @Deprecated
    private void fetchRoadPathsAndUpdatePolylineLegacy(List<StationData> route, Polyline polyline) {
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
                List<LatLng> cachedPath = roadPathCache.get(cacheKey);
                List<LatLng> pathToUse = new ArrayList<>(cachedPath);
                
                // Check if we need to reverse the path (if cached direction is opposite)
                // Cache key uses lexicographic order, so if fromStation.stationId > toStation.stationId,
                // the cached path is in reverse direction
                boolean needReverse = fromStation.stationId.compareTo(toStation.stationId) > 0;
                if (needReverse && pathToUse.size() > 1) {
                    // Reverse the path to match our travel direction
                    Collections.reverse(pathToUse);
                    Log.d(TAG, "Reversed cached path for " + fromStation.stationId + " -> " + toStation.stationId);
                }
                
                segmentPaths[segmentIndex] = pathToUse;
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
                                    // Use detailed step-by-step geometry for accurate road paths
                                    JSONObject routeObj = routes.getJSONObject(0);
                                    List<LatLng> path = extractDetailedPathFromRoute(routeObj);
                                    
                                    // Use the detailed API path - follows real roads precisely
                                    if (path.isEmpty()) {
                                        Log.e(TAG, "Extracted path is empty for segment " + (segmentIndex + 1));
                                        segmentPaths[segmentIndex] = null;
                                    } else {
                                        roadPathCache.put(cacheKey, path); // Cache it (always in lexicographic order)
                                        segmentPaths[segmentIndex] = path;
                                        Log.d(TAG, "Segment " + (segmentIndex + 1) + "/" + totalSegments + 
                                              " (Detailed API): " + fromStation.stationId + " -> " + toStation.stationId + 
                                              " (" + path.size() + " detailed points from real roads)");
                                    }
                                } else {
                                    Log.e(TAG, "No routes found for segment " + (segmentIndex + 1) + 
                                          " (" + fromStation.stationId + " -> " + toStation.stationId + ")");
                                    // Don't use straight line - leave null, will be handled in updatePolylineWithRoadPaths
                                    segmentPaths[segmentIndex] = null;
                                }
                            } else {
                                String errorMessage = response.optString("error_message", "No error message");
                                Log.e(TAG, "API status " + status + " for segment " + (segmentIndex + 1));
                                Log.e(TAG, "Error message: " + errorMessage);
                                // Don't use straight line - leave null, will retry or show error
                                segmentPaths[segmentIndex] = null;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing API response for segment " + (segmentIndex + 1), e);
                            // Don't use straight line - leave null
                            segmentPaths[segmentIndex] = null;
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
                        // Don't use straight line - leave null, will retry or show error
                        segmentPaths[segmentIndex] = null;
                        
                        int completed = completedSegments.incrementAndGet();
                        Log.d(TAG, "Segment " + (segmentIndex + 1) + "/" + totalSegments + " completed (error). " + 
                              completed + "/" + totalSegments + " total segments done");
                        if (completed == totalSegments) {
                            Log.d(TAG, "All " + totalSegments + " segments completed (some with errors)! Updating polyline...");
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
     * Implements active path system: once a road is drawn, it becomes active and is reused by other segments
     * Creates ONE continuous polyline connecting all stations using ONLY real road paths
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
        
        // Check if all segments have valid paths (REQUIRED - no straight lines allowed)
        int missingSegments = 0;
        for (int i = 0; i < segmentPaths.length; i++) {
            if (segmentPaths[i] == null || segmentPaths[i].isEmpty()) {
                missingSegments++;
                Log.e(TAG, "CRITICAL: Segment " + i + " has no road path: " + 
                      route.get(i).stationId + " -> " + route.get(i + 1).stationId);
            }
        }
        if (missingSegments > 0) {
            Log.e(TAG, "ERROR: " + missingSegments + " segments missing road paths. Cannot draw complete route without straight lines.");
            // Don't draw incomplete route - wait for all segments to be fetched
            return;
        }
        
        // Active path system: track which roads are already drawn (active paths)
        // When a new segment uses a road that's already active, reuse the active path
        List<LatLng> activePathPoints = new ArrayList<>(); // All points that are part of active paths
        
        // Merge ALL segments into ONE continuous polyline using active path system
        for (int i = 0; i < segmentPaths.length; i++) {
            StationData fromStation = route.get(i);
            StationData toStation = route.get(i + 1);
            List<LatLng> segmentPath = segmentPaths[i];
            
            if (segmentPath == null || segmentPath.isEmpty()) {
                Log.e(TAG, "Segment " + i + " is null/empty - this should not happen after validation");
                continue;
            }
            
            if (i == 0) {
                // First segment: activate all points (mark as active path)
                allPathPoints.addAll(segmentPath);
                activePathPoints.addAll(segmentPath);
                Log.d(TAG, "Segment 0: Activated " + segmentPath.size() + " points from " + 
                      fromStation.stationId + " -> " + toStation.stationId + " (NEW ACTIVE PATH)");
            } else {
                // Subsequent segments: check each point against active paths
                // If a point is already on an active path (within 10m), skip it (reuse active path)
                // Otherwise, add it and mark it as active
                int pointsAdded = 0;
                int pointsReused = 0;
                
                for (int j = 0; j < segmentPath.size(); j++) {
                    LatLng newPoint = segmentPath.get(j);
                    boolean isOnActivePath = false;
                    
                    // Check if this point is already on an active path (within 10 meters)
                    for (LatLng activePoint : activePathPoints) {
                        double dist = calculateDistance(
                            activePoint.latitude, activePoint.longitude,
                            newPoint.latitude, newPoint.longitude
                        );
                        if (dist < 10.0) { // Within 10m = same road point
                            isOnActivePath = true;
                            pointsReused++;
                            break;
                        }
                    }
                    
                    // Only add if not on an active path (new road segment)
                    if (!isOnActivePath) {
                        allPathPoints.add(newPoint);
                        activePathPoints.add(newPoint); // Mark as active
                        pointsAdded++;
                    }
                }
                
                Log.d(TAG, "Segment " + i + ": " + fromStation.stationId + " -> " + toStation.stationId + 
                      " - Added " + pointsAdded + " new points, Reused " + pointsReused + " active path points");
            }
        }
        
        // Update the single polyline with all merged points (ONE continuous line, no duplicates)
        if (allPathPoints.size() > 1) {
            polyline.setPoints(allPathPoints);
            Log.d(TAG, "SUCCESS: Single continuous polyline created with " + allPathPoints.size() + 
                  " points covering " + route.size() + " stations - Active paths reused, ONE line only");
        } else {
            Log.e(TAG, "ERROR: Failed to create polyline - only " + allPathPoints.size() + " points generated for " + 
                  route.size() + " stations");
        }
        
        // Log route for debugging
        StringBuilder routeStr = new StringBuilder("Complete route: ");
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
                                // Use detailed step-by-step geometry for accurate road paths
                                JSONObject route = routes.getJSONObject(0);
                                List<LatLng> path = extractDetailedPathFromRoute(route);
                                
                                // Cache the detailed path - follows real roads precisely
                                if (!path.isEmpty()) {
                                    roadPathCache.put(cacheKey, path);
                                    Log.d(TAG, "Cached detailed road path: " + from.stationId + " -> " + to.stationId + 
                                          " (" + path.size() + " detailed points from real roads)");
                                } else {
                                    Log.e(TAG, "Extracted detailed path is empty for: " + from.stationId + " -> " + to.stationId);
                                }
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
     * Build Google Directions API URL with waypoints
     * Makes ONE API call for the entire route: origin -> waypoints -> destination
     * Uses optimize=false to preserve station order (as per our logic)
     */
    private String buildDirectionsUrlWithWaypoints(List<StationData> route) {
        String apiKey = "AIzaSyCv2efqVWc7ju_Tdn01pnLSCiPoLSH00yQ";
        
        if (route.size() < 2) {
            return null;
        }
        
        // Origin = first station
        StationData origin = route.get(0);
        String originStr = origin.latitude + "," + origin.longitude;
        
        // Destination = last station
        StationData destination = route.get(route.size() - 1);
        String destStr = destination.latitude + "," + destination.longitude;
        
        // Waypoints = all intermediate stations (optimize=false to preserve order)
        StringBuilder waypointsBuilder = new StringBuilder();
        if (route.size() > 2) {
            for (int i = 1; i < route.size() - 1; i++) {
                if (i > 1) waypointsBuilder.append("|"); // Separate waypoints with |
                waypointsBuilder.append(route.get(i).latitude).append(",").append(route.get(i).longitude);
            }
        }
        
        String url = "https://maps.googleapis.com/maps/api/directions/json" +
                "?origin=" + originStr +
                "&destination=" + destStr +
                (waypointsBuilder.length() > 0 ? "&waypoints=optimize:false|" + waypointsBuilder.toString() : "") +
                "&mode=driving" +
                "&alternatives=false" +
                "&key=" + apiKey;
        
        Log.d(TAG, "Built Directions API URL with " + route.size() + " stations (1 origin, " + 
              (route.size() - 2) + " waypoints, 1 destination)");
        return url;
    }
    
    /**
     * Build Google Directions API URL (legacy - for single segment)
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
     * Extract detailed path from Google Directions API route response
     * Uses step-by-step geometry for accurate road paths (more detailed than overview_polyline)
     */
    private List<LatLng> extractDetailedPathFromRoute(JSONObject routeObj) {
        List<LatLng> allPoints = new ArrayList<>();
        
        try {
            JSONArray legs = routeObj.getJSONArray("legs");
            for (int i = 0; i < legs.length(); i++) {
                JSONObject leg = legs.getJSONObject(i);
                JSONArray steps = leg.getJSONArray("steps");
                
                // Extract detailed geometry from each step
                for (int j = 0; j < steps.length(); j++) {
                    JSONObject step = steps.getJSONObject(j);
                    JSONObject polyline = step.getJSONObject("polyline");
                    String encodedPolyline = polyline.getString("points");
                    List<LatLng> stepPoints = decodePolyline(encodedPolyline);
                    
                    // Merge step points, avoiding duplicates at connection points
                    if (!stepPoints.isEmpty()) {
                        if (allPoints.isEmpty()) {
                            // First step: add all points
                            allPoints.addAll(stepPoints);
                        } else {
                            // Subsequent steps: only skip first point if it's the exact same (within 5m)
                            // Add ALL other points - they represent real road geometry
                            LatLng lastPoint = allPoints.get(allPoints.size() - 1);
                            LatLng firstStepPoint = stepPoints.get(0);
                            double dist = calculateDistance(
                                lastPoint.latitude, lastPoint.longitude,
                                firstStepPoint.latitude, firstStepPoint.longitude
                            );
                            
                            // Only skip if very close (same point), otherwise add all points
                            int startIdx = (dist < 5.0) ? 1 : 0;
                            for (int k = startIdx; k < stepPoints.size(); k++) {
                                allPoints.add(stepPoints.get(k));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting detailed path from route", e);
            // Fallback to overview polyline if detailed extraction fails
            try {
                JSONObject overviewPolyline = routeObj.getJSONObject("overview_polyline");
                String encodedPolyline = overviewPolyline.getString("points");
                return decodePolyline(encodedPolyline);
            } catch (Exception e2) {
                Log.e(TAG, "Error extracting overview polyline as fallback", e2);
                return new ArrayList<>();
            }
        }
        
        return allPoints;
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
        // We'll start route calculation when 100% of distances are cached (required since we don't use straight-line fallback)
        final double requiredCachePercentage = 1.0;
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
     * Ant Colony Optimization Algorithm for Capacitated Vehicle Routing
     * 
     * Parameters:
     * - alpha: pheromone importance (default: 1.0)
     * - beta: distance importance (default: 2.0)
     * - evaporation: pheromone evaporation rate (default: 0.5)
     * - numAnts: number of ants per iteration (default: 10)
     * - maxIterations: maximum iterations (default: 50)
     * 
     * All constraints are enforced:
     * - Vehicle capacity (max 4 cycles)
     * - Route must start with pickup
     * - Capacity constraints at each step
     */
    private List<StationData> calculateAntColonyRoute() {
        Log.d(TAG, "Starting Ant Colony Optimization algorithm...");
        
        // ACO parameters
        double alpha = 1.0; // Pheromone importance
        double beta = 2.0;  // Distance importance
        double evaporation = 0.5; // Evaporation rate
        int numAnts = 15; // Number of ants per iteration
        int maxIterations = 50; // Maximum iterations
        
        List<StationData> allStations = new ArrayList<>();
        allStations.addAll(pickupStations);
        allStations.addAll(dropStations);
        
        if (allStations.size() < 2) {
            return new ArrayList<>();
        }
        
        // Initialize pheromone matrix (symmetric, all pairs)
        Map<String, Double> pheromone = new HashMap<>();
        double initialPheromone = 1.0;
        
        for (int i = 0; i < allStations.size(); i++) {
            for (int j = i + 1; j < allStations.size(); j++) {
                String key1 = allStations.get(i).stationId + "_" + allStations.get(j).stationId;
                String key2 = allStations.get(j).stationId + "_" + allStations.get(i).stationId;
                pheromone.put(key1, initialPheromone);
                pheromone.put(key2, initialPheromone); // Symmetric
            }
        }
        
        List<StationData> bestRoute = null;
        double bestDistance = Double.MAX_VALUE;
        
        // Main ACO loop
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            List<List<StationData>> antRoutes = new ArrayList<>();
            List<Double> antDistances = new ArrayList<>();
            
            // Each ant builds a route
            for (int ant = 0; ant < numAnts; ant++) {
                List<StationData> route = buildAntRoute(pheromone, alpha, beta, allStations);
                // Only accept routes that visit ALL stations AND are valid
                if (!route.isEmpty() && isValidRoute(route) && visitsAllStations(route, allStations)) {
                    double distance = calculateRouteDistance(route);
                    antRoutes.add(route);
                    antDistances.add(distance);
                    
                    // Update best route
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestRoute = new ArrayList<>(route);
                        Log.d(TAG, "Iteration " + iteration + ", Ant " + ant + ": New best distance = " + 
                              String.format("%.2f", bestDistance / 1000.0) + " km");
                    }
                }
            }
            
            // Update pheromones
            // Evaporation
            for (String key : pheromone.keySet()) {
                pheromone.put(key, pheromone.get(key) * (1.0 - evaporation));
            }
            
            // Deposit pheromones (only for valid routes)
            for (int i = 0; i < antRoutes.size(); i++) {
                List<StationData> route = antRoutes.get(i);
                double routeDistance = antDistances.get(i);
                double pheromoneDeposit = 1000.0 / routeDistance; // Inverse of distance
                
                // Deposit pheromone on all edges in this route
                for (int j = 0; j < route.size() - 1; j++) {
                    StationData from = route.get(j);
                    StationData to = route.get(j + 1);
                    String key1 = from.stationId + "_" + to.stationId;
                    String key2 = to.stationId + "_" + from.stationId;
                    
                    if (pheromone.containsKey(key1)) {
                        pheromone.put(key1, pheromone.get(key1) + pheromoneDeposit);
                    }
                    if (pheromone.containsKey(key2)) {
                        pheromone.put(key2, pheromone.get(key2) + pheromoneDeposit);
                    }
                }
            }
        }
        
        if (bestRoute != null && !bestRoute.isEmpty()) {
            Log.d(TAG, "ACO completed. Best route distance: " + String.format("%.2f", bestDistance / 1000.0) + " km");
            return bestRoute;
        } else {
            Log.w(TAG, "ACO failed to find valid route, falling back to nearest neighbor");
            return calculateNearestNeighborRoute();
        }
    }
    
    /**
     * Build a single ant route using pheromone and distance information
     */
    private List<StationData> buildAntRoute(Map<String, Double> pheromone, double alpha, double beta, List<StationData> allStations) {
        RouteState routeState = new RouteState();
        List<StationData> availablePickups = new ArrayList<>(pickupStations);
        List<StationData> availableDrops = new ArrayList<>(dropStations);
        List<StationData> route = new ArrayList<>();
        
        // Start with a random pickup station
        if (availablePickups.isEmpty()) {
            return route;
        }
        
        int startIndex = (int) (Math.random() * availablePickups.size());
        StationData currentStation = availablePickups.get(startIndex);
        route.add(currentStation);
        routeState.visitStation(currentStation);
        availablePickups.remove(currentStation);
        
        // Build route probabilistically (similar to Nearest Neighbor prioritization)
        int maxSteps = allStations.size() * 2; // Prevent infinite loops
        int steps = 0;
        
        while ((!availablePickups.isEmpty() || !availableDrops.isEmpty()) && steps < maxSteps) {
            steps++;
            
            // Get valid candidates (same constraint checking as Nearest Neighbor)
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
            
            // Prioritize like Nearest Neighbor: prefer drops when holding cycles, pickups when empty
            List<StationData> validCandidates = new ArrayList<>();
            int remainingStations = availablePickups.size() + availableDrops.size();
            
            if (remainingStations == 1) {
                // LAST station - MUST be a drop station if possible (same as Nearest Neighbor)
                if (!validDrops.isEmpty()) {
                    validCandidates = validDrops;
                } else {
                    validCandidates.addAll(validPickups);
                    validCandidates.addAll(validDrops);
                }
            } else if (routeState.cyclesHolding > 0 && !validDrops.isEmpty()) {
                // We have cycles, prioritize drops (same as Nearest Neighbor)
                validCandidates.addAll(validDrops);
                validCandidates.addAll(validPickups);
            } else {
                // No cycles or no valid drops, prioritize pickups (same as Nearest Neighbor)
                validCandidates.addAll(validPickups);
                validCandidates.addAll(validDrops);
            }
            
            if (validCandidates.isEmpty()) {
                break; // No valid moves
            }
            
            // Calculate probabilities for each candidate
            List<Double> probabilities = new ArrayList<>();
            double totalProbability = 0.0;
            
            for (StationData candidate : validCandidates) {
                String key = currentStation.stationId + "_" + candidate.stationId;
                double pheromoneLevel = pheromone.getOrDefault(key, 0.1);
                double distance = getRoadDistance(currentStation, candidate);
                if (distance <= 0 || distance == Double.MAX_VALUE) {
                    distance = 10000.0; // Fallback for missing distances
                }
                double heuristic = 1.0 / distance; // Inverse distance
                
                double probability = Math.pow(pheromoneLevel, alpha) * Math.pow(heuristic, beta);
                probabilities.add(probability);
                totalProbability += probability;
            }
            
            // Select next station using roulette wheel selection
            StationData nextStation = null;
            if (totalProbability > 0) {
                double random = Math.random() * totalProbability;
                double cumulative = 0.0;
                for (int i = 0; i < validCandidates.size(); i++) {
                    cumulative += probabilities.get(i);
                    if (random <= cumulative) {
                        nextStation = validCandidates.get(i);
                        break;
                    }
                }
            }
            
            // Fallback to first candidate if selection failed
            if (nextStation == null && !validCandidates.isEmpty()) {
                nextStation = validCandidates.get(0);
            }
            
            if (nextStation != null) {
                route.add(nextStation);
                routeState.visitStation(nextStation);
                availablePickups.remove(nextStation);
                availableDrops.remove(nextStation);
                currentStation = nextStation;
            } else {
                break;
            }
        }
        
        // Ensure route ends with a drop if possible (same constraint as Nearest Neighbor Step 6)
        if (!route.isEmpty() && "pickup".equals(route.get(route.size() - 1).stationType)) {
            // Try to find a valid drop station to end with
            currentStation = route.get(route.size() - 1);
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
                route.add(nearestValidDrop);
                routeState.visitStation(nearestValidDrop);
            }
        }
        
        return route;
    }
    
    // Calculate total distance of a route using road distances only
    private double calculateRouteDistance(List<StationData> route) {
        return RouteUtils.calculateRouteDistance(route, roadDistanceCache);
    }
    
    /**
     * Calculate total distance of a path (polyline points) including straight line segments
     * This includes both road paths and straight line extensions to stations
     */
    private double calculateTotalPathDistance(List<LatLng> pathPoints) {
        return RouteUtils.calculateTotalPathDistance(pathPoints);
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
        return RouteOptimizer.optimizeRouteWith2Opt(route, roadDistanceCache);
    }
    
    /**
     * Validate if a route satisfies vehicle capacity constraints at each step
     * 
     * @param route The route to validate
     * @return true if route is valid, false otherwise
     */
    private boolean isValidRoute(List<StationData> route) {
        return RouteValidator.isValidRoute(route);
    }
    
    /**
     * Check if a route visits all required stations
     * 
     * @param route The route to check
     * @param allStations List of all stations that must be visited
     * @return true if route visits all stations, false otherwise
     */
    private boolean visitsAllStations(List<StationData> route, List<StationData> allStations) {
        return RouteValidator.visitsAllStations(route, allStations);
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
    
    /**
     * Highlight path segment from clicked station to next station with animated arrows
     * Creates a highlighted polyline with different color and pattern for better visibility
     */
    private void highlightPathSegment(StationData clickedStation) {
        // Remove previous highlight (this will also stop animation)
        if (highlightedSegment != null) {
            stopAnimatedPathHighlight(); // Stop animation first
            highlightedSegment.remove();
            highlightedSegment = null;
        }
        
        // Find the clicked station's index in the route
        int stationIndex = -1;
        for (int i = 0; i < currentRoute.size(); i++) {
            if (currentRoute.get(i).stationId.equals(clickedStation.stationId)) {
                stationIndex = i;
                break;
            }
        }
        
        // If station not in route or is the last station, don't highlight
        if (stationIndex < 0 || stationIndex >= currentRoute.size() - 1 || currentPathPoints.isEmpty()) {
            Log.d(TAG, "Station " + clickedStation.stationId + " not in route or is last station - no segment to highlight");
            return;
        }
        
        // Find path points between this station and next station
        StationData nextStation = currentRoute.get(stationIndex + 1);
        LatLng currentStationPoint = new LatLng(clickedStation.latitude, clickedStation.longitude);
        LatLng nextStationPoint = new LatLng(nextStation.latitude, nextStation.longitude);
        
        // Find the segment in the path points
        List<LatLng> segmentPoints = findPathSegment(currentStationPoint, nextStationPoint, currentPathPoints);
        
        if (segmentPoints.size() < 2) {
            Log.w(TAG, "Could not find path segment between " + clickedStation.stationId + " and " + nextStation.stationId);
            return;
        }
        
        // Create highlighted polyline with animated pattern (dashed line with arrows effect)
        PolylineOptions highlightOptions = new PolylineOptions()
                .addAll(segmentPoints)
                .color(Color.parseColor("#FF6B00")) // Orange color for highlight
                .width(16f) // Thicker than main route
                .geodesic(false)
                .zIndex(10.0f); // Above main route
        
        highlightedSegment = googleMap.addPolyline(highlightOptions);
        
        Log.d(TAG, "Highlighted path segment from " + clickedStation.stationId + " to " + 
              nextStation.stationId + " (" + segmentPoints.size() + " points)");
        
        // Start animated highlighting with moving arrows effect
        startAnimatedPathHighlight(segmentPoints);
    }
    
    /**
     * Animate the highlighted path segment with a pulsing color effect
     * Creates a smooth animated highlight that pulses to draw attention to the path
     */
    private void startAnimatedPathHighlight(final List<LatLng> segmentPoints) {
        // Stop any existing animation
        stopAnimatedPathHighlight();
        
        if (segmentPoints.size() < 2 || highlightedSegment == null) {
            return;
        }
        
        final int animationDuration = 1500; // 1.5 seconds for one complete pulse cycle
        final long updateInterval = 16; // ~60 FPS for smooth animation
        
        final long startTime = System.currentTimeMillis();
        final int baseColor = Color.parseColor("#FF6B00"); // Orange
        
        animationRunnable = new Runnable() {
            @Override
            public void run() {
                if (highlightedSegment == null) {
                    return;
                }
                
                long elapsed = System.currentTimeMillis() - startTime;
                float progress = (elapsed % animationDuration) / (float) animationDuration;
                
                // Calculate pulsing alpha (0.7 to 1.0) using sine wave for smooth animation
                // Creates a breathing/pulsing effect
                float alpha = 0.7f + 0.3f * (float) (0.5 * (1.0 + Math.sin(progress * 2 * Math.PI)));
                
                // Update polyline color with animated alpha (full path always visible)
                int alphaValue = (int) (alpha * 255);
                int animatedColor = Color.argb(alphaValue, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor));
                
                highlightedSegment.setColor(animatedColor);
                
                // Schedule next update
                animationHandler.postDelayed(this, updateInterval);
            }
        };
        
        animationHandler.post(animationRunnable);
    }
    
    /**
     * Stop the animated path highlight
     */
    private void stopAnimatedPathHighlight() {
        if (animationRunnable != null) {
            animationHandler.removeCallbacks(animationRunnable);
            animationRunnable = null;
        }
    }
    
    /**
     * Update the route distance display in the top card beside "Optimized Pickup Route"
     */
    private void updateRouteDistanceDisplay(double totalDistanceMeters) {
        if (tvRouteDistance != null) {
            double totalDistanceKm = totalDistanceMeters / 1000.0;
            String distanceText = String.format("• %.2f km", totalDistanceKm);
            tvRouteDistance.setText(distanceText);
            tvRouteDistance.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * Find path segment between two station points in the full path
     */
    private List<LatLng> findPathSegment(LatLng fromPoint, LatLng toPoint, List<LatLng> fullPath) {
        List<LatLng> segment = new ArrayList<>();
        
        if (fullPath.isEmpty()) {
            return segment;
        }
        
        // Find the closest point to 'from' station in the path
        int startIndex = findClosestPointIndex(fromPoint, fullPath);
        if (startIndex < 0) {
            return segment;
        }
        
        // Find the closest point to 'to' station in the path (after startIndex)
        int endIndex = findClosestPointIndex(toPoint, fullPath, startIndex);
        if (endIndex < 0 || endIndex <= startIndex) {
            return segment;
        }
        
        // Extract segment from startIndex to endIndex (inclusive)
        for (int i = startIndex; i <= endIndex; i++) {
            segment.add(fullPath.get(i));
        }
        
        // Ensure segment starts and ends at exact station coordinates
        if (!segment.isEmpty()) {
            segment.set(0, fromPoint); // Start at exact station
            segment.set(segment.size() - 1, toPoint); // End at exact next station
        }
        
        return segment;
    }
    
    /**
     * Find index of closest point in path to given coordinate
     */
    private int findClosestPointIndex(LatLng target, List<LatLng> path) {
        return findClosestPointIndex(target, path, 0);
    }
    
    /**
     * Find index of closest point in path to given coordinate (starting from startIndex)
     */
    private int findClosestPointIndex(LatLng target, List<LatLng> path, int startIndex) {
        if (path.isEmpty() || startIndex >= path.size()) {
            return -1;
        }
        
        int closestIndex = startIndex;
        double minDistance = calculateDistance(
            target.latitude, target.longitude,
            path.get(startIndex).latitude, path.get(startIndex).longitude
        );
        
        for (int i = startIndex + 1; i < path.size(); i++) {
            double dist = calculateDistance(
                target.latitude, target.longitude,
                path.get(i).latitude, path.get(i).longitude
            );
            if (dist < minDistance) {
                minDistance = dist;
                closestIndex = i;
            }
        }
        
        return closestIndex;
    }
}

