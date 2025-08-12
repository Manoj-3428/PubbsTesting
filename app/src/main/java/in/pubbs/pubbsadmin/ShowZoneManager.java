package in.pubbs.pubbsadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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

import in.pubbs.pubbsadmin.Adapter.ZoneManagerAdapter;
import in.pubbs.pubbsadmin.Model.ZoneManager;
import in.pubbs.pubbsadmin.View.CustomLoader;

public class ShowZoneManager extends AppCompatActivity implements View.OnClickListener {
    TextView title;
    ImageView back, addButton;
    RecyclerView recyclerView;
    DatabaseReference databaseReference;
    String operatorName;
    SharedPreferences sharedPreferences;
    ArrayList<ZoneManager> zoneManagerList;
    ZoneManagerAdapter mAdapter;
    private CustomLoader customLoader;
    private String TAG = ShowZoneManager.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_zone_manager);
        init();
    }

    private void init() {
        back = findViewById(R.id.back_button);
        back.setOnClickListener(this);
        addButton = findViewById(R.id.add_button);
        addButton.setVisibility(View.VISIBLE);
        addButton.setOnClickListener(this);
        title = findViewById(R.id.toolbar_title);
        title.setText("Zone Manager");
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        sharedPreferences = getSharedPreferences(getResources().getString(R.string.sharedPreferences), Context.MODE_PRIVATE);
        operatorName = sharedPreferences.getString("organisationName", "no_data").replace(" ", "");
        operatorName = operatorName.replaceAll(" ", "");
        zoneManagerList = new ArrayList<>();
        customLoader = new CustomLoader(this, R.style.WideDialog);//Loader
    }

    public void loadData() {
        customLoader.show();
        zoneManagerList.clear();
        databaseReference = FirebaseDatabase.getInstance()
                .getReference().child(operatorName.replaceAll(" ", ""))
                .child("ZM");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot i : dataSnapshot.getChildren()) {
                    ZoneManager zoneManager = i.getValue(ZoneManager.class);
                    zoneManagerList.add(zoneManager);
                }
                if (zoneManagerList.size() == 0) {
                    customLoader.dismiss();
                    recyclerView.setVisibility(View.GONE);
                    findViewById(R.id.no_data_found).setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    findViewById(R.id.no_data_found).setVisibility(View.GONE);
                    mAdapter = new ZoneManagerAdapter(zoneManagerList,ShowZoneManager.this);
                    recyclerView.setAdapter(mAdapter);
                    mAdapter.activateUser(view -> {
                        Log.d("Clicked Activate User", "data " + zoneManagerList.get(mAdapter.getPosition()).getZoneManagerPhone());
                        databaseReference.child(zoneManagerList.get(mAdapter.getPosition()).getZoneManagerPhone()).child("active").setValue(true);
                    });
                    mAdapter.deactivateUser(view -> {
                        Log.d("Clicked Deactivate User", "data " + zoneManagerList.get(mAdapter.getPosition()).getZoneManagerPhone());
                        databaseReference.child(zoneManagerList.get(mAdapter.getPosition()).getZoneManagerPhone()).child("active").setValue(false);
                    });
                    customLoader.dismiss();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.back_button) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else if (v.getId() == R.id.add_button) {
            Intent intent = new Intent(this, AddZoneManager.class);
            startActivity(intent);
        }

//        switch (v.getId()) {
//            case R.id.back_button:
//                startActivity(new Intent(this, MainActivity.class));
//                finish();
//                break;
//            case R.id.add_button:
//                Intent intent = new Intent(this, AddZoneManager.class);
//                startActivity(intent);
//                break;
//        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }
}
