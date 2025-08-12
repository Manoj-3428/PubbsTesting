package in.pubbs.pubbsadmin;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.tabs.TabLayout;

import in.pubbs.pubbsadmin.Adapter.TabAdapter;

public class ManageUserDetails extends AppCompatActivity {
    Toolbar toolbar;
    TabLayout tabLayout;
    ViewPager viewPager;
    TextView id, phone, title;
    ImageView back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_user_details);
        init();
    }

    private void init() {
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        title = findViewById(R.id.toolbar_title);
        title.setText("ID: " + getIntent().getStringExtra("id"));

        id = findViewById(R.id.user_id);
        id.setText("Name: " + getIntent().getStringExtra("user_name"));

        phone = findViewById(R.id.user_phone);
        phone.setText("Phone: " + getIntent().getStringExtra("mobile"));

        back = findViewById(R.id.back_button);
        back.setOnClickListener(v -> finish());

        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);

        TabAdapter tabAdapter = new TabAdapter(getSupportFragmentManager());
        tabAdapter.addFragment(new TripFragment(), "Trips");
        tabAdapter.addFragment(new TransactionFragment(), "Transaction");

        viewPager.setAdapter(tabAdapter);
        tabLayout.setupWithViewPager(viewPager);

    }

    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }
}
