package in.pubbs.pubbsadmin;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
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

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

import in.pubbs.pubbsadmin.Model.Station;
import in.pubbs.pubbsadmin.View.CustomAlertDialog;

/*Created by: Parita Dey*/
public class MapFragment extends Fragment implements OnMapReadyCallback {

    GoogleMap mGoogleMap;
    MapView mapView;
    String TAG = MapFragment.class.getSimpleName(), operatorName;
    FusedLocationProviderClient mFusedLocationProviderClient;
    Boolean mLocationPermissionsGranted = false;
    float DEFAULT_ZOOM = 15f;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    String path, area_id;
    private ArrayList<Station> stationList = new ArrayList<>();
    ArrayList<LatLng> areaMarkerList = new ArrayList<>();
    ArrayList<LatLng> stationMarkerList = new ArrayList<>();
    ImageView help;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        mLocationPermissionsGranted = requireArguments().getBoolean("LocationPermissionsGranted");
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        init(view, savedInstanceState);
        sharedPreferences = requireActivity().getSharedPreferences("pubbs", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        area_id = sharedPreferences.getString("zone_area_id", "no_data");
        Log.d(TAG, "Area id for zone: " + area_id);
        if (sharedPreferences.getString("admin_id", "").equals("PUBBS")) {
            operatorName = sharedPreferences.getString("sa_operator_name", "no_data").replaceAll(" ", ""); //sa_operator_name= Super Admin operator name. First time when the super admin opens the app and choose operator then it will be saved in the sharedPreferences
            if (!operatorName.equals("no_data"))
                loadAllAreaOfOperator(operatorName);
        }
        return view;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        try {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
        }catch (Exception e){

        }

//        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return;
//        }
        mGoogleMap.setMyLocationEnabled(true);

        mGoogleMap.getUiSettings().setMyLocationButtonEnabled(false);
        if (area_id == null && (sharedPreferences.getString("admin_id", "admin_id").equalsIgnoreCase("Regional Manager")
                || sharedPreferences.getString("admin_id", "admin_id").equalsIgnoreCase("Zone Manager"))) { // checking for Regional Manager and Zone Manager
            getDeviceLocation();
            alertDialog("Area choice", "Please choose an area to see in map");
        } else if (!sharedPreferences.getString("areaId", "area").equals("area")) { // checking for Area Manager and Service Manager
            if ((sharedPreferences.getString("admin_id", "admin_id").equalsIgnoreCase("Regional Manager")
                    || sharedPreferences.getString("admin_id", "admin_id").equalsIgnoreCase("Zone Manager") || sharedPreferences.getString("admin_id", "admin_id").equalsIgnoreCase("PUBBS"))) {
                help.setVisibility(View.VISIBLE);
            } else {
                help.setVisibility(View.GONE);
            }
            try {
                getDeviceLocation();
            } catch (Exception E){

            }

            loadAreaBound();

        } else {
            if ((sharedPreferences.getString("admin_id", "admin_id").equalsIgnoreCase("Regional Manager")
                    || sharedPreferences.getString("admin_id", "admin_id").equalsIgnoreCase("Zone Manager") || sharedPreferences.getString("admin_id", "admin_id").equalsIgnoreCase("PUBBS"))) {
                help.setVisibility(View.VISIBLE);
            } else {
                help.setVisibility(View.GONE);
            }
            getDeviceLocation();
            loadAreaBound();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    //getting the devices current location
    private void getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: getting the devices current location");
        try {
            mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this.requireActivity());
        } catch (Exception excp){
            Log.d(TAG,"MapFragment GetDeviceLocation() catch part ");
        }

        try {
            if (mLocationPermissionsGranted) {
                try {
                    final Task location = mFusedLocationProviderClient.getLastLocation();
                    location.addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: found location!");
                            Location currentLocation = (Location) task.getResult();
                            moveCamera(new LatLng(Objects.requireNonNull(currentLocation).getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM, "My Location");
                        } else {
                            Log.d(TAG, "onComplete: current location is null");
                        }
                    });
                }
                catch (Exception eg){

                }

            }
        } catch (SecurityException e) {
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage());
        }
    }

    private void moveCamera(LatLng latLng, float zoom, String title) {
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude);
        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        if (!title.equals("My Location")) {
            MarkerOptions options = new MarkerOptions().position(latLng).title(title);
            mGoogleMap.addMarker(options);
        }
    }

    void init(View view, Bundle savedInstanceState) {
        mapView = view.findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        help = view.findViewById(R.id.help);
        help.setOnClickListener(v -> {
            alertHelpDialog("Pubbs-Admin Help", "Please choose 'Show Area' option from the menu in the tool bar");
            // Toast.makeText(getContext(), "Help is tapped", Toast.LENGTH_LONG).show();
        });
    }

    private void alertHelpDialog(String title, String message) {
        final CustomAlertDialog dialog = new CustomAlertDialog(requireActivity(),
                R.style.WideDialog, title, message);
        dialog.show();
        dialog.onPositiveButton(view -> dialog.dismiss());
    }

    private void loadAreaBound() {
        if (area_id != null && !area_id.equals("no_data")) {
            Log.d(TAG, "path: " + sharedPreferences.getString("organisationName", "no_data").replace(" ", "") + "/Area/" + sharedPreferences.getString("zone_area_id", "no_data") + "/markerList");
            path = sharedPreferences.getString("organisationName", "no_data").replace(" ", "") + "/Area/" + sharedPreferences.getString("zone_area_id", "no_data") + "/markerList";
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(path);
            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot i : dataSnapshot.getChildren()) {
                        Map<String, Object> objectMap = (Map<String, Object>) i.getValue();
                        Log.d(TAG, "latitude: " + Objects.requireNonNull(objectMap).get("latitude") + " longitude: " + objectMap.get("longitude"));
                        areaMarkerList.add(new LatLng(Double.valueOf(Objects.requireNonNull(objectMap.get("latitude")).toString()), Double.valueOf(Objects.requireNonNull(objectMap.get("longitude")).toString())));
                    }
                    if (areaMarkerList.size() > 0) {
                        Polygon areaPolygon = mGoogleMap.addPolygon(new PolygonOptions()
                                .addAll(areaMarkerList));
                        areaPolygon.setFillColor(Color.argb(20, 0, 255, 0));
                        areaPolygon.setStrokeColor(Color.argb(0, 0, 0, 0));
                        loadStations(sharedPreferences.getString("zone_area_id", null));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.d(TAG, "Error: " + databaseError);
                }
            });
        } else {
            Log.d(TAG, "path: " + sharedPreferences.getString("organisationName", "no_data").replace(" ", "") + "/Area/" + sharedPreferences.getString("areaId", "no_data") + "/markerList");
            path = sharedPreferences.getString("organisationName", "no_data").replace(" ", "") + "/Area/" + sharedPreferences.getString("areaId", "no_data") + "/markerList";
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(path);
            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot i : dataSnapshot.getChildren()) {
                        Map<String, Object> objectMap = (Map<String, Object>) i.getValue();
                        Log.d(TAG, "latitude: " + Objects.requireNonNull(objectMap).get("latitude") + " longitude: " + objectMap.get("longitude"));
                        areaMarkerList.add(new LatLng(Double.parseDouble(Objects.requireNonNull(objectMap.get("latitude")).toString()), Double.parseDouble(Objects.requireNonNull(objectMap.get("longitude")).toString())));
                    }
                    if (areaMarkerList.size() > 0) {
                        Polygon areaPolygon = mGoogleMap.addPolygon(new PolygonOptions()
                                .addAll(areaMarkerList));
                        areaPolygon.setFillColor(Color.argb(20, 0, 255, 0));
                        areaPolygon.setStrokeColor(Color.argb(0, 0, 0, 0));
                        loadStations(sharedPreferences.getString("areaId", null));
                    } else {
                        editor.putString("areaId", "area_not_created");
                        editor.commit();
                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.d(TAG, "Error: " + databaseError);
                }
            });
        }

    }

    //Custom Alert Dialog
    private void alertDialog(String title, String message) {
        if (!sharedPreferences.getString("area_id", "area_not_created").equalsIgnoreCase("area_not_created")) {
            final CustomAlertDialog dialog = new CustomAlertDialog(requireActivity(),
                    R.style.WideDialog, title, message);
            dialog.show();
            dialog.onPositiveButton(view -> {
                dialog.dismiss();
                startActivity(new Intent(getActivity(), ShowMyArea.class));
            });
        }
    }

    private void loadStations(String areaId) {
        stationList.clear();
        stationMarkerList.clear();
        Log.d(TAG, "Area Id: " + areaId);
        Log.d(TAG, "path: " + sharedPreferences.getString("organisationName", "no_data").replace(" ", "") + "/Station");
        path = sharedPreferences.getString("organisationName", "no_data").replace(" ", "") + "/Station";
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(path);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                for (DataSnapshot i : dataSnapshot.getChildren()) {
//                    Map<String, Object> objectMap = (Map<String, Object>) i.getValue();
//                    Log.d(TAG, "latitude: " + Objects.requireNonNull(objectMap).get("stationLatitude") + " longitude: " + objectMap.get("stationLongitude"));
//                    if (Objects.requireNonNull(objectMap.get("areaId")).equals(areaId) && Objects.requireNonNull(objectMap.get("stationStatus")).equals(true)) {
//                        stationMarkerList.add(new LatLng(Double.parseDouble(Objects.requireNonNull(objectMap.get("stationLatitude")).toString()), Double.parseDouble(Objects.requireNonNull(objectMap.get("stationLongitude")).toString())));
//                        Station obj = new Station(Objects.requireNonNull(objectMap.get("stationName")).toString(), Objects.requireNonNull(objectMap.get("stationId")).toString(), Objects.requireNonNull(objectMap.get("stationLatitude")).toString(),
//                                Objects.requireNonNull(objectMap.get("stationLongitude")).toString(), Objects.requireNonNull(objectMap.get("stationRadius")).toString());
//                        stationList.add(obj);
//                        /*mGoogleMap.addMarker(new MarkerOptions()
//                                .title(objectMap.get("stationName").toString())
//                                .position(new LatLng(Double.valueOf(objectMap.get("stationLatitude").toString())
//                                        , Double.valueOf(objectMap.get("stationLongitude").toString())))
//                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.station_pin)));
//                        mGoogleMap.addCircle(new CircleOptions()
//                                .center(new LatLng(Double.valueOf(objectMap.get("stationLatitude").toString())
//                                        , Double.valueOf(objectMap.get("stationLongitude").toString())))
//                                .radius(Double.valueOf(objectMap.get("stationRadius").toString()))
//                                .fillColor(Color.argb(20, 102, 178, 255))
//                                .strokeColor(Color.argb(0, 0, 0, 255)));*/
//                        loadAllCycles(objectMap);
//                    }
//                }
//            }

            // modify by dipankar
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot i : dataSnapshot.getChildren()) {
                    Map<String, Object> objectMap = (Map<String, Object>) i.getValue();

                    // Check if objectMap is not null before accessing
                    if (objectMap == null) {
                        Log.e(TAG, "Data is null for this child.");
                        continue;
                    }

                    // Log data for debugging
                    Log.d(TAG, "Data received: " + objectMap);

                    // Check for necessary keys before proceeding
                    if (objectMap.containsKey("areaId") && objectMap.containsKey("stationStatus")
                            && objectMap.containsKey("stationLatitude") && objectMap.containsKey("stationLongitude")) {

                        Object areaIdValue = objectMap.get("areaId");
                        Object stationStatusValue = objectMap.get("stationStatus");

                        // Ensure areaId and stationStatus are not null
                        if (areaIdValue != null && stationStatusValue != null
                                && areaIdValue.equals(areaId) && Boolean.TRUE.equals(stationStatusValue)) {

                            try {
                                // Parse latitude and longitude safely
                                double latitude = Double.parseDouble(objectMap.get("stationLatitude").toString());
                                double longitude = Double.parseDouble(objectMap.get("stationLongitude").toString());

                                // Add LatLng to stationMarkerList
                                stationMarkerList.add(new LatLng(latitude, longitude));

                                // Check if all required station details exist
                                if (objectMap.containsKey("stationName") && objectMap.containsKey("stationId")
                                        && objectMap.containsKey("stationRadius")
                                        && objectMap.get("stationName") != null && objectMap.get("stationId") != null
                                        && objectMap.get("stationRadius") != null) {

                                    // Create Station object and add it to stationList
                                    Station obj = new Station(
                                            objectMap.get("stationName").toString(),
                                            objectMap.get("stationId").toString(),
                                            objectMap.get("stationLatitude").toString(),
                                            objectMap.get("stationLongitude").toString(),
                                            objectMap.get("stationRadius").toString()
                                    );
                                    stationList.add(obj);

                                    // Uncomment to add marker and circle to Google Map (if required)
                        /*
                        mGoogleMap.addMarker(new MarkerOptions()
                                .title(objectMap.get("stationName").toString())
                                .position(new LatLng(latitude, longitude))
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.station_pin)));

                        mGoogleMap.addCircle(new CircleOptions()
                                .center(new LatLng(latitude, longitude))
                                .radius(Double.parseDouble(objectMap.get("stationRadius").toString()))
                                .fillColor(Color.argb(20, 102, 178, 255))
                                .strokeColor(Color.argb(0, 0, 0, 255)));
                        */

                                    // Call loadAllCycles with valid objectMap
                                    loadAllCycles(objectMap);
                                } else {
                                    Log.e(TAG, "One or more station details are missing.");
                                }
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "Error parsing latitude/longitude: ", e);
                            }
                        }
                    } else {
                        Log.e(TAG, "Required keys are missing or values are null.");
                    }
                }
            }


            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, "Error: " + databaseError);
            }
        });
    }

    private void loadAllAreaOfOperator(String operatorName) {
        String path = operatorName + "/Area";
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(path);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot i : dataSnapshot.getChildren()) {

                    for (DataSnapshot j : i.child("markerList").getChildren()) {
                        Log.d(TAG, Objects.requireNonNull(j.getValue()).toString());
                        areaMarkerList.add(new LatLng(Double.parseDouble(Objects.requireNonNull(j.child("latitude").getValue()).toString()), Double.parseDouble(Objects.requireNonNull(j.child("longitude").getValue()).toString())));
                    }
                    if (areaMarkerList.size() > 0) {
                        Polygon areaPolygon = mGoogleMap.addPolygon(new PolygonOptions()
                                .addAll(areaMarkerList));
                        areaPolygon.setFillColor(Color.argb(20, 0, 255, 0));
                        areaPolygon.setStrokeColor(Color.argb(0, 0, 0, 0));
                        areaMarkerList.clear();
                    }
                    Log.d(TAG, "Area Key " + i.getKey());
                    loadAllStationOfOperator(operatorName, i.getKey());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void loadAllStationOfOperator(String operatorName, String areaId) {
        stationList.clear();
        stationMarkerList.clear();
        Log.d(TAG, "Area Id: " + areaId);
        Log.d(TAG, "path: " + operatorName + "/Station/" + areaId);
        path = operatorName + "/Station";
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(path);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot i : dataSnapshot.getChildren()) {
                    Map<String, Object> objectMap = (Map<String, Object>) i.getValue();
                    Log.d(TAG, "latitude: " + Objects.requireNonNull(objectMap).get("stationLatitude") + " longitude: " + objectMap.get("stationLongitude"));
                    if (Objects.requireNonNull(objectMap.get("areaId")).equals(areaId) && Objects.requireNonNull(objectMap.get("stationStatus")).equals(true)) {
                        /*mGoogleMap.addMarker(new MarkerOptions()
                                .title(objectMap.get("stationName").toString())
                                .position(new LatLng(Double.valueOf(objectMap.get("stationLatitude").toString())
                                        , Double.valueOf(objectMap.get("stationLongitude").toString())))
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.station_pin)));
                        mGoogleMap.addCircle(new CircleOptions()
                                .center(new LatLng(Double.valueOf(objectMap.get("stationLatitude").toString())
                                        , Double.valueOf(objectMap.get("stationLongitude").toString())))
                                .radius(Double.valueOf(objectMap.get("stationRadius").toString()))
                                .fillColor(Color.argb(20, 102, 178, 255))
                                .strokeColor(Color.argb(0, 0, 0, 255)));*/
                        loadAllCyclesOfOperator(objectMap, operatorName);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, "Error: " + databaseError);
            }
        });
    }

    private void loadAllCycles(Map<String, Object> objectMap) {
        String path = Objects.requireNonNull(sharedPreferences.getString("organisationName", "no_data")).replace(" ", "") + "/Bicycle";
        Log.d(TAG, "path: " + path + " area id: " + area_id);
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(path);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long count = 0;
                for (DataSnapshot i : dataSnapshot.getChildren()) {
                    if (Objects.requireNonNull(objectMap.get("stationId")).toString().equals(i.child("inStationId").getValue(String.class))) {
                        count++; // replace by dipankar
                        if(i.child("bicycleLatitude").exists() && i.child("bicycleLongitude").exists()){
//                            count++;
                            //Need to code
                            mGoogleMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(Double.parseDouble(Objects.requireNonNull(i.child("bicycleLatitude").getValue()).toString())
                                            , Double.parseDouble(Objects.requireNonNull(i.child("bicycleLongitude").getValue()).toString())))
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_bicycle_marker)));
                        }
                        else{
                            mGoogleMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(Double.parseDouble(Objects.requireNonNull(objectMap.get("stationLatitude")).toString())
                                            , Double.parseDouble(Objects.requireNonNull(objectMap.get("stationLongitude")).toString())))
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_bicycle_marker)));
                        }
                    }
                }
                mGoogleMap.addMarker(new MarkerOptions()
                        .title(Objects.requireNonNull(objectMap.get("stationName")).toString())
                        .snippet("Total Cycle Count: " + count)
                        .position(new LatLng(Double.parseDouble(Objects.requireNonNull(objectMap.get("stationLatitude")).toString())
                                , Double.parseDouble(Objects.requireNonNull(objectMap.get("stationLongitude")).toString())))
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.station_pin)));
                mGoogleMap.addCircle(new CircleOptions()
                        .center(new LatLng(Double.parseDouble(Objects.requireNonNull(objectMap.get("stationLatitude")).toString())
                                , Double.parseDouble(Objects.requireNonNull(objectMap.get("stationLongitude")).toString())))
                        .radius(Double.parseDouble(Objects.requireNonNull(objectMap.get("stationRadius")).toString()))
                        .fillColor(Color.argb(20, 102, 178, 255))
                        .strokeColor(Color.argb(0, 0, 0, 255)));
                count = 0;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void loadAllCyclesOfOperator(Map<String, Object> objectMap, String operatorName) {
        String path = operatorName + "/Bicycle";
        //Log.d(TAG,"path: "+path+" area id: "+area_id);
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(path);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long count = 0;
                for (DataSnapshot i : dataSnapshot.getChildren()) {
                    if (Objects.requireNonNull(objectMap.get("stationId")).toString().equals(i.child("inStationId").getValue(String.class)) && Objects.requireNonNull(i.child("status").getValue(String.class)).equals("active")) {
                        count++;
                    }
                }
                mGoogleMap.addMarker(new MarkerOptions()
                        .title(Objects.requireNonNull(objectMap.get("stationName")).toString())
                        .snippet("Total Cycle Count: " + count)
                        .position(new LatLng(Double.valueOf(Objects.requireNonNull(objectMap.get("stationLatitude")).toString())
                                , Double.valueOf(Objects.requireNonNull(objectMap.get("stationLongitude")).toString())))
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.station_pin)));
                mGoogleMap.addCircle(new CircleOptions()
                        .center(new LatLng(Double.valueOf(Objects.requireNonNull(objectMap.get("stationLatitude")).toString())
                                , Double.valueOf(Objects.requireNonNull(objectMap.get("stationLongitude")).toString())))
                        .radius(Double.valueOf(Objects.requireNonNull(objectMap.get("stationRadius")).toString()))
                        .fillColor(Color.argb(20, 102, 178, 255))
                        .strokeColor(Color.argb(0, 0, 0, 255)));
                count = 0;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }
}
