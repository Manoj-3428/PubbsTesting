package in.pubbs.pubbsadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import in.pubbs.pubbsadmin.Adapter.ManageEmployeeAdapter;
import in.pubbs.pubbsadmin.View.CustomLoader;

public class ManageEmployee extends AppCompatActivity implements View.OnClickListener {

    TextView title;
    ImageView back, add;
    Toolbar toolbar;
    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;
    ArrayList<Map<String, Object>> list = new ArrayList();
    ManageEmployeeAdapter manageEmployeeAdapter;
    DatabaseReference databaseReference, databaseReference1;
    String TAG = ManageEmployee.class.getSimpleName();
    SharedPreferences sharedPreferences;
    private ManageEmployeeAdapter manageEmployee;
    Map<String, Object> map = new HashMap<>();
    SwipeRefreshLayout swipeRefresh;
    private CustomLoader customLoader;
    private ConstraintLayout noDataFound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_employee);
        init();
    }

    private void init() {
        sharedPreferences = getSharedPreferences("pubbs", MODE_PRIVATE);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        title = findViewById(R.id.toolbar_title);
        title.setText("Manage Employee");
        back = findViewById(R.id.back_button);
        back.setOnClickListener(this);
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        add = findViewById(R.id.add_button);
        add.setVisibility(View.VISIBLE);
        add.setOnClickListener(this);
        recyclerView = findViewById(R.id.recycler_view);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);
        noDataFound = findViewById(R.id.no_data_found);
        customLoader = new CustomLoader(this, R.style.WideDialog);//Loader Definition
        customLoader.show();
        loadData();
        swipeRefresh.setOnRefreshListener(() -> {
            customLoader.show();
            loadData();
            swipeRefresh.setRefreshing(false);
        });
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.back_button) {
            finish();
        }
        else if (v.getId() == R.id.add_button) {
            showDialog("Create User", "Please select any one of the option and press proceed.");
        }

//        switch (v.getId()) {
//            case R.id.back_button:
//                finish();
//                break;
//            case R.id.add_button:
//                showDialog("Create User", "Please select any one of the option and press proceed.");
//                break;
//        }
    }

    private void loadData() {
        String path = sharedPreferences.getString("organisationName", "no_data").replace(" ", "");
        databaseReference = FirebaseDatabase.getInstance().getReference(Objects.requireNonNull(path));
        list.clear();
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot i : dataSnapshot.getChildren()) {
                    if (Objects.requireNonNull(i.getKey()).equals("AM") || i.getKey().equals("SM")) {
                        for (DataSnapshot j : i.getChildren()) {
                            Log.d(TAG, Objects.requireNonNull(j.getValue()).toString());
                            if (Objects.requireNonNull(j.child("operatorDesignation").getValue()).equals("Area Manager")) {
                                Log.d(TAG, Objects.requireNonNull(j.child("areaManagerName").getValue()).toString());
                                map = new HashMap<>();
                                map.put("Name", j.child("areaManagerName").getValue());
                                map.put("Mobile", j.child("areaManagerPhone").getValue());
                                map.put("Designation", j.child("operatorDesignation").getValue());
                                map.put("Status", j.child("active").getValue());
                                list.add(map);
                            } else if (Objects.requireNonNull(j.child("operatorDesignation").getValue()).equals("Service Manager")) {
                                map = new HashMap<>();
                                map.put("Name", j.child("serviceManagerName").getValue());
                                map.put("Mobile", j.child("serviceManagerPhone").getValue());
                                map.put("Designation", j.child("operatorDesignation").getValue());
                                map.put("Status", j.child("active").getValue());
                                list.add(map);
                            }
                        }
                    }
                }
                customLoader.dismiss();
                if (list.size() == 0) {
                    noDataFound.setVisibility(View.VISIBLE);
                } else {
                    manageEmployee = new ManageEmployeeAdapter(list, ManageEmployee.this);
                    recyclerView.setAdapter(manageEmployee);
                    manageEmployee.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    private void showDialog(String heading, String details) {
        TextView title, message;
        RadioGroup radioGroup;
        final Dialog dialog = new Dialog(ManageEmployee.this, R.style.WideDialog);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setContentView(R.layout.radio_button_dialog_loayout);
        dialog.setCancelable(true);
        title = dialog.findViewById(R.id.title);
        title.setText(heading);
        message = dialog.findViewById(R.id.message);
        message.setText(details);
        radioGroup = dialog.findViewById(R.id.radio_group);
        dialog.show();
        Intent intent = new Intent(ManageEmployee.this, ManageEmployeeDetails.class);
        intent.putExtra("action", "add");
        dialog.findViewById(R.id.proceed).setOnClickListener(v -> {
            int checkedId = radioGroup.getCheckedRadioButtonId();

            if (checkedId == R.id.am) {
                intent.putExtra("employee_type", "AM");
                startActivity(intent);
                dialog.dismiss();
            }
            else if (checkedId == R.id.sm) {
                intent.putExtra("employee_type", "SM");
                startActivity(intent);
                dialog.dismiss();
            }
            else {
                Toast.makeText(ManageEmployee.this, "Please choose any one of the above option.", Toast.LENGTH_SHORT).show();
            }

//            switch (radioGroup.getCheckedRadioButtonId()) {
//                case R.id.am:
//                    intent.putExtra("employee_type", "AM");
//                    startActivity(intent);
//                    dialog.dismiss();
//                    break;
//                case R.id.sm:
//                    intent.putExtra("employee_type", "SM");
//                    startActivity(intent);
//                    dialog.dismiss();
//                    break;
//                default:
//                    Toast.makeText(ManageEmployee.this,"Please choose any one of the above option.",Toast.LENGTH_SHORT).show();
//            }
        });
    }
}

