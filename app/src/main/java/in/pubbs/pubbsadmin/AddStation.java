package in.pubbs.pubbsadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import in.pubbs.pubbsadmin.BottomSheet.BottomSheetStationFragment;
import in.pubbs.pubbsadmin.Model.Station;
import in.pubbs.pubbsadmin.View.CustomAlertDialog;

/*created by Parita Dey*/
public class AddStation extends AppCompatActivity implements View.OnClickListener, OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener {
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;
    private Boolean mLocationPermissionsGranted = false;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    ArrayList<LatLng> polygon = new ArrayList<>();
    private String TAG = AddStation.class.getSimpleName();
    ImageButton help;
    Button proceed_btn;
    ImageView back;
    LatLng coordinate;
    String stationName, organisationName, stationId, stationLatitude, stationLongitude, stationRadius;
    double station_latitude, station_longitude;
    ArrayList<LatLng> markerList = new ArrayList<>();
    ArrayList<LatLng> manageArea;
    ArrayList<LatLng> drawArea = new ArrayList<>();
    Gson gson;
    private ArrayList stationList = new ArrayList();
    SharedPreferences sharedPreferences;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference areaDbReference;
    int stationCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_station);
        getLocationPermission();
        initView();
        getStationNumber();
    }

    private void initView() {
        Intent intent = getIntent();
        manageArea = intent.getParcelableArrayListExtra("markerList");
        polygon = intent.getParcelableArrayListExtra("marker_list");
        Log.d(TAG, "area from ManageSystem:" + manageArea);
        Log.d(TAG, "area from area details:" + polygon);
        if (!(polygon == null) && manageArea == null) {
            gson = new Gson();
            //converting string to LatLng
            markerList = gson.fromJson(String.valueOf(polygon), new TypeToken<List<LatLng>>() {
            }.getType());
            Log.d(TAG, "Lat/Long:" + markerList);
            coordinate = markerList.get(0);
            Log.d(TAG, "First coordinate:" + coordinate);
            drawArea = markerList;
        } else if (!(manageArea == null) && polygon == null) {
            coordinate = manageArea.get(0);
            Log.d(TAG, "First coordinate:" + coordinate);
            drawArea = manageArea;
        }
        Log.d(TAG, "Draw area: " + drawArea);
        sharedPreferences = getSharedPreferences(getResources().getString(R.string.sharedPreferences), Context.MODE_PRIVATE);
        organisationName = sharedPreferences.getString("organisationName", "no_data").replace(" ", "");
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        Objects.requireNonNull(getSupportActionBar()).setTitle("");
        help = findViewById(R.id.help);
        help.setOnClickListener(this);
        proceed_btn = findViewById(R.id.proceed_btn);
        proceed_btn.setOnClickListener(this);
        back = findViewById(R.id.back_button);
        back.setOnClickListener(this);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(AddStation.this, AreaDetails.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        drawAreaPolygon(drawArea);
    }

    private void drawAreaPolygon(List<LatLng> coordinates) {
        Log.d(TAG, "Drawing polygon");
        if (coordinates.size() >= 6) {
            PolygonOptions polygonOptions = new PolygonOptions();
            polygonOptions.addAll(coordinates);
            polygonOptions.strokeColor(getResources().getColor(R.color.blue_300));
            polygonOptions.strokeWidth(5);
            polygonOptions.fillColor(Color.argb(15, 20, 20, 255));
            // polygonOptions.fillColor(getResources().getColor(R.color.blue_100));
            Polygon polygon = mMap.addPolygon(polygonOptions);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinate, 15f));
        }
        drawStation(coordinates);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.help) {
            new BottomSheetStationFragment().show(getSupportFragmentManager(), "dialog");
        }
        else if (v.getId() == R.id.back_button) {
            Intent intent = new Intent(AddStation.this, AreaDetails.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
        else if (v.getId() == R.id.proceed_btn) {
            Intent station_data = new Intent(AddStation.this, ManageSystem.class);
            station_data.putExtra("station_id", stationId);
            station_data.putExtra("station_name", stationName);
            station_data.putExtra("station_latitude", stationLatitude);
            station_data.putExtra("station_longitude", stationLongitude);
            station_data.putExtra("station_radius", stationRadius);
            station_data.putExtra("station_details", stationList);
            setResult(3, station_data);
            finish();
        }
        else {
            // Optional: Handle unknown view clicks if needed
        }

//        switch (v.getId()) {
//            case R.id.help:
//                new BottomSheetStationFragment().show(getSupportFragmentManager(), "dialog");
//                break;
//            case R.id.back_button:
//                Intent intent = new Intent(AddStation.this, AreaDetails.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                startActivity(intent);
//                break;
//            case R.id.proceed_btn:
//                Intent station_data = new Intent(AddStation.this, ManageSystem.class);
//                station_data.putExtra("station_id", stationId);
//                station_data.putExtra("station_name", stationName);
//                station_data.putExtra("station_latitude", stationLatitude);
//                station_data.putExtra("station_longitude", stationLongitude);
//                station_data.putExtra("station_radius", stationRadius);
//                station_data.putExtra("station_details", stationList);
//                setResult(3, station_data);
//                finish();
//                break;
//            default:
//                break;
//        }
    }

    private void initMap() {
        Log.d(TAG, "initMap: initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        Objects.requireNonNull(mapFragment).getMapAsync(AddStation.this);
    }

    //getting the location permission from the user by considering all the conditions
    private void getLocationPermission() {
        Log.d(TAG, "getLocationPermission: getting location permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(), COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionsGranted = true;
                initMap();
            } else {
                ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    //if the user permits to access the location of the device then it
    // will move forward and do rest part of the code, otherwise show permission failed msg in the Log
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult: called.");
        mLocationPermissionsGranted = false;

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0) {
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        mLocationPermissionsGranted = false;
                        Log.d(TAG, "onRequestPermissionsResult: permission failed");
                        return;
                    }
                }
                Log.d(TAG, "onRequestPermissionsResult: permission granted");
                mLocationPermissionsGranted = true;
                //initialize our map
                initMap();
            }
        }
    }

    private void moveCamera(LatLng latLng, float zoom, String title) {
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        if (!title.equals("My Location")) {
            MarkerOptions options = new MarkerOptions().position(latLng).title(title);
            mMap.addMarker(options);
        }
    }
    //change by dipankar
    private String selectStationDialog(LatLng latLng) {
        final AlertDialog dialogBuilder = new AlertDialog.Builder(this).create();
        LayoutInflater inflater = this.getLayoutInflater();
        Objects.requireNonNull(dialogBuilder.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        View dialogView = inflater.inflate(R.layout.custom_area_name_dialog, null);

        final EditText area_name = dialogView.findViewById(R.id.area_name);
        final TextView station_radius_header = dialogView.findViewById(R.id.station_radius_header);
        station_radius_header.setVisibility(View.VISIBLE);

        final EditText radius = dialogView.findViewById(R.id.station_radius);
        radius.setVisibility(View.VISIBLE);

        RadioGroup rg = dialogView.findViewById(R.id.radio_group_station);
        rg.setVisibility(View.VISIBLE);

        Button proceed = dialogView.findViewById(R.id.proceed);
        proceed.setOnClickListener(view -> {
            final Animation animShake = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.shake);

            if (area_name.getText().toString().isEmpty()) {
                area_name.startAnimation(animShake);
                dialogBuilder.dismiss();
            }
            else {
                stationName = area_name.getText().toString().trim();
                Log.d(TAG, "StationList Name : " + stationName);
                stationId = generateStationID(); // generate random number station id

                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.title(latLng.latitude + " : " + latLng.longitude);
                markerOptions.snippet(stationId);

                mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                mMap.addMarker(markerOptions);

                Log.d(TAG, "StationList added");
                station_latitude = latLng.latitude;
                stationLatitude = String.valueOf(station_latitude);
                station_longitude = latLng.longitude;
                stationLongitude = String.valueOf(station_longitude);
                stationRadius = radius.getText().toString();

                Log.d(TAG, "StationList details:" + stationName + "\t" + stationId + "\t" + stationLatitude + "\t" + stationLongitude + "\t" + stationRadius);

                Station obj = new Station();
                obj.setStationId(stationId);
                obj.setStationName(stationName);
                obj.setStationLatitude(stationLatitude);
                obj.setStationLongitude(stationLongitude);
                obj.setStationRadius(stationRadius);

                // Replacing switch-case with if-else
                int checkedId = rg.getCheckedRadioButtonId();
                if (checkedId == R.id.primary_station) {
                    obj.setStationType("primary");
                }
                else if (checkedId == R.id.secondary_station) {
                    obj.setStationType("secondary");
                }
                else if (checkedId == R.id.na_station) {
                    obj.setStationType("na");
                }

                stationList.add(obj);
                dialogBuilder.dismiss();
                proceed_btn.setVisibility(View.VISIBLE);
            }
        });

        dialogBuilder.setView(dialogView);
        dialogBuilder.show();
        dialogBuilder.setCancelable(true);

        return stationName;
    }


