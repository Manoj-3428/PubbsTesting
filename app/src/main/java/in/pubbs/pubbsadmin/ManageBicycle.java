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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
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
    private View searchContainer;
    private EditText searchBox;
    private ImageView searchClear;
    private TextView addBicycleButton;
    private final List<SelectStationActivity.StationRow> fullStationRows = new ArrayList<>();
    private final List<ManageCycleListAdapter.Item> fullBicycleItems = new ArrayList<>();
    private final List<ManageCycleListAdapter.Item> fullStationCycleItems = new ArrayList<>();
    private static final int REQUEST_SELECT_STATION_FOR_ADD = 200;

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
        searchContainer = findViewById(R.id.search_container);
        searchBox = findViewById(R.id.search_box);
        searchClear = findViewById(R.id.search_clear);
        addBicycleButton = findViewById(R.id.add_bicycle_button);
        toolbar.setTitle("");
        setupSearchBox();
        if (addBicycleButton != null) {
            addBicycleButton.setOnClickListener(v -> onAddBicycleClicked());
        }
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

    private void setupSearchBox() {
        if (searchBox == null) return;
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (searchClear != null) {
                    searchClear.setVisibility(s != null && s.length() > 0 ? View.VISIBLE : View.GONE);
                }
                applySearchFilter();
            }
        });
        searchBox.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                searchBox.clearFocus();
                hideKeyboard(searchBox);
                return true;
            }
            return false;
        });
        if (searchClear != null) {
            searchClear.setOnClickListener(v -> {
                if (searchBox != null) {
                    searchBox.setText("");
                    searchBox.clearFocus();
                    hideKeyboard(searchBox);
                }
                if (searchClear != null) searchClear.setVisibility(View.GONE);
            });
        }
    }

    private void hideKeyboard(View view) {
        if (view == null) return;
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void applySearchFilter() {
        String query = searchBox != null ? searchBox.getText().toString().trim().toLowerCase() : "";
        if (currentScreen == Screen.STATION_LIST) {
            List<SelectStationActivity.StationRow> filtered = new ArrayList<>();
            for (SelectStationActivity.StationRow row : fullStationRows) {
                String name = row.stationName != null ? row.stationName : row.stationId;
                if (query.isEmpty() || (name != null && name.toLowerCase().contains(query))) {
                    filtered.add(row);
                }
            }
            SelectStationAdapter adapter = new SelectStationAdapter(filtered, stationRow -> {
                stationID = stationRow.stationId;
                stationName = stationRow.stationName == null ? stationRow.stationId : stationRow.stationName;
                areaID = stationRow.areaId;
                showStationCycles(stationID);
            });
            recyclerView.setAdapter(adapter);
            recyclerView.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
            noData.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        } else if (currentScreen == Screen.ALL_BICYCLES) {
            List<ManageCycleListAdapter.Item> filtered = new ArrayList<>();
            for (ManageCycleListAdapter.Item item : fullBicycleItems) {
                if (query.isEmpty() || (item.id != null && item.id.toLowerCase().contains(query))) {
                    filtered.add(item);
                }
            }
            recyclerView.setAdapter(new ManageCycleListAdapter(filtered, item -> {
                Intent detail = new Intent(ManageBicycle.this, BicycleDetailActivity.class);
                detail.putExtra("BICYCLE_ID", item.id);
                startActivity(detail);
            }));
            recyclerView.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
            noData.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        } else if (currentScreen == Screen.STATION_CYCLES) {
            List<ManageCycleListAdapter.Item> filtered = new ArrayList<>();
            for (ManageCycleListAdapter.Item item : fullStationCycleItems) {
                if (query.isEmpty() || (item.id != null && item.id.toLowerCase().contains(query))) {
                    filtered.add(item);
                }
            }
            recyclerView.setAdapter(new ManageCycleListAdapter(filtered, item -> {
                Intent detail = new Intent(ManageBicycle.this, BicycleDetailActivity.class);
                detail.putExtra("BICYCLE_ID", item.id);
                startActivity(detail);
            }));
            recyclerView.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
            noData.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void onAddBicycleClicked() {
        if (currentScreen == Screen.STATION_CYCLES && stationID != null && !stationID.isEmpty()) {
            Intent intentAdd = new Intent(ManageBicycle.this, AddOrRemoveBicycle.class);
            intentAdd.putExtra("Status", "ADD");
            intentAdd.putExtra("StationId", stationID);
            intentAdd.putExtra("StationName", stationName);
            intentAdd.putExtra("AreaId", areaID != null ? areaID : "");
            startActivity(intentAdd);
        } else if (currentScreen == Screen.ALL_BICYCLES) {
            Intent selectStation = new Intent(ManageBicycle.this, SelectStationActivity.class);
            startActivityForResult(selectStation, REQUEST_SELECT_STATION_FOR_ADD);
        } else {
            Toast.makeText(this, "Please select a station first", Toast.LENGTH_SHORT).show();
        }
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem addItem = menu.findItem(R.id.add_bicycle);
        if (addItem != null) {
            // Show Add Bicycle only when viewing a station's cycles or All Bicycle List (not on Station-Wise List root)
            addItem.setVisible(currentScreen == Screen.STATION_CYCLES || currentScreen == Screen.ALL_BICYCLES);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.add_bicycle) {
            if (currentScreen == Screen.STATION_CYCLES && stationID != null && !stationID.isEmpty()) {
                Intent intentAdd = new Intent(ManageBicycle.this, AddOrRemoveBicycle.class);
                intentAdd.putExtra("Status", "ADD");
                intentAdd.putExtra("StationId", stationID);
                intentAdd.putExtra("StationName", stationName);
                intentAdd.putExtra("AreaId", areaID != null ? areaID : "");
                startActivity(intentAdd);
            } else if (currentScreen == Screen.ALL_BICYCLES) {
                // From All Bicycle List: show station picker, then scanner
                Intent selectStation = new Intent(ManageBicycle.this, SelectStationActivity.class);
                startActivityForResult(selectStation, REQUEST_SELECT_STATION_FOR_ADD);
            } else {
                Toast.makeText(this, "Please select a station first", Toast.LENGTH_SHORT).show();
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
        invalidateOptionsMenu();
        title.setText("Station-Wise List");
        stationID = null;
        stationName = null;
        areaID = null;
        if (segmentRoot != null) segmentRoot.setVisibility(View.VISIBLE);
        if (searchContainer != null) searchContainer.setVisibility(View.VISIBLE);
        if (searchBox != null) searchBox.setText("");
        if (searchClear != null) searchClear.setVisibility(View.GONE);
        if (addBicycleButton != null) addBicycleButton.setVisibility(View.GONE);
        if (selectedSegment != 0) selectSegment(0, false);

        String org = Objects.requireNonNull(sharedPreferences.getString("organisationName", "no_data")).replaceAll(" ", "");
        customLoader.show();
        stationIdToAreaId.clear();
        stationIdToName.clear();
        fullStationRows.clear();
        fullBicycleItems.clear();
        fullStationCycleItems.clear();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(org).child("Station");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    String sid = child.getKey();
                    if (sid == null) continue;
                    Object snameObj = child.child("stationName").getValue();
                    String sname = snameObj == null ? sid : String.valueOf(snameObj);
                    Object areaObj = child.child("areaId").getValue();
                    String areaIdStr = areaObj == null ? "" : String.valueOf(areaObj);
                    stationIdToAreaId.put(sid, areaIdStr);
                    stationIdToName.put(sid, sname);
                    fullStationRows.add(new SelectStationActivity.StationRow(sid, sname, areaIdStr));
                }
                customLoader.dismiss();
                applySearchFilter();
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
        invalidateOptionsMenu();
        title.setText("All Bicycle List");
        stationID = null;
        stationName = null;
        areaID = null;
        if (segmentRoot != null) segmentRoot.setVisibility(View.VISIBLE);
        if (searchContainer != null) searchContainer.setVisibility(View.VISIBLE);
        if (searchBox != null) searchBox.setText("");
        if (searchClear != null) searchClear.setVisibility(View.GONE);
        if (addBicycleButton != null) addBicycleButton.setVisibility(View.VISIBLE);
        if (selectedSegment != 1) selectSegment(1, false);

        fullStationRows.clear();
        fullBicycleItems.clear();
        fullStationCycleItems.clear();
        String org = Objects.requireNonNull(sharedPreferences.getString("organisationName", "no_data")).replaceAll(" ", "");
        customLoader.show();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(org).child("Bicycle");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullBicycleItems.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String bikeId = child.getKey();
                    if (bikeId == null) continue;
                    int percent = parseBatteryPercent(child.child("cyclebattery").getValue(), child.child("battery").getValue());
                    boolean isEBike = isEBike(child.child("type").getValue(), child.child("Type").getValue());
                    fullBicycleItems.add(new ManageCycleListAdapter.Item(bikeId, percent, isEBike));
                }
                customLoader.dismiss();
                applySearchFilter();
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
        invalidateOptionsMenu();
        title.setText(stationName == null || stationName.isEmpty() ? "Station Cycles" : stationName);
        if (segmentRoot != null) segmentRoot.setVisibility(View.GONE);
        if (searchContainer != null) searchContainer.setVisibility(View.VISIBLE);
        if (searchBox != null) {
            searchBox.setText("");
            searchBox.clearFocus();
            hideKeyboard(searchBox);
        }
        if (searchClear != null) searchClear.setVisibility(View.GONE);
        if (addBicycleButton != null) addBicycleButton.setVisibility(View.VISIBLE);

        fullStationRows.clear();
        fullBicycleItems.clear();
        fullStationCycleItems.clear();
        String org = Objects.requireNonNull(sharedPreferences.getString("organisationName", "no_data")).replaceAll(" ", "");
        customLoader.show();

        DatabaseReference cyclesRef = FirebaseDatabase.getInstance().getReference(org)
                .child("Station").child(stationId).child("cyclesList");
        DatabaseReference bicycleRef = FirebaseDatabase.getInstance().getReference(org).child("Bicycle");

        cyclesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot cyclesSnap) {
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
                        fullStationCycleItems.clear();
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
                            fullStationCycleItems.add(new ManageCycleListAdapter.Item(cid, percent, isEBike));
                        }

                        customLoader.dismiss();
                        applySearchFilter();
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
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SELECT_STATION_FOR_ADD && resultCode == RESULT_OK && data != null) {
            String sid = data.getStringExtra("stationId");
            String sname = data.getStringExtra("stationName");
            String aid = data.getStringExtra("areaId");
            if (sid != null && !sid.isEmpty()) {
                Intent intentAdd = new Intent(ManageBicycle.this, AddOrRemoveBicycle.class);
                intentAdd.putExtra("Status", "ADD");
                intentAdd.putExtra("StationId", sid);
                intentAdd.putExtra("StationName", sname != null ? sname : sid);
                intentAdd.putExtra("AreaId", aid != null ? aid : "");
                startActivity(intentAdd);
            }
        }
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