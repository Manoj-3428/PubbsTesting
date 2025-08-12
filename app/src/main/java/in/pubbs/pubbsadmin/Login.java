package in.pubbs.pubbsadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

import in.pubbs.pubbsadmin.BottomSheet.BottomsheetFragmentFAQ;
import in.pubbs.pubbsadmin.Model.AreaManager;
import in.pubbs.pubbsadmin.Model.Operator;
import in.pubbs.pubbsadmin.Model.ServiceManager;
import in.pubbs.pubbsadmin.Model.SuperAdmin;
import in.pubbs.pubbsadmin.Model.ZoneManager;
import in.pubbs.pubbsadmin.View.CustomAlertDialog;
import in.pubbs.pubbsadmin.View.CustomLoader;

/*Created by: Parita Dey*/
@SuppressWarnings("IfCanBeSwitch")
public class Login extends AppCompatActivity {
    Button login;
    EditText password, mobile;
    String mobileValue, passwordValue, admin_id;
    FirebaseDatabase databaseUser;
    DatabaseReference databaseReference, dbReference;
    private String TAG = Login.class.getSimpleName();
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    Intent intent;
    private CustomLoader customLoader;
    ImageView faq;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initView();
    }

    private void initView() {
        faq = findViewById(R.id.faq);
        databaseUser = FirebaseDatabase.getInstance();
        databaseReference = databaseUser.getReference("SuperAdmin");

        sharedPreferences = getSharedPreferences(getResources().getString(R.string.sharedPreferences), Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        intent = new Intent(Login.this, MainActivity.class);
        customLoader = new CustomLoader(this, R.style.WideDialog);

        login = findViewById(R.id.log_in_button);
        password = findViewById(R.id.password);
        mobile = findViewById(R.id.mobile_no);
        login.setOnClickListener(view -> {
            if (mobile.getText().toString().trim().isEmpty() || password.getText().toString().trim().isEmpty()) {
                if (mobile.getText().toString().trim().isEmpty() && password.getText().toString().trim().isEmpty()) {
                    mobile.setError("Enter Credentials");
                    password.setError("Enter Password");
                } else if (mobile.getText().toString().trim().isEmpty()) {
                    mobile.setError("Enter Credentials");
                } else if (password.getText().toString().trim().isEmpty()) {
                    password.setError("Enter Password");
                }
            } else {
                customLoader.show();
                mobileValue = mobile.getText().toString().trim();
                passwordValue = password.getText().toString().trim();
                addUser(mobileValue, passwordValue);
                authenticationCheck();//Created By Souvik
            }
        });
        faq.setOnClickListener(v -> {
            BottomsheetFragmentFAQ bottomsheetFragment = new BottomsheetFragmentFAQ();
            bottomsheetFragment.show(getSupportFragmentManager(), "dialog");
        });
    }

    private void addUser(String mobileValue, String passwordValue) {
        admin_id = "PUBBS" + databaseReference.push().getKey();
        final SuperAdmin superAdmin = new SuperAdmin(admin_id, mobileValue, passwordValue);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child(superAdmin.getMobile()).exists()) {
                    Log.d(TAG, "Super Admin already exists");
                    if (!mobileValue.isEmpty()) {
                        SuperAdmin superAdmin = dataSnapshot.child(mobileValue).getValue(SuperAdmin.class);
                        if (Objects.requireNonNull(superAdmin).getPassword().equals(passwordValue)) {
                            Log.d(TAG, "Password matches");
                            editor.putString("admin_id", admin_id);
                            editor.putString("mobileValue", mobileValue);
                            editor.putString("passwordValue", passwordValue);
                            editor.putBoolean("login", true);
                            editor.commit();
                            startActivity(new Intent(Login.this, MainActivity.class));
                            finish();
                        }
                    } else {
                        alertDialog("Sing In message", "Username and Password is incorrect");
                    }
                } else {
                    if (mobileValue.equals("9433944708")) {
                        databaseReference.child(superAdmin.getMobile()).setValue(superAdmin);
                        Log.d(TAG, "thank you for signing up");
                        alertDialog("Sign up message", "Thank you for sign up.");
                    } else {
                        alertSuperAdminDialog("Sign up message", "Please talk to Super Admin");

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, "error:" + databaseError);

            }
        });
    }

    private void alertDialog(String title, String message) {
        final CustomAlertDialog dialog = new CustomAlertDialog(this,
                R.style.WideDialog, title, message);
        dialog.show();
        dialog.onPositiveButton(view -> {
            dialog.dismiss();
            if (!message.contains("Username and Password is incorrect")) {
                startActivity(new Intent(Login.this, MainActivity.class));
                finish();
            }
        });
    }

    private void alertSuperAdminDialog(String title, String message) {
        final CustomAlertDialog dialog = new CustomAlertDialog(this,
                R.style.WideDialog, title, message);
        dialog.show();
        dialog.onPositiveButton(view -> {
            dialog.dismiss();
            // System.exit(1);
        });
    }

    private void authenticationCheck() {
        dbReference = FirebaseDatabase.getInstance().getReference(mobileValue);
        dbReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    Log.d(TAG, mobileValue.substring(0, mobileValue.indexOf('/')));
                    if (mobileValue.substring(0, mobileValue.indexOf('/')).equals("SuperAdmin")) {
                        String password = (String) dataSnapshot.child("password").getValue();
                        if (passwordValue.equalsIgnoreCase(password)) {
                            customLoader.dismiss();//Loader dismissed
                            Log.d(TAG, "Password matches");
                            editor.putString("admin_id", "PUBBS");
                            editor.putString("mobileValue", mobileValue.substring(11));
                            editor.putString("passwordValue", passwordValue);
                            editor.putBoolean("login", true);
                            editor.commit();
                            startActivity(intent);
                            finish();

                        } else {
                            //Wrong Password
                            customLoader.dismiss();//Loader dismissed
                            alertSuperAdminDialog("Wrong credentials or password!!", "Please contact support developer for assistance");
                        }
                    } else {
                        Log.d(TAG, mobileValue.substring(mobileValue.indexOf('/') + 1, mobileValue.lastIndexOf('/')));
                        String employeeType = mobileValue.substring(mobileValue.indexOf('/') + 1, mobileValue.lastIndexOf('/'));
                        if (employeeType.equals("RM")) {
                            Operator operator = dataSnapshot.getValue(Operator.class);
                            if (passwordValue.equalsIgnoreCase(Objects.requireNonNull(dataSnapshot.child("operatorPassword").getValue()).toString())) {
                                customLoader.dismiss();//Loader dismissed
                                Log.d(TAG, " Designation: " + Objects.requireNonNull(dataSnapshot.child("operatorDesignation").getValue()).toString());
                                editor.putString("admin_id", Objects.requireNonNull(dataSnapshot.child("operatorDesignation").getValue()).toString());
                                editor.putString("mobileValue", Objects.requireNonNull(dataSnapshot.child("operatorMobile").getValue()).toString());
                                editor.putString("organisationName", Objects.requireNonNull(dataSnapshot.child("operatorOrganisation").getValue()).toString());
                                editor.putString("passwordValue", passwordValue);
                                editor.putBoolean("login", true);
                                editor.commit();
                                startActivity(intent);
                                finish();
                            } else {
                                //Wrong Password
                                customLoader.dismiss();//Loader dismissed
                                alertSuperAdminDialog("Wrong Credentials or Password!!", "Please contact Customer Support for assistance");
                            }
                        } else if (employeeType.equals("ZM")) {
                            ZoneManager zoneManager = dataSnapshot.getValue(ZoneManager.class);
                            if (passwordValue.equalsIgnoreCase(Objects.requireNonNull(dataSnapshot.child("zoneManagerPassword").getValue()).toString()) && Objects.requireNonNull(zoneManager).getActive() == true) {
                                customLoader.dismiss();//Loader dismissed
                                editor.putString("admin_id", Objects.requireNonNull(dataSnapshot.child("operatorDesignation").getValue()).toString());
                                editor.putString("mobileValue", Objects.requireNonNull(dataSnapshot.child("zoneManagerPhone").getValue()).toString());
                                editor.putString("organisationName", Objects.requireNonNull(dataSnapshot.child("zoneManagerOrganisation").getValue()).toString());
                                editor.putString("passwordValue", passwordValue);
                                editor.putString("zone", Objects.requireNonNull(dataSnapshot.child("zoneManagerCity").getValue()).toString());
                                editor.putBoolean("login", true);
                                editor.commit();
                                startActivity(intent);
                                finish();
                            } else {
                                //wrong password
                                customLoader.dismiss();//Loader dismissed
                                alertSuperAdminDialog("Wrong credentials or password!!", "Please contact Customer Support for assistance");
                            }

                        } else if (employeeType.equals("AM")) {
                            AreaManager areaManager = dataSnapshot.getValue(AreaManager.class);
                            if (passwordValue.equalsIgnoreCase(Objects.requireNonNull(areaManager).getAreaManagerPassword()) && Objects.requireNonNull(dataSnapshot.child("active").getValue()).toString().equals("true")) {
                                customLoader.dismiss();//Loader dismissed
                                editor.putString("admin_id", areaManager.getOperatorDesignation());
                                editor.putString("mobileValue", areaManager.getAreaManagerPhone());
                                editor.putString("organisationName", areaManager.getAreaManagerOrganisation());
                                editor.putString("passwordValue", passwordValue);
                                editor.putString("areaId", areaManager.getAreaManagerAreaId());
                                editor.putString("areaName", areaManager.getAreaManagerAreaName());
                                editor.putBoolean("login", true);
                                editor.commit();
                                startActivity(intent);
                                finish();
                            } else {
                                //wrong password
                                customLoader.dismiss();//Loader dismissed
                                alertSuperAdminDialog("Wrong credentials or password!!", "Please contact Customer Support for assistance");
                            }

                        } else if (employeeType.equals("SM")) {
                            ServiceManager serviceManager = dataSnapshot.getValue(ServiceManager.class);
                            Log.d(TAG, "datasnapshot: " + dataSnapshot.getValue());
                            if (passwordValue.equalsIgnoreCase(Objects.requireNonNull(serviceManager).getServiceManagerPassword()) && Objects.requireNonNull(dataSnapshot.child("active").getValue()).toString().equals("true")) {
                                customLoader.dismiss();//Loader dismissed
                                editor.putString("admin_id", serviceManager.getOperatorDesignation());
                                editor.putString("mobileValue", serviceManager.getServiceManagerPhone());
                                editor.putString("organisationName", serviceManager.getServiceManagerOrganisation());
                                editor.putString("passwordValue", passwordValue);
                                editor.putString("areaId", serviceManager.getServiceManagerAreaId());
                                editor.putString("areaName", serviceManager.getServiceManagerAreaName());
                                editor.putBoolean("login", true);
                                editor.commit();
                                startActivity(intent);
                                finish();
                            } else {
                                //wrong password
                                customLoader.dismiss();//Loader dismissed
                                alertSuperAdminDialog("Wrong credentials or password!!", "Please contact Customer Support for assistance");
                            }

                        } else {
                            customLoader.dismiss();//Loader dismissed
                            Log.d(TAG, "Wrong format");
                        }

                    }
                } catch (Exception e) {
                    Log.d(TAG, "Wrong format");
                    customLoader.dismiss();
                    Toast.makeText(Login.this, "Wrong password or user name", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
