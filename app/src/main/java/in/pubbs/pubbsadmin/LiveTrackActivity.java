package in.pubbs.pubbsadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;
import java.util.ResourceBundle;

public class LiveTrackActivity extends AppCompatActivity implements OnMapReadyCallback {

    private MapView mapView;
    private GoogleMap googleMap;
    private GroundOverlay currentOverlay;
    private DatabaseReference databaseRef;
    private static final String TAG = "LiveTrackActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_track);

        mapView = findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        findViewById(R.id.iv_back).setOnClickListener(v -> onBackPressed());

        TextView tv_title = findViewById(R.id.tv_title);
        tv_title.setText("Live Track");

        ImageView iv_menu = findViewById(R.id.iv_menu);
        iv_menu.setVisibility(View.GONE);
        SharedPreferences sharedPreferences = getSharedPreferences("pubbs", Context.MODE_PRIVATE);

//        String path = Objects.requireNonNull(sharedPreferences.getString("organisationName", "no_data")).replace(" ", "") + "/LiveTrack/";
          //      + sharedPreferences.getString("scanResult", "").replaceAll(":", "") + "/" + sharedPreferences.getString("booking_id", "") + "/location";
        //String path = "PubbsTesting/LiveTrack/C7CEA29F1D1E/C7CEA29F1D1E_0/location";

// Build path dynamically

       // Log.d(TAG, "Firebase path: " + path);

// Get Firebase ref and start listening
       // databaseRef = FirebaseDatabase.getInstance().getReference(path);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        LatLng defaultLocation = new LatLng(22.3178509, 87.3106518);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f));
        startTrackingAllLocations();
    }

private void startTrackingAllLocations() {
    DatabaseReference allTracksRef = FirebaseDatabase.getInstance()
            .getReference("PubbsTesting/LiveTrack");

    allTracksRef.addValueEventListener(new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            if (googleMap == null) return;

            googleMap.clear(); // Clear previous overlays

            for (DataSnapshot deviceSnapshot : snapshot.getChildren()) {
                String bookingIdKey = deviceSnapshot.getKey();
                if (bookingIdKey == null) continue;

                for (DataSnapshot bookingSnapshot : deviceSnapshot.getChildren()) {
                    DataSnapshot locationSnapshot = bookingSnapshot.child("location");

                    Double lat = locationSnapshot.child("latitude").getValue(Double.class);
                    Double lng = locationSnapshot.child("longitude").getValue(Double.class);

                    if (lat != null && lng != null) {
                        LatLng position = new LatLng(lat, lng);

                        Drawable drawable = ContextCompat.getDrawable(LiveTrackActivity.this, R.drawable.live_track);
                        if (drawable instanceof BitmapDrawable) {
                            Bitmap original = ((BitmapDrawable) drawable).getBitmap();
                            Bitmap scaled = Bitmap.createScaledBitmap(original, 120, 120, false);

                            GroundOverlayOptions overlayOptions = new GroundOverlayOptions()
                                    .image(BitmapDescriptorFactory.fromBitmap(scaled))
                                    .position(position, 30f)
                                    .zIndex(10);

                            googleMap.addGroundOverlay(overlayOptions);
                        }

                        // Optionally move camera to the last marker
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