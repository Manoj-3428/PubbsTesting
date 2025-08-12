package in.pubbs.pubbsadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import in.pubbs.pubbsadmin.Model.AreaManager;
import in.pubbs.pubbsadmin.Model.ServiceManager;
import in.pubbs.pubbsadmin.View.CustomAlertDialog;
import in.pubbs.pubbsadmin.View.CustomLoader;

public class ManageEmployeeDetails extends AppCompatActivity implements View.OnClickListener {
    private String action, empType, TAG = ManageEmployeeDetails.class.getSimpleName();
    private static String showPath, type;
    private TextView heading, spinnerText, title;
    private Button button;
    private SharedPreferences sharedPreferences;
    private EditText name, mobile, email, password;
    private Spinner selectArea;
    private ArrayList<Map<String, Object>> list = new ArrayList<>();
    private ArrayList<String> areaNameList = new ArrayList<>();
    private ArrayAdapter<String> stringArrayAdapter;
    private AreaManager areaManager;
    private ServiceManager serviceManager;
    private ImageView backButton;
    private CardView cardView;
    private CheckBox status;
    private CustomLoader customLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_employee_details);
        init();
        loadArea();
        action = getIntent().getStringExtra("action");
        if (Objects.requireNonNull(action).equalsIgnoreCase("add")) {
            empType = getIntent().getStringExtra("employee_type");
            heading.setText("Please enter all the below details and press confirm.");
            button.setText("Confirm");
        } else if (action.equalsIgnoreCase("show")) {
            showPath = getIntent().getStringExtra("path");
            Log.d(TAG, "show path: " + showPath);
            status.setVisibility(View.VISIBLE);
            cardView.setElevation(getResources().getDimension(R.dimen.twenty_dp));
            button.setText("Confirm");
            name.setEnabled(false);
            mobile.setEnabled(false);
            email.setEnabled(false);
            password.setVisibility(View.INVISIBLE);
            customLoader = new CustomLoader(this, R.style.WideDialog);//Loader Definition
            customLoader.show();
            loadShowData(showPath);
        }
    }

    public void init() {
        title = findViewById(R.id.toolbar_title);
        title.setText("Manage Employee Details");
        heading = findViewById(R.id.heading);
        button = findViewById(R.id.button);
        button.setOnClickListener(this);
        backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(this);
        email = findViewById(R.id.email_id);
        name = findViewById(R.id.full_name);
        mobile = findViewById(R.id.mobile_no);
        password = findViewById(R.id.password);
        selectArea = findViewById(R.id.area_select);
        spinnerText = findViewById(R.id.area_select_text);
        cardView = findViewById(R.id.card_view);
        status = findViewById(R.id.status);
        sharedPreferences = getSharedPreferences("pubbs", Context.MODE_PRIVATE);
        areaManager = new AreaManager();
        serviceManager = new ServiceManager();
        selectArea.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItemText = (String) parent.getItemAtPosition(position);
                Log.d(TAG, "data: " + selectedItemText);
                spinnerText.setText(selectedItemText);
                areaManager.setAreaManagerAreaName(selectedItemText);
                areaManager.setAreaManagerAreaId(Objects.requireNonNull(list.get(position).get("id")).toString());
                serviceManager.setServiceManagerAreaId(Objects.requireNonNull(list.get(position).get("id")).toString());
                serviceManager.setServiceManagerAreaName(selectedItemText);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                spinnerText.setText("Please choose an area");
            }
        });
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button) {
            if (action.equalsIgnoreCase("show")) {
                if (button.getText().toString().equalsIgnoreCase("Confirm")) {
                    selectArea.setEnabled(false);
                    heading.setText("Please confirm all the details.");
                    button.setText("Done");
                }
                else if (button.getText().toString().equalsIgnoreCase("Done")) {
                    updateDataToDB();
                }
            }
            else if (action.equalsIgnoreCase("add")) {
                if (button.getText().toString().equalsIgnoreCase("Confirm")) {
                    if (checkForBlankField()) {
                        cardView.setElevation(getResources().getDimension(R.dimen.twenty_dp));
                        heading.setText("Please confirm all the details and press done to insert in database");
                        button.setText("Done");
                        name.setEnabled(false);
                        email.setEnabled(false);
                        password.setEnabled(false);
                        mobile.setEnabled(false);
                        selectArea.setEnabled(false);
                    }
                    else {
                        CustomAlertDialog alertDialog = new CustomAlertDialog(ManageEmployeeDetails.this, R.style.WideDialog, "Error!!", "All inputs have not been provided!");
                        alertDialog.show();
                        alertDialog.onPositiveButton(view -> alertDialog.dismiss());
                    }
                }
                else if (button.getText().toString().equalsIgnoreCase("Done")) {
                    insertDataToDB();
                }
            }
        }
        else if (v.getId() == R.id.back_button) {
            finish();
        }

