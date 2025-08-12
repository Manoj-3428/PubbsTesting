package in.pubbs.pubbsadmin;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import in.pubbs.pubbsadmin.Adapter.InventoryLockDetailsAdapter;
import in.pubbs.pubbsadmin.Model.Lock;

public class InventoryLockDetails extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    public List<Lock> dataSet;
    ImageView back_button;
    String lockType;
    TextView title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory_lock_details);
        init();

    }

    private void init() {
        dataSet = (ArrayList<Lock>) getIntent().getSerializableExtra("dataSet");
        lockType = getIntent().getStringExtra("lockType");
        title = findViewById(R.id.toolbar_title);
        title.setText(lockType);
        back_button = findViewById(R.id.back_button);
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        // use a linear layout manager
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        mAdapter = new InventoryLockDetailsAdapter(dataSet, lockType);
        recyclerView.setAdapter(mAdapter);
        back_button.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAdapter.notifyDataSetChanged();
    }
}
