package in.pubbs.pubbsadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import in.pubbs.pubbsadmin.Model.Zone;
import in.pubbs.pubbsadmin.Model.ZoneManager;
import in.pubbs.pubbsadmin.View.CustomAlertDialog;

public class AddZoneManager extends AppCompatActivity implements View.OnClickListener, ValueEventListener {

    TextView title, organisation_tv, name_tv, mobile_tv, email_tv, key_tv, city_tv;
    ImageView back;
    EditText name, email, phone, organisation, password, authPassword, city;
    Button addManager, confirm, done;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference, zoneDbReference;
    CustomAlertDialog cur;
    SmsManager smsManager;
    String organisationName, employeeID;
    SharedPreferences sharedPreferences;
    String key, passwordKey, parent, TAG = AddZoneManager.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_zone_manager);
        init();
    }

    private void init() {
        sharedPreferences = getSharedPreferences(getResources().getString(R.string.sharedPreferences), Context.MODE_PRIVATE);
        organisationName = sharedPreferences.getString("organisationName", "no_data").replace(" ", "");
        passwordKey = sharedPreferences.getString("passwordValue", null);
        parent = sharedPreferences.getString("mobileValue", null);
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference()
                .child(organisationName.replaceAll(" ", "")).child("ZM");
        zoneDbReference = firebaseDatabase.getReference()
                .child(organisationName.replaceAll(" ", "")).child("Zone");
        key = "PUBBS_" + organisationName.replaceAll(" ", "") + "_ZM_" + databaseReference.push().getKey();
        title = findViewById(R.id.toolbar_title);
        title.setText("Add Zone Manager");
        back = findViewById(R.id.back_button);
        back.setOnClickListener(this);
        name = findViewById(R.id.zone_manager_name);
        email = findViewById(R.id.email_id);
        phone = findViewById(R.id.mobile_no);
        city = findViewById(R.id.zone_manager_area);
        addManager = findViewById(R.id.add_zone_manger_button);
        addManager.setOnClickListener(this);
        password = findViewById(R.id.password);
        authPassword = findViewById(R.id.auth_password);
        organisation = findViewById(R.id.organisation_name);
        organisation.setText(organisationName);
        confirm = findViewById(R.id.confirm_button);
        confirm.setOnClickListener(this);
        done = findViewById(R.id.done_button);
        done.setOnClickListener(this);
        cur = new CustomAlertDialog(this, R.style.WideDialog, "Success!", "Zone Manager is added successfully");
        cur.onPositiveButton(view -> {
            smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phone.getText().toString(), null,
                    "Congratulations you are now a Zone Manager Login Credentials: " + organisationName
                            + "/ZM/" + phone + " Password: " + password.getText(), null, null);
            cur.dismiss();
        });
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.back_button) {
            finish();
        }
        else if (v.getId() == R.id.add_zone_manger_button) {
            if (criteriaCheck()) {
                findViewById(R.id.add_zone_manager_container).setVisibility(View.GONE);
                findViewById(R.id.detail_zone_manager_container).setVisibility(View.VISIBLE);
                appendData(R.id.organisation_name_tv,
                        R.id.zone_manager_name_tv,
                        R.id.email_id_tv,
                        R.id.mobile_number_tv,
                        R.id.zone_manager_key_tv,
                        R.id.zone_manager_city);
            }
        }
        else if (v.getId() == R.id.confirm_button) {
            if (!passwordKey.equals(authPassword.getText().toString())) {
                authPassword.setError("Authorization Password is wrong");
            }
            else {
                findViewById(R.id.detail_zone_manager_container).setVisibility(View.GONE);
                findViewById(R.id.zone_manager_added_container).setVisibility(View.VISIBLE);
                appendData(R.id.new_organisation_name_tv,
                        R.id.new_zone_manager_name_tv,
                        R.id.new_email_id_tv,
                        R.id.new_mobile_number_tv,
                        R.id.new_zone_manager_key_tv,
                        R.id.new_city_tv);
                databaseReference.addListenerForSingleValueEvent(this);
                zoneDbReference.addListenerForSingleValueEvent(this);
            }
        }
        else if (v.getId() == R.id.done_button) {
            String path = organisationName + "/ZM/" + phone.getText().toString();
            sendSms(phone.getText().toString(), password.getText().toString(), path);
            finish();
        }

