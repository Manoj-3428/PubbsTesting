package in.pubbs.pubbsadmin;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.view.Window;
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

import java.util.Objects;

import in.pubbs.pubbsadmin.Model.AreaManager;
import in.pubbs.pubbsadmin.Model.Operator;
import in.pubbs.pubbsadmin.Model.ServiceManager;
import in.pubbs.pubbsadmin.Model.SuperAdmin;
import in.pubbs.pubbsadmin.Model.ZoneManager;
import in.pubbs.pubbsadmin.View.CustomLoader;

/*Created by: Souvik Datta*/
public class Profile extends AppCompatActivity implements View.OnClickListener {

    TextView title, name_tv, designation_tv, email_tv, phone_tv, area_tv, changePassword, bankDetails, aboutUs, faq, organisation_tv;
    ImageView back, editName;
    Button done;
    SharedPreferences sharedPreferences;
    String phoneNumber, designation = "Super Admin", organisation, name = "PUBBS", email, area, path, TAG = Profile.class.getSimpleName();
    DatabaseReference databaseReference, editData;
    private CustomLoader customLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        sharedPreferences = getSharedPreferences(getResources().getString(R.string.sharedPreferences), Context.MODE_PRIVATE);
        init();
        loadData();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Profile.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void init() {
        title = findViewById(R.id.toolbar_title);
        back = findViewById(R.id.back_button);
        title.setText("Profile");
        name_tv = findViewById(R.id.name_tv);
        organisation_tv = findViewById(R.id.organisation_name);
        designation_tv = findViewById(R.id.designation_tv);
        email_tv = findViewById(R.id.email_tv);
        phone_tv = findViewById(R.id.mobile_tv);
        area_tv = findViewById(R.id.area_tv);
        editName = findViewById(R.id.edit_button);
        changePassword = findViewById(R.id.change_password);
        bankDetails = findViewById(R.id.bank_details);
        done = findViewById(R.id.done_button);
        back.setOnClickListener(this);
        changePassword.setOnClickListener(this);
        done.setOnClickListener(this);
        editName.setOnClickListener(this);
        bankDetails.setOnClickListener(this);
        customLoader = new CustomLoader(this, R.style.WideDialog);//Loader
        aboutUs = findViewById(R.id.about_us);
        aboutUs.setOnClickListener(this);
        faq = findViewById(R.id.faq);
        faq.setOnClickListener(this);
    }

