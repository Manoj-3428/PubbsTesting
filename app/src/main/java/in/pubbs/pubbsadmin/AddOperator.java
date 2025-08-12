package in.pubbs.pubbsadmin;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import in.pubbs.pubbsadmin.R;


import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

import in.pubbs.pubbsadmin.Model.Operator;

/*Created by: Parita Dey*/
public class AddOperator extends AppCompatActivity implements View.OnClickListener {

    ImageView back;
    Button addOperator, confirm, done;
    EditText organisationName, fullName, mobileNo, emailId, password;
    String superAdmin, operatorOrganisation, operatorName, operatorMobile, operatorEmail, operatorKey, operatorPassword;
    TextView organisationTv, ownerTv, mobileTv, emailTv, operatorKeyTv, newOperatorOrg, newOperatorName, newOperatorMobile, newOperatorEmail, newOperatorKey;
    FrameLayout addOperatorContainer, detailOperatorContainer, operatorAddedContainer;
    private String TAG = AddOperator.class.getSimpleName();
    FirebaseDatabase databaseOperator, databaseSuperAdmin;
    DatabaseReference databaseReference, databaseOperatorReference;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_operator);
        initView();
    }

    private void initView() {
        sharedPreferences = getSharedPreferences(getResources().getString(R.string.sharedPreferences), Context.MODE_PRIVATE);
        superAdmin = sharedPreferences.getString("mobileValue", null);
        Log.d(TAG, "Super Admin:" + superAdmin);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        Objects.requireNonNull(getSupportActionBar()).setTitle("");
        back = findViewById(R.id.back_button);
        back.setOnClickListener(this);
        organisationName = findViewById(R.id.organisation_name);
        fullName = findViewById(R.id.full_name);
        mobileNo = findViewById(R.id.mobile_no);
        emailId = findViewById(R.id.email_id);
        //city = findViewById(R.id.city);
        addOperatorContainer = findViewById(R.id.add_operator_container);
        detailOperatorContainer = findViewById(R.id.detail_operator_container);
        addOperator = findViewById(R.id.add_operator_button);
        addOperator.setOnClickListener(this);
        organisationTv = findViewById(R.id.organisation_name_tv);
        ownerTv = findViewById(R.id.owner_name_tv);
        mobileTv = findViewById(R.id.mobile_number_tv);
        emailTv = findViewById(R.id.email_id_tv);
        //cityTv = findViewById(R.id.city_tv);
        operatorKeyTv = findViewById(R.id.operator_key_tv);
        password = findViewById(R.id.password);
        confirm = findViewById(R.id.confirm_button);
        confirm.setOnClickListener(this);
        operatorAddedContainer = findViewById(R.id.operator_added_container);
        done = findViewById(R.id.done_button);
        done.setOnClickListener(this);
        newOperatorOrg = findViewById(R.id.new_organisation_name_tv);
        newOperatorName = findViewById(R.id.new_owner_name_tv);
        newOperatorMobile = findViewById(R.id.new_mobile_number_tv);
        newOperatorEmail = findViewById(R.id.new_email_id_tv);
        //newOperatorCity = findViewById(R.id.new_city_tv);
        newOperatorKey = findViewById(R.id.new_operator_key_tv);

        databaseOperator = FirebaseDatabase.getInstance();
        databaseReference = databaseOperator.getReference("SuperAdmin/9433944708/OperatorList");//.child(superAdmin); // create a node under the superadmin for creating a new operator in firebase
        databaseSuperAdmin = FirebaseDatabase.getInstance();
        //databaseOperatorReference = databaseSuperAdmin.getReference("Operators");//With respect to the new database design
        databaseOperatorReference = databaseSuperAdmin.getReference();// create an other node name as 'Operators' which will stores all the details of the operators in firebase
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(AddOperator.this, ManageOperator.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.back_button) {
            Intent intent = new Intent(AddOperator.this, ManageOperator.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
        else if (view.getId() == R.id.add_operator_button) {
            if (organisationName.getText().toString().trim().isEmpty() || fullName.getText().toString().trim().isEmpty()
                    || mobileNo.getText().toString().trim().isEmpty() || emailId.getText().toString().trim().isEmpty()) {
                if (organisationName.getText().toString().trim().isEmpty() && fullName.getText().toString().trim().isEmpty()
                        && mobileNo.getText().toString().trim().isEmpty() && emailId.getText().toString().trim().isEmpty()) {
                    organisationName.setError("Enter Organisation Name");
                    fullName.setError("Enter Operator Name");
                    mobileNo.setError("Enter Mobile Number");
                    emailId.setError("Enter Email ID");
                }
                else if (organisationName.getText().toString().trim().isEmpty()) {
                    organisationName.setError("Enter Organisation Name");
                }
                else if (fullName.getText().toString().trim().isEmpty()) {
                    fullName.setError("Enter Operator Name");
                }
                else if (mobileNo.getText().toString().trim().isEmpty()) {
                    mobileNo.setError("Enter Mobile Number");
                }
                else if (emailId.getText().toString().trim().isEmpty()) {
                    emailId.setError("Enter Email ID");
                }
            }
            else {
                operatorOrganisation = organisationName.getText().toString().trim();
                operatorName = fullName.getText().toString().trim();
                operatorMobile = mobileNo.getText().toString().trim();
                operatorEmail = emailId.getText().toString().trim();
                addOperatorContainer.setVisibility(View.GONE);
                detailOperatorContainer.setVisibility(View.VISIBLE);
                Log.d(TAG, "Add Operator holds data:" + operatorOrganisation + "\t" + operatorName + "\t" + operatorMobile + "\t" + operatorEmail);
                organisationTv.setText("Organisation: " + operatorOrganisation);
                ownerTv.setText("Owner: " + operatorName);
                mobileTv.setText("Mobile: " + operatorMobile);
                emailTv.setText("Email: " + operatorEmail);
                operatorKey = "PUBBS_" + operatorOrganisation.replaceAll(" ", "") + "_RM_" + databaseReference.push().getKey();
                Log.d(TAG, "Operator key:" + operatorKey);
                operatorKeyTv.setText(operatorKey);
            }
        }
        else if (view.getId() == R.id.confirm_button) {
            if (password.getText().toString().isEmpty()) {
                password.setError("Enter default password");
            }
            else {
                operatorPassword = password.getText().toString().trim();
                addOperator(operatorOrganisation, operatorName, operatorMobile, operatorEmail, operatorKey, operatorPassword);
            }
        }
        else if (view.getId() == R.id.done_button) {
            sendSms(operatorMobile, operatorPassword);
        }
        else {
            // Optional: Handle unknown view clicks if needed
        }

//        switch (view.getId()) {
//            case R.id.back_button:
//                Intent intent = new Intent(AddOperator.this, ManageOperator.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                startActivity(intent);
//                break;
//            case R.id.add_operator_button:
//                if (organisationName.getText().toString().trim().isEmpty() || fullName.getText().toString().trim().isEmpty()
//                        || mobileNo.getText().toString().trim().isEmpty() || emailId.getText().toString().trim().isEmpty()
//                    /*|| city.getText().toString().trim().isEmpty()*/) {
//                    if (organisationName.getText().toString().trim().isEmpty() && fullName.getText().toString().trim().isEmpty()
//                            && mobileNo.getText().toString().trim().isEmpty() && emailId.getText().toString().trim().isEmpty()
//                        /*&& city.getText().toString().trim().isEmpty()*/) {
//                        organisationName.setError("Enter Organisation Name");
//                        fullName.setError("Enter Operator Name");
//                        mobileNo.setError("Enter Mobile Number");
//                        emailId.setError("Enter Email ID");
//                        /*city.setError("Enter City");*/
//                    } else if (organisationName.getText().toString().trim().isEmpty()) {
//                        organisationName.setError("Enter Organisation Name");
//                    } else if (fullName.getText().toString().trim().isEmpty()) {
//                        fullName.setError("Enter Operator Name");
//                    } else if (mobileNo.getText().toString().trim().isEmpty()) {
//                        mobileNo.setError("Enter Mobile Number");
//                    } else if (emailId.getText().toString().trim().isEmpty()) {
//                        emailId.setError("Enter Email ID");
//                    } /*else if (city.getText().toString().trim().isEmpty()) {
//                        city.setError("Enter City");
//                    }*/
//                } else {
//                    operatorOrganisation = organisationName.getText().toString().trim();
//                    operatorName = fullName.getText().toString().trim();
//                    operatorMobile = mobileNo.getText().toString().trim();
//                    operatorEmail = emailId.getText().toString().trim();
//                    //operatorCity = city.getText().toString().trim();
//                    addOperatorContainer.setVisibility(View.GONE);
//                    detailOperatorContainer.setVisibility(View.VISIBLE);
//                    Log.d(TAG, "Add Operator holds data:" + operatorOrganisation + "\t" + operatorName + "\t" + operatorMobile + "\t" + operatorEmail /*+ "\t" + operatorCity*/);
//                    organisationTv.setText("Organisation: " + operatorOrganisation);
//                    ownerTv.setText("Owner: " + operatorName);
//                    mobileTv.setText("Mobile: " + operatorMobile);
//                    emailTv.setText("Email: " + operatorEmail);
//                    //cityTv.setText("City: " + operatorCity);
//                    operatorKey = "PUBBS_" + operatorOrganisation.replaceAll(" ", "") + "_RM_" + databaseReference.push().getKey();//Key Modified
//                    Log.d(TAG, "Operator key:" + operatorKey);
//                    operatorKeyTv.setText(operatorKey);
//                }
//                break;
//            case R.id.confirm_button:
//                if (password.getText().toString().isEmpty()) {
//                    password.setError("Enter default password");
//                } else {
//                    operatorPassword = password.getText().toString().trim();
//                    addOperator(operatorOrganisation, operatorName, operatorMobile, operatorEmail,/* operatorCity,*/ operatorKey, operatorPassword);
//                }
//                break;
//            case R.id.done_button:
//                sendSms(operatorMobile, operatorPassword);
//                break;
//            default:
//                break;
//        }
    }

    private void addOperator(String operatorOrganisation, String operatorName, String operatorMobile, String operatorEmail, /*String operatorCity,*/ String operatorKey, String operatorPassword) {
        Log.d(TAG, "Operator details: " + operatorOrganisation + "\t" + operatorName + "\t" + operatorMobile + "\t" + operatorEmail + "\t" + /*operatorCity + "\t" +*/ operatorKey + "\t" + operatorPassword);
        //Operator class has two constructors, which will call when a new operator is added by super_admin or PUBBS; operatorList will create a node under SuperAdmin and operator will create a node under Operators
        //Please see the structure in firebase
        final Operator operator = new Operator(operatorOrganisation, operatorName, operatorMobile, operatorEmail, /*operatorCity,*/ operatorKey, operatorPassword);
        final Operator operatorList = new Operator(operatorOrganisation, operatorName, operatorMobile, operatorEmail, operatorKey);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child(operatorList.getOperatorMobile()).exists()) {
                    Log.d(TAG, "Operator already exists");
                } else {
                    databaseReference.child(operator.getOperatorMobile()).setValue(operatorList);
                    Log.d(TAG, "Thank you for inserting an operator");
                    detailOperatorContainer.setVisibility(View.GONE);
                    operatorAddedContainer.setVisibility(View.VISIBLE);
                    newOperatorOrg.setText(operatorOrganisation);
                    newOperatorName.setText(operatorName);
                    newOperatorMobile.setText(operatorMobile);
                    newOperatorEmail.setText(operatorEmail);
                    //newOperatorCity.setText(operatorCity);
                    newOperatorKey.setText(operatorKey);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, "error:" + databaseError);
            }
        });
        databaseOperatorReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child(operator.getOperatorMobile()).exists()) {
                    Log.d(TAG, "Operator already exists");
                } else {
                    databaseOperatorReference
                            .child(operator.getOperatorOrganisation().replaceAll(" ", ""))
                            .child("RM")
                            .child(operator.getOperatorMobile())
                            .setValue(operator);
                    Log.d(TAG, "Thank you for inserting an Operators node");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, "error:" + databaseError);
            }
        });

    }

    public void sendSms(String adminmobile, String password) {
        String msg = "Your Login id for PubbsAdmin application :" + adminmobile + "\n" + "Password:" + password;
        Log.d(TAG, "Message: " + msg);
        Intent smsIntent = new Intent(Intent.ACTION_VIEW);

        smsIntent.setData(Uri.parse("smsto:"));
        smsIntent.setType("vnd.android-dir/mms-sms");
        smsIntent.putExtra("address", adminmobile);
        smsIntent.putExtra("sms_body", msg);

        try {
            startActivity(smsIntent);
            finish();
            Log.d("Finished sending SMS...", "");
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(AddOperator.this, "SMS faild, please try again later.", Toast.LENGTH_SHORT).show();
        }
    }

}
