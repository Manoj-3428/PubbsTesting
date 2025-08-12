package in.pubbs.pubbsadmin;

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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

import in.pubbs.pubbsadmin.Model.Area;
import in.pubbs.pubbsadmin.Model.AreaRate;
import in.pubbs.pubbsadmin.Model.Station;
import in.pubbs.pubbsadmin.Model.StationList;
import in.pubbs.pubbsadmin.Model.Subscription;
import in.pubbs.pubbsadmin.View.CustomAlertDialog;

/*created by Parita Dey*/
public class ManageSystem extends AppCompatActivity implements View.OnClickListener, ValueEventListener {

    TextView toolbarText, area_name, addAreaLegal, addAreaSubscription, addStations, addRateChart, serviceStartTime, serviceEndTime;
    RelativeLayout layoutAddAreaLegal, layoutAddAreaSubscription, layoutAddStations, layoutAddRateChart;
    ImageView back, edit;
    public ArrayList<LatLng> markerList = new ArrayList<>();
    private String TAG = ManageSystem.class.getSimpleName();
    Button done;
    String amPm, currentDate, organisationName, parent, areaId, subscriptionId, areaName, updatedAreaName, geofencingCondition, baseFareCondition, serviceCondition, subscriptionCondition, areaCondition, planName, validityTime, pricePlan, maxFreeRide, subscriptionDescription, valueHourSlab, valueMinSlab, stationName, stationId, stationLatitude, stationLongitude, stationRadius, rateId, rateHourSlab, rateMinSlab;
    int carryForward, money, subscriptionPlanTime, rateChartTime, currentHour, currentMinute;
    EditText maximumRideTime, maximumRideTimeExceedingFine, maximumHoldTime, maximumHoldTimeExceedingFine, serviceHourExceedingFine, geofencineFine, customerServiceNumber, trackBicycle;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference areaDbReference, stationDbReference, subscriptionDbReference, rateChartDbReference, stationListDbReference;
    SharedPreferences sharedPreferences;
    CustomAlertDialog customAlertDialog;
    private ArrayList<Station> stationArrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_system);
        initView();
    }

    private void initView() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        currentDate = sdf.format(new Date());
        Log.d(TAG, "Current date:" + currentDate);
        sharedPreferences = getSharedPreferences(getResources().getString(R.string.sharedPreferences), Context.MODE_PRIVATE);
        organisationName = sharedPreferences.getString("organisationName", "no_data").replace(" ", "");
        parent = sharedPreferences.getString("mobileValue", null);
        Log.d(TAG, "SharedPreference value:" + organisationName + "\t" + parent);
        //database creation
        firebaseDatabase = FirebaseDatabase.getInstance();
        areaDbReference = firebaseDatabase.getReference().child(organisationName.replaceAll(" ", "")).child("Area");
        stationDbReference = firebaseDatabase.getReference().child(organisationName.replaceAll(" ", "")).child("Station");
        subscriptionDbReference = firebaseDatabase.getReference().child(organisationName.replace(" ", "")).child("Subscription");
        rateChartDbReference = firebaseDatabase.getReference().child(organisationName.replaceAll(" ", "")).child("AreaRate");
        stationListDbReference = firebaseDatabase.getReference().child(organisationName.replaceAll(" ", "")).child("Area").child("StationList");
        toolbarText = findViewById(R.id.toolbar_title);
        toolbarText.setText("Area Details");
        back = findViewById(R.id.back_button);
        back.setOnClickListener(this);
        Intent intent = getIntent();
        markerList = intent.getParcelableArrayListExtra("markerList");
        Log.d(TAG, "area: " + markerList);
        areaId = intent.getStringExtra("areaId");
        areaName = intent.getStringExtra("areaName");
        Log.d(TAG, "area details: " + markerList + "\t" + areaId + "\t" + areaName);
        area_name = findViewById(R.id.area_name);
        area_name.setText(areaName);
        edit = findViewById(R.id.edit);
        edit.setOnClickListener(this);
        maximumRideTime = findViewById(R.id.maximum_ride_time);
        maximumRideTimeExceedingFine = findViewById(R.id.maximum_ride_exceeding_fine);
        maximumHoldTime = findViewById(R.id.maximum_hold_time);
        maximumHoldTimeExceedingFine = findViewById(R.id.max_hold_exceeding_fine);
        trackBicycle = findViewById(R.id.bicycle_track);
        //    minimumAmountFirstRide = findViewById(R.id.min_amount_first_ride);
        // baseValueRatechart = findViewById(R.id.base_vale_rate_chart);
        serviceStartTime = findViewById(R.id.service_start_time);
        serviceEndTime = findViewById(R.id.service_end_time);
        serviceHourExceedingFine = findViewById(R.id.service_hour_exceeding_fine);
        geofencineFine = findViewById(R.id.geofencing_fine);
        customerServiceNumber = findViewById(R.id.customer_support_number);
        layoutAddAreaLegal = findViewById(R.id.layout_add_area_legal);
        addAreaLegal = findViewById(R.id.add_area_legal);
        layoutAddAreaLegal.setOnClickListener(this);
        addAreaLegal.setOnClickListener(this);
        layoutAddAreaSubscription = findViewById(R.id.layout_add_area_subscription);
        addAreaSubscription = findViewById(R.id.add_area_subscription);
        layoutAddAreaSubscription.setOnClickListener(this);
        addAreaSubscription.setOnClickListener(this);
        layoutAddStations = findViewById(R.id.layout_add_stations);
        addStations = findViewById(R.id.add_stations);
        layoutAddStations.setOnClickListener(this);
        addStations.setOnClickListener(this);
        layoutAddRateChart = findViewById(R.id.layout_add_rate_chart);
        addRateChart = findViewById(R.id.add_rate_chart);
        layoutAddRateChart.setOnClickListener(this);
        addRateChart.setOnClickListener(this);
        done = findViewById(R.id.done_button);
        done.setOnClickListener(this);
        serviceStartTime.setOnClickListener(this);
        serviceEndTime.setOnClickListener(this);

        customAlertDialog = new CustomAlertDialog(this, R.style.WideDialog, "Success!", "Area is added successfully");
        customAlertDialog.onPositiveButton(view -> {
            customAlertDialog.dismiss();
            startActivity(new Intent(ManageSystem.this, MainActivity.class));
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(ManageSystem.this, AddArea.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        if (id == R.id.back_button) {
            Intent intent = new Intent(ManageSystem.this, AddArea.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else if (id == R.id.edit) {
            changeAreaName();
        } else if (id == R.id.layout_add_area_legal || id == R.id.add_area_legal) {
            Intent area_legal = new Intent(ManageSystem.this, AreaLegal.class);
            startActivityForResult(area_legal, 1);
        } else if (id == R.id.layout_add_area_subscription || id == R.id.add_area_subscription) {
            Intent area_subscription = new Intent(ManageSystem.this, AreaSubscription.class);
            startActivityForResult(area_subscription, 2);
        } else if (id == R.id.layout_add_stations || id == R.id.add_stations) {
            Intent station = new Intent(ManageSystem.this, AddStation.class);
            station.putExtra("markerList", markerList);
            startActivityForResult(station, 3);
        } else if (id == R.id.layout_add_rate_chart || id == R.id.add_rate_chart) {
            Intent rate = new Intent(ManageSystem.this, RateChart.class);
            startActivityForResult(rate, 4);
        } else if (id == R.id.service_start_time) {
            Calendar calendar = Calendar.getInstance();
            currentHour = calendar.get(Calendar.HOUR_OF_DAY);
            currentMinute = calendar.get(Calendar.MINUTE);
            TimePickerDialog timePickerDialog = new TimePickerDialog(ManageSystem.this, (timePicker, hourOfDay, minutes) -> {
                amPm = (hourOfDay >= 12) ? "PM" : "AM";
                serviceStartTime.setText(String.format("%02d:%02d", hourOfDay, minutes) + amPm);
            }, currentHour, currentMinute, false);
            timePickerDialog.show();
        } else if (id == R.id.service_end_time) {
            Calendar calendar_one = Calendar.getInstance();
            currentHour = calendar_one.get(Calendar.HOUR_OF_DAY);
            currentMinute = calendar_one.get(Calendar.MINUTE);
            TimePickerDialog timePickerDialog_one = new TimePickerDialog(ManageSystem.this, (timePicker, hourOfDay, minutes) -> {
                amPm = (hourOfDay >= 12) ? "PM" : "AM";
                serviceEndTime.setText(String.format("%02d:%02d", hourOfDay, minutes) + amPm);
            }, currentHour, currentMinute, false);
            timePickerDialog_one.show();
        } else if (id == R.id.done_button) {
            if (criteriaCheck()) {
                areaDbReference.addListenerForSingleValueEvent(this);
                stationDbReference.addListenerForSingleValueEvent(this);
                stationListDbReference.addListenerForSingleValueEvent(this);
                updateZoneDetails(areaName, areaId);

                rateChartDbReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String organisationName = sharedPreferences.getString("organisationName", "no_data").replace(" ", "");
                        String rateChartId = organisationName + "_RC_" + dataSnapshot.getChildrenCount();
                        rateChartDbReference.child(rateChartId).child("areaId").setValue(areaId);
                        rateChartDbReference.child(rateChartId).child("rateMoney").setValue(money);
                        rateChartDbReference.child(rateChartId).child("rateTime").setValue(rateChartTime);
                        rateChartDbReference.child(rateChartId).child("createdBy").setValue(parent);
                        rateChartDbReference.child(rateChartId).child("createdDate").setValue(currentDate);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                });

                subscriptionDbReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String organisationName = sharedPreferences.getString("organisationName", "no_data").replace(" ", "");
                        String subscriptionId = organisationName + "_SP_" + dataSnapshot.getChildrenCount();
                        subscriptionDbReference.child(subscriptionId).child("areaId").setValue(areaId);
                        subscriptionDbReference.child(subscriptionId).child("subscriptionId").setValue(subscriptionId);
                        subscriptionDbReference.child(subscriptionId).child("areaName").setValue(area_name.getText().toString().trim());
                        subscriptionDbReference.child(subscriptionId).child("subscriptionPlanName").setValue(planName);
                        subscriptionDbReference.child(subscriptionId).child("subscriptionValidityTime").setValue(validityTime);
                        subscriptionDbReference.child(subscriptionId).child("subscriptionPlanPrice").setValue(pricePlan);
                        subscriptionDbReference.child(subscriptionId).child("subscriptionMaxFreeRide").setValue(maxFreeRide);
                        subscriptionDbReference.child(subscriptionId).child("subscriptionDescription").setValue(subscriptionDescription);
                        subscriptionDbReference.child(subscriptionId).child("subscriptionCarryForward").setValue(carryForward);
                        subscriptionDbReference.child(subscriptionId).child("subscriptionStatus").setValue("true");
                        subscriptionDbReference.child(subscriptionId).child("createdBy").setValue(parent);
                        subscriptionDbReference.child(subscriptionId).child("createdDate").setValue(currentDate);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                });
            }
        }
    }


