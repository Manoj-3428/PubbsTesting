package in.pubbs.pubbsadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class BicycleHoldStatus extends AppCompatActivity implements OnMapReadyCallback {
    private String bicycleId, status, booking_id, excessTime, rideStartTime, actualRideTime;
    private long bookingNumber;
    private String TAG = BicycleHoldStatus.class.getSimpleName();
    private SharedPreferences sharedPreferences;
    private TextView booking_tv, startTime, duration, status_tv, title;
    private MapView mapView;
    private GoogleMap mGoogleMap;
    private Button forceStop;
    private ImageView back;
    private LatLng location;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bicycle_hold_status);
        init(savedInstanceState);
    }

    private void init(Bundle bundle) {
        sharedPreferences = getSharedPreferences("pubbs", MODE_PRIVATE);
        Intent intent = getIntent();
        bicycleId = intent.getStringExtra("bicycleId");
        rideStartTime = intent.getStringExtra("rideStartTime");
        status = intent.getStringExtra("status");
        excessTime = intent.getStringExtra("elapsedTime");
        booking_id = intent.getStringExtra("bookingId");
        actualRideTime = intent.getStringExtra("actualRideTime");
        Log.d(TAG, "Intent data from BicycleOnHold: " + actualRideTime + "\t" + bicycleId + "\t" + rideStartTime + "\t" + status + "\t" + excessTime);

        title = findViewById(R.id.toolbar_title);
        title.setText("Ride Details");
        back = findViewById(R.id.back_button);
        booking_tv = findViewById(R.id.booking_id);
        status_tv = findViewById(R.id.area_id);
        startTime = findViewById(R.id.booking_date);
        duration = findViewById(R.id.ride_duration);
        mapView = findViewById(R.id.map);
        forceStop = findViewById(R.id.force_stop);

        booking_tv.setText("Booking Id: " + booking_id);
        status_tv.setText("Status: " + status);
        startTime.setText("Ride Start Time: " + rideStartTime);
        duration.setText("Exceeded Ride Time By: " + excessTime);
        mapView.onCreate(bundle);
        mapView.getMapAsync(this::onMapReady);
        getLocationFromLiveTrack();

        forceStop.setOnClickListener(v -> {
            //initiateRideEndProcess();
            //getLocationFromLiveTrack();
        });
    }

    private void initiateRideEndProcess() {
        //This function will end a ride

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;

    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }
    private void getLocationFromLiveTrack(){
        String path = Objects.requireNonNull(sharedPreferences.getString("organisationName", "no_data")).replaceAll(" ", "")+"/LiveTrack/"+bicycleId+"/"+booking_id;
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(path);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long child = dataSnapshot.getChildrenCount();
                Log.d(TAG,"latitude: "+ dataSnapshot.child(String.valueOf(child)).child("location").child("latitude").getValue() +" longitude: "+ dataSnapshot.child(String.valueOf(child)).child("location").child("longitude").getValue());
                location = new LatLng(Double.parseDouble(String.valueOf(dataSnapshot.child(String.valueOf(child)).child("location").child("latitude").getValue())),
                        Double.parseDouble(String.valueOf(dataSnapshot.child(String.valueOf(child)).child("location").child("longitude").getValue())));
                mGoogleMap.addMarker(new MarkerOptions()
                        .position(location));
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