//        switch (v.getId()) {
//            case R.id.back_button:
//                finish();
//                break;
//            case R.id.add_zone_manger_button:
//                if (criteriaCheck()) {
//                    //databaseReference.addListenerForSingleValueEvent(this);
//                    findViewById(R.id.add_zone_manager_container).setVisibility(View.GONE);
//                    findViewById(R.id.detail_zone_manager_container).setVisibility(View.VISIBLE);
//                    appendData(R.id.organisation_name_tv,
//                            R.id.zone_manager_name_tv,
//                            R.id.email_id_tv,
//                            R.id.mobile_number_tv,
//                            R.id.zone_manager_key_tv,
//                            R.id.zone_manager_city);
//                }
//                break;
//            case R.id.confirm_button:
//                if (!passwordKey.equals(authPassword.getText().toString())) {
//                    authPassword.setError("Authorization Password is wrong");
//                } else {
//                    findViewById(R.id.detail_zone_manager_container).setVisibility(View.GONE);
//                    findViewById(R.id.zone_manager_added_container).setVisibility(View.VISIBLE);
//                    appendData(R.id.new_organisation_name_tv,
//                            R.id.new_zone_manager_name_tv,
//                            R.id.new_email_id_tv,
//                            R.id.new_mobile_number_tv,
//                            R.id.new_zone_manager_key_tv,
//                            R.id.new_city_tv);
//                    databaseReference.addListenerForSingleValueEvent(this);
//                    zoneDbReference.addListenerForSingleValueEvent(this);
//                }
//                break;
//            case R.id.done_button:
//                /*smsManager = SmsManager.getDefault();
//                smsManager.sendTextMessage(phone.getText().toString(), null,
//                        "Congratulations you are now a Zone Manager Login Credentials: " + organisationName
//                                + "/ZM/" + phone + " Password: " + password.getText(), null, null);
//                Toast.makeText(this, "Alert sent to Zone Manger", Toast.LENGTH_LONG).show();
//                */
//                String path = organisationName + "/ZM/" + phone.getText().toString();
//                sendSms(phone.getText().toString(), password.getText().toString(), path);
//                finish();
//                break;
//        }
    }

    private void appendData(int operator, int name_id, int email_id, int mobile_id, int key_id, int city_id) {
        organisation_tv = findViewById(operator);
        name_tv = findViewById(name_id);
        email_tv = findViewById(email_id);
        mobile_tv = findViewById(mobile_id);
        key_tv = findViewById(key_id);
        city_tv = findViewById(city_id);
        organisation_tv.append(" : " + organisation.getText());
        name_tv.setText("Name : " + name.getText());
        email_tv.append(" : " + email.getText());
        mobile_tv.append(" : " + phone.getText());
        key_tv.setText(key);
        city_tv.append(" : " + city.getText());
    }

    private boolean criteriaCheck() {
        if (TextUtils.isEmpty(name.getText())) {
            name.setError(getResources().getString(R.string.empty_field_message));
            return false;
        }
        if (TextUtils.isEmpty(email.getText())) {
            email.setError(getResources().getString(R.string.empty_field_message));
            return false;
        }
        if (TextUtils.isEmpty(phone.getText())) {
            phone.setError(getResources().getString(R.string.empty_field_message));
            return false;
        }
        if (TextUtils.isEmpty(password.getText())) {
            password.setError(getResources().getString(R.string.empty_field_message));
            return false;
        }
        if (TextUtils.isEmpty(city.getText())) {
            city.setError(getResources().getString(R.string.empty_field_message));
            return false;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
        //employeeID = "PUBBS_ZM"+empID.getText()+databaseReference.push();
        //employeeID = "PUBBS_ZM"+databaseReference.push().getKey();
        Log.d(TAG, "Parent: " + parent);
        ZoneManager zoneManager = new ZoneManager(name.getText().toString(),
                email.getText().toString(), phone.getText().toString()
                , key, password.getText().toString(), organisationName, organisationName + "/RM/" + parent, city.getText().toString(), true);
        databaseReference.child(zoneManager.getZoneManagerPhone()).setValue(zoneManager);
        Zone zone = new Zone(zoneManager.getZoneManagerPhone());
        zoneDbReference
                .child(zoneManager.getZoneManagerCity().toUpperCase())
                .child("OperatorList")
                .setValue(zone);
    }

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {
        cur.setTitle("Failure!");
        cur.setMsg("Zone Manager could not be added to the database");
        cur.show();
    }

    public void sendSms(String mobile, String password, String path) {
        String msg = "Your Login id for PubbsAdmin application :" + path + "\n" + "Password:" + password;
        Log.d(TAG, "Message: " + msg);
        Intent smsIntent = new Intent(Intent.ACTION_VIEW);

        smsIntent.setData(Uri.parse("smsto:"));
        smsIntent.setType("vnd.android-dir/mms-sms");
        smsIntent.putExtra("address", mobile);
        smsIntent.putExtra("sms_body", msg);

        try {
            startActivity(smsIntent);
            finish();
            Log.d("Finished sending SMS...", "");
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "SMS failed, please try again later.", Toast.LENGTH_SHORT).show();
        }
    }
}
