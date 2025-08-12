package in.pubbs.pubbsadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

import in.pubbs.pubbsadmin.Adapter.BicycleOnHoldAdapter;
import in.pubbs.pubbsadmin.Model.HoldList;
import in.pubbs.pubbsadmin.View.CustomLoader;

public class BicycleOnHold extends AppCompatActivity {

    private String areaId;
    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private SharedPreferences sharedPreferences;
    private String TAG = BicycleOnHold.class.getSimpleName();
    private DatabaseReference databaseReference;
    private ArrayList<HoldList> list;
    private ConstraintLayout noDataFound;
    private TextView title;
    private CustomLoader customLoader;
    private ImageView back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_bicycle_on_hold);
        }catch (Exception excp){

        }
        setContentView(R.layout.activity_bicycle_on_hold);
        areaId = getIntent().getStringExtra("AreaId");
        init();
    }

    private void init() {
        sharedPreferences = getSharedPreferences("pubbs", MODE_PRIVATE);
        recyclerView = findViewById(R.id.recycler_view);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.hasFixedSize();
        list = new ArrayList();
        noDataFound = findViewById(R.id.no_data_found);
        title = findViewById(R.id.toolbar_title);
        title.setText("Ride on Hold");
        back = findViewById(R.id.back_button);
        customLoader = new CustomLoader(this, R.style.WideDialog);//Loader
        customLoader.show();
        loadData();
        back.setOnClickListener(v -> finish());
    }

    private void loadData() {
        String path = Objects.requireNonNull(sharedPreferences.getString("organisationName", "no_data")).replaceAll(" ", "") + "/Bicycle";
        Log.d(TAG, "path: " + path);
        databaseReference = FirebaseDatabase.getInstance().getReference(path);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long count=0;
                for (DataSnapshot i : dataSnapshot.getChildren()) {
                    count++;
                    if (Objects.requireNonNull(i.child("inAreaId").getValue()).toString().equalsIgnoreCase(areaId) && Objects.requireNonNull(i.child("status").getValue()).toString().equalsIgnoreCase("busy")) {
                        Log.d(TAG, "KEY: " + i.getKey());
                        String path1 = Objects.requireNonNull(sharedPreferences.getString("organisationName", "no_data")).replaceAll(" ", "") + "/Booking/" + i.getKey();
                        DatabaseReference db = FirebaseDatabase.getInstance().getReference(path1);
                        db.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                long bookingNumber = dataSnapshot.getChildrenCount() - 1;
                                String booking_id = Objects.requireNonNull(i.getKey()).concat("_" + bookingNumber);
                                Log.d(TAG, "Last booking id number: " + bookingNumber + "\tBicycle id:" + booking_id);
                                if (dataSnapshot.child(booking_id).child("bookingDateTime").exists()) {
                                    Log.d(TAG, Objects.requireNonNull(dataSnapshot.child(booking_id).child("bookingDateTime").getValue()).toString());
                                    String timeStamp = Objects.requireNonNull(dataSnapshot.child(booking_id).child("bookingDateTime").getValue()).toString();
                                    getTimeDifference(timeStamp, i.getKey(), booking_id);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                    else if(dataSnapshot.getChildrenCount()==count){
                        customLoader.dismiss();
                        noDataFound.setVisibility(View.VISIBLE);
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getTimeDifference(String timeStamp, String bicycleId, String bookingId) {
        Date rideDate, currentDate;
        String currentTime;
        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;
        SimpleDateFormat s = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        currentTime = s.format(new Date());
        Log.d(TAG, "Current time: " + currentTime);
        try {
            rideDate = s.parse(timeStamp);
            currentDate = s.parse(currentTime);
            Log.d(TAG, "Date formatted days: " + rideDate + "\t" + currentDate);
            long different = Objects.requireNonNull(currentDate).getTime() - Objects.requireNonNull(rideDate).getTime();
            Log.d(TAG, "Difference between two time: " + different);
            long elapsedDays = different / daysInMilli;
            different = different % daysInMilli;
            long elapsedHours = different / hoursInMilli;
            different = different % hoursInMilli;
            long elapsedMinutes = different / minutesInMilli;
            different = different % minutesInMilli;
            long elapsedSeconds = different / secondsInMilli;
            Log.d(TAG, "Elapsed Time: " + elapsedDays + "days\t" + elapsedHours + "hours\t" + elapsedMinutes + "minutes\t" + elapsedSeconds + "\tseconds");
            loadTimeConstraints(bicycleId, elapsedDays, elapsedHours, elapsedMinutes, elapsedSeconds, timeStamp, bookingId);

        } catch (ParseException e) {
            e.printStackTrace();
        }

    }

    private void loadTimeConstraints(String bicycleId, long day, long hr, long min, long sec, String startDataTime, String bookingId) {
        String path = Objects.requireNonNull(sharedPreferences.getString("organisationName", "no_data")).replaceAll(" ", "") + "/Area/" + areaId;
        Log.d(TAG, "path: " + path);
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(path);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long maxHoldTime, maxRideTime, totalTime, actualTime;
                if (dataSnapshot.child("maxHoldTime").exists() && dataSnapshot.child("maximumRideTime").exists()) {
                    maxHoldTime = Integer.parseInt(Objects.requireNonNull(dataSnapshot.child("maxHoldTime").getValue()).toString());
                    maxRideTime = Integer.parseInt(Objects.requireNonNull(dataSnapshot.child("maximumRideTime").getValue()).toString());
                    totalTime = maxHoldTime + maxRideTime + 40; //buffer time=40 minutes
                    HoldList obj = new HoldList(bicycleId);
                    obj.setRideStartTime(startDataTime);
                    obj.setBookingId(bookingId);
                    if (day == 0) {
                        actualTime = min + hr * 60;
                        obj.setActualRidetime(String.valueOf(actualTime));
                        if (actualTime > totalTime) {
                            obj.setStatus("Ride Time Elapsed");
                            obj.setExcessElapsed((actualTime - totalTime) + " min");
                        } else {
                            obj.setStatus("Ride On Hold");
                            obj.setExcessElapsed("0 min");
                        }
                    } else {
                        obj.setStatus("Ride Time Elapsed");
                        obj.setExcessElapsed(day + " day");
                    }

                    list.add(obj);
                    if (list.size() > 0) {
                        BicycleOnHoldAdapter adapter = new BicycleOnHoldAdapter(list, BicycleOnHold.this);
                        recyclerView.setAdapter(adapter);
                        adapter.notifyDataSetChanged();
                    } else {
                        noDataFound.setVisibility(View.VISIBLE);
                    }
                    customLoader.dismiss();//Loader ended
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

}
