package in.pubbs.pubbsadmin;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import in.pubbs.pubbsadmin.View.CustomLoader;

public class SelectStationActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private View noData;
    private CustomLoader loader;
    private String org;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_station);

        SharedPreferences sp = getSharedPreferences("pubbs", Context.MODE_PRIVATE);
        org = Objects.requireNonNull(sp.getString("organisationName", "")).replaceAll(" ", "");

        ImageView back = findViewById(R.id.back_button);
        back.setOnClickListener(v -> finish());
        TextView title = findViewById(R.id.toolbar_title);
        title.setText("Select Station");

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        noData = findViewById(R.id.no_data_found);

        loader = new CustomLoader(this, R.style.WideDialog);
        loader.show();

        loadStations();
    }

    private void loadStations() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(org).child("Station");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<StationRow> rows = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String stationId = child.getKey();
                    String stationName = String.valueOf(child.child("stationName").getValue());
                    if (stationId != null) rows.add(new StationRow(stationId, stationName));
                }

                SelectStationAdapter adapter = new SelectStationAdapter(rows, stationRow -> {
                    Intent data = new Intent();
                    data.putExtra("stationId", stationRow.stationId);
                    setResult(RESULT_OK, data);
                    finish();
                });
                recyclerView.setAdapter(adapter);

                loader.dismiss();
                noData.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loader.dismiss();
                noData.setVisibility(View.VISIBLE);
            }
        });
    }

    static class StationRow {
        final String stationId;
        final String stationName;
        StationRow(String stationId, String stationName) {
            this.stationId = stationId;
            this.stationName = stationName;
        }
    }
}

