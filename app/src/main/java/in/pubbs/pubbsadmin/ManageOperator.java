package in.pubbs.pubbsadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
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
import java.util.Objects;

import in.pubbs.pubbsadmin.Model.Operator;
import in.pubbs.pubbsadmin.View.CustomDivider;
import in.pubbs.pubbsadmin.View.CustomLoader;

import static android.view.View.GONE;

/*Created by: Parita Dey*/
public class ManageOperator extends AppCompatActivity implements View.OnClickListener {
    ImageView back, add;
    TextView manageOperator;
    private RecyclerView recyclerView;
    private in.pubbs.pubbsadmin.Adapter.ManageOperator manageOperatorAdapter;
    private List<Operator> operators = new ArrayList<>();
    DatabaseReference databaseOperatorReference;
    private String TAG = ManageOperator.class.getSimpleName();
    private CustomLoader customLoader;
    ConstraintLayout noData;
    SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_operator);
        initView();

    }

    private void initView() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        Objects.requireNonNull(getSupportActionBar()).setTitle("");
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        databaseOperatorReference = FirebaseDatabase.getInstance().getReference("SuperAdmin/9433944708/OperatorList");
        back = findViewById(R.id.back_button);
        add = findViewById(R.id.add_button);
        manageOperator = findViewById(R.id.manage_operator);
        back.setOnClickListener(this);
        add.setOnClickListener(this);
        recyclerView = findViewById(R.id.recycler_view);
        //RecyclerView will show all the objects
        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addItemDecoration(new CustomDivider(this, LinearLayoutManager.VERTICAL, 8));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        customLoader = new CustomLoader(this, R.style.WideDialog);//Loader
        noData = findViewById(R.id.no_data_found);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            operators.clear();
            loadData();
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    public void loadData() {//Added By Souvik, solving repeated occurrence in the list
        customLoader.show();
        operators.clear();
        databaseOperatorReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getValue() == null) {
                    customLoader.dismiss();
                    recyclerView.setVisibility(GONE);
                    noData.setVisibility(View.VISIBLE);
                } else {
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        Operator operator = dataSnapshot.getValue(Operator.class);
                        operators.add(operator);
                    }
                    manageOperatorAdapter = new in.pubbs.pubbsadmin.Adapter.ManageOperator(operators);
                    recyclerView.setAdapter(manageOperatorAdapter);
                    customLoader.dismiss();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, "Database Error:" + databaseError);
            }
        });
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(ManageOperator.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.back_button) {
            Intent intent = new Intent(ManageOperator.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
        else if (view.getId() == R.id.add_button) {
            startActivity(new Intent(ManageOperator.this, AddOperator.class));
        }
        else {
            // Default case - No action needed
        }

//        switch (view.getId()) {
//            case R.id.back_button:
//                Intent intent = new Intent(ManageOperator.this, MainActivity.class);
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                startActivity(intent);
//                break;
//            case R.id.add_button:
//                startActivity(new Intent(ManageOperator.this, AddOperator.class));
//                break;
//            default:
//                break;
//        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        operators.clear();
        loadData();
    }
}
