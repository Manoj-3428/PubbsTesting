package in.pubbs.pubbsadmin;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import in.pubbs.pubbsadmin.Adapter.BicycleListAdapter;
import in.pubbs.pubbsadmin.View.CustomLoader;

public class BicycleListActivity extends AppCompatActivity implements View.OnClickListener {
    private RecyclerView rv_bicycle_list;
    private BicycleListAdapter bicycleListAdapter;
    private Toolbar toolbar;
    private TextView tv_title, addBicycle;
    private String TAG = BicycleListActivity.class.getSimpleName();
    private SharedPreferences sharedPreferences;
    private ArrayList<Map<String, Object>> list = new ArrayList<>();
    private LinearLayout noDataFound;
    private CustomLoader customLoader;
    SwipeRefreshLayout swipeRefresh;
    private String type = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bicycle_list);
        init();
        loadData();
        swipeRefresh.setOnRefreshListener(() -> {
            if (!type.equals("Station")) {
                list.clear();
                loadData();
            }
            swipeRefresh.setRefreshing(false);
        });
    }

    private void init() {
        sharedPreferences = getSharedPreferences("pubbs", Context.MODE_PRIVATE);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        findViewById(R.id.back_button).setOnClickListener(v -> onBackPressed());
        tv_title = findViewById(R.id.toolbar_title);
        tv_title.setText("Bicycle List");
        addBicycle = findViewById(R.id.add_bicycle);
        addBicycle.setOnClickListener(this);
        noDataFound = findViewById(R.id.no_data_found);
        noDataFound.setVisibility(View.GONE);
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        rv_bicycle_list = findViewById(R.id.rv_bicycle_list);
        rv_bicycle_list.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        customLoader = new CustomLoader(this, R.style.WideDialog);//Loader
        customLoader.show();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.manage_bicycle_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.all_cycle) {
            rv_bicycle_list.setVisibility(View.VISIBLE);
            noDataFound.setVisibility(View.GONE);
            addBicycle.setVisibility(View.VISIBLE);
            Log.d(TAG, "All Cycles");
            tv_title.setText("All Cycles");
            customLoader.show();
            loadData();
            swipeRefresh.setOnRefreshListener(() -> {
                list.clear();
                loadData();
                swipeRefresh.setRefreshing(false);
            });
        }
        else if (item.getItemId() == R.id.low_battery_bicycle) {
            rv_bicycle_list.setVisibility(View.VISIBLE);
            noDataFound.setVisibility(View.GONE);
            addBicycle.setVisibility(View.VISIBLE);
            Log.d(TAG, "Low Battery Bicycle");
            tv_title.setText("Low Battery Bicycle");
            customLoader.show();
            loadLowBatteryCycle();
            swipeRefresh.setOnRefreshListener(() -> {
                list.clear();
                loadLowBatteryCycle();
                swipeRefresh.setRefreshing(false);
            });
        }
        else if (item.getItemId() == R.id.reported_bicycle) {
            rv_bicycle_list.setVisibility(View.VISIBLE);
            noDataFound.setVisibility(View.GONE);
            addBicycle.setVisibility(View.VISIBLE);
            Log.d(TAG, "Reported Bicycle");
            tv_title.setText("Reported Bicycle");
            customLoader.show();
            loadReportedCycle();
            swipeRefresh.setOnRefreshListener(() -> {
                list.clear();
                loadReportedCycle();
                swipeRefresh.setRefreshing(false);
            });
        }
        else if (item.getItemId() == R.id.repair_bicycle) {
            rv_bicycle_list.setVisibility(View.VISIBLE);
            noDataFound.setVisibility(View.GONE);
            addBicycle.setVisibility(View.VISIBLE);
            Log.d(TAG, "Repair Bicycle");
            tv_title.setText("Repair Bicycle");
            customLoader.show();
            loadRepairCycle();
            swipeRefresh.setOnRefreshListener(() -> {
                list.clear();
                loadRepairCycle();
                swipeRefresh.setRefreshing(false);
            });
        }
        else if (item.getItemId() == R.id.hold_bicycle) {
            Intent holdBicycle = new Intent(BicycleListActivity.this, BicycleOnHold.class);
            holdBicycle.putExtra("AreaId", sharedPreferences.getString("areaId", "no_area"));
            startActivity(holdBicycle);
        }



