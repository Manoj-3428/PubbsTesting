package in.pubbs.pubbsadmin;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import in.pubbs.pubbsadmin.BottomSheet.BottomSheetAreaLegalFragment;

/*created by Parita Dey*/
public class AreaLegal extends AppCompatActivity implements View.OnClickListener {
    TextView areaLegalTv, toolbarTitle;
    ImageView back;
    EditText geofencingFine, baseFareCondition, serviceCondition, subscriptionCondition, areaCondition;
    Button done;
    String geofencing_condition, area_condition, baseFare_condition, service_condition, subscription_condition;
    private String TAG = AreaLegal.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_area_legal);
        initView();
    }

    private void initView() {
        areaLegalTv = findViewById(R.id.area_legal_tv);
        areaLegalTv.setOnClickListener(this);
        toolbarTitle = findViewById(R.id.toolbar_title);
        toolbarTitle.setText(R.string.set_area_legal);
        back = findViewById(R.id.back_button);
        back.setOnClickListener(this);
        geofencingFine = findViewById(R.id.geofencing_conditions);
        baseFareCondition = findViewById(R.id.baseFare_condition);
        serviceCondition = findViewById(R.id.service_condition);
        subscriptionCondition = findViewById(R.id.area_subscription_condition);
        areaCondition = findViewById(R.id.area_condition);
        done = findViewById(R.id.done_button);
        done.setOnClickListener(this);
        try {
            Intent intent = getIntent();
            area_condition = intent.getStringExtra("areaCondition");
            baseFare_condition = intent.getStringExtra("baseFareCondition");
            geofencing_condition = intent.getStringExtra("geofencingCondition");
            service_condition = intent.getStringExtra("serviceCondition");
            subscription_condition = intent.getStringExtra("subscriptionCondition");
            Log.d(TAG, "area legal details: " + area_condition + "\t" + baseFare_condition + "\t" + geofencing_condition + "\t" + service_condition + "\t" + subscription_condition);
            if (area_condition != null && baseFare_condition != null && geofencing_condition != null && service_condition != null && subscription_condition != null) {
                areaCondition.setText(area_condition);
                baseFareCondition.setText(baseFare_condition);
                geofencingFine.setText(geofencing_condition);
                serviceCondition.setText(service_condition);
                subscriptionCondition.setText(subscription_condition);
            } else {
                areaCondition.setText("");
                baseFareCondition.setText("");
                geofencingFine.setText("");
                serviceCondition.setText("");
                subscriptionCondition.setText("");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "Intent value is null:" + e.toString());
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(AreaLegal.this, AreaDetails.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.area_legal_tv) {
            new BottomSheetAreaLegalFragment().show(getSupportFragmentManager(), "dialog");
        }
        else if (v.getId() == R.id.back_button) {
            Intent intent = new Intent(AreaLegal.this, AreaDetails.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
        else if (v.getId() == R.id.done_button) {
            if (criteriaCheck()) {
                Intent area = new Intent(AreaLegal.this, ManageSystem.class);
                area.putExtra("geofencing_fine", geofencingFine.getText().toString().trim());
                area.putExtra("base_fare_condition", baseFareCondition.getText().toString().trim());
                area.putExtra("service_condition", serviceCondition.getText().toString().trim());
                area.putExtra("subscription_condition", subscriptionCondition.getText().toString().trim());
                area.putExtra("area_condition", areaCondition.getText().toString().trim());
                setResult(1, area);
                finish();
            }
        }


//        switch (v.getId()) {
//            case R.id.area_legal_tv:
//                new BottomSheetAreaLegalFragment().show(getSupportFragmentManager(), "dialog");
//                break;
//            case R.id.back_button:
//                Intent intent = new Intent(AreaLegal.this, AreaDetails.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                startActivity(intent);
//                break;
//            case R.id.done_button:
//                if (criteriaCheck()) {
//                    Intent area = new Intent(AreaLegal.this, ManageSystem.class);
//                    area.putExtra("geofencing_fine", geofencingFine.getText().toString().trim());
//                    area.putExtra("base_fare_condition", baseFareCondition.getText().toString().trim());
//                    area.putExtra("service_condition", serviceCondition.getText().toString().trim());
//                    area.putExtra("subscription_condition", subscriptionCondition.getText().toString().trim());
//                    area.putExtra("area_condition", areaCondition.getText().toString().trim());
//                    setResult(1, area);
//                    finish();
//                }
//                break;
//            default:
//                break;
//        }
    }

    private boolean criteriaCheck() {
        if (TextUtils.isEmpty(geofencingFine.getText())) {
            geofencingFine.setError(getResources().getString(R.string.empty_field_message));
            return false;
        }
        if (TextUtils.isEmpty(baseFareCondition.getText())) {
            baseFareCondition.setError(getResources().getString(R.string.empty_field_message));
            return false;
        }
        if (TextUtils.isEmpty(serviceCondition.getText())) {
            serviceCondition.setError(getResources().getString(R.string.empty_field_message));
            return false;
        }
        if (TextUtils.isEmpty(subscriptionCondition.getText())) {
            subscriptionCondition.setError(getResources().getString(R.string.empty_field_message));
            return false;
        }
        if (TextUtils.isEmpty(areaCondition.getText())) {
            areaCondition.setError(getResources().getString(R.string.empty_field_message));
            return false;
        }
        return true;
    }
}
