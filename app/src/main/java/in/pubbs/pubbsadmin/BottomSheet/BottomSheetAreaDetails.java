package in.pubbs.pubbsadmin.BottomSheet;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

import in.pubbs.pubbsadmin.Model.ViewPanel;
import in.pubbs.pubbsadmin.R;

public class BottomSheetAreaDetails extends BottomSheetDialogFragment {
    private BottomSheetBehavior mBehavior;
    private TextView descriptionHeader, area_id, area_name, max_ride_time, max_hold_time, customer_care_number, area_now, service_hour;
    private ImageView showMap;
    String areaId, areaName, maxRideTime, maxHoldTime, customerCare, serviceHourStart, serviceHourEnd, organisationName;
    DatabaseReference manageAreaDbReference;
    SharedPreferences sharedPreferences;
    private String TAG = BottomSheetAreaDetails.class.getSimpleName();
    private Button service;
    private boolean status;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        View view = View.inflate(getContext(), R.layout.custom_area_details, null);
        dialog.setContentView(view);
        areaId = Objects.requireNonNull(getArguments()).getString("areaId");
        areaName = getArguments().getString("areaName");
        sharedPreferences = Objects.requireNonNull(getActivity()).getSharedPreferences(getResources().getString(R.string.sharedPreferences), Context.MODE_PRIVATE);
        organisationName = sharedPreferences.getString("organisationName", "no_data").replace(" ", "");
        loadData(areaId);
        mBehavior = BottomSheetBehavior.from((View) view.getParent());
        descriptionHeader = view.findViewById(R.id.description_header);
        area_id = view.findViewById(R.id.area_id);
        area_name = view.findViewById(R.id.area_name);
        max_ride_time = view.findViewById(R.id.max_ride_time);
        max_hold_time = view.findViewById(R.id.max_hold_time);
        customer_care_number = view.findViewById(R.id.customer_care_number);
        service_hour = view.findViewById(R.id.service_hour);
        area_now = view.findViewById(R.id.area_now);
        showMap = view.findViewById(R.id.show_map);
        showMap.setOnClickListener(v -> dismiss());
        service = view.findViewById(R.id.launch);
        return dialog;
    }

    private void loadData(String areaId) {
        manageAreaDbReference = FirebaseDatabase.getInstance().getReference(organisationName.replaceAll(" ", "") + "/Area/" + areaId);
        manageAreaDbReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ViewPanel areaDetails = dataSnapshot.getValue(ViewPanel.class);
                maxRideTime = Objects.requireNonNull(areaDetails).getMaximumRideTime();
                maxHoldTime = areaDetails.getMaxHoldTime();
                customerCare = areaDetails.getCustomerServiceNumber();
                serviceHourStart = areaDetails.getServiceStartTime();
                serviceHourEnd = areaDetails.getServiceEndTime();
                if(dataSnapshot.child("areaStatus").exists())
                status = dataSnapshot.child("areaStatus").getValue(Boolean.class);
                else status =false;
                Log.d(TAG, "Area details: " + maxRideTime + "\t" + maxHoldTime + "\t" + customerCare + "\t" + serviceHourStart + "\t" + serviceHourEnd);
                updateUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, "Database error: " + databaseError.toString());
            }
        });
    }

    private void updateUI() {
        area_id.setText("Area ID: " + areaId);
        area_name.setText("Area Name: " + areaName);
        max_ride_time.setText("Maximum Ride Time: " + maxRideTime + "" + "minutes");
        max_hold_time.setText("Maximum Hold Time: " + maxHoldTime + "" + "minutes");
        customer_care_number.setText("Customer Care: " + customerCare);
        service_hour.setText("Service Hour: " + serviceHourStart + " to" + serviceHourEnd);
        if (status) {
            service.setText("Close Area");
        } else {
            service.setText("Launch Area");
        }
        service.setVisibility(View.VISIBLE);
        service.setOnClickListener(v -> {
            if (status) {
                //Close Area
                manageAreaDbReference.child("areaStatus").setValue(false);
                service.setText("Launch Area");
                status = false;
            } else {
                //Launch Area
                manageAreaDbReference.child("areaStatus").setValue(true);
                service.setText("Close Area");
                status = true;
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }
}
