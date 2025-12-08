package in.pubbs.pubbsadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CycleTrack extends AppCompatActivity implements OnMapReadyCallback {

    private MapView mapView;
    private GoogleMap googleMap;
    private static final String TAG = "CycleTrack";
    private SharedPreferences sharedPreferences;
    private String organisation;

    private MediaPlayer mediaPlayer;
    private boolean theftSoundPlayed = false; // prevent repeated sound loop

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cycle_track);

        sharedPreferences = getSharedPreferences("pubbs", MODE_PRIVATE);
        organisation = sharedPreferences.getString("organisationName", "").replaceAll(" ", "");

        // Load theft alert sound
        mediaPlayer = MediaPlayer.create(this, R.raw.theft_alert);

        mapView = findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        findViewById(R.id.iv_back).setOnClickListener(v -> onBackPressed());

        TextView tv_title = findViewById(R.id.tv_title);
        tv_title.setText("Cycle Track");

        ImageView iv_menu = findViewById(R.id.iv_menu);
        iv_menu.setVisibility(View.GONE);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.googleMap = map;

        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(22.3178509, 87.3106518), 14f));

        googleMap.setOnMarkerClickListener(marker -> {
            showBicycleDetails(marker.getTitle(), marker.getPosition());
            return true;
        });

        startTrackingAllBicycles();
    }

    private void startTrackingAllBicycles() {

        DatabaseReference allBicyclesRef = FirebaseDatabase.getInstance()
                .getReference(organisation)
                .child("Bicycle");

        allBicyclesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (googleMap == null) return;

                googleMap.clear();
                boolean theftDetected = false;

                for (DataSnapshot bicycleSnapshot : snapshot.getChildren()) {

                    String bicycleId = bicycleSnapshot.getKey();

                    String latString = String.valueOf(bicycleSnapshot.child("latitude").getValue());
                    String lngString = String.valueOf(bicycleSnapshot.child("longitude").getValue());
                    String theftString = String.valueOf(bicycleSnapshot.child("theft").getValue());
                    String status = String.valueOf(bicycleSnapshot.child("status").getValue());

                    if (latString == null || lngString == null || latString.equals("null") || lngString.equals("null")) {
                        continue;
                    }

                    double lat = Double.parseDouble(latString);
                    double lng = Double.parseDouble(lngString);

                    LatLng pos = new LatLng(lat, lng);

                    boolean isTheft = theftString != null && theftString.equals("1");

                    // -------------------------
                    // 🔥 Marker Color Logic
                    // -------------------------
                    int iconRes;

                    if (isTheft) {
                        iconRes = R.drawable.bicycle_red;      // theft = red
                        theftDetected = true;
                    } else if ("Active".equalsIgnoreCase(status)) {
                        iconRes = R.drawable.bicycle_green;    // active = green
                    } else {
                        iconRes = R.drawable.bicycle_blue;     // others = blue
                    }

                    googleMap.addMarker(new MarkerOptions()
                            .position(pos)
                            .title(bicycleId)
                            .icon(getBitmapIcon(iconRes))
                    );
                }

                if (theftDetected && !theftSoundPlayed) {
                    playTheftSound();
                } else if (!theftDetected) {
                    theftSoundPlayed = false;
                }
            }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase failed", error.toException());
            }
        });
    }

    private void playTheftSound() {
        try {
            theftSoundPlayed = true;
            mediaPlayer.start();
        } catch (Exception e) {
            Log.e(TAG, "Sound play error", e);
        }
    }

    private BitmapDescriptor getBitmapIcon(int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(this, drawableId);
        if (drawable instanceof BitmapDrawable) {
            Bitmap original = ((BitmapDrawable) drawable).getBitmap();
            Bitmap scaled = Bitmap.createScaledBitmap(original, 100, 100, false);
            return BitmapDescriptorFactory.fromBitmap(scaled);
        }
        return BitmapDescriptorFactory.defaultMarker();
    }

    private void showBicycleDetails(String bicycleId, LatLng position) {

        DatabaseReference bicycleRef = FirebaseDatabase.getInstance()
                .getReference(organisation)
                .child("Bicycle")
                .child(bicycleId);

        bicycleRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (!snapshot.exists()) {
                    Toast.makeText(CycleTrack.this, "Bicycle data not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                String station = String.valueOf(snapshot.child("inStationName").getValue());
                String battery = String.valueOf(snapshot.child("battery").getValue());
                String mobile = String.valueOf(snapshot.child("userMobile").getValue());
                String theft = String.valueOf(snapshot.child("theft").getValue());
                String status = String.valueOf(snapshot.child("status").getValue());
                if (status.equals("null")) status = "N/A";

                if (station.equals("null")) station = "N/A";
                if (battery.equals("null")) battery = "N/A";
                if (mobile.equals("null")) mobile = "N/A";
                if (theft.equals("null")) theft = "0";

                View dialogView = getLayoutInflater().inflate(R.layout.dialog_bicycle_details, null);

                TextView tvDetails = dialogView.findViewById(R.id.tvDetails);
                TextView tvTheft = dialogView.findViewById(R.id.tvTheft);
                Button btnOk = dialogView.findViewById(R.id.btnOk);
                Button btnCall = dialogView.findViewById(R.id.btnCall);
                Button btnNavigate = dialogView.findViewById(R.id.btnNavigate);

                tvDetails.setText(
                        "🚲 Bicycle ID: " + bicycleId +
                                "\n🏠 Station: " + station +
                                "\n🔋 Battery: " + battery +
                                "\n📱 User Mobile: " + mobile +
                                "\n🚦 Status: " + status
                );

                tvTheft.setText(
                        theft.equals("1") ? "⚠ Theft Alert: ACTIVE" : "✔ No Theft"
                );

                AlertDialog dialog = new AlertDialog.Builder(CycleTrack.this)
                        .setView(dialogView)
                        .create();

                dialog.show();

                btnOk.setOnClickListener(v -> dialog.dismiss());

                String finalMobile = mobile;
                btnCall.setOnClickListener(v -> {
                    if (!finalMobile.equals("N/A")) {
                        Intent intent = new Intent(Intent.ACTION_CALL);
                        intent.setData(Uri.parse("tel:" + finalMobile));

                        if (ContextCompat.checkSelfPermission(CycleTrack.this,
                                Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                            startActivity(intent);
                        } else {
                            ActivityCompat.requestPermissions(CycleTrack.this,
                                    new String[]{Manifest.permission.CALL_PHONE}, 1);
                        }
                    } else {
                        Toast.makeText(CycleTrack.this, "No mobile number", Toast.LENGTH_SHORT).show();
                    }
                });

                btnNavigate.setOnClickListener(v -> {
                    String uri = "google.navigation:q=" + position.latitude + "," + position.longitude;
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                    intent.setPackage("com.google.android.apps.maps");
                    startActivity(intent);
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to fetch", error.toException());
            }
        });
    }

    @Override protected void onStart() { super.onStart(); mapView.onStart(); }
    @Override protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override protected void onPause() { mapView.onPause(); super.onPause(); }
    @Override protected void onStop() { mapView.onStop(); super.onStop(); }
    @Override protected void onDestroy() {
        if (mediaPlayer != null) mediaPlayer.release();
        mapView.onDestroy();
        super.onDestroy();
    }
    @Override public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }
}
