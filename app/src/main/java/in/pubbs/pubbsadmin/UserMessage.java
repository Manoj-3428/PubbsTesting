package in.pubbs.pubbsadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.SharedPreferences;
import android.os.Bundle;
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

import in.pubbs.pubbsadmin.Adapter.UserMessageAdapter;
import in.pubbs.pubbsadmin.View.CustomLoader;

/*Created by Souvik Datta*/
public class UserMessage extends AppCompatActivity {
    RecyclerView recyclerView;
    ConstraintLayout noData;
    RecyclerView.LayoutManager layoutManager;
    ArrayList arrayList = new ArrayList();
    TextView title;
    ImageView back;
    ArrayList<Map<String, Object>> list = new ArrayList();
    DatabaseReference databaseReference;
    String TAG = UserMessage.class.getSimpleName();
    UserMessageAdapter userMessageAdapter;
    private CustomLoader customLoader;//Loader
    SharedPreferences sharedPreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_message);
        init();
    }

    private void init() {
        sharedPreferences = getSharedPreferences("pubbs", MODE_PRIVATE);
        title = findViewById(R.id.toolbar_title);
        title.setText("Reported Bicycles");
        back = findViewById(R.id.back_button);
        back.setOnClickListener(v -> finish());
        recyclerView = findViewById(R.id.recycler_view);
        noData = findViewById(R.id.no_data_found);
        layoutManager = new LinearLayoutManager(UserMessage.this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        customLoader = new CustomLoader(this, R.style.WideDialog);
        customLoader.show();
        loadData();
    }

    //May require to implement onResume();
    private void loadData() {
        arrayList.clear();
        databaseReference = FirebaseDatabase.getInstance().getReference(Objects.requireNonNull(sharedPreferences.getString("organisationName", "no_data")).replaceAll(" ", "") + "/ReportCycle");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot i : dataSnapshot.getChildren()) {
                    Map<String, Object> areaMap = (Map<String, Object>) i.getValue();
                    arrayList.add(areaMap);
                }
                if (arrayList.size() == 0) {
                    noData.setVisibility(View.VISIBLE);
                } else {
                    userMessageAdapter = new UserMessageAdapter(arrayList, getSupportFragmentManager());
                    recyclerView.setAdapter(userMessageAdapter);
                    userMessageAdapter.notifyDataSetChanged();
                }
                customLoader.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }
}