//        switch (item.getItemId()) {
//            case R.id.all_cycle:
//                rv_bicycle_list.setVisibility(View.VISIBLE);
//                noDataFound.setVisibility(View.GONE);
//                addBicycle.setVisibility(View.VISIBLE);
//                Log.d(TAG, "All Cycles");
//                tv_title.setText("All Cycles");
//                customLoader.show();
//                loadData();
//                swipeRefresh.setOnRefreshListener(() -> {
//                    list.clear();
//                    loadData();
//                    swipeRefresh.setRefreshing(false);
//                });
//                break;
//            case R.id.low_battery_bicycle:
//                rv_bicycle_list.setVisibility(View.VISIBLE);
//                noDataFound.setVisibility(View.GONE);
//                addBicycle.setVisibility(View.VISIBLE);
//                Log.d(TAG, "Low Battery Bicycle");
//                tv_title.setText("Low Battery Bicycle");
//                customLoader.show();
//                loadLowBatteryCycle();
//                swipeRefresh.setOnRefreshListener(() -> {
//                    list.clear();
//                    loadLowBatteryCycle();
//                    swipeRefresh.setRefreshing(false);
//                });
//                break;
//            case R.id.reported_bicycle:
//                rv_bicycle_list.setVisibility(View.VISIBLE);
//                noDataFound.setVisibility(View.GONE);
//                addBicycle.setVisibility(View.VISIBLE);
//                Log.d(TAG, "Reported Bicycle");
//                tv_title.setText("Reported Bicycle");
//                customLoader.show();
//                loadReportedCycle();
//                swipeRefresh.setOnRefreshListener(() -> {
//                    list.clear();
//                    loadReportedCycle();
//                    swipeRefresh.setRefreshing(false);
//                });
//                break;
//            case R.id.repair_bicycle:
//                rv_bicycle_list.setVisibility(View.VISIBLE);
//                noDataFound.setVisibility(View.GONE);
//                addBicycle.setVisibility(View.VISIBLE);
//                Log.d(TAG, "Repair Bicycle");
//                tv_title.setText("Repair Bicycle");
//                customLoader.show();
//                loadRepairCycle();
//                swipeRefresh.setOnRefreshListener(() -> {
//                    list.clear();
//                    loadRepairCycle();
//                    swipeRefresh.setRefreshing(false);
//                });
//                break;
//            case R.id.hold_bicycle:
//                Intent holdBicycle = new Intent(BicycleListActivity.this, BicycleOnHold.class);
//                holdBicycle.putExtra("AreaId", sharedPreferences.getString("areaId", "no_area"));
//                startActivity(holdBicycle);
//                break;
//        }
        return super.onOptionsItemSelected(item);
    }

    private void loadData() {
        String path = sharedPreferences.getString("organisationName", "no_data").replace(" ", "") + "/Bicycle";
        Log.d(TAG, "path: " + path);
        list.clear();
        type = "";
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(path);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot i : dataSnapshot.getChildren()) {
                    try {
                        Map<String, Object> map = (Map<String, Object>) i.getValue();
                        list.add(map);
                    } catch (Exception exc){

                    }
                }
                if (list.size() == 0) {
                    rv_bicycle_list.setVisibility(View.GONE);
                    noDataFound.setVisibility(View.VISIBLE);
                } else {
                    noDataFound.setVisibility(View.GONE);
                    rv_bicycle_list.setVisibility(View.VISIBLE);
                    bicycleListAdapter = new BicycleListAdapter(list, BicycleListActivity.this, type);
                    rv_bicycle_list.setAdapter(bicycleListAdapter);
                    bicycleListAdapter.notifyDataSetChanged();
                }
                customLoader.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void loadLowBatteryCycle() {
        list.clear();
        type = "";
        String path = sharedPreferences.getString("organisationName", "no_data").replace(" ", "") + "/Bicycle";
        Log.d(TAG, "path: " + path);
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(path);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot i : dataSnapshot.getChildren()) {
                    if (i.child("battery").exists()) {
                        if (Integer.valueOf(Objects.requireNonNull(i.child("battery").getValue()).toString().replace("", "0")) < 40) {
                            Map<String, Object> map = (Map<String, Object>) i.getValue();
                            list.add(map);
                        }
                    }
                }
                if (list.size() == 0) {
                    rv_bicycle_list.setVisibility(View.GONE);
                    noDataFound.setVisibility(View.VISIBLE);
                } else {
                    noDataFound.setVisibility(View.GONE);
                    rv_bicycle_list.setVisibility(View.VISIBLE);
                    bicycleListAdapter = new BicycleListAdapter(list, BicycleListActivity.this, type);
                    rv_bicycle_list.setAdapter(bicycleListAdapter);
                    bicycleListAdapter.notifyDataSetChanged();
                }
                customLoader.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void loadReportedCycle() {
        list.clear();
        type = "";
        String path = Objects.requireNonNull(sharedPreferences.getString("organisationName", null)).replaceAll(" ", "") + "/ReportCycle";
        Log.d(TAG, "path: " + path);
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(path);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot i : dataSnapshot.getChildren()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", i.child("bicycleId").getValue());
                    map.put("status", "Unknown");
                    list.add(map);
                }
                if (list.size() == 0) {
                    rv_bicycle_list.setVisibility(View.GONE);
                    noDataFound.setVisibility(View.VISIBLE);
                } else {
                    rv_bicycle_list.setVisibility(View.VISIBLE);
                    bicycleListAdapter = new BicycleListAdapter(list, BicycleListActivity.this, type);
                    rv_bicycle_list.setAdapter(bicycleListAdapter);
                    bicycleListAdapter.notifyDataSetChanged();
                }
                customLoader.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.add_bicycle) {
            if (addBicycle.getText().equals("Add to repair")) {
                // No action if "Add to repair" is selected
            }
            else if (addBicycle.getText().equals("Add Bicycle")) {
                noDataFound.setVisibility(View.GONE);
                rv_bicycle_list.setVisibility(View.VISIBLE);
                loadStationDetails(sharedPreferences.getString("areaId", null));
            }
        }