//        switch (v.getId()) {
//            case R.id.button:
//                if (action.equalsIgnoreCase("show") && button.getText().toString().equalsIgnoreCase("Confirm")) {
//                    selectArea.setEnabled(false);
//                    heading.setText("Please confirm all the details.");
//                    button.setText("Done");
//                } else if (action.equalsIgnoreCase("show") && button.getText().toString().equalsIgnoreCase("Done")) {
//                    updateDataToDB();
//                } else if (action.equalsIgnoreCase("add") && button.getText().toString().equalsIgnoreCase("Confirm")) {
//                    if(checkForBlankField()){
//                        cardView.setElevation(getResources().getDimension(R.dimen.twenty_dp));
//                        heading.setText("Please confirm all the details and press done to insert in database");
//                        button.setText("Done");
//                        name.setEnabled(false);
//                        email.setEnabled(false);
//                        password.setEnabled(false);
//                        mobile.setEnabled(false);
//                        selectArea.setEnabled(false);
//                    }
//                    else{
//                        CustomAlertDialog alertDialog = new CustomAlertDialog(ManageEmployeeDetails.this,R.style.WideDialog,"Error!!","All inputs have not been provided!");
//                        alertDialog.show();
//                        alertDialog.onPositiveButton(view -> alertDialog.dismiss());
//                    }
//                } else if (action.equalsIgnoreCase("add") && button.getText().toString().equalsIgnoreCase("Done")) {
//                    insertDataToDB();
//                }
//                break;
//            case R.id.back_button:
//                finish();
//                break;
//        }
    }

    private Boolean checkForBlankField() {
        if (TextUtils.isEmpty(name.getText()))
            return false;
        if (TextUtils.isEmpty(email.getText()))
            return false;
        if (TextUtils.isEmpty(mobile.getText()))
            return false;
        if (TextUtils.isEmpty(password.getText()))
            return false;
        String txt = spinnerText.getText().toString();
        return !txt.equals("Please chose an area");
    }

    private void loadArea() {
        String path = Objects.requireNonNull(sharedPreferences.getString("organisationName", null)).replace(" ","") + "/Zone/" + Objects.requireNonNull(sharedPreferences.getString("zone", null)).toUpperCase() + "/AreaList";
        Log.d(TAG, "loadArea path: " + path);
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(path);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot i : dataSnapshot.getChildren()) {
                    Map<String, Object> map = (Map<String, Object>) i.getValue();
                    areaNameList.add(Objects.requireNonNull(Objects.requireNonNull(map).get("name")).toString());
                    list.add(map);
                }
                stringArrayAdapter = new ArrayAdapter<>(ManageEmployeeDetails.this, android.R.layout.simple_spinner_item, areaNameList);
                stringArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                selectArea.setAdapter(stringArrayAdapter);
                stringArrayAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void insertDataToDB() {
        if (action.equalsIgnoreCase("add") && checkForBlankField() && empType.equalsIgnoreCase("AM")) {
            String path = Objects.requireNonNull(sharedPreferences.getString("organisationName", null)).replace(" ","") + "/" + empType + "/" + mobile.getText();
            Log.d(TAG, "path: " + path);
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(path);
            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    String areaManagerKey = "PUBBS_" + sharedPreferences.getString("organisationName", "no_data").replace(" ", "") + "_AM_" + databaseReference.push().getKey();
                    areaManager.setAreaManagerName(name.getText().toString());
                    areaManager.setAreaManagerEmail(email.getText().toString());
                    areaManager.setAreaManagerPhone(mobile.getText().toString());
                    areaManager.setActive("true");
                    areaManager.setAreaManagerPassword(password.getText().toString());
                    areaManager.setAreaManagerOrganisation(sharedPreferences.getString("organisationName", "no_data").replace(" ", ""));
                    areaManager.setAreaManagerKey(areaManagerKey);
                    areaManager.setOperatorDesignation("Area Manager");
                    areaManager.setCreatedBy(sharedPreferences.getString("organisationName", "no_data").replace(" ", "") + "/ZM/" + sharedPreferences.getString("mobileValue", null));
                    //Inserting data in the DB
                    databaseReference.setValue(areaManager);
                    CustomAlertDialog dialog = new CustomAlertDialog(ManageEmployeeDetails.this, R.style.WideDialog, "User Created", "The user has been successfully created.");
                    dialog.show();
                    dialog.onPositiveButton(view -> {
                        dialog.dismiss();
                        String message = "Pubbs Credentials\nLogin Id: " + areaManager.getAreaManagerOrganisation() + "/AM/" + areaManager.getAreaManagerPhone() + "\nPassword: " + areaManager.getAreaManagerPassword();
                        sendSMS(areaManager.getAreaManagerPhone(), message);
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
        else if (action.equalsIgnoreCase("add") && checkForBlankField() && empType.equalsIgnoreCase("SM")) {
            String path = Objects.requireNonNull(sharedPreferences.getString("organisationName", null)).replace(" ","") + "/" + empType + "/" + mobile.getText();
            Log.d(TAG, "path: " + path);
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(path);
            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    String serviceManagerKey = "PUBBS_" + sharedPreferences.getString("organisationName", "no_data").replace(" ", "") + "_SM_" + databaseReference.push().getKey();
                    serviceManager.setServiceManagerName(name.getText().toString());
                    serviceManager.setServiceManagerEmail(email.getText().toString());
                    serviceManager.setServiceManagerPhone(mobile.getText().toString());
                    serviceManager.setActive("true");
                    serviceManager.setServiceManagerPassword(password.getText().toString());
                    serviceManager.setServiceManagerOrganisation(sharedPreferences.getString("organisationName", "no_data").replace(" ", ""));
                    serviceManager.setServiceManagerKey(serviceManagerKey);
                    serviceManager.setOperatorDesignation("Service Manager");
                    serviceManager.setCreatedBy(sharedPreferences.getString("organisationName", "no_data").replace(" ", "") + "/ZM/" + sharedPreferences.getString("mobileValue", null));
                    //Inserting data in the DB
                    databaseReference.setValue(serviceManager);
                    CustomAlertDialog dialog = new CustomAlertDialog(ManageEmployeeDetails.this, R.style.WideDialog, "User Created", "The user has been successfully created.");
                    dialog.show();
                    dialog.onPositiveButton(view -> {
                        dialog.dismiss();
                        String message = "Pubbs Credentials\nLogin Id: " + serviceManager.getServiceManagerOrganisation() + "/SM/" + serviceManager.getServiceManagerPhone() + "\nPassword: " + serviceManager.getServiceManagerPassword();
                        sendSMS(serviceManager.getServiceManagerPhone(), message);
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
        else{
            CustomAlertDialog dialog = new CustomAlertDialog(ManageEmployeeDetails.this, R.style.WideDialog, "Error!!", "Please provide input for all the fields.");
            dialog.show();
            dialog.onPositiveButton(view -> dialog.dismiss());
        }
    }

    public void sendSMS(String to, String message) {
        //ManageEmployeeDetails.this.finish();
        Intent intent = new Intent(ManageEmployeeDetails.this, ManageEmployee.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.fromParts("sms", to, null)).putExtra("sms_body", message));
    }

    private void loadShowData(String path) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(path);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "Show Snapshot: " + dataSnapshot.getValue());
                if (Objects.requireNonNull(dataSnapshot.child("operatorDesignation").getValue()).equals("Area Manager")) {
                    AreaManager areaManager = dataSnapshot.getValue(AreaManager.class);
                    name.setText(Objects.requireNonNull(areaManager).getAreaManagerName());
                    mobile.setText(areaManager.getAreaManagerPhone());
                    email.setText(areaManager.getAreaManagerEmail());
                    /*password.setText(areaManager.getAreaManagerPassword());
                    password.setInputType(InputType.TYPE_CLASS_TEXT);
                    password.setVisibility(View.GONE);*/
                    spinnerText.setText(areaManager.getAreaManagerAreaName());
                    type = "AM";
                } else if (Objects.requireNonNull(dataSnapshot.child("operatorDesignation").getValue()).equals("Service Manager")) {
                    ServiceManager serviceManager = dataSnapshot.getValue(ServiceManager.class);
                    name.setText(Objects.requireNonNull(serviceManager).getServiceManagerName());
                    mobile.setText(serviceManager.getServiceManagerPhone());
                    email.setText(serviceManager.getServiceManagerEmail());
                    /*password.setText(serviceManager.getServiceManagerPassword());
                    password.setInputType(InputType.TYPE_CLASS_TEXT);
                    password.setVisibility(View.GONE);*/
                    spinnerText.setText(serviceManager.getServiceManagerAreaName());
                    type = "SM";
                }
                if (Objects.requireNonNull(dataSnapshot.child("active").getValue()).equals("true")) {
                    status.setText("Deactivate");
                    status.setTextSize(14f);
                } else {
                    status.setText("Activate");
                    status.setTextSize(14f);
                }
                customLoader.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void updateDataToDB() {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(showPath);
        Map<String, Object> map = new HashMap<>();
        if (status.isChecked() && status.getText().equals("Activate"))
            map.put("active", "true");
        if (status.isChecked() && status.getText().equals("Deactivate"))
            map.put("active", "false");
        if (type.equals("AM")) {
            map.put("areaManagerAreaName", areaManager.getAreaManagerAreaName());
            map.put("areaManagerAreaId", areaManager.getAreaManagerAreaId());
        }
        if (type.equals("SM")) {
            map.put("serviceManagerAreaName", serviceManager.getServiceManagerAreaName());
            map.put("serviceManagerAreaId", serviceManager.getServiceManagerAreaId());
        }

        Log.d(TAG, "type: " + showPath.substring(showPath.length() - 2));
        databaseReference.updateChildren(map, (databaseError, databaseReference1) -> {
            CustomAlertDialog dialog = new CustomAlertDialog(ManageEmployeeDetails.this, R.style.WideDialog, "Data Updated", "The user data has been successfully updated.");
            dialog.show();
            dialog.onPositiveButton(view -> {
                dialog.dismiss();
                Intent intent = new Intent(ManageEmployeeDetails.this, ManageEmployee.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            });
        });
    }

}
