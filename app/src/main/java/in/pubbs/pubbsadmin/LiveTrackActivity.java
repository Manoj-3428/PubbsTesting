package in.pubbs.pubbsadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.Manifest;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LiveTrackActivity extends AppCompatActivity implements OnMapReadyCallback {

    private MapView mapView;
    private GoogleMap googleMap;
    private static final String TAG = "LiveTrackActivity";
    private SharedPreferences sharedPreferences;
    private String organisation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_track);

        // Read organisation from SharedPreferences
        sharedPreferences = getSharedPreferences("pubbs", MODE_PRIVATE);
        organisation = sharedPreferences.getString("organisationName", "").replaceAll(" ", "");

        mapView = findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        findViewById(R.id.iv_back).setOnClickListener(v -> onBackPressed());

        TextView tv_title = findViewById(R.id.tv_title);
        tv_title.setText("Live Track");

        ImageView iv_menu = findViewById(R.id.iv_menu);
        iv_menu.setVisibility(View.GONE);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        LatLng defaultLocation = new LatLng(22.3178509, 87.3106518);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f));

        // Click listener for markers
        googleMap.setOnMarkerClickListener(marker -> {
            showBicycleDetails(marker);
            return true; // prevents default zoom behavior
        });

        startTrackingAllLocations();
    }

    private void startTrackingAllLocations() {
        if (organisation == null || organisation.isEmpty()) {
            Log.e(TAG, "Organisation name not found in SharedPreferences");
            return;
        }

        DatabaseReference allTracksRef = FirebaseDatabase.getInstance()
                .getReference(organisation)
                .child("LiveTrack");

        allTracksRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (googleMap == null) return;

                googleMap.clear(); // Clear previous overlays

                for (DataSnapshot deviceSnapshot : snapshot.getChildren()) {
                    String deviceId = deviceSnapshot.getKey();
                    if (deviceId == null) continue;

                    for (DataSnapshot bookingSnapshot : deviceSnapshot.getChildren()) {
                        String bookingId = bookingSnapshot.getKey();
                        DataSnapshot locationSnapshot = bookingSnapshot.child("location");

                        Double lat = locationSnapshot.child("latitude").getValue(Double.class);
                        Double lng = locationSnapshot.child("longitude").getValue(Double.class);

                        if (lat != null && lng != null) {
                            LatLng position = new LatLng(lat, lng);

                            // Load drawable and create scaled bitmap if possible
                            Drawable drawable = ContextCompat.getDrawable(LiveTrackActivity.this, R.drawable.bicycle_green);
                            Bitmap scaled = null;
                            if (drawable instanceof BitmapDrawable) {
                                Bitmap original = ((BitmapDrawable) drawable).getBitmap();
                                scaled = Bitmap.createScaledBitmap(original, 120, 120, false);
                            }

                            BitmapDescriptor icon;
                            if (scaled != null) {
                                icon = BitmapDescriptorFactory.fromBitmap(scaled);
                            } else {
                                // fallback to resource marker (ensure R.drawable.live_track exists)
                                icon = BitmapDescriptorFactory.fromResource(R.drawable.bicycle_green);
                            }

                            // Add marker (title = deviceId | bookingId)
                            Marker marker = googleMap.addMarker(new MarkerOptions()
                                    .position(position)
                                    .title(deviceId + "|" + bookingId)
                                    .icon(icon));

                            // Move camera to latest
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 16f));
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("LiveTrack", "Firebase read failed", error.toException());
            }
        });
    }

    private void showBicycleDetails(Marker marker) {
        if (marker.getTitle() == null) return;

        String[] parts = marker.getTitle().split("\\|");
        if (parts.length < 2) return;

        String deviceId = parts[0];
        String bookingId = parts[1];

        if (organisation == null || organisation.isEmpty()) {
            Log.e(TAG, "Organisation name not found in SharedPreferences");
            return;
        }

        DatabaseReference mobileRef = FirebaseDatabase.getInstance()
                .getReference(organisation)
                .child("Bicycle")
                .child(deviceId)
                .child("userMobile");

        mobileRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String mobile = snapshot.getValue(String.class);
                final String userMobile = (mobile == null) ? "N/A" : mobile;

                // Inflate custom layout
                View dialogView = getLayoutInflater().inflate(R.layout.dialog_bicycle_details, null);

                TextView tvDetails = dialogView.findViewById(R.id.tvDetails);
                Button btnOk = dialogView.findViewById(R.id.btnOk);
                Button btnCall = dialogView.findViewById(R.id.btnCall);
                Button btnNavigate = dialogView.findViewById(R.id.btnNavigate);

                // Set details text
                String details = "Bicycle ID: " + deviceId +
                        "\nBooking ID: " + bookingId +
                        "\nUser Mobile: " + userMobile;
                tvDetails.setText(details);

                // Build dialog
                AlertDialog dialog = new AlertDialog.Builder(LiveTrackActivity.this)
                        .setView(dialogView)
                        .create();

                dialog.show();

                // Button actions
                btnOk.setOnClickListener(v -> dialog.dismiss());

                btnCall.setOnClickListener(v -> {
                    if (!userMobile.equals("N/A")) {
                        Intent intent = new Intent(Intent.ACTION_CALL);
                        intent.setData(Uri.parse("tel:" + userMobile));
                        if (ContextCompat.checkSelfPermission(LiveTrackActivity.this,
                                Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                            startActivity(intent);
                        } else {
                            ActivityCompat.requestPermissions(LiveTrackActivity.this,
                                    new String[]{Manifest.permission.CALL_PHONE}, 1);
                        }
                    } else {
                        Toast.makeText(LiveTrackActivity.this, "No mobile number available", Toast.LENGTH_SHORT).show();
                    }
                });

                btnNavigate.setOnClickListener(v -> {
                    LatLng pos = marker.getPosition();
                    String uri = "google.navigation:q=" + pos.latitude + "," + pos.longitude;
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                    intent.setPackage("com.google.android.apps.maps");
                    startActivity(intent);
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to fetch userMobile", error.toException());
            }
        });
    }




    // MapView lifecycle
    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        mapView.onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
