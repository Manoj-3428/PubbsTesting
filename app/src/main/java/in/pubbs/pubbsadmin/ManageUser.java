package in.pubbs.pubbsadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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

import in.pubbs.pubbsadmin.Adapter.ManageUserAdapter;
import in.pubbs.pubbsadmin.View.CustomLoader;

/*Created by Souvik Datta*/
public class ManageUser extends AppCompatActivity implements View.OnClickListener {

    TextView title;
    ImageView back;
    Toolbar toolbar;
    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;
    ArrayList<Map<String, Object>> list = new ArrayList();
    ManageUserAdapter manageUserAdapter;
    DatabaseReference databaseReference;
    String TAG = ManageUser.class.getSimpleName();
    SharedPreferences sharedPreferences;
    private CustomLoader customLoader;//Loader
    ConstraintLayout noData;
    SwipeRefreshLayout swipeRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_user);
        init();
    }

    private void init() {
        sharedPreferences = getSharedPreferences("pubbs", MODE_PRIVATE);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        title = findViewById(R.id.toolbar_title);
        title.setText("Manage User");
        back = findViewById(R.id.back_button);
        back.setOnClickListener(this);
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        recyclerView = findViewById(R.id.recycler_view);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);
        customLoader = new CustomLoader(this, R.style.WideDialog);
        customLoader.show();
        noData = findViewById(R.id.no_data_found);
        loadData();
        swipeRefresh.setOnRefreshListener(() -> {
            customLoader.show();
            //simulateProgressUpdate();
            loadData();
            swipeRefresh.setRefreshing(false);
        });
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.back_button) {
            finish();
        }

//        switch (v.getId()) {
//            case R.id.back_button:
//                finish();
//                break;
//        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.manage_user_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.message_list) {
            startActivity(new Intent(ManageUser.this, UserMessage.class));
        }

//        switch (item.getItemId()) {
//            case R.id.message_list:
//                startActivity(new Intent(ManageUser.this, UserMessage.class));
//                break;
//        }
        return true;
    }

    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }

    private void loadData() {
        list.clear();
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot i : dataSnapshot.getChildren()) {
                    Log.d(TAG, "Phone Number: " + i.child("mobile").getValue() + "Name: " + i.child("name").getValue());
                    Map<String, Object> data = (Map<String, Object>) i.getValue();
                    if (i.child("operator").exists() && Objects.requireNonNull(i.child("operator").getValue()).toString().trim().equalsIgnoreCase(Objects.requireNonNull(sharedPreferences.getString("organisationName", null)).replaceAll(" ", ""))) {
                        list.add(data);
                        //noData.setVisibility(View.GONE);
                    } /*else {
                        //noData.setVisibility(View.VISIBLE);
                    }*/
                }
                if(list.size()==0){
                    noData.setVisibility(View.VISIBLE);
                }else {
                    manageUserAdapter = new ManageUserAdapter(list, ManageUser.this);
                    recyclerView.setAdapter(manageUserAdapter);
                    manageUserAdapter.notifyDataSetChanged();
                }
                customLoader.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
