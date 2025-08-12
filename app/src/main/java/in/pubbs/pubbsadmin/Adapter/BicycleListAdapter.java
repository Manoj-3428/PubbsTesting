package in.pubbs.pubbsadmin.Adapter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

import in.pubbs.pubbsadmin.AddOrRemoveBicycle;
import in.pubbs.pubbsadmin.R;

public class BicycleListAdapter extends RecyclerView.Adapter<BicycleListAdapter.MyViewHolder> {
    ArrayList<Map<String, Object>> list;
    Context context;
    String type = "";

    public BicycleListAdapter(ArrayList<Map<String, Object>> list, Context context) {
        this.list = list;
        this.context = context;
    }

    public BicycleListAdapter(ArrayList<Map<String, Object>> list, Context context, String type) {
        this.list = list;
        this.context = context;
        this.type = type;
    }

    public BicycleListAdapter() {

    }

    @NonNull
    @Override
    public BicycleListAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_bicycle_list_adapter_items, parent, false);
        return new MyViewHolder(view);
    }

//    @Override
//    public void onBindViewHolder(@NonNull BicycleListAdapter.MyViewHolder holder, int position) {
//        Log.d("BicycleListAdapter: ", list.get(position).toString());
//        if (type.equals("Station")) {
//            holder.lockId.setText(Objects.requireNonNull(list.get(position).get("stationName")).toString());
//            holder.battery_tv.setText(Objects.requireNonNull(list.get(position).get("stationId")).toString());
//            holder.constraintLayout.setOnClickListener(v -> {
//                Intent intent = new Intent(context, AddOrRemoveBicycle.class);
//                intent.putExtra("StationId", Objects.requireNonNull(list.get(position).get("stationId")).toString());
//                intent.putExtra("StationName", Objects.requireNonNull(list.get(position).get("stationName")).toString());
//                intent.putExtra("AreaId", holder.sharedPreferences.getString("areaId", null));
//                intent.putExtra("Status", "ADD");
//                context.startActivity(intent);
//            });
//        } else {
//            holder.lockId.setText(Objects.requireNonNull(list.get(position).get("id")).toString());
//            holder.battery.setText(list.get(position).get("battery") == null ? "Data not available" : Objects.requireNonNull(list.get(position).get("battery")).toString()+"%");
//            if ( Objects.requireNonNull(list.get(position).get("theft")).toString().equals("1") )  {
//                holder.status.setBackground(context.getDrawable(R.drawable.solid_circle_black));//solid_circle_black
//            } else if (Objects.requireNonNull(list.get(position).get("status")).toString().equals("active")) {
//                holder.status.setBackground(context.getDrawable(R.drawable.solid_circle_green));
//
//            }
//            else if (Objects.requireNonNull(list.get(position).get("status")).toString().equals("busy") && Objects.requireNonNull(list.get(position).get("operation")).toString().equals("20") ){
//                holder.status.setBackground(context.getDrawable(R.drawable.solid_circle_yellow));
//            }
//            else {
//                holder.status.setBackground(context.getDrawable(R.drawable.solid_circle_red));
//            }
//        }
//    }
    // modify by dipankar
@Override
public void onBindViewHolder(@NonNull BicycleListAdapter.MyViewHolder holder, int position) {
    Log.d("BicycleListAdapter: ", list.get(position).toString());

    // Check if list and item are not null
    if (list == null || list.get(position) == null) {
        Log.e("BicycleListAdapter", "Data is null or empty at position: " + position);
        return;
    }

    Map<String, Object> item = list.get(position);

    // Handling for "Station"
    if ("Station".equals(type)) {
        // Check for required keys before accessing
        if (item.containsKey("stationName") && item.get("stationName") != null) {
            holder.lockId.setText(item.get("stationName").toString());
        } else {
            holder.lockId.setText("Unknown Station");
        }

        if (item.containsKey("stationId") && item.get("stationId") != null) {
            holder.battery_tv.setText(item.get("stationId").toString());
        } else {
            holder.battery_tv.setText("No ID Available");
        }

        holder.constraintLayout.setOnClickListener(v -> {
            Intent intent = new Intent(context, AddOrRemoveBicycle.class);
            intent.putExtra("StationId", item.get("stationId") != null ? item.get("stationId").toString() : "N/A");
            intent.putExtra("StationName", item.get("stationName") != null ? item.get("stationName").toString() : "N/A");
            intent.putExtra("AreaId", holder.sharedPreferences.getString("areaId", "N/A"));
            intent.putExtra("Status", "ADD");
            context.startActivity(intent);
        });
    }
    // Handling for other types
    else {
        if (item.containsKey("id") && item.get("id") != null) {
            holder.lockId.setText(item.get("id").toString());
        } else {
            holder.lockId.setText("No ID Available");
        }

        // Handling battery data with fallback
        if (item.containsKey("battery") && item.get("battery") != null) {
            holder.battery.setText(item.get("battery").toString() + "%");
        } else {
            holder.battery.setText("Data not available");
        }

        // Handle "theft" status
        if (item.containsKey("theft") && "1".equals(item.get("theft").toString())) {
            holder.status.setBackground(context.getDrawable(R.drawable.solid_circle_black));
        }
        // Handle "status" with fallback
        else if (item.containsKey("status") && "active".equals(item.get("status").toString())) {
            holder.status.setBackground(context.getDrawable(R.drawable.solid_circle_green));
        }
        // Handle "busy" and "operation" conditions
        else if (item.containsKey("status") && "busy".equals(item.get("status").toString())
                && item.containsKey("operation") && "20".equals(item.get("operation").toString())) {
            holder.status.setBackground(context.getDrawable(R.drawable.solid_circle_yellow));
        }
        // Default case if none of the conditions match
        else {
            holder.status.setBackground(context.getDrawable(R.drawable.solid_circle_red));
        }
    }
}


    @Override
    public int getItemCount() {
        return list.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView lockId, battery, battery_tv;
        View status;
        ImageView imageView;
        ConstraintLayout constraintLayout;
        SharedPreferences sharedPreferences;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            lockId = itemView.findViewById(R.id.tv_lock_id);
            battery = itemView.findViewById(R.id.battery_value);
            battery_tv = itemView.findViewById(R.id.battery_hint);
            status = itemView.findViewById(R.id.status);
            imageView = itemView.findViewById(R.id.iv_bike);
            sharedPreferences = context.getSharedPreferences("pubbs", Context.MODE_PRIVATE);
            constraintLayout = itemView.findViewById(R.id.container);
            if (type.equals("Station")) {
                imageView.setImageResource(R.drawable.ic_station_black);
                battery.setVisibility(View.GONE);
            }
        }
    }
}
