package in.pubbs.pubbsadmin.Adapter;

import android.content.Intent;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import in.pubbs.pubbsadmin.AreaDetails;
import in.pubbs.pubbsadmin.Model.Area;
import in.pubbs.pubbsadmin.R;

public class ManageArea extends RecyclerView.Adapter<ManageArea.MyViewHolder> {
    private List<Area> areas;
    ArrayList<Map<String, Object>> arrayList;
    private String TAG = ManageArea.class.getSimpleName();


    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView area_name;
        public ConstraintLayout areaListLayout;

        public MyViewHolder(View view) {
            super(view);
            area_name = view.findViewById(R.id.area_name);
            areaListLayout = view.findViewById(R.id.area_list_layout);
        }
    }

    public ManageArea(ArrayList areas) {
        this.arrayList = areas;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.area_list, parent, false);

        return new ManageArea.MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        String areaId = (String) arrayList.get(position).get("areaId");
        String areaName = (String) arrayList.get(position).get("areaName");
        holder.area_name.setText(areaName);
        holder.areaListLayout.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), AreaDetails.class);
            intent.putExtra("area_id", areaId);
            intent.putExtra("area_name", Objects.requireNonNull(arrayList.get(position).get("areaName")).toString());
            intent.putExtra("max_ride_time", Objects.requireNonNull(arrayList.get(position).get("maximumRideTime")).toString());
            intent.putExtra("max_ride_exceeding_fine", Objects.requireNonNull(arrayList.get(position).get("maxRideTimeExceedingFine")).toString());
            intent.putExtra("max_hold_time", Objects.requireNonNull(arrayList.get(position).get("maxHoldTime")).toString());
            intent.putExtra("max_hold_exceeding_fine", Objects.requireNonNull(arrayList.get(position).get("maxHoldTimeExceedingFine")).toString());
            intent.putExtra("trackBicycle", Objects.requireNonNull(arrayList.get(position).get("trackBicycle")).toString());
/*            intent.putExtra("min_first_amount_ride", arrayList.get(position).get("minFirstAmountRide").toString());
            intent.putExtra("base_value_rate", arrayList.get(position).get("baseValueRateChart").toString());*/
            intent.putExtra("service_start_time", Objects.requireNonNull(arrayList.get(position).get("serviceStartTime")).toString());
            intent.putExtra("service_end_time", Objects.requireNonNull(arrayList.get(position).get("serviceEndTime")).toString());
            intent.putExtra("service_hour_exceeding_fine", Objects.requireNonNull(arrayList.get(position).get("serviceHourExceedingFine")).toString());
            intent.putExtra("geofencing_fine", Objects.requireNonNull(arrayList.get(position).get("geofencingFine")).toString());
            intent.putExtra("customer_number", Objects.requireNonNull(arrayList.get(position).get("customerServiceNumber")).toString());
            intent.putParcelableArrayListExtra("marker_list", (ArrayList<? extends Parcelable>) arrayList.get(position).get("markerList"));
            intent.putExtra("area_condition", Objects.requireNonNull(arrayList.get(position).get("areaCondition")).toString());
            intent.putExtra("base_fare_condition", Objects.requireNonNull(arrayList.get(position).get("baseFareCondition")).toString());
            intent.putExtra("geofencing_condition", Objects.requireNonNull(arrayList.get(position).get("geofencingCondition")).toString());
            intent.putExtra("service_condition", Objects.requireNonNull(arrayList.get(position).get("serviceCondition")).toString());
            intent.putExtra("subscription_condition", Objects.requireNonNull(arrayList.get(position).get("subscriptionCondition")).toString());
            v.getContext().startActivity(intent);
        });
    }


    @Override
    public int getItemCount() {
        return arrayList.size();
    }

}
