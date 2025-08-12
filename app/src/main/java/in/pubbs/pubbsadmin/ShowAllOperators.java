package in.pubbs.pubbsadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Objects;

import in.pubbs.pubbsadmin.Adapter.ShowAllOperatorsAdapter;
import in.pubbs.pubbsadmin.View.CustomLoader;

public class ShowAllOperators extends AppCompatActivity {
    private SharedPreferences sharedPreferences;
    private String TAG = ShowAllOperators.class.getSimpleName();
    private ArrayList operatorList = new ArrayList();
    private LinearLayout noDataFound;
    private RecyclerView recyclerView;
    private TextView heading;
    private CustomLoader customLoader;
    private SwipeRefreshLayout swipeRefresh;
    private ImageView back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_all_operators);
        init();
        loadAllOperators();
    }

    private void init() {
        sharedPreferences = getSharedPreferences("pubbs", Context.MODE_PRIVATE);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        noDataFound = findViewById(R.id.no_data_found);
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        heading = findViewById(R.id.toolbar_title);
        heading.setText("Select Operator");
        back = findViewById(R.id.back_button);
        customLoader = new CustomLoader(this, R.style.WideDialog);//Loader Definition
        customLoader.show();
        swipeRefresh.setOnRefreshListener(() -> {
            loadAllOperators();
            swipeRefresh.setRefreshing(false);
        });
        back.setOnClickListener(v -> {
            Intent intent = new Intent(ShowAllOperators.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(ShowAllOperators.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void loadAllOperators() {
        operatorList.clear();
        String path = "SuperAdmin/" + sharedPreferences.getString("mobileValue", "") + "/OperatorList";
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(path);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot i : dataSnapshot.getChildren()) {
                    Log.d(TAG, "Operator: " + i.child("operatorOrganisation").getValue());
                    operatorList.add(Objects.requireNonNull(i.child("operatorOrganisation").getValue()).toString());
                }
                if (operatorList.size() == 0) {
                    noDataFound.setVisibility(View.VISIBLE);
                } else {
                    ShowAllOperatorsAdapter obj = new ShowAllOperatorsAdapter(operatorList, ShowAllOperators.this);
                    recyclerView.setAdapter(obj);
                    obj.notifyDataSetChanged();
                }
                customLoader.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

}
