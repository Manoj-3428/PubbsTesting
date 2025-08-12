package in.pubbs.pubbsadmin.Adapter;

import android.content.Context;
import android.content.Intent;
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

import in.pubbs.pubbsadmin.BicycleHoldStatus;
import in.pubbs.pubbsadmin.Model.HoldList;
import in.pubbs.pubbsadmin.R;

public class BicycleOnHoldAdapter extends RecyclerView.Adapter<BicycleOnHoldAdapter.HolderClass> {

    private ArrayList<HoldList> list;
    private Context context;
    private String TAG = BicycleOnHoldAdapter.class.getSimpleName();

    public BicycleOnHoldAdapter(ArrayList list, Context context) {
        this.list = list;
        this.context = context;
    }

    @NonNull
    @Override
    public HolderClass onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.bicycle_holder_list, parent, false);

        return new HolderClass(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull HolderClass holder, int position) {
        holder.data.setText(list.get(position).getBicycle());
        holder.data1.setText(list.get(position).getStatus());
        if (list.get(position).getStatus().equalsIgnoreCase("Ride Time Elapsed")) {
            holder.data1.setTextColor(context.getResources().getColor(R.color.red_800));
        } else {
            holder.data1.setTextColor(context.getResources().getColor(R.color.green_800));
        }

        holder.container.setOnClickListener(v -> {
            Log.d(TAG, "Container touch: " + list.get(position).getBicycle());
            Intent intent = new Intent(v.getContext(), BicycleHoldStatus.class);
            intent.putExtra("bicycleId", list.get(position).getBicycle());
            intent.putExtra("status", list.get(position).getStatus());
            intent.putExtra("rideStartTime", list.get(position).getRideStartTime());
            intent.putExtra("elapsedTime", list.get(position).getExcessElapsed());
            intent.putExtra("bookingId", list.get(position).getBookingId());
            intent.putExtra("actualRideTime", list.get(position).getActualRidetime());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class HolderClass extends RecyclerView.ViewHolder {
        ConstraintLayout container;
        ImageView details;
        TextView data, data1;

        public HolderClass(@NonNull View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.container);
            details = itemView.findViewById(R.id.details_icon);
            details.setVisibility(View.GONE);
            data = itemView.findViewById(R.id.data);
            data1 = itemView.findViewById(R.id.data1);
        }
    }
}
