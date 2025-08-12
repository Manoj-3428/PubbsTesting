package in.pubbs.pubbsadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import in.pubbs.pubbsadmin.Model.Station;
import in.pubbs.pubbsadmin.View.CustomDivider;
import in.pubbs.pubbsadmin.View.CustomLoader;

public class ManageStation extends AppCompatActivity implements View.OnClickListener {
    String TAG = ManageStation.class.getSimpleName();
    SharedPreferences sharedPreferences;
    String organisationName;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference stationDbReference;
    private CustomLoader customLoader;//Loader
    ImageView back;
    TextView toolbarTitle;
    private RecyclerView recyclerView;
    private List<Station> stations = new ArrayList<>();
    private ArrayList list = new ArrayList<String>();
    private in.pubbs.pubbsadmin.Adapter.ManageStation manageStationAdapter;
    ArrayList<Map<String, Object>> arrayList = new ArrayList<>();
    private static String areaId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_station);
        initView();
    }

    private void initView() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        Objects.requireNonNull(getSupportActionBar()).setTitle("");
        toolbarTitle = findViewById(R.id.toolbar_title);
        toolbarTitle.setText("Stations");
        back = findViewById(R.id.back_button);
        back.setOnClickListener(this);
        sharedPreferences = getSharedPreferences(getResources().getString(R.string.sharedPreferences), Context.MODE_PRIVATE);
        organisationName = sharedPreferences.getString("organisationName", "no_data").replace(" ", "");
        Intent intent = getIntent();
        if (intent.getStringExtra("area_id") != null)
            areaId = intent.getStringExtra("area_id");
        Log.d(TAG, "Organisation Name:" + organisationName + "\t" + areaId);
        firebaseDatabase = FirebaseDatabase.getInstance();
        stationDbReference = firebaseDatabase.getReference().child(organisationName.replaceAll(" ", "")).child("Station");
        recyclerView = findViewById(R.id.recycler_view);
        //RecyclerView will show all the objects
        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addItemDecoration(new CustomDivider(this, LinearLayoutManager.VERTICAL, 8));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        customLoader = new CustomLoader(this, R.style.WideDialog);//Loader Definition
    }

    private void loadData() {
        customLoader.show();
        arrayList.clear();
        stationDbReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Log.d(TAG, "Area id:" + areaId);
                    Map<String, Object> stationMap = (Map<String, Object>) snapshot.getValue();
                    Log.d(TAG, "Map object area id :" + Objects.requireNonNull(stationMap).get("areaId"));
                    if (areaId.equals(stationMap.get("areaId"))) {
                        arrayList.add(stationMap);
                    }
                }
                Log.d(TAG, "station size:" + stations.size());
                manageStationAdapter = new in.pubbs.pubbsadmin.Adapter.ManageStation(arrayList);
                recyclerView.setAdapter(manageStationAdapter);
                customLoader.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, "Database Error:" + databaseError);
            }
        });
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(ManageStation.this, AreaDetails.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        stations.clear();
        loadData();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.back_button) {
            Intent intent = new Intent(ManageStation.this, AreaDetails.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }

//        switch (v.getId()) {
//            case R.id.back_button:
//                Intent intent = new Intent(ManageStation.this, AreaDetails.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                startActivity(intent);
//                break;
//            default:
//                break;
//        }
    }
}
