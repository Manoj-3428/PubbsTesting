package in.pubbs.pubbsadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import java.util.Objects;

import in.pubbs.pubbsadmin.View.CustomLoader;

/*Created by Parita Dey*/
public class BankList extends AppCompatActivity {
    TextView title;
    ImageView back;
    SharedPreferences sharedPreferences;
    String TAG = BankList.class.getSimpleName(), mobile, path;
    DatabaseReference manageBankDbReference;
    TextView upi_tv, phone_tv;
    String upi, phone;
    ConstraintLayout addBank, noData;
    CardView cardView;
    private CustomLoader customLoader;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bank_list);
        init();
        loadData();
    }

    private void init() {
        title = findViewById(R.id.toolbar_title);
        back = findViewById(R.id.back_button);
        title.setText("Bank Details");
        back.setOnClickListener(v -> finish());
        customLoader = new CustomLoader(this, R.style.WideDialog);//Loader
        upi_tv = findViewById(R.id.upi_tv);
        phone_tv = findViewById(R.id.phone_tv);
        sharedPreferences = getSharedPreferences(getResources().getString(R.string.sharedPreferences), Context.MODE_PRIVATE);
        mobile = sharedPreferences.getString("mobileValue", null);
        Log.d(TAG, "User's mobile: " + mobile);
        addBank = findViewById(R.id.add_bank_layout);
        addBank.setOnClickListener(v -> startActivity(new Intent(BankList.this, AddBankDetails.class)));
        noData = findViewById(R.id.no_data_found);
        cardView = findViewById(R.id.card_view);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    private void loadData() {
        customLoader.show();
        path = Objects.requireNonNull(sharedPreferences.getString("organisationName", null)).replaceAll(" ", "") + "/BankDetails/" + mobile;
        manageBankDbReference = FirebaseDatabase.getInstance().getReference(path);
        manageBankDbReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() == null) { //if BankDetails node is not present in the Organisation, then show no data found
                    customLoader.dismiss();
                    cardView.setVisibility(View.GONE);
                    noData.setVisibility(View.VISIBLE);
                } else {
                    in.pubbs.pubbsadmin.Model.AddBank bankList = dataSnapshot.getValue(in.pubbs.pubbsadmin.Model.AddBank.class);
                    upi = Objects.requireNonNull(bankList).getUpi();
                    phone = bankList.getBankAccountHolderPhone();
                    Log.d(TAG, "Bank details: " + upi + "\t" + phone);
                    updateUI();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, "Error: " + databaseError);
            }
        });
    }


    private void updateUI() {
        customLoader.dismiss();
        upi_tv.setText("UPI ID : " + upi);
        phone_tv.setText("Phone Number : " + phone);
    }

}
