package in.pubbs.pubbsadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import java.util.Objects;

import in.pubbs.pubbsadmin.Model.AddBank;

public class AddBankDetails extends AppCompatActivity {
    TextView add_bank_account_tv, description;
    TextView upi_layout, phone_number_layout;
    EditText upi, phone_number;
    Button confirm;
    String user_phone_number, admin_mobile, adminId, organisationName;
    SharedPreferences sharedPreferences;
    private String TAG = AddBankDetails.class.getSimpleName();
    FirebaseDatabase firebaseDatabase;
    DatabaseReference bankDetailsReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_bank_details);
        initView();
    }

    private void initView() {
        sharedPreferences = getSharedPreferences(getResources().getString(R.string.sharedPreferences), MODE_PRIVATE);
        admin_mobile = sharedPreferences.getString("mobileValue", null);
        adminId = sharedPreferences.getString("admin_id", null);
        organisationName = sharedPreferences.getString("organisationName", "no_data").replace(" ", "");
        Log.d(TAG, "Admin Details:" + admin_mobile + "\t" + adminId + "\t" + organisationName);
        add_bank_account_tv = findViewById(R.id.add_bank_account_tv);
        description = findViewById(R.id.description);
        upi_layout = findViewById(R.id.upi_layout);
        upi = findViewById(R.id.upi);
        phone_number_layout = findViewById(R.id.phone_number_layout);
        phone_number = findViewById(R.id.phone_number);
        user_phone_number = phone_number.getText().toString();


        confirm = findViewById(R.id.confirm);
        confirm.setOnClickListener(v -> {
            if (criteriaCheck()) {
                firebaseDatabase = FirebaseDatabase.getInstance();
                bankDetailsReference = firebaseDatabase.getReference().child(organisationName.replaceAll(" ", "")).child("BankDetails");
                AddBank addBank = new AddBank(adminId, admin_mobile, upi.getText().toString().trim(), phone_number.getText().toString().trim(), organisationName);
                bankDetailsReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (!dataSnapshot.child(addBank.getMobile()).exists()) {
                            bankDetailsReference.child(addBank.getMobile()).setValue(addBank);
                            showBankDetailsDialog("Bank Details is added");
                        } else {
                            bankDetailsReference.child(addBank.getMobile()).setValue(addBank);
                            showBankDetailsDialog("Bank Details is updated");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.d(TAG, "Error: " + databaseError);
                    }
                });
            } else {
                Log.d(TAG, "No data present");
            }
        });

    }

    private boolean criteriaCheck() {
        final Animation animShake = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.shake);
        if (TextUtils.isEmpty(upi.getText().toString().trim())) {
            upi.startAnimation(animShake);
            return false;
        }
        if (TextUtils.isEmpty(phone_number.getText().toString().trim())) {
            phone_number.startAnimation(animShake);
            return false;
        }
        return true;
    }

    public void showBankDetailsDialog(String msg) {
        final AlertDialog dialogBuilder = new AlertDialog.Builder(this).create();
        LayoutInflater inflater = this.getLayoutInflater();
        Objects.requireNonNull(dialogBuilder.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        View dialogView = inflater.inflate(R.layout.custom_bank_details_dialog, null);
        final TextView areaAdd = dialogView.findViewById(R.id.area_add_tv);
        areaAdd.setText(msg);
        final Button ok = dialogView.findViewById(R.id.ok_btn);
        ok.setOnClickListener(view -> {
            dialogBuilder.dismiss();
            finish();
            Intent intent = new Intent(AddBankDetails.this, Profile.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
        dialogBuilder.setView(dialogView);
        dialogBuilder.show();
        dialogBuilder.setCancelable(false);
    }

}