//        switch (v.getId()) {
//            case R.id.add_bicycle:
//                if (addBicycle.getText().equals("Add to repair")) {
//
//                } else if (addBicycle.getText().equals("Add Bicycle")) {
//                    noDataFound.setVisibility(View.GONE);
//                    rv_bicycle_list.setVisibility(View.VISIBLE);
//                    loadStationDetails(sharedPreferences.getString("areaId", null));
//                }
//                break;
//        }
    }

    private void loadStationDetails(String areaId) {
        customLoader.show();
        String path = Objects.requireNonNull(sharedPreferences.getString("organisationName", null)).replaceAll(" ", "") + "/Station";
        list.clear();
        type = "Station";
        tv_title.setText("Station List");
        addBicycle.setVisibility(View.GONE);
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(path);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "datasnapshot: " + dataSnapshot.getValue());
                for (DataSnapshot i : dataSnapshot.getChildren()) {
                    if (Objects.requireNonNull(i.child("areaId").getValue()).equals(areaId)) {
                        Log.d(TAG, "Matched: " + Objects.requireNonNull(i.getValue()).toString());
                        Map<String, Object> map = (Map<String, Object>) i.getValue();
                        list.add(map);
                    }
                    if (list.size() == 0) {
                        noDataFound.setVisibility(View.VISIBLE);
                        rv_bicycle_list.setVisibility(View.GONE);
                    } else {
                        rv_bicycle_list.setVisibility(View.VISIBLE);
                        noDataFound.setVisibility(View.GONE);
                        bicycleListAdapter = new BicycleListAdapter(list, BicycleListActivity.this, type);
                        rv_bicycle_list.setAdapter(bicycleListAdapter);
                        bicycleListAdapter.notifyDataSetChanged();
                    }
                    customLoader.dismiss();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (!tv_title.getText().equals("Station List"))
            addBicycle.setVisibility(View.VISIBLE);
    }

    private void loadRepairCycle() {
        list.clear();
        type = "";
        String path = Objects.requireNonNull(sharedPreferences.getString("organisationName", null)).replaceAll(" ", "") + "/RepairBicycle";
        Log.d(TAG, "path: " + path);
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(path);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot i : dataSnapshot.getChildren()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", i.child("id").getValue());
                    map.put("battery", i.child("battery").getValue());
                    map.put("status", "active");
                    list.add(map);
                }
                if (list.size() == 0) {
                    rv_bicycle_list.setVisibility(View.GONE);
                    noDataFound.setVisibility(View.VISIBLE);
                } else {
                    rv_bicycle_list.setVisibility(View.VISIBLE);
                    bicycleListAdapter = new BicycleListAdapter(list, BicycleListActivity.this, type);
                    rv_bicycle_list.setAdapter(bicycleListAdapter);
                    bicycleListAdapter.notifyDataSetChanged();
                }
                customLoader.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
