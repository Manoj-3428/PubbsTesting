package in.pubbs.pubbsadmin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

import in.pubbs.pubbsadmin.Adapter.ShowAllAdsAdapter;
import in.pubbs.pubbsadmin.Model.DiscountDetails;

public class ShowAllAds extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private DatabaseReference databaseReference;
    private SharedPreferences sharedPreferences;
    private ArrayList<DiscountDetails> list;
    private TextView title;
    private ImageView back;
    private ShowAllAdsAdapter adsAdapter;
    public Button removeAds;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_all_ads);
        init();
    }

    private void init() {
        sharedPreferences = getSharedPreferences(getResources().getString(R.string.sharedPreferences), MODE_PRIVATE);
        recyclerView = findViewById(R.id.recycler_view);
        layoutManager = new GridLayoutManager(ShowAllAds.this, 2, RecyclerView.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);
        list = new ArrayList<>();
        title = findViewById(R.id.toolbar_title);
        title.setText("Posted Ads");
        back = findViewById(R.id.back_button);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        removeAds = findViewById(R.id.remove_ads);
        removeAds.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ShowAllAds.this, "Removal process initiated.", Toast.LENGTH_LONG).show();
                ArrayList<String> selectedList = adsAdapter.getSelectedId();
                String path = sharedPreferences.getString("organisationName", "no_data").replace(" ", "") + "/DiscountDetails";
                databaseReference = FirebaseDatabase.getInstance().getReference(path);
                for (String s : selectedList) {
                    databaseReference.child(s).removeValue();
                    list.remove(s);
                }
                removeAds.setVisibility(View.INVISIBLE);
                startActivity(new Intent(ShowAllAds.this, ManageAds.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
        });
        loadData();
    }

    private void loadData() {
        String path = sharedPreferences.getString("organisationName", "no_data").replace(" ", "") + "/DiscountDetails";
        databaseReference = FirebaseDatabase.getInstance().getReference(path);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    DiscountDetails obj = ds.getValue(DiscountDetails.class);
                    obj.setId(ds.getKey());
                    list.add(obj);
                }
                adsAdapter = new ShowAllAdsAdapter(list, ShowAllAds.this);
                recyclerView.setAdapter(adsAdapter);
                adsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onBackPressed() {
        if (adsAdapter.isLongClicked()) {
            adsAdapter.updateUi();
        } else {
            finish();
        }
    }
}
