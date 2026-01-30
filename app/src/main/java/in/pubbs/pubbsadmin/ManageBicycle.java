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
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import in.pubbs.pubbsadmin.Adapter.ManageCycleListAdapter;
import in.pubbs.pubbsadmin.View.CustomLoader;

public class ManageBicycle extends AppCompatActivity {

    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;
    private final ArrayList arrayList = new ArrayList(); // legacy; keep for compatibility
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
    private FrameLayout segmentRoot;
    private View segmentIndicator;
    private TextView segStation;
    private TextView segAll;
    private int selectedSegment = 0; // 0 = station, 1 = all

    private enum Screen {
        STATION_LIST,
        STATION_CYCLES,
        ALL_BICYCLES
    }

    private Screen currentScreen = Screen.STATION_LIST;
    private final HashMap<String, String> stationIdToAreaId = new HashMap<>();
    private final HashMap<String, String> stationIdToName = new HashMap<>();

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
        segmentRoot = findViewById(R.id.segment_root);
        segmentIndicator = findViewById(R.id.segment_indicator);
        segStation = findViewById(R.id.seg_station);
        segAll = findViewById(R.id.seg_all);
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
        customLoader = new CustomLoader(this, R.style.WideDialog);//Loader Definition

        setupSegmentedTabs();
        showStationList();

