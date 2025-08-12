package in.pubbs.pubbsadmin;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Objects;

public class OperatorDetails extends AppCompatActivity implements View.OnClickListener {
    TextView sellLockTv, organisationName, ownerName, mobileNumber, emailId, operatorKeyTv, title,operatorName;
    String orgName, orgOwner, orgMobile, orgEmail, orgOperatorKey;
    ImageView back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operator_details);
        initView();
    }

    private void initView() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        Objects.requireNonNull(getSupportActionBar()).setTitle("");
        title = findViewById(R.id.toolbar_title);
        title.setText(getResources().getString(R.string.operator_name));
        back = findViewById(R.id.back_button);
        back.setOnClickListener(this);
        sellLockTv = findViewById(R.id.sell_locks);
        sellLockTv.setOnClickListener(this);
        Intent intent = getIntent();
        orgName = intent.getStringExtra("operatorOrganisation");
        orgOwner = intent.getStringExtra("operatorName");
        orgMobile = intent.getStringExtra("operatorMobile");
        orgEmail = intent.getStringExtra("operatorEmail");
        orgOperatorKey = intent.getStringExtra("operatorKey");
        organisationName = findViewById(R.id.organisation_name_tv);
        organisationName.setText(orgName);
        ownerName = findViewById(R.id.owner_name_tv);
        ownerName.setText(orgOwner);
        mobileNumber = findViewById(R.id.mobile_number_tv);
        mobileNumber.setText(orgMobile);
        emailId = findViewById(R.id.email_id_tv);
        emailId.setText(orgEmail);
        operatorKeyTv = findViewById(R.id.operator_key_tv);
        operatorKeyTv.setText(orgOperatorKey);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(OperatorDetails.this, ManageOperator.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.back_button) {
            Intent intent = new Intent(OperatorDetails.this, ManageOperator.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else if (view.getId() == R.id.sell_locks) {
            Intent intent_sell = new Intent(OperatorDetails.this, SellLocks.class);
            intent_sell.putExtra("operatorOrganisation", orgName);
            intent_sell.putExtra("operatorEmail", orgEmail);
            intent_sell.putExtra("operatorMobile", orgMobile);
            startActivity(intent_sell);
        } else {
            // Do nothing or handle default case if needed
        }

//        switch (view.getId()) {
//            case R.id.back_button:
//                Intent intent = new Intent(OperatorDetails.this, ManageOperator.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                startActivity(intent);
//                break;
//            case R.id.sell_locks:
//                Intent intent_sell = new Intent(OperatorDetails.this, SellLocks.class);
//                intent_sell.putExtra("operatorOrganisation", orgName);
//                intent_sell.putExtra("operatorEmail", orgEmail);
//                intent_sell.putExtra("operatorMobile", orgMobile);
//                startActivity(intent_sell);
//                break;
//            default:
//                break;
//        }
    }
}
