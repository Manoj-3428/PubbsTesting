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
import java.util.List;
import java.util.Map;

import in.pubbs.pubbsadmin.Adapter.ManageSubscriptionAdapter;
import in.pubbs.pubbsadmin.Model.SubscriptionList;
import in.pubbs.pubbsadmin.View.CustomLoader;

public class ManageSubscription extends AppCompatActivity {
    TextView title;
    ImageView back;
    Toolbar toolbar;
    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;
    ManageSubscriptionAdapter manageSubscriptionAdapter;
    private String TAG = ManageSubscription.class.getSimpleName();
    private List<SubscriptionList> subscriptionLists = new ArrayList<>();
    String organisationName;
    SharedPreferences sharedPreferences;
    ConstraintLayout noData;
    ArrayList subscriptionList;
    ArrayList<Map<String, Object>> arrayList = new ArrayList<>();
    private CustomLoader customLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_subscription);
        initView();
    }

    private void initView() {
        sharedPreferences = getSharedPreferences(getResources().getString(R.string.sharedPreferences), Context.MODE_PRIVATE);
        recyclerView = findViewById(R.id.recycler_view);
        title = findViewById(R.id.toolbar_title);
        title.setText("Manage Subscription");
        back = findViewById(R.id.back_button);
        toolbar = findViewById(R.id.toolbar);
        noData = findViewById(R.id.no_data_found);
        toolbar.setTitle("");
        back.setOnClickListener(v -> {
            startActivity(new Intent(ManageSubscription.this, MainActivity.class));
            finish();
        });
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);
        subscriptionList = new ArrayList();
        customLoader = new CustomLoader(this, R.style.WideDialog);//Loader
        loadData();
    }

    private void loadData() {
        customLoader.show();
        DatabaseReference manageSubscriptionReference;
        String path;
        path = sharedPreferences.getString("organisationName", "no_data").replace(" ", "")  + "/Subscription";
        Log.d(TAG, "path: " + path);
        manageSubscriptionReference = FirebaseDatabase.getInstance().getReference(path);
        manageSubscriptionReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot i : dataSnapshot.getChildren()) {
                    Map<String, Object> subscriptionMap = (Map<String, Object>) i.getValue();
                    arrayList.add(subscriptionMap);
                }
                if (arrayList.size() == 0) {
                    noData.setVisibility(View.VISIBLE);
                    customLoader.dismiss();
                } else {
                    Log.d(TAG, "subscription size:" + arrayList.size());
                    manageSubscriptionAdapter = new in.pubbs.pubbsadmin.Adapter.ManageSubscriptionAdapter(arrayList, getSupportFragmentManager());
                    recyclerView.setAdapter(manageSubscriptionAdapter);
                    customLoader.dismiss();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, "Error: " + databaseError);
            }
        });
    }
}