//    private String selectStationDialog(LatLng latLng) {
//        final AlertDialog dialogBuilder = new AlertDialog.Builder(this).create();
//        LayoutInflater inflater = this.getLayoutInflater();
//        Objects.requireNonNull(dialogBuilder.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//        View dialogView = inflater.inflate(R.layout.custom_area_name_dialog, null);
//        final EditText area_name = dialogView.findViewById(R.id.area_name);
//        final TextView station_radius_header = dialogView.findViewById(R.id.station_radius_header);
//        station_radius_header.setVisibility(View.VISIBLE);
//        final EditText radius = dialogView.findViewById(R.id.station_radius);
//        radius.setVisibility(View.VISIBLE);
//        RadioGroup rg = dialogView.findViewById(R.id.radio_group_station);
//        rg.setVisibility(View.VISIBLE);
//        Button proceed = dialogView.findViewById(R.id.proceed);
//        proceed.setOnClickListener(view -> {
//            final Animation animShake = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.shake);
//            if (area_name.getText().toString().isEmpty()) {
//                area_name.startAnimation(animShake);
//                dialogBuilder.dismiss();
//            } else {
//                stationName = area_name.getText().toString().trim();
//                Log.d(TAG, "StationList Name : " + stationName);
//                stationId = generateStationID(); //generate random number station id
//                // BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.pin_station);
//                MarkerOptions markerOptions = new MarkerOptions();
//                markerOptions.position(latLng);
//                markerOptions.title(latLng.latitude + " : " + latLng.longitude);
//                //     markerOptions.icon(icon);
//                markerOptions.snippet(stationId);
//                mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
//                mMap.addMarker(markerOptions);
//                Log.d(TAG, "StationList added");
//                station_latitude = latLng.latitude;
//                stationLatitude = String.valueOf(station_latitude);
//                station_longitude = latLng.longitude;
//                stationLongitude = String.valueOf(station_longitude);
//                stationRadius = radius.getText().toString();
//                Log.d(TAG, "StationList details:" + stationName + "\t" + stationId + "\t" + stationLatitude + "\t" + stationLongitude + "\t" + stationRadius);
//                Station obj = new Station();
//                obj.setStationId(stationId);
//                obj.setStationName(stationName);
//                obj.setStationLatitude(stationLatitude);
//                obj.setStationLongitude(stationLongitude);
//                obj.setStationRadius(stationRadius);
//                switch (rg.getCheckedRadioButtonId()) {
//                    case R.id.primary_station:
//                        obj.setStationType("primary");
//                        break;
//                    case R.id.secondary_station:
//                        obj.setStationType("secondary");
//                        break;
//                    case R.id.na_station:
//                        obj.setStationType("na");
//                        break;
//                }
//                stationList.add(obj);
//                dialogBuilder.dismiss();
//                proceed_btn.setVisibility(View.VISIBLE);
//            }
//        });
//        dialogBuilder.setView(dialogView);
//        dialogBuilder.show();
//        dialogBuilder.setCancelable(true);
//        return stationName;
//    }

    public String generateStationID() {
        String stationNumber = "Station_";
        String station;
      /*  int max = 999;
        int min = 1;
        int randomNum = (int) (Math.random() * (max - min)) + min;*/
        station = stationNumber + stationCount;
        Log.d(TAG, "Area Number: " + station);
        return station;

    }

    public void drawStation(List<LatLng> coordinates) {
        mMap.setOnMapClickListener(latLng -> {
            boolean point = pointInPolygon_v1(latLng, coordinates);
            Log.d(TAG, "Point value:" + point);
            if (point) {
                selectStationDialog(latLng);
            } else {
                alertDialog("Add StationList", "Please add station inside the area.");
            }
        });
    }

    // Lascha Lagidse  method for finding whether the touch point latitude and longitude lies within the polygon
    // Reference code: http://alienryderflex.com/polygon/
    private boolean pointInPolygon_v1(LatLng tap, List<LatLng> vertices) {
        int i, j = vertices.size() - 1;
        boolean oddNodes = false;
        for (i = 0; i < vertices.size(); i++) {
            if ((vertices.get(i).longitude < tap.longitude && vertices.get(j).longitude >= tap.longitude) ||
                    (vertices.get(j).longitude < tap.longitude && vertices.get(i).longitude >= tap.longitude) &&
                            (vertices.get(i).latitude <= tap.latitude || vertices.get(j).latitude <= tap.latitude)) {
                oddNodes ^= (vertices.get(i).latitude + (tap.longitude - vertices.get(i).longitude) /
                        (vertices.get(j).longitude - vertices.get(i).longitude) * (vertices.get(j).latitude - vertices.get(i).latitude) < tap.latitude);
            }
            j = i;
        }
        return oddNodes;
    }

    private void alertDialog(String title, String message) {
        final CustomAlertDialog dialog = new CustomAlertDialog(this,
                R.style.WideDialog, title, message);
        dialog.show();
        dialog.onPositiveButton(view -> dialog.dismiss());
    }

    private void getStationNumber() {
        firebaseDatabase = FirebaseDatabase.getInstance();
        areaDbReference = firebaseDatabase.getReference().child(organisationName.replaceAll(" ", ""));
        areaDbReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child("Station").exists()) {
                    stationCount = (int) dataSnapshot.child("Station").getChildrenCount();
                    Log.d(TAG, "Station count: " + stationCount);
                } else {
                    stationCount = (int) dataSnapshot.child("Area").getChildrenCount();
                    Log.d(TAG, "No station is present under this node: " + stationCount);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
