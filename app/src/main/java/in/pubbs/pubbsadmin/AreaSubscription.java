package in.pubbs.pubbsadmin;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import in.pubbs.pubbsadmin.BottomSheet.BottomSheetAreaSubscriptionFragment;

/*created by Parita Dey*/
public class AreaSubscription extends AppCompatActivity implements View.OnClickListener {
    TextView areaSubscriptionTv, toolbarTitle;
    ImageView back;
    EditText planName, validityTime, pricePlan, maxFreeRide, subscriptionDescription, valueHourSlab, valueMinSlab;
    private String TAG = AreaSubscription.class.getSimpleName();
    RadioGroup radioGroupChoice;
    int carryForward = 1;
    ImageButton hourTabInc, hourTabDesc, minTabInc, minTabDesc;
    RadioButton radioNo, radioYes;
    Button addPlan;
    String organisationName, subscriptionId;
    SharedPreferences sharedPreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_area_subscription);
        initView();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(AreaSubscription.this, AreaDetails.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void initView() {
        sharedPreferences = getSharedPreferences(getResources().getString(R.string.sharedPreferences), Context.MODE_PRIVATE);
        organisationName = sharedPreferences.getString("organisationName", "no_data").replace(" ", "");
        Log.d(TAG, "Organisation Name:" + organisationName);
        areaSubscriptionTv = findViewById(R.id.area_subscription_tv);
        areaSubscriptionTv.setOnClickListener(this);
        toolbarTitle = findViewById(R.id.toolbar_title);
        toolbarTitle.setText(R.string.add_subscription);
        back = findViewById(R.id.back_button);
        back.setOnClickListener(this);
        planName = findViewById(R.id.plan_name);
        validityTime = findViewById(R.id.validity_time);
        pricePlan = findViewById(R.id.price_plan);
        maxFreeRide = findViewById(R.id.number_free_rides);
        subscriptionDescription = findViewById(R.id.description);
        /*valueHourSlab = findViewById(R.id.value_hour_slab);
        hourTabInc = findViewById(R.id.increment_hour_slab);
        hourTabInc.setOnClickListener(this);
        hourTabDesc = findViewById(R.id.decrement_hour_slab);
        hourTabDesc.setOnClickListener(this);
        valueMinSlab = findViewById(R.id.value_minute_slab);
        minTabInc = findViewById(R.id.increment_minute_slab);
        minTabInc.setOnClickListener(this);
        minTabDesc = findViewById(R.id.decrement_minute_slab);
        minTabDesc.setOnClickListener(this);
        */
        addPlan = findViewById(R.id.add_plan);
        addPlan.setOnClickListener(this);
        radioGroupChoice = findViewById(R.id.radio_group_carry_forward);
        radioYes = findViewById(R.id.radioYes);
        radioNo = findViewById(R.id.radioNo);
        radioGroupChoice.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioYes) {
                carryForward = 1;
                Log.d(TAG, "Carry forward is on:" + carryForward);
            } else {
                carryForward = 0;
                Log.d(TAG, "Carry forward is off:" + carryForward);
            }
        });
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.area_subscription_tv) {
            new BottomSheetAreaSubscriptionFragment().show(getSupportFragmentManager(), "dialog");
        }
        else if (v.getId() == R.id.back_button) {
            Intent intent = new Intent(AreaSubscription.this, AreaDetails.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
/* else if (v.getId() == R.id.increment_hour_slab) {
    increment(valueHourSlab);
}
else if (v.getId() == R.id.decrement_hour_slab) {
    decrement(valueHourSlab);
}
else if (v.getId() == R.id.increment_minute_slab) {
    increment(valueMinSlab);
}
else if (v.getId() == R.id.decrement_minute_slab) {
    decrement(valueMinSlab);
} */
        else if (v.getId() == R.id.add_plan) {
            if (criteriaCheck()) {
                // subscriptionId = generateSubscriptionID();
                Intent area = new Intent(AreaSubscription.this, ManageSystem.class);
                //area.putExtra("subscription_id", subscriptionId);
                area.putExtra("plan_name", planName.getText().toString().trim());
                area.putExtra("validity_time", validityTime.getText().toString().trim());
                area.putExtra("plan_price", pricePlan.getText().toString().trim());
                area.putExtra("max_free_ride", maxFreeRide.getText().toString().trim());
                area.putExtra("subscription_description", subscriptionDescription.getText().toString().trim());
                // area.putExtra("hour_slab", valueHourSlab.getText().toString().equals("") ? "0" : valueHourSlab.getText().toString().trim());
                // area.putExtra("min_slab", valueMinSlab.getText().toString().equals("") ? "0" : valueMinSlab.getText().toString().trim());
                area.putExtra("carry_forward", carryForward);
                setResult(2, area);
                finish();
            }
        }



//        switch (v.getId()) {
//            case R.id.area_subscription_tv:
//                new BottomSheetAreaSubscriptionFragment().show(getSupportFragmentManager(), "dialog");
//                break;
//            case R.id.back_button:
//                Intent intent = new Intent(AreaSubscription.this, AreaDetails.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                startActivity(intent);
//                break;
//          /*  case R.id.increment_hour_slab:
//                increment(valueHourSlab);
//                break;
//            case R.id.decrement_hour_slab:
//                decrement(valueHourSlab);
//                break;
//            case R.id.increment_minute_slab:
//                increment(valueMinSlab);
//                break;
//            case R.id.decrement_minute_slab:
//                decrement(valueMinSlab);
//                break;
//          */
//            case R.id.add_plan:
//                if (criteriaCheck()) {
//                    // subscriptionId = generateSubscriptionID();
//                    Intent area = new Intent(AreaSubscription.this, ManageSystem.class);
//                    //area.putExtra("subscription_id", subscriptionId);
//                    area.putExtra("plan_name", planName.getText().toString().trim());
//                    area.putExtra("validity_time", validityTime.getText().toString().trim());
//                    area.putExtra("plan_price", pricePlan.getText().toString().trim());
//                    area.putExtra("max_free_ride", maxFreeRide.getText().toString().trim());
//                    area.putExtra("subscription_description", subscriptionDescription.getText().toString().trim());
//                    //   area.putExtra("hour_slab", valueHourSlab.getText().toString().equals("") ? "0" : valueHourSlab.getText().toString().trim());
//                    //     area.putExtra("min_slab", valueMinSlab.getText().toString().equals("") ? "0" : valueMinSlab.getText().toString().trim());
//                    area.putExtra("carry_forward", carryForward);
//                    setResult(2, area);
//                    finish();
//                }
//                break;
//            default:
//                break;
//        }
    }

    private void increment(EditText view) {
        String value;
        int val = 0;
        value = view.getText().toString();
        if (!value.equals("")) {
            val = Integer.valueOf(value);
            val++;
        }
        view.setText(String.valueOf(val));
    }

    private void decrement(EditText view) {
        String value;
        int val = 0;
        value = view.getText().toString();
        if (!value.equals("")) {
            val = Integer.valueOf(value);
            if (val > 0)
                val--;
        }
        view.setText(String.valueOf(val));
    }

    private boolean criteriaCheck() {
        if (TextUtils.isEmpty(planName.getText())) {
            planName.setError(getResources().getString(R.string.empty_field_message));
            return false;
        }
        if (TextUtils.isEmpty(validityTime.getText())) {
            validityTime.setError(getResources().getString(R.string.empty_field_message));
            return false;
        }
        if (TextUtils.isEmpty(pricePlan.getText())) {
            pricePlan.setError(getResources().getString(R.string.empty_field_message));
            return false;
        }
        if (TextUtils.isEmpty(maxFreeRide.getText())) {
            maxFreeRide.setError(getResources().getString(R.string.empty_field_message));
            return false;
        }
        if (TextUtils.isEmpty(subscriptionDescription.getText())) {
            subscriptionDescription.setError(getResources().getString(R.string.empty_field_message));
            return false;
        }
        return true;
    }

   /* public String generateSubscriptionID() {
        String subscriptionNumber = organisationName + "_SP_"; // SP stands for Subscription Plane
        String subscription;
        int max = 999;
        int min = 1;
        int randomNum = (int) (Math.random() * (max - min)) + min;
        subscription = subscriptionNumber + randomNum;
        Log.d(TAG, "RateNumber: " + subscription);
        return subscription;

    }*/
}
