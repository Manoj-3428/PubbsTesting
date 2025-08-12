package in.pubbs.pubbsadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;

public class StationDetails extends AppCompatActivity implements View.OnClickListener, OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener {

    public String stationName, stationId, stationLatitude, stationLongitude, areaName, stationRadius, stationType, stationStatus, updatedStationName;
    private String TAG = StationDetails.class.getSimpleName();
    TextView toolbarText, station_name, station_type, area_name;
    ImageView back;
    private GoogleMap mMap;
    Button done;
    SharedPreferences sharedPreferences;
    DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station_details);
        initView();
        initMap();
    }

    private void initView() {
        sharedPreferences = getSharedPreferences(getResources().getString(R.string.sharedPreferences), Context.MODE_PRIVATE);
        Intent intent = getIntent();//organisationName
        stationId = intent.getStringExtra("stationId");
        stationName = intent.getStringExtra("stationName");
        stationLatitude = intent.getStringExtra("stationLatitude");
        stationLongitude = intent.getStringExtra("stationLongitude");
        areaName = intent.getStringExtra("areaName");
        stationRadius = intent.getStringExtra("stationRadius");
        stationType = intent.getStringExtra("stationType");
        Log.d(TAG, "Station details:" + stationId + "\t" + stationName + "\t" + stationLatitude + "\t" + stationLongitude + areaName + "\t" + stationRadius + "\t" + stationType);
        toolbarText = findViewById(R.id.toolbar_title);
        toolbarText.setText(stationName);
        back = findViewById(R.id.back_button);
        back.setOnClickListener(this);
        station_name = findViewById(R.id.station_name);
        station_name.setText(stationName);
        station_type = findViewById(R.id.station_type);
        station_type.setText("Station Type: " + stationType);
        area_name = findViewById(R.id.area_name);
        area_name.setText("Area Name: " + areaName);
        done = findViewById(R.id.done_button);
        done.setOnClickListener(this);
        station_type.setOnClickListener(this);
        String path = sharedPreferences.getString("organisationName", "no_data").replace(" ", "") + "/Station/" + stationId + "/stationType";
        Log.d(TAG, "Database path: " + path);
        databaseReference = FirebaseDatabase.getInstance().getReference(path);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private void initMap() {
        Log.d(TAG, "initMap: initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        Objects.requireNonNull(mapFragment).getMapAsync(StationDetails.this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng station = new LatLng(Double.parseDouble(stationLatitude), Double.parseDouble(stationLongitude));
        mMap.addMarker(new MarkerOptions().position(station).title(stationName).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(station, 20f));
        Circle circle = mMap.addCircle(new CircleOptions()
                .center(new LatLng(Double.parseDouble(stationLatitude), Double.parseDouble(stationLongitude)))
                .radius(Integer.valueOf(stationRadius))
                .strokeColor(Color.argb(10, 15, 15, 15))
                .fillColor(Color.argb(10, 15, 15, 15)));
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(StationDetails.this, AreaDetails.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.back_button) {
            Intent intent = new Intent(StationDetails.this, AreaDetails.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else if (v.getId() == R.id.done_button) {
            Intent done = new Intent(StationDetails.this, AreaDetails.class);
            done.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(done);
        } else if (v.getId() == R.id.station_type) {
            Log.d(TAG, "Station Type Clicked");
            showDialog("Assign Station Type", "Please select any one of the option and press proceed");
        }

//        switch (v.getId()) {
//            case R.id.back_button:
//                Intent intent = new Intent(StationDetails.this, AreaDetails.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                startActivity(intent);
//                break;
//            case R.id.done_button:
//                Intent done = new Intent(StationDetails.this, AreaDetails.class);
//                done.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                startActivity(done);
//                break;
//            case R.id.station_type:
//                Log.d(TAG, "Station Type Clicked");
//                showDialog("Assign Station Type", "Please select any one of the option and press proceed");
//                break;
//            default:
//                break;
//        }
    }
    //This message dialog is fired when the user clicks on the station type, the user is given an option to select the type of station primary or secondary the user has to select any one ant thus it will reflect to the db.
    private void showDialog(String heading, String details) {
        TextView title, message;
        RadioGroup radioGroup;
        RadioButton primary, secondary;
        final Dialog dialog = new Dialog(StationDetails.this, R.style.WideDialog);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setContentView(R.layout.radio_button_dialog_loayout);
        dialog.setCancelable(true);
        title = dialog.findViewById(R.id.title);
        title.setText(heading);
        message = dialog.findViewById(R.id.message);
        message.setText(details);
        radioGroup = dialog.findViewById(R.id.radio_group);
        primary = dialog.findViewById(R.id.am);
        primary.setText("Primary");
        secondary = dialog.findViewById(R.id.sm);
        secondary.setText("Secondary");
        dialog.show();
        dialog.findViewById(R.id.proceed).setOnClickListener(v -> {
            int checkedId = radioGroup.getCheckedRadioButtonId();

            if (checkedId == R.id.am) {
                databaseReference.setValue("primary");
                stationType = "primary";
                station_type.setText("Station Type: " + stationType);
                dialog.dismiss();
            } else if (checkedId == R.id.sm) {
                databaseReference.setValue("secondary");
                stationType = "secondary";
                station_type.setText("Station Type: " + stationType);
                dialog.dismiss();
            } else {
                Toast.makeText(StationDetails.this, "Please choose any one of the above option.", Toast.LENGTH_SHORT).show();
            }

//            switch (radioGroup.getCheckedRadioButtonId()) {
//                case R.id.am:
//                    databaseReference.setValue("primary");
//                    stationType = "primary";
//                    station_type.setText("Station Type: " + stationType);
//                    dialog.dismiss();
//                    break;
//                case R.id.sm:
//                    databaseReference.setValue("secondary");
//                    stationType = "secondary";
//                    station_type.setText("Station Type: " + stationType);
//                    dialog.dismiss();
//                    break;
//                default:
//                    Toast.makeText(StationDetails.this, "Please choose any one of the above option.", Toast.LENGTH_SHORT).show();
//            }
        });
    }
}
