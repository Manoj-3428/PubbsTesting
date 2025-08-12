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
import android.widget.TextView;

import in.pubbs.pubbsadmin.BottomSheet.BottomSheetRateChartFragment;
/*created by Parita Dey*/
public class RateChart extends AppCompatActivity implements View.OnClickListener {
    TextView areaRateChartTv, toolbarTitle;
    ImageView back;
    EditText money, valueHourSlab, valueMinSlab;
    ImageButton hourTabInc, hourTabDesc, minTabInc, minTabDesc;
    Button addRate;
    String organisationName, rateId;
    SharedPreferences sharedPreferences;
    private String TAG = RateChart.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rate_chart);
        initView();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(RateChart.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void initView() {
        sharedPreferences = getSharedPreferences(getResources().getString(R.string.sharedPreferences), Context.MODE_PRIVATE);
        organisationName = sharedPreferences.getString("organisationName", "no_data").replace(" ", "");
        Log.d(TAG, "Organisation Name:" + organisationName);
        areaRateChartTv = findViewById(R.id.area_rate_chart_tv);
        areaRateChartTv.setOnClickListener(this);
        money = findViewById(R.id.money);
        toolbarTitle = findViewById(R.id.toolbar_title);
        toolbarTitle.setText(R.string.add_subscription);
        back = findViewById(R.id.back_button);
        back.setOnClickListener(this);
        valueHourSlab = findViewById(R.id.value_hour_slab);
        hourTabInc = findViewById(R.id.increment_hour_slab);
        hourTabInc.setOnClickListener(this);
        hourTabDesc = findViewById(R.id.decrement_hour_slab);
        hourTabDesc.setOnClickListener(this);
        valueMinSlab = findViewById(R.id.value_minute_slab);
        minTabInc = findViewById(R.id.increment_minute_slab);
        minTabInc.setOnClickListener(this);
        minTabDesc = findViewById(R.id.decrement_minute_slab);
        minTabDesc.setOnClickListener(this);
        addRate = findViewById(R.id.add_rate);
        addRate.setOnClickListener(this);
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

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.area_rate_chart_tv) {
            new BottomSheetRateChartFragment().show(getSupportFragmentManager(), "dialog");
        } else if (v.getId() == R.id.back_button) {
            Intent intent = new Intent(RateChart.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else if (v.getId() == R.id.increment_hour_slab) {
            increment(valueHourSlab);
        } else if (v.getId() == R.id.decrement_hour_slab) {
            decrement(valueHourSlab);
        } else if (v.getId() == R.id.increment_minute_slab) {
            increment(valueMinSlab);
        } else if (v.getId() == R.id.decrement_minute_slab) {
            decrement(valueMinSlab);
        } else if (v.getId() == R.id.add_rate) {
            if (criteriaCheck()) {
                // rateId = generateRateID();
                Intent rate_chart = new Intent(RateChart.this, ManageSystem.class);
                rate_chart.putExtra("money", Integer.parseInt(money.getText().toString().trim()));
                // rate_chart.putExtra("rate_id", rateId);
                rate_chart.putExtra("hour_slab", valueHourSlab.getText().toString().equals("") ? "0" : valueHourSlab.getText().toString().trim());
                rate_chart.putExtra("min_slab", valueMinSlab.getText().toString().equals("") ? "0" : valueMinSlab.getText().toString().trim());
                setResult(4, rate_chart);
                finish();
            }
        } else {
            // Handle default case or do nothing
        }

//        switch (v.getId()) {
//            case R.id.area_rate_chart_tv:
//                new BottomSheetRateChartFragment().show(getSupportFragmentManager(), "dialog");
//                break;
//            case R.id.back_button:
//                Intent intent = new Intent(RateChart.this, MainActivity.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                startActivity(intent);
//                break;
//            case R.id.increment_hour_slab:
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
//            case R.id.add_rate:
//                if (criteriaCheck()) {
//                    //rateId = generateRateID();
//                    Intent rate_chart = new Intent(RateChart.this, ManageSystem.class);
//                    rate_chart.putExtra("money", Integer.parseInt(money.getText().toString().trim()));
//                  //  rate_chart.putExtra("rate_id", rateId);
//                    rate_chart.putExtra("hour_slab", valueHourSlab.getText().toString().equals("") ? "0" : valueHourSlab.getText().toString().trim());
//                    rate_chart.putExtra("min_slab", valueMinSlab.getText().toString().equals("") ? "0" : valueMinSlab.getText().toString().trim());
//                    setResult(4, rate_chart);
//                    finish();
//                }
//                break;
//            default:
//                break;
//        }
    }

   /* public String generateRateID() {
        String rateNumber = organisationName+"_RC_"; // RC stands for Rate Chart
        String rate;
        int max = 999;
        int min = 1;
        int randomNum = (int) (Math.random() * (max - min)) + min;
        rate = rateNumber + randomNum;
        Log.d(TAG, "RateNumber: " + rate);
        return rate;

    }
*/
    private boolean criteriaCheck() {
        if (TextUtils.isEmpty(money.getText())) {
            money.setError(getResources().getString(R.string.empty_field_message));
            return false;
        }
        return true;
    }
}
