package in.pubbs.pubbsadmin.Adapter;

import android.content.Intent;
import android.util.Log;
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

import in.pubbs.pubbsadmin.Model.Station;
import in.pubbs.pubbsadmin.R;
import in.pubbs.pubbsadmin.StationDetails;

@SuppressWarnings("ConstantConditions")
public class ManageStation extends RecyclerView.Adapter<ManageStation.MyViewHolder> {
    private List<Station> stations;
    ArrayList<Map<String, Object>> arrayList;
    private String TAG = ManageStation.class.getSimpleName();


    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView station_name;
        public ConstraintLayout stationListLayout;

        public MyViewHolder(View view) {
            super(view);
            station_name = view.findViewById(R.id.station_name);
            stationListLayout = view.findViewById(R.id.station_list_layout);
        }
    }

    public ManageStation(ArrayList arrayList) {
        this.arrayList = arrayList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.station_list, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        String stationId = (String) arrayList.get(position).get("stationId");
        String stationName = (String) arrayList.get(position).get("stationName");
        String stationLatitude = (String) arrayList.get(position).get("stationLatitude");
        String stationLongitude = (String) arrayList.get(position).get("stationLongitude");
        String areaName = (String) arrayList.get(position).get("areaId");
        String stationRadius = (String) arrayList.get(position).get("stationRadius");
        boolean stationStatus = (Boolean) arrayList.get(position).get("stationStatus");
        String stationType = (String) arrayList.get(position).get("stationType");
        Log.d(TAG, "Station : " + stationId + "\t" + stationName + "\t" + stationLatitude + "\t" + stationLongitude + "\t" + areaName +
                "\t" + stationRadius + "\t" + stationStatus + "\t" + stationType);
        holder.station_name.setText(stationName);
        holder.stationListLayout.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), StationDetails.class);
            intent.putExtra("stationId", stationId);
            intent.putExtra("stationName", stationName);
            intent.putExtra("stationLatitude", stationLatitude);
            intent.putExtra("stationLongitude", stationLongitude);
            intent.putExtra("areaName", areaName);
            intent.putExtra("stationRadius", stationRadius);
            intent.putExtra("stationStatus", stationStatus);
            intent.putExtra("stationType", stationType);
            v.getContext().startActivity(intent);
        });
    }


    @Override
    public int getItemCount() {
        return arrayList.size();
    }
}
