package in.pubbs.pubbsadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
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
import java.util.Map;
import java.util.Objects;

import in.pubbs.pubbsadmin.Adapter.ShowMyAreaAdapter;
import in.pubbs.pubbsadmin.View.CustomLoader;

public class ShowMyArea extends AppCompatActivity {
    TextView title;
    ImageView back;
    Toolbar toolbar;
    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;
    ShowMyAreaAdapter showMyAreaAdapter;
    private String TAG = ManageSubscription.class.getSimpleName();
    String organisationName, mobileValue;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    ConstraintLayout noData;
    DatabaseReference manageAreaDbReference;
    ArrayList subscriptionList;
    ArrayList<Map<String, Object>> arrayList = new ArrayList<>();
    private CustomLoader customLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_my_area);
        initView();
    }

    private void initView() {
        sharedPreferences = getSharedPreferences(getResources().getString(R.string.sharedPreferences), Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        organisationName = sharedPreferences.getString("organisationName", "no_data").replace(" ", "");
        mobileValue = sharedPreferences.getString("mobileValue", null);
        Log.d(TAG, "Organisation and mobile: " + organisationName + "\t" + mobileValue);
        recyclerView = findViewById(R.id.recycler_view);
        title = findViewById(R.id.toolbar_title);
        title.setText("Manage Areas");
        back = findViewById(R.id.back_button);
        toolbar = findViewById(R.id.toolbar);
        noData = findViewById(R.id.no_data_found);
        toolbar.setTitle("");
        back.setOnClickListener(v -> {
            Intent intent = new Intent(ShowMyArea.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);
        subscriptionList = new ArrayList();
        customLoader = new CustomLoader(this, R.style.WideDialog);//Loader Definition
        manageAreaDbReference = FirebaseDatabase.getInstance().getReference(organisationName.replaceAll(" ", "") + "/Area");
        loadData();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(ShowMyArea.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void loadData() {
        customLoader.show();
        arrayList.clear();
        manageAreaDbReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getValue() == null) {
                    recyclerView.setVisibility(View.GONE);
                    customLoader.dismiss();
                    noData.setVisibility(View.VISIBLE);
                } else if (Objects.requireNonNull(sharedPreferences.getString("admin_id", "no_data")).equalsIgnoreCase("Zone Manager")) {
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        if (!Objects.requireNonNull(dataSnapshot.getKey()).contains("StationList")) {
                            if (Objects.requireNonNull(dataSnapshot.child("createdBy").getValue()).equals(mobileValue)) {
                                Map<String, Object> areaMap = (Map<String, Object>) dataSnapshot.getValue();
                                arrayList.add(areaMap);
                            }
                        }
                    }
                    Log.d(TAG, "area size:" + arrayList.size());
                    showMyAreaAdapter = new in.pubbs.pubbsadmin.Adapter.ShowMyAreaAdapter(arrayList, getApplicationContext());
                    recyclerView.setAdapter(showMyAreaAdapter);
                    customLoader.dismiss();
                } else if (Objects.requireNonNull(sharedPreferences.getString("admin_id", null)).equalsIgnoreCase("Regional Manager")) {
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        if (!Objects.requireNonNull(dataSnapshot.getKey()).contains("StationList")) {
                            Map<String, Object> areaMap = (Map<String, Object>) dataSnapshot.getValue();
                            arrayList.add(areaMap);
                        }
                    }
                    Log.d(TAG, "area size:" + arrayList.size());
                    showMyAreaAdapter = new in.pubbs.pubbsadmin.Adapter.ShowMyAreaAdapter(arrayList, getApplicationContext());
                    recyclerView.setAdapter(showMyAreaAdapter);
                    customLoader.dismiss();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, "Database error: " + databaseError.toString());
            }
        });
    }


}
