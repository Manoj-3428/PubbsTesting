package in.pubbs.pubbsadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import in.pubbs.pubbsadmin.Model.Area;
import in.pubbs.pubbsadmin.View.CustomDivider;
import in.pubbs.pubbsadmin.View.CustomLoader;

public class ViewPanel extends AppCompatActivity {
    TextView title;
    ImageView back;
    Toolbar toolbar;
    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;
    private String TAG = ViewPanel.class.getSimpleName();
    String organisationName;
    SharedPreferences sharedPreferences;
    ConstraintLayout noData;
    DatabaseReference manageAreaDbReference;
    in.pubbs.pubbsadmin.Adapter.ViewPanel viewPanel;
    ArrayList<Map<String, Object>> arrayList = new ArrayList<>();
    private List<Area> areaList = new ArrayList<>();
    ArrayList areaLists;
    CustomLoader customLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_panel);
        initView();
    }

    private void initView() {
        sharedPreferences = getSharedPreferences(getResources().getString(R.string.sharedPreferences), Context.MODE_PRIVATE);
        title = findViewById(R.id.toolbar_title);
        title.setText("Area List");
        back = findViewById(R.id.back_button);
        toolbar = findViewById(R.id.toolbar);
        noData = findViewById(R.id.no_data_found);
        toolbar.setTitle("");
        manageAreaDbReference = FirebaseDatabase.getInstance().getReference(Objects.requireNonNull(sharedPreferences.getString("organisationName", "no_data")).replaceAll(" ", "") + "/Zone");
        back.setOnClickListener(v -> {
            startActivity(new Intent(ViewPanel.this, MainActivity.class));
            finish();
        });
        recyclerView = findViewById(R.id.recycler_view);
        //RecyclerView will show all the objects
        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addItemDecoration(new CustomDivider(this, LinearLayoutManager.VERTICAL, 8));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        areaLists = new ArrayList();
        customLoader = new CustomLoader(this, R.style.WideDialog);//Loader Definition
        loadData();
    }

    private void loadData() {
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
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) { //first getting all the data present in Zone node in firebase
                        for (DataSnapshot i : dataSnapshot.getChildren()) {
                            if (!i.getKey().contains("OperatorList")) { //getting all the areas present under Zone/Kharagpur/AreaList, just excluding OperatorList
                                for (DataSnapshot j : i.getChildren()) { // getting the details of area under AreaList node
                                    Map<String, Object> areaMap = (Map<String, Object>) j.getValue(); // getting the values inside the AreaList node
                                    arrayList.add(areaMap);
                                }
                            }
                        }
                    }
                    Log.d(TAG, "area size:" + arrayList.size() + "\t" + Arrays.toString(arrayList.toArray()));
                    viewPanel = new in.pubbs.pubbsadmin.Adapter.ViewPanel(arrayList, getSupportFragmentManager());
                    recyclerView.setAdapter(viewPanel);
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
