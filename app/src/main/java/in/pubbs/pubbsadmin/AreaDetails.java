package in.pubbs.pubbsadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Objects;

import in.pubbs.pubbsadmin.Model.Area;
import in.pubbs.pubbsadmin.Model.Station;
import in.pubbs.pubbsadmin.Model.StationList;
import in.pubbs.pubbsadmin.Model.Subscription;
import in.pubbs.pubbsadmin.View.CustomAlertDialog;

/*created by parita dey*/
public class AreaDetails extends AppCompatActivity implements View.OnClickListener {
    String[] spinnerValue = {"Stations", "Add New Station", "View Station List"};
    Spinner stationSpinner;
    TextView toolbarText, area_name, addAreaLegal, addAreaSubscription, addRateChart, serviceStartTime, serviceEndTime;
    ImageView back, edit;
    String planName, validityTime, pricePlan, maxFreeRide, subscriptionDescription;
    String amPm, currentDate;
    private static String areaId, areaName, updatedAreaName, station, maxRideTime, maxRideTimeExceedingFine, maxHoldTime, maxHoldTimeExceedingFine, customerHelpServiceNumber, geofencingFine, serviceHourEndTime, serviceHourTimeExceedingFine, serviceHourStartTime, trackBicycleData;
    private static String organisationName, parent, subscriptionId, geofencingCondition, baseFareCondition, serviceCondition, subscriptionCondition, areaCondition, stationName, stationId, stationLatitude, stationLongitude, stationRadius;
    int currentHour, currentMinute, carryForward, subscriptionPlanTime;
    String TAG = AreaDetails.class.getSimpleName();
    Button done;
    RelativeLayout layoutAddAreaLegal, layoutAddAreaSubscription, layoutAddStations, layoutAddRateChart;
    EditText maximumRideTime, maximumRideTimeExceedingFine, maximumHoldTime, maximumHoldTimeExceedingFine, minimumAmountFirstRide, baseValueRatechart, serviceHourExceedingFine, geofencineFine, customerServiceNumber, trackBicycle;
    DatabaseReference areaDbPath, stationDbPath, stationListDbPath, subscriptionDbPath;
    private static ArrayList<LatLng> markerList = new ArrayList<>();
    SharedPreferences sharedPreferences;
    FirebaseDatabase firebaseDatabase;
    String phoneNumber, designation = "Super Admin", organisation, areaPath, subscriptionPath, stationPath, stationListPath;
    CustomAlertDialog customAlertDialog;
    private ArrayList<Station> stationArrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_area_details);
        initView();
        databaseInitiation();
    }

    private void databaseInitiation() {
        firebaseDatabase = FirebaseDatabase.getInstance();
        areaPath = organisation.replaceAll(" ", "") + "/Area/";
        areaDbPath = firebaseDatabase.getReference(areaPath); //This will update the data after getting the value from edit text
        subscriptionPath = organisation.replaceAll(" ", "") + "/Subscription/";
        subscriptionDbPath = firebaseDatabase.getReference(subscriptionPath);
        stationPath = organisation.replaceAll(" ", "") + "/Station/";
        stationDbPath = firebaseDatabase.getReference(stationPath); // on clicking of spinner 's Add New Station option it will create a new station
        stationListPath = organisation.replaceAll(" ", "") + "/Area/StationList/";
        stationListDbPath = firebaseDatabase.getReference(stationListPath); // on adding a new station it will create a child data under stationList node
    }

    private void initView() {
        toolbarText = findViewById(R.id.toolbar_title);
        toolbarText.setText("Area Details");
        back = findViewById(R.id.back_button);
        back.setOnClickListener(this);
        Intent intent = getIntent();
        if (intent.getStringExtra("area_id") != null)
            areaId = intent.getStringExtra("area_id");
        if (intent.getStringExtra("area_name") != null)
            areaName = intent.getStringExtra("area_name");
        if (intent.getStringExtra("max_ride_time") != null)
            maxRideTime = intent.getStringExtra("max_ride_time");
        if (intent.getStringExtra("max_ride_exceeding_fine") != null)
            maxRideTimeExceedingFine = intent.getStringExtra("max_ride_exceeding_fine");
        if (intent.getStringExtra("max_hold_time") != null)
            maxHoldTime = intent.getStringExtra("max_hold_time");
        if (intent.getStringExtra("max_hold_exceeding_fine") != null)
            maxHoldTimeExceedingFine = intent.getStringExtra("max_hold_exceeding_fine");
      /*  if (intent.getStringExtra("min_first_amount_ride") != null)
            minFirstAmountRide = intent.getStringExtra("min_first_amount_ride");
        if (intent.getStringExtra("base_value_rate") != null)
            baseValueRateChart = intent.getStringExtra("base_value_rate");
      */
        if (intent.getStringExtra("trackBicycle") != null)
            trackBicycleData = intent.getStringExtra("trackBicycle");
        if (intent.getStringExtra("service_start_time") != null)
            serviceHourStartTime = intent.getStringExtra("service_start_time");
        if (intent.getStringExtra("service_end_time") != null)
            serviceHourEndTime = intent.getStringExtra("service_end_time");
        if (intent.getStringExtra("service_hour_exceeding_fine") != null)
            serviceHourTimeExceedingFine = intent.getStringExtra("service_hour_exceeding_fine");
        if (intent.getStringExtra("geofencing_fine") != null)
            geofencingFine = intent.getStringExtra("geofencing_fine");
        if (intent.getStringExtra("customer_number") != null)
            customerHelpServiceNumber = intent.getStringExtra("customer_number");
        if (intent.getParcelableArrayListExtra("marker_list") != null)
            markerList = intent.getParcelableArrayListExtra("marker_list");
        if (intent.getStringExtra("area_condition") != null)
            areaCondition = intent.getStringExtra("area_condition");
        if (intent.getStringExtra("base_fare_condition") != null)
            baseFareCondition = intent.getStringExtra("base_fare_condition");
        if (intent.getStringExtra("geofencing_condition") != null)
            geofencingCondition = intent.getStringExtra("geofencing_condition");
        if (intent.getStringExtra("service_condition") != null)
            serviceCondition = intent.getStringExtra("service_condition");
        if (intent.getStringExtra("subscription_condition") != null)
            subscriptionCondition = intent.getStringExtra("subscription_condition");
        Log.d(TAG, "Area Details:" + areaId + "\t" + areaName + "\t" + maxRideTime + "\t" + maxRideTimeExceedingFine + "\t" + maxHoldTime + "\t" + maxHoldTimeExceedingFine + "\t" +
                "\t" + serviceHourStartTime + "\t" + serviceHourEndTime + "\t" + serviceHourTimeExceedingFine + "\t" + geofencingFine + "\t" + customerHelpServiceNumber + "\t" + markerList
                + "\t" + areaCondition + "\t" + baseFareCondition + "\t" + geofencingCondition + "\t" + serviceCondition + "\t" + subscriptionCondition);
        sharedPreferences = getSharedPreferences(getResources().getString(R.string.sharedPreferences), Context.MODE_PRIVATE);
        phoneNumber = sharedPreferences.getString("mobileValue", null);
        designation = sharedPreferences.getString("admin_id", null);
        organisation = sharedPreferences.getString("organisationName", "no_data").replace(" ", "");
        Log.d(TAG, "Details of Admin: " + phoneNumber + "\t" + designation + "\t" + organisation);
        area_name = findViewById(R.id.area_name);
        area_name.setText(areaName);
      /*  edit = findViewById(R.id.edit);
        edit.setOnClickListener(this);*/
        maximumRideTime = findViewById(R.id.maximum_ride_time);
        maximumRideTime.setText(maxRideTime);
        maximumRideTimeExceedingFine = findViewById(R.id.maximum_ride_exceeding_fine);
        maximumRideTimeExceedingFine.setText(maxRideTimeExceedingFine);
        maximumHoldTime = findViewById(R.id.maximum_hold_time);
        maximumHoldTime.setText(maxHoldTime);
        maximumHoldTimeExceedingFine = findViewById(R.id.max_hold_exceeding_fine);
        maximumHoldTimeExceedingFine.setText(maxHoldTimeExceedingFine);
        trackBicycle = findViewById(R.id.bicycle_track);
        trackBicycle.setText(trackBicycleData);
        /*minimumAmountFirstRide = findViewById(R.id.min_amount_first_ride);
        minimumAmountFirstRide.setText(minFirstAmountRide);
        baseValueRatechart = findViewById(R.id.base_vale_rate_chart);
        baseValueRatechart.setText(baseValueRateChart);
        */
        serviceStartTime = findViewById(R.id.service_start_time);
        serviceStartTime.setText(serviceHourStartTime);
        serviceEndTime = findViewById(R.id.service_end_time);
        serviceEndTime.setText(serviceHourEndTime);
        serviceHourExceedingFine = findViewById(R.id.service_hour_exceeding_fine);
        serviceHourExceedingFine.setText(serviceHourTimeExceedingFine);
        geofencineFine = findViewById(R.id.geofencing_fine);
        geofencineFine.setText(geofencingFine);
        customerServiceNumber = findViewById(R.id.customer_support_number);
        customerServiceNumber.setText(customerHelpServiceNumber);
        layoutAddAreaLegal = findViewById(R.id.layout_add_area_legal);
        addAreaLegal = findViewById(R.id.add_area_legal);
        layoutAddAreaLegal.setOnClickListener(this);
        addAreaLegal.setOnClickListener(this);
        layoutAddAreaSubscription = findViewById(R.id.layout_add_area_subscription);
        addAreaSubscription = findViewById(R.id.add_area_subscription);
        layoutAddAreaSubscription.setOnClickListener(this);
        addAreaSubscription.setOnClickListener(this);
        layoutAddStations = findViewById(R.id.layout_add_stations);
        layoutAddStations.setOnClickListener(this);
       /* layoutAddRateChart = findViewById(R.id.layout_add_rate_chart);
        addRateChart = findViewById(R.id.add_rate_chart);
        layoutAddRateChart.setOnClickListener(this);
        addRateChart.setOnClickListener(this);*/
        done = findViewById(R.id.done_button);
        done.setOnClickListener(this);
        serviceStartTime.setOnClickListener(this);
        serviceEndTime.setOnClickListener(this);
        stationSpinner = findViewById(R.id.add_stations);
        stationSpinner.setAdapter(new MyAdapter(this, R.layout.custom_spinner, spinnerValue));
        stationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        final Animation animShake = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.shake);
                        stationSpinner.startAnimation(animShake);
                        break;
                    case 1:
                        station = stationSpinner.getSelectedItem().toString();
                        Log.d(TAG, "Option:" + station);
                        Intent layout_station = new Intent(AreaDetails.this, AddStation.class);
                        layout_station.putExtra("marker_list", markerList);
                        startActivityForResult(layout_station, 3);
                        break;
                    case 2:
                        station = stationSpinner.getSelectedItem().toString();
                        Log.d(TAG, "Option:" + station + "\t" + areaId);
                        Intent area_station = new Intent(AreaDetails.this, ManageStation.class);
                        area_station.putExtra("area_id", areaId);
                        startActivity(area_station); // This will not get any updated data from the ManageStation->StationDetails. this is only to inflate the station details
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        customAlertDialog = new CustomAlertDialog(this, R.style.WideDialog, "Success!", "Area is added successfully");
        customAlertDialog.onPositiveButton(view -> {
            customAlertDialog.dismiss();
            startActivity(new Intent(AreaDetails.this, MainActivity.class));
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(AreaDetails.this, ManageArea.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.back_button) {
            Intent intent = new Intent(AreaDetails.this, ManageArea.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
/*else if (v.getId() == R.id.edit) {
    changeAreaName();
}*/
        else if (v.getId() == R.id.layout_add_area_legal) {
            Intent layout_area_legal = new Intent(AreaDetails.this, AreaLegal.class);
            layout_area_legal.putExtra("areaCondition", areaCondition);
            layout_area_legal.putExtra("baseFareCondition", baseFareCondition);
            layout_area_legal.putExtra("geofencingCondition", geofencingCondition);
            layout_area_legal.putExtra("serviceCondition", serviceCondition);
            layout_area_legal.putExtra("subscriptionCondition", subscriptionCondition);
            startActivityForResult(layout_area_legal, 1);
        }
        else if (v.getId() == R.id.add_area_legal) {
            Intent area_legal = new Intent(AreaDetails.this, AreaLegal.class);
            area_legal.putExtra("areaCondition", areaCondition);
            area_legal.putExtra("baseFareCondition", baseFareCondition);
            area_legal.putExtra("geofencingCondition", geofencingCondition);
            area_legal.putExtra("serviceCondition", serviceCondition);
            area_legal.putExtra("subscriptionCondition", subscriptionCondition);
            startActivityForResult(area_legal, 1);
        }
        else if (v.getId() == R.id.layout_add_area_subscription) {
            Intent layout_area_subscription = new Intent(AreaDetails.this, AreaSubscription.class);
            startActivityForResult(layout_area_subscription, 2);
        }
        else if (v.getId() == R.id.add_area_subscription) {
            Intent area_subscription = new Intent(AreaDetails.this, AreaSubscription.class);
            startActivityForResult(area_subscription, 2);
        }
/*else if (v.getId() == R.id.layout_add_rate_chart) {
    Intent area_rate = new Intent(AreaDetails.this, RateChart.class);
    startActivityForResult(area_rate, 4);
}
else if (v.getId() == R.id.add_rate_chart) {
    Intent rate = new Intent(AreaDetails.this, RateChart.class);
    startActivityForResult(rate, 4);
}*/
        else if (v.getId() == R.id.service_start_time) {
            Calendar calendar = Calendar.getInstance();
            currentHour = calendar.get(Calendar.HOUR_OF_DAY);
            currentMinute = calendar.get(Calendar.MINUTE);
            TimePickerDialog timePickerDialog = new TimePickerDialog(AreaDetails.this, (timePicker, hourOfDay, minutes) -> {
                if (hourOfDay >= 12) {
                    amPm = "PM";
                } else {
                    amPm = "AM";
                }
                serviceStartTime.setText(String.format("%02d:%02d", hourOfDay, minutes) + amPm);
            }, currentHour, currentMinute, false);
            timePickerDialog.show();
        }
        else if (v.getId() == R.id.service_end_time) {
            Calendar calendar_one = Calendar.getInstance();
            currentHour = calendar_one.get(Calendar.HOUR_OF_DAY);
            currentMinute = calendar_one.get(Calendar.MINUTE);
            TimePickerDialog timePickerDialog_one = new TimePickerDialog(AreaDetails.this, (timePicker, hourOfDay, minutes) -> {
                if (hourOfDay >= 12) {
                    amPm = "PM";
                } else {
                    amPm = "AM";
                }
                serviceEndTime.setText(String.format("%02d:%02d", hourOfDay, minutes) + amPm);
            }, currentHour, currentMinute, false);
            timePickerDialog_one.show();
        }
        else if (v.getId() == R.id.done_button) {
            updateAreaDetails(organisation, areaId);
        }


//        switch (v.getId()) {
//            case R.id.back_button:
//                Intent intent = new Intent(AreaDetails.this, ManageArea.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                startActivity(intent);
//                break;
//            /*case R.id.edit:
//                changeAreaName();
//                break;*/
//            case R.id.layout_add_area_legal:
//                Intent layout_area_legal = new Intent(AreaDetails.this, AreaLegal.class);
//                layout_area_legal.putExtra("areaCondition", areaCondition);
//                layout_area_legal.putExtra("baseFareCondition", baseFareCondition);
//                layout_area_legal.putExtra("geofencingCondition", geofencingCondition);
//                layout_area_legal.putExtra("serviceCondition", serviceCondition);
//                layout_area_legal.putExtra("subscriptionCondition", subscriptionCondition);
//                startActivityForResult(layout_area_legal, 1);
//                break;
//            case R.id.add_area_legal:
//                Intent area_legal = new Intent(AreaDetails.this, AreaLegal.class);
//                area_legal.putExtra("areaCondition", areaCondition);
//                area_legal.putExtra("baseFareCondition", baseFareCondition);
//                area_legal.putExtra("geofencingCondition", geofencingCondition);
//                area_legal.putExtra("serviceCondition", serviceCondition);
//                area_legal.putExtra("subscriptionCondition", subscriptionCondition);
//                startActivityForResult(area_legal, 1);
//                break;
//            case R.id.layout_add_area_subscription:
//                Intent layout_area_subscription = new Intent(AreaDetails.this, AreaSubscription.class);
//                startActivityForResult(layout_area_subscription, 2);
//                break;
//            case R.id.add_area_subscription:
//                Intent area_subscription = new Intent(AreaDetails.this, AreaSubscription.class);
//                startActivityForResult(area_subscription, 2);
//                break;
//           /* case R.id.layout_add_rate_chart:
//                Intent area_rate = new Intent(AreaDetails.this, RateChart.class);
//                startActivityForResult(area_rate, 4);
//                break;
//            case R.id.add_rate_chart:
//                Intent rate = new Intent(AreaDetails.this, RateChart.class);
//                startActivityForResult(rate, 4);
//                break;*/
//            case R.id.service_start_time:
//                Calendar calendar = Calendar.getInstance();
//                currentHour = calendar.get(Calendar.HOUR_OF_DAY);
//                currentMinute = calendar.get(Calendar.MINUTE);
//                TimePickerDialog timePickerDialog = new TimePickerDialog(AreaDetails.this, (timePicker, hourOfDay, minutes) -> {
//                    if (hourOfDay >= 12) {
//                        amPm = "PM";
//                    } else {
//                        amPm = "AM";
//                    }
//                    serviceStartTime.setText(String.format("%02d:%02d", hourOfDay, minutes) + amPm);
//                }, currentHour, currentMinute, false);
//                timePickerDialog.show();
//                break;
//            case R.id.service_end_time:
//                Calendar calendar_one = Calendar.getInstance();
//                currentHour = calendar_one.get(Calendar.HOUR_OF_DAY);
//                currentMinute = calendar_one.get(Calendar.MINUTE);
//                TimePickerDialog timePickerDialog_one = new TimePickerDialog(AreaDetails.this, (timePicker, hourOfDay, minutes) -> {
//                    if (hourOfDay >= 12) {
//                        amPm = "PM";
//                    } else {
//                        amPm = "AM";
//                    }
//                    serviceEndTime.setText(String.format("%02d:%02d", hourOfDay, minutes) + amPm);
//                }, currentHour, currentMinute, false);
//                timePickerDialog_one.show();
//                break;
//            case R.id.done_button:
//                updateAreaDetails(organisation, areaId);
//                break;
//            default:
//                break;
//        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) { //area legal data from Area Legal
            geofencingCondition = data.getStringExtra("geofencing_fine"); //gefoencing fine condition
            baseFareCondition = data.getStringExtra("base_fare_condition");
            serviceCondition = data.getStringExtra("service_condition");
            subscriptionCondition = data.getStringExtra("subscription_condition");
            areaCondition = data.getStringExtra("area_condition");
            Log.d(TAG, "From AreaLegal:" + geofencingCondition + "\t" + baseFareCondition + "\t" + serviceCondition + "\t" + subscriptionCondition + "\t" + areaCondition);
        }
        if (requestCode == 2) { //area subscription data from Area Subscription
            //subscriptionId = data.getStringExtra("subscription_id");
            planName = data.getStringExtra("plan_name");
            validityTime = data.getStringExtra("validity_time");
            pricePlan = data.getStringExtra("plan_price");
            maxFreeRide = data.getStringExtra("max_free_ride");
            subscriptionDescription = data.getStringExtra("subscription_description");
            carryForward = data.getIntExtra("carry_forward", 5);
            Log.d(TAG, "From AreaSubscription:" + planName + "\t" + validityTime + "\t" + pricePlan + "\t" + maxFreeRide + "\t" + subscriptionDescription + "\t" +
                    "\t" + carryForward);
        }
        if (requestCode == 3) { //add new station from Add Station
            stationId = data.getStringExtra("station_id");
            stationName = data.getStringExtra("station_name");
            stationLatitude = data.getStringExtra("station_latitude");
            stationLongitude = data.getStringExtra("station_longitude");
            stationRadius = data.getStringExtra("station_radius");
            stationArrayList = (ArrayList) data.getSerializableExtra("station_details");
            Log.d(TAG, "From AddStation:" + stationId + "\t" + stationName + "\t" + stationLatitude + "\t" + stationLongitude + "\t" + stationRadius);
        }
       /* if (requestCode == 4) { //add rate chart from Rate Chart
            rateId = data.getStringExtra("rate_id");
            money = data.getIntExtra("money", 6);
            rateHourSlab = data.getStringExtra("hour_slab");
            rateMinSlab = data.getStringExtra("min_slab");
            rateChartTime = ((Integer.parseInt(rateHourSlab) * 60) + Integer.parseInt(rateMinSlab));
            Log.d(TAG, "From AreaRate:" + rateId + "\t" + money + "\t" + rateHourSlab + "\t" + rateMinSlab + "\t" + rateChartTime);
        }*/
    }

    private void changeAreaName() {
        final AlertDialog dialogBuilder = new AlertDialog.Builder(this).create();
        LayoutInflater inflater = this.getLayoutInflater();
        Objects.requireNonNull(dialogBuilder.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        View dialogView = inflater.inflate(R.layout.custom_area_name_dialog, null);
        final EditText areaname = dialogView.findViewById(R.id.area_name);
        final TextView area_tv = dialogView.findViewById(R.id.area_tv);
        area_tv.setText("Change the name of the area");
        Button proceed = dialogView.findViewById(R.id.proceed);
        proceed.setOnClickListener(view -> {
            final Animation animShake = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.shake);
            if (area_name.getText().toString().isEmpty()) {
                area_name.startAnimation(animShake);
            } else {
                updatedAreaName = areaname.getText().toString().trim();
                Log.d(TAG, "Area Name : " + updatedAreaName);
                area_name.setText(updatedAreaName);
                dialogBuilder.dismiss();
                //clearField();
            }
        });
        dialogBuilder.setView(dialogView);
        dialogBuilder.show();
        dialogBuilder.setCancelable(true);
    }

    public class MyAdapter extends ArrayAdapter<String> {
        public MyAdapter(Context ctx, int txtViewResourceId, String[] objects) {
            super(ctx, txtViewResourceId, objects);
        }

        @Override
        public View getDropDownView(int position, View cnvtView, @NonNull ViewGroup prnt) {
            return getCustomView(position, cnvtView, prnt);
        }

        @NonNull
        @Override
        public View getView(int pos, View cnvtView, @NonNull ViewGroup prnt) {
            return getCustomView(pos, cnvtView, prnt);
        }

        public View getCustomView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = getLayoutInflater();
            View stationSpinner = inflater.inflate(R.layout.custom_spinner, parent, false);
            TextView spinner_text = stationSpinner.findViewById(R.id.spinner_text);
            spinner_text.setText(spinnerValue[position]);
            return stationSpinner;
        }
    }

    private void updateAreaDetails(String organisation, String areaId) {
        Log.d(TAG, "Organisation Details:" + organisation + "\t" + areaId + "\t" + designation);
        if (designation.equals("Zone Manager") && criteriaCheck()) {
            final Area area = new Area(areaId);
            if ((subscriptionId != null && planName != null && validityTime != null && pricePlan != null && maxFreeRide != null && subscriptionDescription != null || carryForward != 0 || subscriptionPlanTime != 0) && !markerList.isEmpty() && stationId != null && stationName != null && stationLatitude != null && stationLongitude != null && stationRadius != null) {
                Log.d(TAG, "Area details, new station, new subscription will be added");
                areaUpdate(area);
                addSubscription();
                addStation();
                addStationList();

            } else if (planName != null && validityTime != null && pricePlan != null && maxFreeRide != null &&
                    subscriptionDescription != null || carryForward != 0) {
                Log.d(TAG, "Area details, new subscription will be added");
                areaUpdate(area);
                addSubscription();

            } else if ((!markerList.isEmpty() && stationId != null && stationName
                    != null && stationLatitude != null && stationLongitude != null && stationRadius != null)) {
                Log.d(TAG, "Area details, new station will be added");
                areaUpdate(area);
                addStation();
                addStationList();

            } else {
                Log.d(TAG, "Area details will be added");
                areaUpdate(area);

            }
            customAlertDialog.setTitle("Area Details!");
            customAlertDialog.setMsg("Area details is successfully updated to the database");
            customAlertDialog.show();
        }
    }

    private boolean criteriaCheck() {
        // final Animation animShake = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.shake);
        if (TextUtils.isEmpty(maximumRideTime.getText())) {
            maximumRideTime.setError(getResources().getString(R.string.empty_field_message));
            return false;
        }
        if (TextUtils.isEmpty(maximumRideTimeExceedingFine.getText())) {
            maximumRideTimeExceedingFine.setError(getResources().getString(R.string.empty_field_message));
            return false;
        }
        if (TextUtils.isEmpty(maximumHoldTime.getText())) {
            maximumHoldTime.setError(getResources().getString(R.string.empty_field_message));
            return false;
        }
        if (TextUtils.isEmpty(maximumHoldTimeExceedingFine.getText())) {
            maximumHoldTimeExceedingFine.setError(getResources().getString(R.string.empty_field_message));
            return false;
        }
        /*if (TextUtils.isEmpty(minimumAmountFirstRide.getText())) {
            minimumAmountFirstRide.setError(getResources().getString(R.string.empty_field_message));
            return false;
        }
        if (TextUtils.isEmpty(baseValueRatechart.getText())) {
            baseValueRatechart.setError(getResources().getString(R.string.empty_field_message));
            return false;
        }*/
        if (TextUtils.isEmpty(serviceStartTime.getText())) {
            serviceStartTime.setError(getResources().getString(R.string.empty_field_message));
            return false;
        }
        if (TextUtils.isEmpty(serviceEndTime.getText())) {
            serviceEndTime.setError(getResources().getString(R.string.empty_field_message));
            return false;
        }
        if (TextUtils.isEmpty(serviceHourExceedingFine.getText())) {
            serviceHourExceedingFine.setError(getResources().getString(R.string.empty_field_message));
            return false;
        }
        if (TextUtils.isEmpty(geofencineFine.getText())) {
            geofencineFine.setError(getResources().getString(R.string.empty_field_message));
            return false;
        }
        if (TextUtils.isEmpty(customerServiceNumber.getText())) {
            customerServiceNumber.setError(getResources().getString(R.string.empty_field_message));
            return false;
        }
        /*if (TextUtils.isEmpty(geofencingCondition) && TextUtils.isEmpty(baseFareCondition) && TextUtils.isEmpty(serviceCondition) &&
                TextUtils.isEmpty(subscriptionCondition) && TextUtils.isEmpty(areaCondition)) {
            layoutAddAreaLegal.startAnimation(animShake);
        }
        if (TextUtils.isEmpty(stationId) && TextUtils.isEmpty(stationName) && TextUtils.isEmpty(stationLatitude) && TextUtils.isEmpty(stationLongitude) && TextUtils.isEmpty(stationRadius)) {
            layoutAddStations.startAnimation(animShake);
        }*/
        return true;
    }

    private void areaUpdate(Area area) {
        areaDbPath.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child(area.getAreaId()).exists()) {
                    Log.d(TAG, "Area id is present: " + area.getAreaId());
                    try {
                        areaDbPath.child(area.getAreaId()).child("areaName").setValue(area_name.getText().toString().trim());
                        areaDbPath.child(area.getAreaId()).child("maximumRideTime").setValue(maximumRideTime.getText().toString().trim());
                        areaDbPath.child(area.getAreaId()).child("maxRideTimeExceedingFine").setValue(maximumRideTimeExceedingFine.getText().toString().trim());
                        areaDbPath.child(area.getAreaId()).child("maxHoldTime").setValue(maximumHoldTime.getText().toString().trim());
                        areaDbPath.child(area.getAreaId()).child("maxHoldTimeExceedingFine").setValue(maximumHoldTimeExceedingFine.getText().toString().trim());
                        areaDbPath.child(area.getAreaId()).child("trackBicycle").setValue(trackBicycle.getText().toString().trim());
                       /* areaDbPath.child(area.getAreaId()).child("minFirstAmountRide").setValue(minimumAmountFirstRide.getText().toString().trim());
                        areaDbPath.child(area.getAreaId()).child("baseValueRateChart").setValue(baseValueRatechart.getText().toString().trim());
                       */
                        areaDbPath.child(area.getAreaId()).child("serviceStartTime").setValue(serviceStartTime.getText().toString().trim());
                        areaDbPath.child(area.getAreaId()).child("serviceEndTime").setValue(serviceEndTime.getText().toString().trim());
                        areaDbPath.child(area.getAreaId()).child("serviceHourExceedingFine").setValue(serviceHourExceedingFine.getText().toString().trim());
                        areaDbPath.child(area.getAreaId()).child("geofencingFine").setValue(geofencineFine.getText().toString().trim());
                        areaDbPath.child(area.getAreaId()).child("customerServiceNumber").setValue(customerServiceNumber.getText().toString().trim());
                        areaDbPath.child(area.getAreaId()).child("geofencingCondition").setValue(geofencingCondition);
                        areaDbPath.child(area.getAreaId()).child("baseFareCondition").setValue(baseFareCondition);
                        areaDbPath.child(area.getAreaId()).child("serviceCondition").setValue(serviceCondition);
                        areaDbPath.child(area.getAreaId()).child("subscriptionCondition").setValue(subscriptionCondition);
                        areaDbPath.child(area.getAreaId()).child("areaCondition").setValue(areaCondition);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.d(TAG, "Catch error: " + e.toString());
                    }
                } else {
                    Log.d(TAG, "Area id is not present");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, "Error in Area: " + databaseError.toString());
            }
        });
    }

    private void addSubscription() {
        subscriptionDbPath.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                /*Subscription createSubscription = new Subscription(areaId, area_name.getText().toString(), subscriptionId, planName, validityTime, pricePlan, maxFreeRide, subscriptionDescription,
                        carryForward, true,  parent, currentDate);
                subscriptionDbPath.child(createSubscription.getSubscriptionId()).setValue(createSubscription);
*/
                subscriptionDbPath.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        subscriptionDbPath.child(sharedPreferences.getString("organisationName", "no_data").
                                replace(" ", "") + "_SP_" + dataSnapshot.getChildrenCount()).child("areaId").setValue(areaId);
                        subscriptionDbPath.child(sharedPreferences.getString("organisationName", "no_data").
                                replace(" ", "") + "_SP_" + dataSnapshot.getChildrenCount()).child("subscriptionId")
                                .setValue(sharedPreferences.getString("organisationName", "no_data").replace(" ", "") + "_SP_" + dataSnapshot.getChildrenCount());
                        subscriptionDbPath.child(sharedPreferences.getString("organisationName", "no_data").
                                replace(" ", "") + "_SP_" + dataSnapshot.getChildrenCount()).child("areaName").setValue(area_name.getText().toString().trim());
                        subscriptionDbPath.child(sharedPreferences.getString("organisationName", "no_data").
                                replace(" ", "") + "_SP_" + dataSnapshot.getChildrenCount()).child("subscriptionPlanName").setValue(planName);
                        subscriptionDbPath.child(sharedPreferences.getString("organisationName", "no_data").
                                replace(" ", "") + "_SP_" + dataSnapshot.getChildrenCount()).child("subscriptionValidityTime").setValue(validityTime);
                        subscriptionDbPath.child(sharedPreferences.getString("organisationName", "no_data").
                                replace(" ", "") + "_SP_" + dataSnapshot.getChildrenCount()).child("subscriptionPlanPrice").setValue(pricePlan);
                        subscriptionDbPath.child(sharedPreferences.getString("organisationName", "no_data").
                                replace(" ", "") + "_SP_" + dataSnapshot.getChildrenCount()).child("subscriptionMaxFreeRide").setValue(maxFreeRide);
                        subscriptionDbPath.child(sharedPreferences.getString("organisationName", "no_data").
                                replace(" ", "") + "_SP_" + dataSnapshot.getChildrenCount()).child("subscriptionDescription").setValue(subscriptionDescription);
                        subscriptionDbPath.child(sharedPreferences.getString("organisationName", "no_data").
                                replace(" ", "") + "_SP_" + dataSnapshot.getChildrenCount()).child("subscriptionCarryForward").setValue(carryForward);
                        subscriptionDbPath.child(sharedPreferences.getString("organisationName", "no_data").
                                replace(" ", "") + "_SP_" + dataSnapshot.getChildrenCount()).child("subscriptionStatus").setValue("true");
                        subscriptionDbPath.child(sharedPreferences.getString("organisationName", "no_data").
                                replace(" ", "") + "_SP_" + dataSnapshot.getChildrenCount()).child("createdBy").setValue(parent);
                        subscriptionDbPath.child(sharedPreferences.getString("organisationName", "no_data").
                                replace(" ", "") + "_SP_" + dataSnapshot.getChildrenCount()).child("createdDate").setValue(currentDate);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, "Error in station list: " + databaseError.toString());
            }
        });
    }

    private void addStation() {
        stationDbPath.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (Station i : stationArrayList) {
                    Station createStation = new Station(areaId, area_name.getText().toString(), markerList, i.getStationId(), i.getStationName(), i.getStationLatitude(), i.getStationLongitude(), i.getStationRadius(),
                            i.getStationType(), true, parent, currentDate);
                    stationDbPath.child(createStation.getStationId()).setValue(createStation);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, "Error in Station: " + databaseError.toString());
            }
        });
    }

    private void addStationList() {
        stationListDbPath.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (Station i : stationArrayList) {
                    StationList stationList = new StationList(i.getStationId(), true);
                    stationListDbPath.child(stationList.getStationId()).setValue(stationList);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, "Error in station list: " + databaseError.toString());
            }
        });
    }
}