        swipeRefresh.setOnRefreshListener(() -> {
            switch (currentScreen) {
                case STATION_CYCLES:
                    if (stationID != null && !stationID.isEmpty()) {
                        showStationCycles(stationID);
                    } else {
                        showStationList();
                    }
                    break;
                case ALL_BICYCLES:
                    showAllBicycles();
                    break;
                case STATION_LIST:
                default:
                    showStationList();
                    break;
            }
            swipeRefresh.setRefreshing(false);
        });
    }

    private void setupSegmentedTabs() {
        if (segmentRoot == null || segmentIndicator == null || segStation == null || segAll == null) return;

        segStation.setOnClickListener(v -> selectSegment(0, true));
        segAll.setOnClickListener(v -> selectSegment(1, true));

        // Configure indicator width after layout pass
        segmentRoot.post(() -> {
            int w = segmentRoot.getWidth();
            if (w <= 0) return;
            int segW = w / 2;
            ViewGroup.LayoutParams lp = segmentIndicator.getLayoutParams();
            lp.width = segW;
            segmentIndicator.setLayoutParams(lp);
            // Apply current selection without animation on first layout
            selectSegment(selectedSegment, false);
        });
    }

    private void selectSegment(int index, boolean animate) {
        selectedSegment = index;
        if (segStation != null) segStation.setTextColor(index == 0 ? 0xFF000000 : 0xFF666666);
        if (segAll != null) segAll.setTextColor(index == 1 ? 0xFF000000 : 0xFF666666);

        if (segmentRoot == null || segmentIndicator == null) return;
        int w = segmentRoot.getWidth();
        if (w <= 0) return;
        float targetX = (index == 0) ? 0f : (w / 2f);

        if (animate) {
            segmentIndicator.animate()
                    .translationX(targetX)
                    .setDuration(320)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        } else {
            segmentIndicator.setTranslationX(targetX);
        }

        if (index == 0) {
            showStationList();
        } else {
            showAllBicycles();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bicycle_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.add_bicycle) {
            // Use existing AddOrRemoveBicycle screen (scanner + manual entry UI)
            if (stationID != null && !stationID.isEmpty()) {
                Intent intentAdd = new Intent(ManageBicycle.this, AddOrRemoveBicycle.class);
                intentAdd.putExtra("Status", "ADD");
                intentAdd.putExtra("StationId", stationID);
                intentAdd.putExtra("StationName", stationName);
                intentAdd.putExtra("AreaId", areaID);
                startActivity(intentAdd);
            } else {
                android.widget.Toast.makeText(this, "Please select a station first", android.widget.Toast.LENGTH_SHORT).show();
            }

            // Previous multi-select flow (kept for reference)
            // if (stationID != null && !stationID.isEmpty()) {
            //     Intent intentAdd = new Intent(ManageBicycle.this, SelectBicyclesToAddActivity.class);
            //     intentAdd.putExtra("StationId", stationID);
            //     intentAdd.putExtra("StationName", stationName);
            //     intentAdd.putExtra("AreaId", areaID != null ? areaID : "");
            //     startActivityForResult(intentAdd, 100);
            // }
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

    private void showStationList() {
        currentScreen = Screen.STATION_LIST;
        title.setText("Station-Wise List");
        stationID = null;
        stationName = null;
        areaID = null;
        if (segmentRoot != null) segmentRoot.setVisibility(View.VISIBLE);
        if (selectedSegment != 0) selectSegment(0, false);

        String org = Objects.requireNonNull(sharedPreferences.getString("organisationName", "no_data")).replaceAll(" ", "");
        customLoader.show();
        stationIdToAreaId.clear();
        stationIdToName.clear();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(org).child("Station");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<SelectStationActivity.StationRow> rows = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String sid = child.getKey();
                    if (sid == null) continue;
                    Object snameObj = child.child("stationName").getValue();
                    String sname = snameObj == null ? sid : String.valueOf(snameObj);
                    rows.add(new SelectStationActivity.StationRow(sid, sname));

                    Object areaObj = child.child("areaId").getValue();
                    if (areaObj != null) stationIdToAreaId.put(sid, String.valueOf(areaObj));
                    stationIdToName.put(sid, sname);
                }

                SelectStationAdapter adapter = new SelectStationAdapter(rows, stationRow -> {
                    stationID = stationRow.stationId;
                    stationName = stationRow.stationName == null ? stationRow.stationId : stationRow.stationName;
                    areaID = stationIdToAreaId.get(stationID);
                    showStationCycles(stationID);
                });
                recyclerView.setAdapter(adapter);

                customLoader.dismiss();
                recyclerView.setVisibility(rows.isEmpty() ? View.GONE : View.VISIBLE);
                noData.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                customLoader.dismiss();
                recyclerView.setVisibility(View.GONE);
                noData.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showAllBicycles() {
        currentScreen = Screen.ALL_BICYCLES;
        title.setText("All Bicycle List");
        stationID = null;
        stationName = null;
        areaID = null;
        if (segmentRoot != null) segmentRoot.setVisibility(View.VISIBLE);
        if (selectedSegment != 1) selectSegment(1, false);

        String org = Objects.requireNonNull(sharedPreferences.getString("organisationName", "no_data")).replaceAll(" ", "");
        customLoader.show();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(org).child("Bicycle");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ManageCycleListAdapter.Item> items = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String bikeId = child.getKey();
                    if (bikeId == null) continue;
                    int percent = parseBatteryPercent(child.child("cyclebattery").getValue(), child.child("battery").getValue());
                    boolean isEBike = isEBike(child.child("type").getValue(), child.child("Type").getValue());
                    items.add(new ManageCycleListAdapter.Item(bikeId, percent, isEBike));
                }

                recyclerView.setAdapter(new ManageCycleListAdapter(items, item -> {
                    Intent detail = new Intent(ManageBicycle.this, BicycleDetailActivity.class);
                    detail.putExtra("BICYCLE_ID", item.id);
                    startActivity(detail);
                }));

                customLoader.dismiss();
                recyclerView.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
                noData.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                customLoader.dismiss();
                recyclerView.setVisibility(View.GONE);
                noData.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showStationCycles(@NonNull String stationId) {
        currentScreen = Screen.STATION_CYCLES;
        title.setText(stationName == null || stationName.isEmpty() ? "Station Cycles" : stationName);
        // Hide the selector when showing cycles (as requested)
        if (segmentRoot != null) segmentRoot.setVisibility(View.GONE);

        String org = Objects.requireNonNull(sharedPreferences.getString("organisationName", "no_data")).replaceAll(" ", "");
        customLoader.show();

        DatabaseReference cyclesRef = FirebaseDatabase.getInstance().getReference(org)
                .child("Station").child(stationId).child("cyclesList");
        DatabaseReference bicycleRef = FirebaseDatabase.getInstance().getReference(org).child("Bicycle");

        cyclesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot cyclesSnap) {
                // Collect station cycles with their stored percentage (fallback to 100)
                HashMap<String, Integer> stationCycles = new HashMap<>();
                for (DataSnapshot cycleEntry : cyclesSnap.getChildren()) {
                    String cid = cycleEntry.getKey();
                    if (cid == null) continue;
                    Object pObj = cycleEntry.child("percentage").getValue();
                    int p = 100;
                    if (pObj != null) {
                        try { p = (int) Math.round(Double.parseDouble(String.valueOf(pObj))); } catch (Exception ignored) { }
                    }
                    if (p < 0) p = 0;
                    if (p > 100) p = 100;
                    stationCycles.put(cid, p);
                }

                bicycleRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot bikeSnap) {
                        List<ManageCycleListAdapter.Item> items = new ArrayList<>();
                        // Build a lookup of type by id
                        HashMap<String, String> idToType = new HashMap<>();
                        for (DataSnapshot b : bikeSnap.getChildren()) {
                            String bid = b.getKey();
                            if (bid == null) continue;
                            Object tObj = b.child("type").getValue();
                            if (tObj == null) tObj = b.child("Type").getValue();
                            if (tObj != null) idToType.put(bid, String.valueOf(tObj));
                        }

                        for (Map.Entry<String, Integer> entry : stationCycles.entrySet()) {
                            String cid = entry.getKey();
                            int percent = entry.getValue();
                            boolean isEBike = isEBike(idToType.get(cid), null);
                            items.add(new ManageCycleListAdapter.Item(cid, percent, isEBike));
                        }

                        recyclerView.setAdapter(new ManageCycleListAdapter(items, item -> {
                            Intent detail = new Intent(ManageBicycle.this, BicycleDetailActivity.class);
                            detail.putExtra("BICYCLE_ID", item.id);
                            startActivity(detail);
                        }));

                        customLoader.dismiss();
                        recyclerView.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
                        noData.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        customLoader.dismiss();
                        recyclerView.setVisibility(View.GONE);
                        noData.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                customLoader.dismiss();
                recyclerView.setVisibility(View.GONE);
                noData.setVisibility(View.VISIBLE);
            }
        });
    }

    private int parseBatteryPercent(Object cyclebatteryObj, Object batteryObj) {
        Object obj = cyclebatteryObj != null ? cyclebatteryObj : batteryObj;
        if (obj == null) return 100;
        try {
            double p = Double.parseDouble(String.valueOf(obj));
            int v = (int) Math.round(p);
            if (v < 0) v = 0;
            if (v > 100) v = 100;
            return v;
        } catch (Exception e) {
            return 100;
        }
    }

    private boolean isEBike(Object typeObj, Object typeObj2) {
        Object obj = typeObj != null ? typeObj : typeObj2;
        if (obj == null) return false;
        String t = String.valueOf(obj).trim();
        if (t.isEmpty()) return false;
        return t.toLowerCase().endsWith("e");
    }

    @Override
    public void onBackPressed() {
        if (currentScreen == Screen.STATION_CYCLES) {
            showStationList();
            return;
        }
        super.onBackPressed();
    }
}