//    @Override
//    public void onClick(View view) {
//        switch (view.getId()) {
//            case R.id.back_button:
//                Intent intent = new Intent(ManageSystem.this, AddArea.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                startActivity(intent);
//                break;
//            case R.id.edit:
//                changeAreaName();
//                break;
//            case R.id.layout_add_area_legal:
//                Intent layout_area_legal = new Intent(ManageSystem.this, AreaLegal.class);
//                startActivityForResult(layout_area_legal, 1);
//                break;
//            case R.id.add_area_legal:
//                Intent area_legal = new Intent(ManageSystem.this, AreaLegal.class);
//                startActivityForResult(area_legal, 1);
//                break;
//            case R.id.layout_add_area_subscription:
//                Intent layout_area_subscription = new Intent(ManageSystem.this, AreaSubscription.class);
//                startActivityForResult(layout_area_subscription, 2);
//                break;
//            case R.id.add_area_subscription:
//                Intent area_subscription = new Intent(ManageSystem.this, AreaSubscription.class);
//                startActivityForResult(area_subscription, 2);
//                break;
//            case R.id.layout_add_stations:
//                Intent layout_station = new Intent(ManageSystem.this, AddStation.class);
//                layout_station.putExtra("markerList", markerList);
//                startActivityForResult(layout_station, 3);
//                break;
//            case R.id.add_stations:
//                Intent station = new Intent(ManageSystem.this, AddStation.class);
//                station.putExtra("markerList", markerList);
//                startActivityForResult(station, 3);
//                break;
//            case R.id.layout_add_rate_chart:
//                Intent area_rate = new Intent(ManageSystem.this, RateChart.class);
//                startActivityForResult(area_rate, 4);
//                break;
//            case R.id.add_rate_chart:
//                Intent rate = new Intent(ManageSystem.this, RateChart.class);
//                startActivityForResult(rate, 4);
//                break;
//            case R.id.service_start_time:
//                Calendar calendar = Calendar.getInstance();
//                currentHour = calendar.get(Calendar.HOUR_OF_DAY);
//                currentMinute = calendar.get(Calendar.MINUTE);
//                TimePickerDialog timePickerDialog = new TimePickerDialog(ManageSystem.this, (timePicker, hourOfDay, minutes) -> {
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
//                TimePickerDialog timePickerDialog_one = new TimePickerDialog(ManageSystem.this, (timePicker, hourOfDay, minutes) -> {
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
//                if (criteriaCheck()) {
//                    areaDbReference.addListenerForSingleValueEvent(this);
//                    stationDbReference.addListenerForSingleValueEvent(this);
//                    // subscriptionDbReference.addListenerForSingleValueEvent(this);
//                    //rateChartDbReference.addListenerForSingleValueEvent(this);
//                    stationListDbReference.addListenerForSingleValueEvent(this);
//                    updateZoneDetails(areaName, areaId);
//                    rateChartDbReference.addListenerForSingleValueEvent(new ValueEventListener() {
//                        @Override
//                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                            rateChartDbReference.child(sharedPreferences.getString("organisationName", "no_data").
//                                    replace(" ", "") + "_RC_" + dataSnapshot.getChildrenCount()).child("areaId").setValue(areaId);
//                            rateChartDbReference.child(sharedPreferences.getString("organisationName", "no_data").
//                                    replace(" ", "") + "_RC_" + dataSnapshot.getChildrenCount()).child("rateMoney").setValue(money);
//                            rateChartDbReference.child(sharedPreferences.getString("organisationName", "no_data").
//                                    replace(" ", "") + "_RC_" + dataSnapshot.getChildrenCount()).child("rateTime").setValue(rateChartTime);
//                            rateChartDbReference.child(sharedPreferences.getString("organisationName", "no_data").
//                                    replace(" ", "") + "_RC_" + dataSnapshot.getChildrenCount()).child("createdBy").setValue(parent);
//                            rateChartDbReference.child(sharedPreferences.getString("organisationName", "no_data").
//                                    replace(" ", "") + "_RC_" + dataSnapshot.getChildrenCount()).child("createdDate").setValue(currentDate);
//                        }
//
//                        @Override
//                        public void onCancelled(@NonNull DatabaseError databaseError) {
//
//                        }
//                    });
//                    subscriptionDbReference.addListenerForSingleValueEvent(new ValueEventListener() {
//                        @Override
//                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                            subscriptionDbReference.child(sharedPreferences.getString("organisationName", "no_data").
//                                    replace(" ", "") + "_SP_" + dataSnapshot.getChildrenCount()).child("areaId").setValue(areaId);
//                            subscriptionDbReference.child(sharedPreferences.getString("organisationName", "no_data").
//                                    replace(" ", "") + "_SP_" + dataSnapshot.getChildrenCount()).child("subscriptionId")
//                                    .setValue(sharedPreferences.getString("organisationName", "no_data").replace(" ", "") + "_SP_" + dataSnapshot.getChildrenCount());
//                            subscriptionDbReference.child(sharedPreferences.getString("organisationName", "no_data").
//                                    replace(" ", "") + "_SP_" + dataSnapshot.getChildrenCount()).child("areaName").setValue(area_name.getText().toString().trim());
//                            subscriptionDbReference.child(sharedPreferences.getString("organisationName", "no_data").
//                                    replace(" ", "") + "_SP_" + dataSnapshot.getChildrenCount()).child("subscriptionPlanName").setValue(planName);
//                            subscriptionDbReference.child(sharedPreferences.getString("organisationName", "no_data").
//                                    replace(" ", "") + "_SP_" + dataSnapshot.getChildrenCount()).child("subscriptionValidityTime").setValue(validityTime);
//                            subscriptionDbReference.child(sharedPreferences.getString("organisationName", "no_data").
//                                    replace(" ", "") + "_SP_" + dataSnapshot.getChildrenCount()).child("subscriptionPlanPrice").setValue(pricePlan);
//                            subscriptionDbReference.child(sharedPreferences.getString("organisationName", "no_data").
//                                    replace(" ", "") + "_SP_" + dataSnapshot.getChildrenCount()).child("subscriptionMaxFreeRide").setValue(maxFreeRide);
//                            subscriptionDbReference.child(sharedPreferences.getString("organisationName", "no_data").
//                                    replace(" ", "") + "_SP_" + dataSnapshot.getChildrenCount()).child("subscriptionDescription").setValue(subscriptionDescription);
//                            subscriptionDbReference.child(sharedPreferences.getString("organisationName", "no_data").
//                                    replace(" ", "") + "_SP_" + dataSnapshot.getChildrenCount()).child("subscriptionCarryForward").setValue(carryForward);
//                            subscriptionDbReference.child(sharedPreferences.getString("organisationName", "no_data").
//                                    replace(" ", "") + "_SP_" + dataSnapshot.getChildrenCount()).child("subscriptionStatus").setValue("true");
//                            subscriptionDbReference.child(sharedPreferences.getString("organisationName", "no_data").
//                                    replace(" ", "") + "_SP_" + dataSnapshot.getChildrenCount()).child("createdBy").setValue(parent);
//                            subscriptionDbReference.child(sharedPreferences.getString("organisationName", "no_data").
//                                    replace(" ", "") + "_SP_" + dataSnapshot.getChildrenCount()).child("createdDate").setValue(currentDate);
//
//                        }
//
//                        @Override
//                        public void onCancelled(@NonNull DatabaseError databaseError) {
//
//                        }
//                    });
//                }
//                break;
//            default:
//                break;
//        }
//    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            geofencingCondition = data.getStringExtra("geofencing_fine"); //gefoencing fine condition
            baseFareCondition = data.getStringExtra("base_fare_condition");
            serviceCondition = data.getStringExtra("service_condition");
            subscriptionCondition = data.getStringExtra("subscription_condition");
            areaCondition = data.getStringExtra("area_condition");
            Log.d(TAG, "From AreaLegal:" + geofencingCondition + "\t" + baseFareCondition + "\t" + serviceCondition + "\t" + subscriptionCondition + "\t" + areaCondition);
        }
        if (requestCode == 2) {
            //subscriptionId = data.getStringExtra("subscription_id");
            planName = data.getStringExtra("plan_name");
            validityTime = data.getStringExtra("validity_time");
            pricePlan = data.getStringExtra("plan_price");
            maxFreeRide = data.getStringExtra("max_free_ride");
            subscriptionDescription = data.getStringExtra("subscription_description");
            /*valueHourSlab = data.getStringExtra("hour_slab");
            valueMinSlab = data.getStringExtra("min_slab");
            */
            carryForward = data.getIntExtra("carry_forward", 5);
            // subscriptionPlanTime = ((Integer.parseInt(valueHourSlab) * 60) + Integer.parseInt(valueMinSlab));
            Log.d(TAG, "From AreaSubscription:" + planName + "\t" + validityTime + "\t" + pricePlan + "\t" + maxFreeRide + "\t" + subscriptionDescription + "\t" +
                    /*valueHourSlab + "\t" + valueMinSlab */ "\t" + carryForward);//+ "\t" + subscriptionPlanTime);
        }
        if (requestCode == 3) {
            stationId = data.getStringExtra("station_id");
            stationName = data.getStringExtra("station_name");
            stationLatitude = data.getStringExtra("station_latitude");
            stationLongitude = data.getStringExtra("station_longitude");
            stationRadius = data.getStringExtra("station_radius");
            stationArrayList = (ArrayList) data.getSerializableExtra("station_details");
            Log.d(TAG, "From AddStation:" + stationId + "\t" + stationName + "\t" + stationLatitude + "\t" + stationLongitude + "\t" + stationRadius);
        }
        if (requestCode == 4) {
            // rateId = data.getStringExtra("rate_id");
            money = data.getIntExtra("money", 6);
            rateHourSlab = data.getStringExtra("hour_slab");
            rateMinSlab = data.getStringExtra("min_slab");
            rateChartTime = ((Integer.parseInt(rateHourSlab) * 60) + Integer.parseInt(rateMinSlab));
            Log.d(TAG, "From AreaRate:" + money + "\t" + rateHourSlab + "\t" + rateMinSlab + "\t" + rateChartTime);
        }
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

    private void clearField() {
        maximumRideTime.setText("");
        maximumRideTimeExceedingFine.setText("");
        maximumHoldTime.setText("");
        maximumHoldTimeExceedingFine.setText("");
        trackBicycle.setText("");
        //   minimumAmountFirstRide.setText("");
        //  baseValueRatechart.setText("");
        serviceStartTime.setText("");
        serviceEndTime.setText("");
        serviceHourExceedingFine.setText("");
        geofencineFine.setText("");
        customerServiceNumber.setText("");
    }

    private boolean criteriaCheck() {
        final Animation animShake = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.shake);
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
        if (TextUtils.isEmpty(trackBicycle.getText())) {
            trackBicycle.setError(getResources().getString(R.string.empty_field_message));
            return false;
        }
      /*  if (TextUtils.isEmpty(minimumAmountFirstRide.getText())) {
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
        if (TextUtils.isEmpty(geofencingCondition) && TextUtils.isEmpty(baseFareCondition) && TextUtils.isEmpty(serviceCondition) &&
                TextUtils.isEmpty(subscriptionCondition) && TextUtils.isEmpty(areaCondition)) {
            layoutAddAreaLegal.startAnimation(animShake);
        }
        if (TextUtils.isEmpty(planName) && TextUtils.isEmpty(validityTime) && TextUtils.isEmpty(pricePlan) && TextUtils.isEmpty(maxFreeRide) && TextUtils.isEmpty(subscriptionDescription)
                && TextUtils.isEmpty(valueHourSlab) && TextUtils.isEmpty(valueMinSlab) && TextUtils.isEmpty(Integer.toString(carryForward))) {
            layoutAddAreaSubscription.startAnimation(animShake);
        }
        if (TextUtils.isEmpty(stationId) && TextUtils.isEmpty(stationName) && TextUtils.isEmpty(stationLatitude) && TextUtils.isEmpty(stationLongitude) && TextUtils.isEmpty(stationRadius)) {
            layoutAddStations.startAnimation(animShake);
        }
        if (TextUtils.isEmpty(rateId) && TextUtils.isEmpty(Integer.toString(money)) && TextUtils.isEmpty(rateHourSlab) && TextUtils.isEmpty(rateMinSlab)) {
            layoutAddRateChart.startAnimation(animShake);
        }
        return true;
    }

    @Override
    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
        Log.d(TAG, "Parent: " + parent);
        Area createArea = new Area(areaId, area_name.getText().toString(), markerList, maximumRideTime.getText().toString().trim(), maximumRideTimeExceedingFine.getText().toString().trim(),
                maximumHoldTime.getText().toString().trim(), maximumHoldTimeExceedingFine.getText().toString().trim(),
                serviceStartTime.getText().toString().trim(), serviceEndTime.getText().toString().trim(),
                serviceHourExceedingFine.getText().toString().trim(), geofencineFine.getText().toString().trim(), customerServiceNumber.getText().toString().trim(), geofencingCondition, baseFareCondition,
                serviceCondition, subscriptionCondition, areaCondition, false, parent, currentDate, trackBicycle.getText().toString().trim());
        areaDbReference.child(createArea.getAreaId()).setValue(createArea);

        for (Station i : stationArrayList) {
            Station createStation = new Station(areaId, area_name.getText().toString(), markerList, i.getStationId(), i.getStationName(), i.getStationLatitude(), i.getStationLongitude(), i.getStationRadius(),
                    i.getStationType(), true, parent, currentDate);
            stationDbReference.child(createStation.getStationId()).setValue(createStation);
        }

        /*Subscription createSubscription = new Subscription(areaId, area_name.getText().toString(), subscriptionId, planName, validityTime, pricePlan, maxFreeRide, subscriptionDescription,
                carryForward, true, parent, currentDate);
        subscriptionDbReference.child(createSubscription.getSubscriptionId()).setValue(createSubscription);
*/

       /* AreaRate createRate = new AreaRate(rateId, money, rateChartTime, parent, currentDate);
        rateChartDbReference.child(createRate.getRateId()).setValue(createRate);
        rateChartDbReference.child(createRate.getRateId()).child("areaId").setValue(areaId);
*/
        for (Station i : stationArrayList) {
            StationList stationList = new StationList(i.getStationId(), true);
            stationListDbReference.child(stationList.getStationId()).setValue(stationList);
        }

        customAlertDialog.setTitle("Add Area!");
        customAlertDialog.setMsg("Area is successfully added to the database");
        customAlertDialog.show();

    }

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {
        customAlertDialog.setTitle("Failure!");
        customAlertDialog.setMsg("Area could not be added to the database");
        customAlertDialog.show();
    }

    private void updateZoneDetails(String areaName, String areaId) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        class area {
            String name, id;

            area() {
            }

            area(String name, String id) {
                this.name = name;
                this.id = id;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }
        }
        if (!sharedPreferences.getString("zone", "na").equals("na")) {
            area obj = new area(areaName, areaId);
            databaseReference.child(organisationName.replaceAll(" ", "")).child("Zone").child(sharedPreferences.getString("zone", "na").trim().toUpperCase()).child("AreaList")
                    .child(obj.getId()).setValue(obj);
        }
    }
}
