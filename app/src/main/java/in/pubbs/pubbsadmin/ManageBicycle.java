package in.pubbs.pubbsadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

import in.pubbs.pubbsadmin.Adapter.ManageBicycleAdapter;
import in.pubbs.pubbsadmin.View.CustomLoader;

public class ManageBicycle extends AppCompatActivity {

    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;
    ManageBicycleAdapter manageBicycleAdapter;
    ArrayList arrayList = new ArrayList();
    ArrayList areaIdList, stationIdList;
    TextView title;
    ImageView back;
    Toolbar toolbar;
    SharedPreferences sharedPreferences;
    String TAG = ManageBicycle.class.getSimpleName();
    ConstraintLayout noData;
    String stationName;
    private CustomLoader customLoader;
    private static String areaID, stationID, bicycleID;
    SwipeRefreshLayout swipeRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_bicycle);
        init();
    }

    private void init() {
        sharedPreferences = getSharedPreferences(getResources().getString(R.string.sharedPreferences), Context.MODE_PRIVATE);
        recyclerView = findViewById(R.id.recycler_view);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        title = findViewById(R.id.toolbar_title);
        title.setText("Manage Bicycle");
        back = findViewById(R.id.back_button);
        toolbar = findViewById(R.id.toolbar);
        noData = findViewById(R.id.no_data_found);
        toolbar.setTitle("");
        back.setOnClickListener(v -> {
            /*startActivity(new Intent(ManageBicycle.this, MainActivity.class));
            finish();*/
            this.onBackPressed();//This operation will call the override function that is implemented in this class.
        });
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);
        areaIdList = new ArrayList();
        stationIdList = new ArrayList();
        customLoader = new CustomLoader(this, R.style.WideDialog);//Loader Definition
        //loadData("area");
        loadArea();
        //Swipe refresh load that the respective data according to is data content in the arrayList
        swipeRefresh.setOnRefreshListener(() -> {
            if (arrayList.size() > 0) {
                if (arrayList.get(0).toString().contains("Area")) {
                    loadArea();
                } else if (arrayList.get(0).toString().contains("Station")) {
                    loadStation();
                } else {
                    loadBicycle();
                }
                swipeRefresh.setRefreshing(false);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bicycle_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.add_bicycle) {
            Intent intentAdd = new Intent(ManageBicycle.this, AddOrRemoveBicycle.class);
            intentAdd.putExtra("Status", "ADD");
            intentAdd.putExtra("StationId", stationID);
            intentAdd.putExtra("StationName", stationName);
            intentAdd.putExtra("AreaId", areaID);
            startActivity(intentAdd);
        }
        else if (item.getItemId() == R.id.repair_bicycle) {
            Intent intentRepair = new Intent(ManageBicycle.this, AddOrRemoveBicycle.class);
            intentRepair.putExtra("Status", "REPAIR");
            startActivity(intentRepair);
        }
        else if (item.getItemId() == R.id.remove_bicycle) {
            // No action defined for remove_bicycle
        }

//        switch (item.getItemId()) {
//            case R.id.add_bicycle:
//                Intent intentAdd = new Intent(ManageBicycle.this, AddOrRemoveBicycle.class);
//                intentAdd.putExtra("Status", "ADD");
//                intentAdd.putExtra("StationId", stationID);
//                intentAdd.putExtra("StationName", stationName);
//                intentAdd.putExtra("AreaId", areaID);
//                startActivity(intentAdd);
//                break;
//            case R.id.repair_bicycle:
//                Intent intentRepair = new Intent(ManageBicycle.this, AddOrRemoveBicycle.class);
//                intentRepair.putExtra("Status", "REPAIR");
//                startActivity(intentRepair);
//                break;
//            case R.id.remove_bicycle:
//                break;
//        }
        return true;
    }
    /*
    //This section has been commented due to ambiguity in code, this section can later be removed if necessary, this section is used as a reference to the below function defined below.
    //Section Start
    private void loadData(String option) {
        customLoader.show();
        DatabaseReference databaseReference;
        String path;
        path = sharedPreferences.getString("organisationName", "no_data").replaceAll(" ", "") + "/Zone/" + sharedPreferences.getString("zone", null).trim().toUpperCase() + "/AreaList";
        Log.d(TAG, "path: " + path);
        databaseReference = FirebaseDatabase.getInstance().getReference(path);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                customLoader.dismiss();
                Log.d(TAG, dataSnapshot.getValue().toString());
                for (DataSnapshot i : dataSnapshot.getChildren()) {
                    Map<String, Object> map = (Map<String, Object>) i.getValue();
                    if (option.equals("area")) {
                        Log.d(TAG, "area name: " + map.get("name"));
                        arrayList.add("Area : " + map.get("name"));
                        areaIdList.add(map.get("id"));
                    }
                }
                if (arrayList.size() == 0) {
                    noData.setVisibility(View.VISIBLE);
                } else {
                    manageBicycleAdapter = new ManageBicycleAdapter(arrayList);
                    recyclerView.setAdapter(manageBicycleAdapter);
                    manageBicycleAdapter.notifyDataSetChanged();
                    manageBicycleAdapter
                            .onMenuItemClick(view -> {
                                Log.d("Clicked", "data " + arrayList.get(manageBicycleAdapter.getPosition()));
                                if (arrayList.get(manageBicycleAdapter.getPosition()).toString().contains("Area")) {
                                    customLoader.show();
                                    String areaName = arrayList.get(manageBicycleAdapter.getPosition()).toString();
                                    areaID = areaIdList.get(manageBicycleAdapter.getPosition()).toString();
                                    Log.d(TAG, "areaName: " + areaName.substring(areaName.indexOf(':') + 1).trim());
                                    arrayList.clear();
                                    String path1 = sharedPreferences.getString("organisationName", "no_data").replaceAll(" ", "") + "/Station";
                                    Log.d(TAG, "path: " + path1);
                                    DatabaseReference databaseReference1 = FirebaseDatabase.getInstance().getReference(path1);
                                    databaseReference1.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot1) {
                                            customLoader.dismiss();
                                            for (DataSnapshot i : dataSnapshot1.getChildren()) {
                                                Log.d(TAG, i.child("areaName").getValue().toString());
                                                if (areaID.equalsIgnoreCase(i.child("areaId").getValue().toString())) {
                                                    arrayList.add("Station: " + i.child("stationName").getValue().toString());
                                                    stationIdList.add(i.child("stationId").getValue().toString());
                                                }
                                            }
                                            if (arrayList.size() == 0) {
                                                noData.setVisibility(View.VISIBLE);
                                            }
                                            manageBicycleAdapter.notifyDataSetChanged();
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });
                                } else if (arrayList.get(manageBicycleAdapter.getPosition()).toString().contains("Station")) {
                                    customLoader.show();
                                    stationName = arrayList.get(manageBicycleAdapter.getPosition()).toString();
                                    stationName = stationName.substring(stationName.indexOf(":") + 1).trim();
                                    stationID = stationIdList.get(manageBicycleAdapter.getPosition()).toString();
                                    Log.d(TAG, "Station Name: " + stationName);
                                    arrayList.clear();
                                    setSupportActionBar(toolbar);
                                    //Code to be written
                                    String path1 = sharedPreferences.getString("organisationName", "no_data").replaceAll(" ", "") + "/Bicycle";
                                    Log.d(TAG, "path: " + path1);
                                    DatabaseReference databaseReference1 = FirebaseDatabase.getInstance().getReference(path1);
                                    databaseReference1.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot1) {
                                            customLoader.dismiss();
                                            for (DataSnapshot i : dataSnapshot1.getChildren()) {
                                                Log.d(TAG, "data: " + i.getValue());

                                                if (i.child("inStationId").getValue().equals(stationID)) {
                                                    Log.d(TAG, "Lock id: " + i.child("id").getValue() + " Status: " + i.child("status").getValue());
                                                    arrayList.add("Bicycle: " + i.child("id").getValue() + "\n\nStatus: " + i.child("status").getValue());
                                                }
                                            }
                                            if (arrayList.size() == 0) {
                                                noData.setVisibility(View.VISIBLE);
                                            }
                                            manageBicycleAdapter.notifyDataSetChanged();
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });

                                } else if (arrayList.get(manageBicycleAdapter.getPosition()).toString().contains("Bicycle")) {
                                    bicycleID = arrayList.get(manageBicycleAdapter.getPosition()).toString();
                                    if (bicycleID.contains("Status: busy")) {
                                        bicycleID = bicycleID.substring(bicycleID.indexOf(":") + 1).replaceAll("Status: busy", "");
                                        bicycleID = bicycleID.trim();
                                        Log.d(TAG, "Bicycle id clicked: " + bicycleID);
                                        String path = sharedPreferences.getString("organisationName", "no_data").replaceAll(" ", "") + "/Bicycle";
                                        Log.d(TAG, "Bicycle id clicked: " + path);
                                        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(path);
                                        databaseReference
                                                .child(bicycleID)
                                                .child("status")
                                                .setValue("active");
                                    }
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        //Section End
    }*/

    //This function loads all the area under the operator. This is the first function that is called.
    private void loadArea() {
        customLoader.show();
        DatabaseReference databaseReference;
        String path;
        path = Objects.requireNonNull(sharedPreferences.getString("organisationName", "no_data")).replaceAll(" ", "") + "/Zone/" + Objects.requireNonNull(sharedPreferences.getString("zone", null)).trim().toUpperCase() + "/AreaList";
        Log.d(TAG, "path: " + path);
        arrayList.clear();
        databaseReference = FirebaseDatabase.getInstance().getReference(path);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, Objects.requireNonNull(dataSnapshot.getValue()).toString());
                for (DataSnapshot i : dataSnapshot.getChildren()) {
                    Map<String, Object> map = (Map<String, Object>) i.getValue();
                    Log.d(TAG, "area name: " + Objects.requireNonNull(map).get("name"));
                    arrayList.add("Area : " + map.get("name"));
                    areaIdList.add(map.get("id"));

                }
                customLoader.dismiss();
                if (arrayList.size() == 0) {
                    recyclerView.setVisibility(View.GONE);
                    noData.setVisibility(View.VISIBLE);
                } else {
                    manageBicycleAdapter = new ManageBicycleAdapter(arrayList);
                    recyclerView.setAdapter(manageBicycleAdapter);
                    manageBicycleAdapter.notifyDataSetChanged();
                    manageBicycleAdapter.onMenuItemClick(view -> {
                        Log.d("Area Clicked", "data " + arrayList.get(manageBicycleAdapter.getPosition()));
                        areaID = areaIdList.get(manageBicycleAdapter.getPosition()).toString();
                        loadStation();
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    //This function loads all the stations under that area that the user have selected. This is the second function that is being called after loadArea().
    private void loadStation() {
        customLoader.show();
        String path = Objects.requireNonNull(sharedPreferences.getString("organisationName", "no_data")).replaceAll(" ", "") + "/Station";
        Log.d(TAG, "path: " + path);
        arrayList.clear();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(path);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot1) {
                for (DataSnapshot i : dataSnapshot1.getChildren()) {
//                    Log.d(TAG, Objects.requireNonNull(i.child("areaName").getValue()).toString());
                    if (areaID.equalsIgnoreCase(Objects.requireNonNull(i.child("areaId").getValue()).toString())) {
                        arrayList.add("Station: " + Objects.requireNonNull(i.child("stationName").getValue()).toString());
                        stationIdList.add(Objects.requireNonNull(i.child("stationId").getValue()).toString());
                    }
                }
                customLoader.dismiss();
                if (arrayList.size() == 0) {
                    recyclerView.setVisibility(View.GONE);
                    noData.setVisibility(View.VISIBLE);
                } else {
                    manageBicycleAdapter.notifyDataSetChanged();
                    manageBicycleAdapter.onMenuItemClick(view -> {
                        Log.d("Station Clicked", "data " + arrayList.get(manageBicycleAdapter.getPosition()));
                        stationID = stationIdList.get(manageBicycleAdapter.getPosition()).toString();
                        loadBicycle();
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    //This function is called to load all the Bicycles under that Station, on click on a cycles this function checks if its status is busy and if busy it will activate the cycles and reload the data.
    private void loadBicycle() {
        customLoader.show();
        setSupportActionBar(toolbar);
        String path = Objects.requireNonNull(sharedPreferences.getString("organisationName", "no_data")).replaceAll(" ", "") + "/Bicycle";
        Log.d(TAG, "path: " + path);
        arrayList.clear();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(path);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot1) {
                for (DataSnapshot i : dataSnapshot1.getChildren()) {
                    Log.d(TAG, "data: " + i.getValue());
                    try {
                        if (Objects.requireNonNull(i.child("inStationId").getValue()).equals(stationID)) {
                            Log.d(TAG, "Lock id: " + i.child("id").getValue() + " Status: " + i.child("status").getValue());
                            arrayList.add("Bicycle: " + i.child("id").getValue() + "\n\nStatus: " + i.child("status").getValue());
                        }
                    } catch (Exception excp){

                    }

                }
                customLoader.dismiss();
                if (arrayList.size() == 0) {
                    recyclerView.setVisibility(View.GONE);
                    noData.setVisibility(View.VISIBLE);
                } else {
                    manageBicycleAdapter.notifyDataSetChanged();
                    manageBicycleAdapter.onMenuItemClick(view -> {
                        Log.d("Bicycle Clicked", "data " + arrayList.get(manageBicycleAdapter.getPosition()));
                        bicycleID = arrayList.get(manageBicycleAdapter.getPosition()).toString();
                        if (bicycleID.contains("Status: busy")) {
                            bicycleID = bicycleID.substring(bicycleID.indexOf(":") + 1).replaceAll("Status: busy", "");

                            bicycleID = bicycleID.trim();
                            Log.d(TAG, "Bicycle id clicked: " + bicycleID);
                            String path1 = Objects.requireNonNull(sharedPreferences.getString("organisationName", "no_data")).replaceAll(" ", "") + "/Bicycle";
                            Log.d(TAG, "Bicycle id clicked: " + path1);
                            DatabaseReference databaseReference1 = FirebaseDatabase.getInstance().getReference(path1);
                            databaseReference1
                                    .child(bicycleID)
                                    .child("status")
                                    .setValue("active").addOnCompleteListener(task -> loadBicycle());



                        } else {
                            bicycleID = bicycleID.substring(bicycleID.indexOf(":") + 1).replaceAll("Status: active", "");
                            bicycleID = bicycleID.trim();
                            Log.d(TAG, "Bicycle id clicked: " + bicycleID);
                            /*databaseReference
                                    .child(bicycleID)
                                    .child("status")
                                    .setValue("busy").addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    loadBicycle();
                                }
                            });*/
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onBackPressed() {
        //This section is very much important for sequential back function. If the user is in Bicycle section it will back down to Station nad there after to Area and then finally back to the MainActivity.
        if (arrayList.size() > 0) {
            if (arrayList.get(0).toString().contains("Bicycle")) {
                super.onBackPressed();
            } else if (arrayList.get(0).toString().contains("Station")) {
                loadArea();
            } else {
                super.onBackPressed();
            }

        } else {
            super.onBackPressed();
        }
    }
}