    private void loadData() {
        customLoader.show();
        phoneNumber = sharedPreferences.getString("mobileValue", null);
        designation = sharedPreferences.getString("admin_id", null);
        organisation = sharedPreferences.getString("organisationName", "no_data").replace(" ", "");
        if (designation.equals("Regional Manager")) {
            path = organisation.replaceAll(" ", "") + "/RM/" + phoneNumber;
            area_tv.setVisibility(View.GONE);
        } else if (designation.equals("Zone Manager")) {
            path = organisation.replaceAll(" ", "") + "/ZM/" + phoneNumber;
            bankDetails.setVisibility(View.GONE);
        } else if (designation.equals("Area Manager")) {
            path = organisation.replaceAll(" ", "") + "/AM/" + phoneNumber;
            bankDetails.setVisibility(View.GONE);
        } else if (designation.equals("Service Manager")) {
            path = organisation.replaceAll(" ", "") + "/SM/" + phoneNumber;
            bankDetails.setVisibility(View.GONE);
        } else {
            path = "SuperAdmin/9433944708";
            designation = "Super Admin";
            area_tv.setVisibility(View.GONE);
            bankDetails.setVisibility(View.GONE);
            editName.setVisibility(View.GONE);
            email_tv.setVisibility(View.GONE);
        }
        Log.d(TAG, "path: " + path);
        databaseReference = FirebaseDatabase.getInstance().getReference(path);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (designation.equals("Regional Manager")) {
                    Operator operator = dataSnapshot.getValue(Operator.class);
                    name = Objects.requireNonNull(operator).getOperatorName();
                    email = operator.getOperatorEmail();
                } else if (designation.equals("Zone Manager")) {
                    ZoneManager zoneManager = dataSnapshot.getValue(ZoneManager.class);
                    name = Objects.requireNonNull(zoneManager).getZoneManagerName();
                    email = zoneManager.getZoneManagerEmail();
                    area = zoneManager.getZoneManagerCity();
                } else if (designation.equals("Area Manager")) {
                    AreaManager areaManager = dataSnapshot.getValue(AreaManager.class);
                    name = Objects.requireNonNull(areaManager).getAreaManagerName();
                    email = areaManager.getAreaManagerEmail();
                    area = areaManager.getAreaManagerAreaId();
                } else if (designation.equals("Service Manager")) {
                    ServiceManager serviceManager = dataSnapshot.getValue(ServiceManager.class);
                    name = Objects.requireNonNull(serviceManager).getServiceManagerName();
                    email = serviceManager.getServiceManagerEmail();
                    area = serviceManager.getServiceManagerAreaId();
                } else if (designation.equals("PUBBS")) {
                    SuperAdmin superAdmin = dataSnapshot.getValue(SuperAdmin.class);
                    name = "Super Admin";
                }
                updateUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, "Error: " + databaseError);
            }
        });
    }

    private void updateUI() {
        customLoader.dismiss();
        name_tv.setText("Name : " + name);
        designation_tv.setText("Designation : " + designation);
        phone_tv.setText("Phone : " + phoneNumber);
        email_tv.setText("Email : " + email);
        area_tv.setText("Area : " + area);
        organisation_tv.setText("Organisation: " + sharedPreferences.getString("organisationName","No Data"));
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.back_button) {
            Intent intent = new Intent(Profile.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else if (v.getId() == R.id.change_password) {
            setChangePassword();
        } else if (v.getId() == R.id.done_button) {
            finish();
        } else if (v.getId() == R.id.edit_button) {
            setNameChange();
        } else if (v.getId() == R.id.bank_details) {
            startActivity(new Intent(Profile.this, BankList.class));
        } else if (v.getId() == R.id.about_us) {
            startActivity(new Intent(Profile.this, AboutUs.class));
        } else if (v.getId() == R.id.faq) {
            startActivity(new Intent(Profile.this, FAQ.class));
        } else {
            // Do nothing or handle default case if needed
        }

//        switch (v.getId()) {
//            case R.id.back_button:
//                Intent intent = new Intent(Profile.this, MainActivity.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                startActivity(intent);
//                break;
//            case R.id.change_password:
//                setChangePassword();
//                break;
//            case R.id.done_button:
//                finish();
//                break;
//            case R.id.edit_button:
//                setNameChange();
//                break;
//            case R.id.bank_details:
//                startActivity(new Intent(Profile.this, BankList.class));
//                break;
//            case R.id.about_us:
//                startActivity(new Intent(Profile.this, AboutUs.class));
//                break;
//            case R.id.faq:
//                startActivity(new Intent(Profile.this, FAQ.class));
//                break;
//            default:
//                break;
//        }
    }

    public static class EditAlertDialog extends Dialog {
        private TextView title, content, content1;
        private EditText data, data1;
        private ImageView positiveBtn, negativeBtn;
        private ClickListener positiveClick, negativeClick;
        private String titleText, contentText;

        public EditAlertDialog(@NonNull Context context) {
            super(context);
        }

        public EditAlertDialog(@NonNull Context context, int themeResId) {
            super(context, themeResId);
        }

        public EditAlertDialog(@NonNull Context context, int themeResId, String title, String Content) {
            super(context, themeResId);
            titleText = title;
            contentText = Content;
        }

        protected EditAlertDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
            super(context, cancelable, cancelListener);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Window window = getWindow();
            Objects.requireNonNull(window).setBackgroundDrawableResource(android.R.color.transparent);
            setContentView(R.layout.custom_dialog_edittext);
            init();
        }

        public void init() {
            title = findViewById(R.id.title);
            title.setText(titleText);
            content = findViewById(R.id.content);
            content.setText(contentText);
            content1 = findViewById(R.id.content1);
            data = findViewById(R.id.data);
            data1 = findViewById(R.id.data1);
            positiveBtn = findViewById(R.id.positive);
            negativeBtn = findViewById(R.id.negetive);
            positiveBtn.setOnClickListener((ClickListener) view -> positiveClick.onClick(view));
            negativeBtn.setOnClickListener((ClickListener) view -> negativeClick.onClick(view));

        }

        public void onPositiveClickListener(ClickListener clickListener) {
            positiveClick = clickListener;
        }

        public void onNegativeClickListener(ClickListener clickListener) {
            negativeClick = clickListener;
        }

        public interface ClickListener extends View.OnClickListener {
            @Override
            void onClick(View view);
        }

        public String getData() {
            return data.getText().toString();
        }

        public String getData1() {
            return data1.getText().toString();
        }

        public void setDataError() {
            data.setError("Field cannot be left empty");
        }

        public void setData1Error() {
            data1.setError("Field cannot be left empty");
        }

        public void enableForgotPassword(Boolean value) {
            if (value) {
                title.setText("Change Password");
                content1.setVisibility(View.VISIBLE);
                data1.setVisibility(View.VISIBLE);
                content.setText("Enter old password");
                content1.setText("Enter new password");
                data.setHint("Enter Password");
                data.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);//Setting Edit text as password
                data.setTransformationMethod(PasswordTransformationMethod.getInstance());
                data1.setHint("Enter Password");
                data1.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);//Setting Edit text as password
                data1.setTransformationMethod(PasswordTransformationMethod.getInstance());

            }
        }
    }

    public void setChangePassword() {
        EditAlertDialog dialog = new EditAlertDialog(Profile.this, R.style.WideDialog);
        dialog.show();
        dialog.enableForgotPassword(true);
        dialog.onNegativeClickListener(view -> {
            dialog.dismiss();
            done.setVisibility(View.GONE);
        });
        dialog.onPositiveClickListener(view -> {
            if (TextUtils.isEmpty(dialog.getData())) {
                dialog.setDataError();
            } else if (TextUtils.isEmpty(dialog.getData1())) {
                dialog.setData1Error();
            } else {
                if (designation.equals("Regional Manager")) {
                    path = organisation.replaceAll(" ", "") + "/RM/";
                } else if (designation.equals("Zone Manager")) {
                    path = organisation.replaceAll(" ", "") + "/ZM/";
                } else if (designation.equals("Area Manager")) {
                    path = organisation.replaceAll(" ", "") + "/AM/";
                } else if (designation.equals("Service Manager")) {
                    path = organisation.replaceAll(" ", "") + "/SM/";
                } else {
                    //SuperAdmin
                    path = "SuperAdmin";
                }
                if (dialog.getData1().length() >= 6) {
                    editData = FirebaseDatabase.getInstance().getReference(path);
                    editData.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for (DataSnapshot i : dataSnapshot.getChildren()) {
                                if (designation.equals("Regional Manager")) {
                                    Operator operator = i.getValue(Operator.class);
                                    if (Objects.requireNonNull(operator).getOperatorMobile().equals(phoneNumber)) {
                                        if (operator.getOperatorPassword().equals(dialog.getData())) {
                                            Log.d(TAG, "Password Match");
                                            editData.child(phoneNumber).child("operatorPassword").setValue(dialog.getData1());
                                            Toast.makeText(Profile.this, "Password Updated Successfully!!", Toast.LENGTH_LONG).show();
                                        } else {
                                            Toast.makeText(Profile.this, "Wrong Password!!", Toast.LENGTH_LONG).show();
                                        }
                                    }
                                } else if (designation.equals("Zone Manager")) {
                                    ZoneManager zoneManager = i.getValue(ZoneManager.class);
                                    if (Objects.requireNonNull(zoneManager).getZoneManagerPhone().equals(phoneNumber)) {
                                        if (zoneManager.getZoneManagerPassword().equals(dialog.getData())) {
                                            Log.d(TAG, "Password Match");
                                            editData.child(phoneNumber)
                                                    .child("zoneManagerPassword").setValue(dialog.getData1());
                                            Toast.makeText(Profile.this, "Password Updated Successfully!!", Toast.LENGTH_LONG).show();
                                        } else {
                                            Toast.makeText(Profile.this, "Wrong Password!!", Toast.LENGTH_LONG).show();
                                        }
                                    }
                                } else if (designation.equals("Area Manager")) {
                                    AreaManager areaManager = i.getValue(AreaManager.class);
                                    if (Objects.requireNonNull(areaManager).getAreaManagerPhone().equals(phoneNumber)) {
                                        if (areaManager.getAreaManagerPassword().equals(dialog.getData())) {
                                            Log.d(TAG, "Password Match");
                                            editData.child(phoneNumber)
                                                    .child("areaManagerPassword").setValue(dialog.getData1());
                                            Toast.makeText(Profile.this, "Password Updated Successfully!!", Toast.LENGTH_LONG).show();
                                        } else {
                                            Toast.makeText(Profile.this, "Wrong Password!!", Toast.LENGTH_LONG).show();
                                        }
                                    }
                                } else if (designation.equals("Service Manager")) {
                                    ServiceManager serviceManager = i.getValue(ServiceManager.class);
                                    if (Objects.requireNonNull(serviceManager).getServiceManagerPhone().equals(phoneNumber)) {
                                        if (serviceManager.getServiceManagerPassword().equals(dialog.getData())) {
                                            Log.d(TAG, "Password Match");
                                            editData.child(phoneNumber)
                                                    .child("serviceManagerPassword").setValue(dialog.getData1());
                                            Toast.makeText(Profile.this, "Password Updated Successfully!!", Toast.LENGTH_LONG).show();
                                        } else {
                                            Toast.makeText(Profile.this, "Wrong Password!!", Toast.LENGTH_LONG).show();
                                        }
                                    }
                                } else if (designation.equals("Super Admin")) {
                                    SuperAdmin superAdmin = i.getValue(SuperAdmin.class);
                                    if (Objects.requireNonNull(superAdmin).getMobile().equals(phoneNumber)) {
                                        if (superAdmin.getPassword().equals(dialog.getData())) {
                                            Log.d(TAG, "Password Match");
                                            editData.child(phoneNumber).child("password").setValue(dialog.getData1());
                                            Toast.makeText(Profile.this, "Password Updated Successfully!!", Toast.LENGTH_LONG).show();
                                        } else {
                                            Toast.makeText(Profile.this, "Wrong Password!!", Toast.LENGTH_LONG).show();
                                        }
                                    }
                                }
                            }
                            dialog.dismiss();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                } else {
                    dialog.data1.setError("Please enter password of length 6 or more.");
                }
            }
        });
    }

    public void setNameChange() {
        EditAlertDialog obj = new EditAlertDialog(Profile.this, R.style.WideDialog
                , "Edit Name", "Please enter the new name");
        obj.show();
        obj.onNegativeClickListener(view -> {
            obj.dismiss();
            done.setVisibility(View.GONE);
        });
        obj.onPositiveClickListener(view -> {
            if (TextUtils.isEmpty(obj.getData())) {
                obj.setDataError();
            } else {
                if (designation.equals("Regional Manager")) {
                    path = organisation.replaceAll(" ", "") + "/RM/" + phoneNumber + "/operatorName";
                } else if (designation.equals("Zone Manager")) {
                    path = organisation.replaceAll(" ", "") + "/ZM/" + phoneNumber + "/zoneManagerName";
                } else if (designation.equals("Area Manager")) {
                    path = organisation.replaceAll(" ", "") + "/AM/" + phoneNumber + "/areaManagerName";
                } else if (designation.equals("Service Manager")) {
                    path = organisation.replaceAll(" ", "") + "/SM/" + phoneNumber + "/serviceManagerName";
                } else {
                    //SuperAdmin
                }
                editData = FirebaseDatabase.getInstance().getReference(path);
                editData.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        editData.setValue(obj.getData());
                        obj.dismiss();
                        customLoader.show();
                        loadData();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.d(TAG, "Error: " + databaseError);
                    }
                });

            }
        });
    }
}
