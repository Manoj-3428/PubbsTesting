package in.pubbs.pubbsadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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

import in.pubbs.pubbsadmin.Model.Area;
import in.pubbs.pubbsadmin.View.CustomDivider;
import in.pubbs.pubbsadmin.View.CustomLoader;

public class ManageArea extends AppCompatActivity implements View.OnClickListener {
    ImageView back, add;
    TextView manageArea;
    private RecyclerView recyclerView;
    private String TAG = ManageArea.class.getSimpleName();
    DatabaseReference manageAreaDbReference;
    private List<Area> area = new ArrayList<>();
    private ArrayList list = new ArrayList<String>();
    String organisationName;
    SharedPreferences sharedPreferences;
    private in.pubbs.pubbsadmin.Adapter.ManageArea manageAreaAdapter;
    ArrayList<Map<String, Object>> arrayList = new ArrayList<>();
    ConstraintLayout noData;
    SwipeRefreshLayout swipeRefresh;
    private CustomLoader customLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_area);
        initView();
    }

    private void initView() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        Objects.requireNonNull(getSupportActionBar()).setTitle("");
        swipeRefresh = findViewById(R.id.swipe_refresh);
        back = findViewById(R.id.back_button);
        add = findViewById(R.id.add_button);
        manageArea = findViewById(R.id.title);
        manageArea.setText("Manage Area");
        back.setOnClickListener(this);
        add.setOnClickListener(this);
        sharedPreferences = getSharedPreferences(getResources().getString(R.string.sharedPreferences), Context.MODE_PRIVATE);
        organisationName = sharedPreferences.getString("organisationName", "no_data").replace(" ", "");
        Log.d(TAG, "Organisation Name:" + organisationName);
        //database path in firebase
        manageAreaDbReference = FirebaseDatabase.getInstance().getReference(organisationName.replaceAll(" ", "") + "/Area");
        recyclerView = findViewById(R.id.recycler_view);
        //RecyclerView will show all the objects
        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addItemDecoration(new CustomDivider(this, LinearLayoutManager.VERTICAL, 2));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        customLoader = new CustomLoader(this, R.style.WideDialog);//Loader
        customLoader.show();
        noData = findViewById(R.id.no_data_found);
        swipeRefresh.setOnRefreshListener(() -> {
            arrayList.clear();
            loadData();
            swipeRefresh.setRefreshing(false);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        arrayList.clear();
        loadData();
    }

    public void loadData() {
        customLoader.show();
        arrayList.clear();
        manageAreaDbReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getValue() == null) {
                    customLoader.dismiss();
                    recyclerView.setVisibility(View.GONE);
                    noData.setVisibility(View.VISIBLE);
                } else {
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        if (!Objects.requireNonNull(dataSnapshot.getKey()).contains("StationList")) {
                            Map<String, Object> areaMap = (Map<String, Object>) dataSnapshot.getValue();
                            arrayList.add(areaMap);
                        }
                    }
                    Log.d(TAG, "area size:" + arrayList.size());
                    manageAreaAdapter = new in.pubbs.pubbsadmin.Adapter.ManageArea(arrayList);
                    recyclerView.setAdapter(manageAreaAdapter);
                    customLoader.dismiss();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, "Database error: " + databaseError.toString());
            }
        });

    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(ManageArea.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onClick(View view) {

        if (view.getId() == R.id.back_button) {
            Intent intent = new Intent(ManageArea.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
        else if (view.getId() == R.id.add_button) {
            startActivity(new Intent(ManageArea.this, AddArea.class));
        }

//        switch (view.getId()) {
//            case R.id.back_button:
//                Intent intent = new Intent(ManageArea.this, MainActivity.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                startActivity(intent);
//                break;
//            case R.id.add_button:
//                startActivity(new Intent(ManageArea.this, AddArea.class));
//                break;
//            default:
//                break;
//        }
    }
}
