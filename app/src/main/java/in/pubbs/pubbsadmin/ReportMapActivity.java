package in.pubbs.pubbsadmin;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ReportMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "ReportMapActivity";
    private static final int REQ_LOCATION = 1011;
    private MapView mapView;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private RequestQueue requestQueue;

    private double destLat;
    private double destLng;
    private String bicycleId;
    private String issue;

    private Marker cycleMarker;
    private Circle pulseCircle;
    private Circle pulseCircle2;
    private Circle pulseCircle3;
    private Polyline routePolyline;

    private View infoCard;
    private TextView tvCycleId, tvIssueType, tvTime;
    private Button btnDirection;
    
    private String dateTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_map);

        double rawLat = getIntent().getDoubleExtra("lat", 0d);
        double rawLng = getIntent().getDoubleExtra("lng", 0d);
        bicycleId = getIntent().getStringExtra("bicycleId");
        issue = getIntent().getStringExtra("issue");
        dateTime = getIntent().getStringExtra("dateTime");
        
        // Log all received intent data
        Log.d(TAG, "=== onCreate - Intent Data ===");
        Log.d(TAG, "Raw destLat from intent: " + rawLat);
        Log.d(TAG, "Raw destLng from intent: " + rawLng);
        Log.d(TAG, "bicycleId: " + (bicycleId != null ? bicycleId : "NULL"));
        Log.d(TAG, "issue: " + (issue != null ? issue : "NULL"));
        Log.d(TAG, "dateTime: " + (dateTime != null ? dateTime : "NULL"));
        
        // Validate coordinates - check if they might be swapped
        // Latitude should be between -90 and 90, Longitude between -180 and 180
        if (rawLat == 0d && rawLng == 0d) {
            Log.e(TAG, "ERROR: Both coordinates are 0! Location data is missing!");
            destLat = rawLat;
            destLng = rawLng;
        } else if (Math.abs(rawLat) <= 90 && Math.abs(rawLng) <= 180) {
            // Valid range, use as is
            destLat = rawLat;
            destLng = rawLng;
            Log.d(TAG, "Coordinates in valid range - Using lat: " + destLat + ", lng: " + destLng);
        } else if (Math.abs(rawLng) <= 90 && Math.abs(rawLat) <= 180) {
            // Likely swapped - swap them
            destLat = rawLng;
            destLng = rawLat;
            Log.w(TAG, "WARNING: Coordinates appear to be swapped! Swapped to lat: " + destLat + ", lng: " + destLng);
        } else {
            // Use as is but log warning
            destLat = rawLat;
            destLng = rawLng;
            Log.w(TAG, "WARNING: Coordinates outside normal range - lat: " + destLat + ", lng: " + destLng);
            Log.w(TAG, "This might be in the ocean or invalid location!");
        }
        
        Log.d(TAG, "Final destination coordinates - lat: " + destLat + ", lng: " + destLng);
        
        // Log all intent extras
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            Log.d(TAG, "All intent extras:");
            for (String key : extras.keySet()) {
                Object value = extras.get(key);
                Log.d(TAG, "  " + key + " = " + (value != null ? value.toString() : "NULL"));
            }
        } else {
            Log.w(TAG, "Intent extras bundle is NULL!");
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        requestQueue = Volley.newRequestQueue(this);

        findViewById(R.id.back_button).setOnClickListener(v -> finish());

        infoCard = findViewById(R.id.info_card);
        tvCycleId = findViewById(R.id.tv_cycle_id);
        tvIssueType = findViewById(R.id.tv_issue_type_chip);
        tvTime = findViewById(R.id.tv_time);
        btnDirection = findViewById(R.id.btn_get_direction);

        // Set initial values - always set text, even if empty
        Log.d(TAG, "=== Setting initial UI values ===");
        
        // Always set bicycleId text (clear placeholder if empty)
        if (bicycleId != null && !bicycleId.isEmpty()) {
            Log.d(TAG, "Setting bicycleId: " + bicycleId);
            tvCycleId.setText(bicycleId);
            tvCycleId.setVisibility(View.VISIBLE);
        } else {
            Log.w(TAG, "bicycleId is null or empty, clearing text and hiding view");
            tvCycleId.setText(""); // Clear placeholder text
            tvCycleId.setVisibility(View.GONE);
        }
        
        // Always set issue text (clear placeholder if empty)
        if (issue != null && !issue.isEmpty()) {
            Log.d(TAG, "Setting issue: " + issue);
            tvIssueType.setText(issue);
            tvIssueType.setVisibility(View.VISIBLE);
        } else {
            Log.w(TAG, "issue is null or empty, clearing text and hiding view");
            tvIssueType.setText(""); // Clear placeholder text
            tvIssueType.setVisibility(View.GONE);
        }
        
        // Format time
        if (dateTime != null && !dateTime.isEmpty()) {
            String formattedTime = formatTimeAgo(dateTime);
            Log.d(TAG, "Setting dateTime: " + dateTime + " -> formatted: " + formattedTime);
            tvTime.setText(formattedTime);
        } else {
            Log.w(TAG, "dateTime is null or empty, using 'Just now'");
            tvTime.setText("Just now");
        }
        
        // Log view states
        Log.d(TAG, "tvCycleId visibility: " + (tvCycleId.getVisibility() == View.VISIBLE ? "VISIBLE" : "GONE"));
        Log.d(TAG, "tvCycleId text: '" + tvCycleId.getText().toString() + "'");
        Log.d(TAG, "tvIssueType visibility: " + (tvIssueType.getVisibility() == View.VISIBLE ? "VISIBLE" : "GONE"));
        Log.d(TAG, "tvIssueType text: '" + tvIssueType.getText().toString() + "'");
        Log.d(TAG, "tvTime text: '" + tvTime.getText().toString() + "'");

        btnDirection.setOnClickListener(v -> drawRouteFromCurrentLocation());

        mapView = findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        Log.d(TAG, "=== onMapReady ===");
        Log.d(TAG, "Setting marker at lat: " + destLat + ", lng: " + destLng);
        
        // Validate coordinates before using
        if (destLat == 0d && destLng == 0d) {
            Log.e(TAG, "ERROR: Cannot show map - coordinates are 0,0!");
            return;
        }
        
        if (Math.abs(destLat) > 90 || Math.abs(destLng) > 180) {
            Log.e(TAG, "ERROR: Invalid coordinates - lat: " + destLat + ", lng: " + destLng);
            Log.e(TAG, "Latitude must be between -90 and 90, Longitude between -180 and 180");
            return;
        }
        
        LatLng dest = new LatLng(destLat, destLng);
        Log.d(TAG, "Creating marker at: " + dest.toString());
        addAnimatedCycleMarker(dest);
        
        Log.d(TAG, "Moving camera to location with zoom 16");
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(dest, 16f));

        googleMap.setOnMarkerClickListener(marker -> {
            if (marker.equals(cycleMarker)) {
                Log.d(TAG, "=== Marker clicked - Updating tooltip ===");
                Log.d(TAG, "Current bicycleId: '" + (bicycleId != null ? bicycleId : "NULL") + "'");
                Log.d(TAG, "Current issue: '" + (issue != null ? issue : "NULL") + "'");
                Log.d(TAG, "Current dateTime: '" + (dateTime != null ? dateTime : "NULL") + "'");
                
                // Update tooltip content when marker is clicked - always set text to clear placeholders
                if (bicycleId != null && !bicycleId.isEmpty()) {
                    Log.d(TAG, "Setting bicycleId in tooltip: " + bicycleId);
                    tvCycleId.setText(bicycleId);
                    tvCycleId.setVisibility(View.VISIBLE);
                } else {
                    Log.w(TAG, "bicycleId is null/empty, clearing text and hiding bicycle ID view");
                    tvCycleId.setText(""); // Clear any placeholder
                    tvCycleId.setVisibility(View.GONE);
                }
                
                if (issue != null && !issue.isEmpty()) {
                    Log.d(TAG, "Setting issue in tooltip: " + issue);
                    tvIssueType.setText(issue);
                    tvIssueType.setVisibility(View.VISIBLE);
                } else {
                    Log.w(TAG, "issue is null/empty, clearing text and hiding issue type view");
                    tvIssueType.setText(""); // Clear any placeholder
                    tvIssueType.setVisibility(View.GONE);
                }
                
                // Update time
                if (dateTime != null && !dateTime.isEmpty()) {
                    String formattedTime = formatTimeAgo(dateTime);
                    Log.d(TAG, "Setting time in tooltip: " + dateTime + " -> " + formattedTime);
                    tvTime.setText(formattedTime);
                } else {
                    Log.w(TAG, "dateTime is null/empty, using 'Just now'");
                    tvTime.setText("Just now");
                }
                
                Log.d(TAG, "Final tooltip state - bicycleId visible: " + (tvCycleId.getVisibility() == View.VISIBLE));
                Log.d(TAG, "Final tooltip state - issueType visible: " + (tvIssueType.getVisibility() == View.VISIBLE));
                Log.d(TAG, "Final tooltip state - bicycleId text: '" + tvCycleId.getText().toString() + "'");
                Log.d(TAG, "Final tooltip state - issueType text: '" + tvIssueType.getText().toString() + "'");
                
                infoCard.setVisibility(View.VISIBLE);
                return true;
            }
            return false;
        });

        googleMap.setOnMapClickListener(latLng -> infoCard.setVisibility(View.GONE));

        enableMyLocationIfPermitted();
    }

    private void addAnimatedCycleMarker(LatLng position) {
        // Use ReportIcon.png instead of bicycle marker
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.reporticon);
        Bitmap scaled = Bitmap.createScaledBitmap(bm, 80, 80, true);
        cycleMarker = googleMap.addMarker(new MarkerOptions()
                .position(position)
                .icon(BitmapDescriptorFactory.fromBitmap(scaled))
                .anchor(0.5f, 0.5f));

        // Create multiple pulse circles for ring animation effect (red color)
        // Using red color #FF4444 for attractive pulsing animation
        int redColor = 0xFFFF4444; // Red color
        
        pulseCircle = googleMap.addCircle(new CircleOptions()
                .center(position)
                .radius(40)
                .strokeWidth(4f)
                .strokeColor(0x80FF4444)
                .fillColor(0x00FF4444));

        pulseCircle2 = googleMap.addCircle(new CircleOptions()
                .center(position)
                .radius(40)
                .strokeWidth(4f)
                .strokeColor(0x80FF4444)
                .fillColor(0x00FF4444));

        pulseCircle3 = googleMap.addCircle(new CircleOptions()
                .center(position)
                .radius(40)
                .strokeWidth(4f)
                .strokeColor(0x80FF4444)
                .fillColor(0x00FF4444));

        // Animate rings expanding outward continuously (slower and smoother)
        mapView.postDelayed(new Runnable() {
            double r1 = 40, r2 = 60, r3 = 80;
            @Override
            public void run() {
                if (pulseCircle == null || pulseCircle2 == null || pulseCircle3 == null) return;
                
                // Slower animation - increment by 1.5 instead of 2
                r1 += 1.5;
                r2 += 1.5;
                r3 += 1.5;
                
                // Larger max radius for more visible effect
                if (r1 > 120) r1 = 40;
                if (r2 > 120) r2 = 40;
                if (r3 > 120) r3 = 40;
                
                pulseCircle.setRadius(r1);
                pulseCircle2.setRadius(r2);
                pulseCircle3.setRadius(r3);
                
                // Fade out as radius increases (smoother fade)
                int alpha1 = (int) (255 * (1 - (r1 - 40) / 80.0));
                int alpha2 = (int) (255 * (1 - (r2 - 40) / 80.0));
                int alpha3 = (int) (255 * (1 - (r3 - 40) / 80.0));
                
                // Ensure alpha doesn't go negative
                alpha1 = Math.max(0, Math.min(255, alpha1));
                alpha2 = Math.max(0, Math.min(255, alpha2));
                alpha3 = Math.max(0, Math.min(255, alpha3));
                
                // Red color with alpha
                pulseCircle.setStrokeColor((alpha1 << 24) | 0xFF4444);
                pulseCircle2.setStrokeColor((alpha2 << 24) | 0xFF4444);
                pulseCircle3.setStrokeColor((alpha3 << 24) | 0xFF4444);
                
                // Slower update - 80ms instead of 50ms for smoother animation
                mapView.postDelayed(this, 80);
            }
        }, 80);
    }
    
    private String formatTimeAgo(String dateTimeStr) {
        try {
            if (dateTimeStr == null || dateTimeStr.isEmpty()) {
                return "Just now";
            }
            
            // Try multiple date formats
            java.util.Date reportDate = null;
            String[] formats = {
                "yyyy-MM-dd HH:mm:ss",
                "dd-MM-yyyy HH:mm:ss",
                "MM/dd/yyyy HH:mm:ss",
                "yyyy/MM/dd HH:mm:ss"
            };
            
            for (String format : formats) {
                try {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(format, java.util.Locale.getDefault());
                    reportDate = sdf.parse(dateTimeStr);
                    if (reportDate != null) break;
                } catch (Exception e) {
                    // Try next format
                }
            }
            
            // If parsing failed, try timestamp (long value)
            if (reportDate == null) {
                try {
                    long timestamp = Long.parseLong(dateTimeStr);
                    reportDate = new java.util.Date(timestamp);
                } catch (Exception e) {
                    return "Just now";
                }
            }
            
            if (reportDate == null) return "Just now";
            
            long diff = System.currentTimeMillis() - reportDate.getTime();
            long minutes = diff / (60 * 1000);
            
            if (minutes < 1) return "Just now";
            if (minutes < 60) return minutes + " Min ago";
            long hours = minutes / 60;
            if (hours < 24) return hours + " Hour ago";
            long days = hours / 24;
            return days + " Day ago";
        } catch (Exception e) {
            return "Just now";
        }
    }

    private void enableMyLocationIfPermitted() {
        if (googleMap == null) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQ_LOCATION);
        }
    }

    private void drawRouteFromCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            enableMyLocationIfPermitted();
            return;
        }

        Log.d(TAG, "Getting current location for directions...");
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location == null) {
                Log.w(TAG, "Location is null, requesting location updates...");
                // Try to get current location if last location is null
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    com.google.android.gms.location.LocationRequest locationRequest = com.google.android.gms.location.LocationRequest.create();
                    locationRequest.setPriority(com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY);
                    locationRequest.setNumUpdates(1);
                    fusedLocationClient.requestLocationUpdates(locationRequest, new com.google.android.gms.location.LocationCallback() {
                        @Override
                        public void onLocationResult(com.google.android.gms.location.LocationResult locationResult) {
                            fusedLocationClient.removeLocationUpdates(this);
                            if (locationResult != null && locationResult.getLastLocation() != null && googleMap != null) {
                                android.location.Location currentLocation = locationResult.getLastLocation();
                                drawRoute(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
                            } else {
                                Log.e(TAG, "Could not get current location");
                            }
                        }
                    }, getMainLooper());
                }
                return;
            }
            
            if (googleMap == null) {
                Log.e(TAG, "GoogleMap is null");
                return;
            }

            drawRoute(new LatLng(location.getLatitude(), location.getLongitude()));
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to get location: " + e.getMessage());
        });
    }
    
    private void drawRoute(LatLng from) {
        Log.d(TAG, "=== drawRoute ===");
        Log.d(TAG, "From location - lat: " + from.latitude + ", lng: " + from.longitude);
        Log.d(TAG, "To location (destLat, destLng) - lat: " + destLat + ", lng: " + destLng);
        
        // Validate destination coordinates
        if (destLat == 0d && destLng == 0d) {
            Log.e(TAG, "ERROR: Destination coordinates are 0,0! Cannot draw route.");
            return;
        }
        
        if (Math.abs(destLat) > 90 || Math.abs(destLng) > 180) {
            Log.e(TAG, "ERROR: Invalid destination coordinates - lat: " + destLat + ", lng: " + destLng);
            return;
        }
        
        LatLng to = new LatLng(destLat, destLng);
        Log.d(TAG, "Drawing route from: " + from.latitude + "," + from.longitude + " to: " + to.latitude + "," + to.longitude);

        String url = buildDirectionsUrl(from, to);
        Log.d(TAG, "Directions API URL: " + url);
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        Log.d(TAG, "Directions API response received");
                        String status = response.optString("status", "UNKNOWN");
                        Log.d(TAG, "API Status: " + status);
                        
                        if (!"OK".equals(status)) {
                            String errorMessage = response.optString("error_message", "No error message");
                            Log.e(TAG, "Directions API error - Status: " + status + ", Message: " + errorMessage);
                            return;
                        }
                        
                        List<LatLng> points = parsePolylinePoints(response);
                        if (points.isEmpty()) {
                            Log.w(TAG, "No route points found in response");
                            return;
                        }
                        
                        Log.d(TAG, "Parsed " + points.size() + " route points");
                        
                        // Remove existing route if any
                        if (routePolyline != null) {
                            routePolyline.remove();
                        }
                        
                        // Draw new route
                        routePolyline = googleMap.addPolyline(new PolylineOptions()
                                .addAll(points)
                                .width(10f)
                                .color(0xFF18B8DB)
                                .geodesic(true));
                        
                        // Zoom to show both locations
                        com.google.android.gms.maps.model.LatLngBounds.Builder builder = new com.google.android.gms.maps.model.LatLngBounds.Builder();
                        builder.include(from);
                        builder.include(to);
                        com.google.android.gms.maps.model.LatLngBounds bounds = builder.build();
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
                        
                        Log.d(TAG, "Route drawn successfully");
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing directions response: " + e.getMessage(), e);
                    }
                },
                error -> {
                    Log.e(TAG, "Directions API request failed: " + (error != null ? error.getMessage() : "Unknown error"));
                    if (error != null && error.networkResponse != null) {
                        Log.e(TAG, "Network response code: " + error.networkResponse.statusCode);
                    }
                });
        requestQueue.add(request);
    }

    private String buildDirectionsUrl(LatLng from, LatLng to) {
        String key = getString(R.string.google_maps_key);
        return "https://maps.googleapis.com/maps/api/directions/json?origin="
                + from.latitude + "," + from.longitude
                + "&destination=" + to.latitude + "," + to.longitude
                + "&mode=driving&key=" + key;
    }

    private List<LatLng> parsePolylinePoints(JSONObject response) {
        List<LatLng> out = new ArrayList<>();
        try {
            JSONArray routes = response.getJSONArray("routes");
            if (routes.length() == 0) return out;
            JSONObject overviewPolyline = routes.getJSONObject(0).getJSONObject("overview_polyline");
            String points = overviewPolyline.getString("points");
            return decodePolyline(points);
        } catch (Exception ignored) {
            return out;
        }
    }

    // Standard polyline decode
    private static List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            poly.add(new LatLng(lat / 1E5, lng / 1E5));
        }
        return poly;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION) {
            enableMyLocationIfPermitted();
        }
    }

    @Override protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override protected void onPause() { super.onPause(); mapView.onPause(); }
    @Override protected void onDestroy() { super.onDestroy(); mapView.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }
}

