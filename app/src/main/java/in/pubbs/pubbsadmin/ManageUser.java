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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
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
    ArrayList<Map<String, Object>> filteredList = new ArrayList();
    ManageUserAdapter manageUserAdapter;
    DatabaseReference databaseReference;
    String TAG = ManageUser.class.getSimpleName();
    SharedPreferences sharedPreferences;
    private CustomLoader customLoader;//Loader
    ConstraintLayout noData;
    SwipeRefreshLayout swipeRefresh;
    EditText searchEditText;
    ImageView clearButton;

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
        
        // Initialize adapter immediately to prevent "No adapter attached" error
        manageUserAdapter = new ManageUserAdapter(filteredList, ManageUser.this);
        recyclerView.setAdapter(manageUserAdapter);
        
        customLoader = new CustomLoader(this, R.style.WideDialog);
        customLoader.show();
        noData = findViewById(R.id.no_data_found);
        searchEditText = findViewById(R.id.search_edit_text);
        clearButton = findViewById(R.id.clear_button);
        setupSearchFilter();
        loadData();
        swipeRefresh.setOnRefreshListener(() -> {
            customLoader.show();
            //simulateProgressUpdate();
            loadData();
            swipeRefresh.setRefreshing(false);
        });
    }
    
    private void setupSearchFilter() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString());
                // Show/hide clear button based on text
                if (s.length() > 0) {
                    clearButton.setVisibility(View.VISIBLE);
                } else {
                    clearButton.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        
        // Clear button click listener
        clearButton.setOnClickListener(v -> {
            searchEditText.setText("");
            clearButton.setVisibility(View.GONE);
            // Hide keyboard
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
            }
            searchEditText.clearFocus();
        });
        
        // Handle keyboard done/search action
        searchEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEARCH) {
                    // Hide keyboard
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
                    }
                    // Remove focus from search field
                    searchEditText.clearFocus();
                    return true;
                }
                return false;
            }
        });
    }
    
    private void filterUsers(String searchText) {
        filteredList.clear();
        
        if (searchText.isEmpty()) {
            filteredList.addAll(list);
        } else {
            String searchLower = searchText.toLowerCase().trim();
            for (Map<String, Object> user : list) {
                String name = user.get("name") != null ? user.get("name").toString().toLowerCase() : "";
                String mobile = user.get("mobile") != null ? user.get("mobile").toString().toLowerCase() : "";
                
                if (name.contains(searchLower) || mobile.contains(searchLower)) {
                    filteredList.add(user);
                }
            }
        }
        
        updateRecyclerView();
    }
    
    private void updateRecyclerView() {
        if (filteredList.size() == 0) {
            noData.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            noData.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
        
        // Always update adapter's data (adapter is already attached)
        if (manageUserAdapter != null) {
            manageUserAdapter.updateList(filteredList);
        }
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
        String organisationName = Objects.requireNonNull(sharedPreferences.getString("organisationName", null));
        if (organisationName == null || organisationName.isEmpty()) {
            customLoader.dismiss();
            noData.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            return;
        }
        
        String orgNameFilter = organisationName.replaceAll(" ", "");
        Log.d(TAG, "Loading users for organisation: " + orgNameFilter);
        
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long startTime = System.currentTimeMillis();
                int count = 0;
                
                for (DataSnapshot i : dataSnapshot.getChildren()) {
                    // Quick check if operator exists before processing
                    if (i.child("operator").exists()) {
                        String operator = i.child("operator").getValue() != null ? 
                            i.child("operator").getValue().toString().trim() : "";
                        
                        if (operator.equalsIgnoreCase(orgNameFilter)) {
                            try {
                                Map<String, Object> data = (Map<String, Object>) i.getValue();
                                if (data != null) {
                                    list.add(data);
                                    count++;
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing user data: " + e.getMessage());
                            }
                        }
                    }
                }
                
                long endTime = System.currentTimeMillis();
                Log.d(TAG, "Loaded " + count + " users in " + (endTime - startTime) + "ms");
                
                // Update filtered list with all data
                filteredList.clear();
                filteredList.addAll(list);
                
                // Update recyclerview
                updateRecyclerView();
                customLoader.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error loading users: " + databaseError.getMessage());
                customLoader.dismiss();
                noData.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        });
    }
}
