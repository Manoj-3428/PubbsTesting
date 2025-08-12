package in.pubbs.pubbsadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import in.pubbs.pubbsadmin.BottomSheet.BottomsheetAddAreaFragment;

public class AddArea extends AppCompatActivity implements View.OnClickListener,
        OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener {
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;
    private Boolean mLocationPermissionsGranted = false;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    public ArrayList<LatLng> markerList = new ArrayList<>();
    private static final String TAG = AddArea.class.getSimpleName();
    ImageButton help;
    EditText searchBar;
    Button proceed;
    int counter = 0, areaCount = 0;
    ImageView back;
    String areaName, areaId, organisationName;
    SharedPreferences sharedPreferences;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference areaDbReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_area);
        getLocationPermission();
        initView();
        getAreaNumber();
    }

    private void initView() {
        sharedPreferences = getSharedPreferences(getResources().getString(R.string.sharedPreferences), Context.MODE_PRIVATE);
        organisationName = sharedPreferences.getString("organisationName", "no_data").replace(" ", "");
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        Objects.requireNonNull(getSupportActionBar()).setTitle("");
        help = findViewById(R.id.help);
        help.setOnClickListener(this);
        searchBar = findViewById(R.id.search_bar);//search bar to find an address in the map
        searchBar.setOnClickListener(this);
        proceed = findViewById(R.id.proceed_btn);
        proceed.setOnClickListener(this);
        back = findViewById(R.id.back_button);
        back.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
//        switch (view.getId()) {
//            case R.id.help:
//                new BottomsheetAddAreaFragment().show(getSupportFragmentManager(), "dialog");
//                break;
//            case R.id.search_bar:
//                Log.d(TAG, "geoLocate: geolocation");
//                String searchString = searchBar.getText().toString();
//                Geocoder geocoder = new Geocoder(AddArea.this);
//                try {
//                    List<Address> addressList = geocoder.getFromLocationName(searchString, 1);
//                    Address address = addressList.get(0);
//                    double search_latitude = address.getLatitude();
//                    double search_longitude = address.getLongitude();
//                    LatLng latlng = new LatLng(search_latitude, search_longitude);
//                    MarkerOptions searchedMarker = new MarkerOptions().position(latlng).title(searchString);
//                    mMap.addMarker(searchedMarker);//.icon(icon));
//                    mMap.animateCamera(CameraUpdateFactory.newLatLng(latlng));
//                    hideSoftKeyboard(getApplicationContext(), view);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                break;
//            case R.id.proceed_btn:
//                Intent intent_add = new Intent(AddArea.this, ManageSystem.class);
//                intent_add.putExtra("markerList", markerList);
//                intent_add.putExtra("areaId", areaId);
//                intent_add.putExtra("areaName", areaName);
//                startActivity(intent_add);
//                break;
//            case R.id.back_button:
//                Intent intent = new Intent(AddArea.this, MainActivity.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                startActivity(intent);
//                break;
//            default:
//                break;
//        }
        if (view.getId() == R.id.help) {
            new BottomsheetAddAreaFragment().show(getSupportFragmentManager(), "dialog");
        }
        else if (view.getId() == R.id.search_bar) {
            Log.d(TAG, "geoLocate: geolocation");
            String searchString = searchBar.getText().toString();
            Geocoder geocoder = new Geocoder(AddArea.this);
            try {
                List<Address> addressList = geocoder.getFromLocationName(searchString, 1);
                Address address = addressList.get(0);
                double search_latitude = address.getLatitude();
                double search_longitude = address.getLongitude();
                LatLng latlng = new LatLng(search_latitude, search_longitude);
                MarkerOptions searchedMarker = new MarkerOptions().position(latlng).title(searchString);
                mMap.addMarker(searchedMarker);
                mMap.animateCamera(CameraUpdateFactory.newLatLng(latlng));
                hideSoftKeyboard(getApplicationContext(), view);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if (view.getId() == R.id.proceed_btn) {
            Intent intent_add = new Intent(AddArea.this, ManageSystem.class);
            intent_add.putExtra("markerList", markerList);
            intent_add.putExtra("areaId", areaId);
            intent_add.putExtra("areaName", areaName);
            startActivity(intent_add);
        }
        else if (view.getId() == R.id.back_button) {
            Intent intent = new Intent(AddArea.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
        else {
            // Do nothing for unknown views
        }


    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(AddArea.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void initMap() {
        Log.d(TAG, "initMap: initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        Objects.requireNonNull(mapFragment).getMapAsync(AddArea.this);
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

    //hide the keyboard on loading the map
    private void hideSoftKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) getApplicationContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
        Objects.requireNonNull(imm).hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    //getting the devices current location
    private void getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: getting the devices current location");
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            if (mLocationPermissionsGranted) {
                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "onComplete: found location!");
                        Location currentLocation = (Location) task.getResult();
                        moveCamera(new LatLng(Objects.requireNonNull(currentLocation).getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM, "My Location");
                    } else {
                        Log.d(TAG, "onComplete: current location is null");
                        Toast.makeText(AddArea.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage());
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

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (mLocationPermissionsGranted) {
            getDeviceLocation();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            //placing a dynamic marker in the map
            mMap.setOnMapClickListener(latLng -> {
                BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.polygon_dot);
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                counter++;
                Toast.makeText(getApplicationContext(), "Point No:" + counter, Toast.LENGTH_SHORT).show();
                markerOptions.icon(icon);
                mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                mMap.addMarker(markerOptions);
                Log.d(TAG, "Area added");
                markerList.add(latLng);
                drawPolygon(markerList);
            });
        }
    }

    public void drawPolygon(ArrayList<LatLng> myLatLng) {
        Log.d(TAG, "Drawing polygon");
        if (myLatLng.size() >= 12) {//6) {
            PolygonOptions polygonOptions = new PolygonOptions();
            polygonOptions.addAll(myLatLng);
            polygonOptions.strokeColor(getResources().getColor(R.color.blue_300));
            polygonOptions.strokeWidth(5);
            polygonOptions.fillColor(Color.argb(10, 15, 15, 15)); // setting the color of the map after creating 12 points in the map
            Polygon polygon = mMap.addPolygon(polygonOptions);
            setAreaName();
            areaId = generateAreaID();
            proceed.setVisibility(View.VISIBLE);
        }
    }

    private String setAreaName() {
        final AlertDialog dialogBuilder = new AlertDialog.Builder(this).create();
        LayoutInflater inflater = this.getLayoutInflater();
        Objects.requireNonNull(dialogBuilder.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        View dialogView = inflater.inflate(R.layout.custom_area_name_dialog, null);
        final EditText area_name = dialogView.findViewById(R.id.area_name);
        Button proceed = dialogView.findViewById(R.id.proceed);
        proceed.setOnClickListener(view -> {
            final Animation animShake = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.shake);
            if (area_name.getText().toString().isEmpty()) {
                area_name.startAnimation(animShake);
            } else {
                areaName = area_name.getText().toString().trim();
                Log.d(TAG, "Area Name : " + areaName);
                dialogBuilder.dismiss();
            }
        });
        dialogBuilder.setView(dialogView);
        dialogBuilder.show();
        dialogBuilder.setCancelable(true);
        return areaName;
    }

    //generate a random number starting from 1 to 999. This random number will concatenate with the word 'area_'.
    //Every time when the user create a new area with its name then this function will create an area_id with this random number function
    public String generateAreaID() {
        String areaId = "Area_";
        String area;
       /* int max = 99999;
        int min = 1;
        int randomNum = (int) (Math.random() * (max - min)) + min;*/
        area = areaId + areaCount;
        Log.d(TAG, "Area Number: " + area);
        return area;
    }

    private void getAreaNumber() {
        firebaseDatabase = FirebaseDatabase.getInstance();
        areaDbReference = firebaseDatabase.getReference().child(organisationName.replaceAll(" ", ""));
        areaDbReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child("Area").exists()) {
                    areaCount = (int) dataSnapshot.child("Area").getChildrenCount() - 1;
                    Log.d(TAG, "Area count: " + areaCount);
                } else {
                    areaCount = (int) dataSnapshot.child("Area").getChildrenCount();
                    Log.d(TAG, "No area is present under this node: " + areaCount);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
