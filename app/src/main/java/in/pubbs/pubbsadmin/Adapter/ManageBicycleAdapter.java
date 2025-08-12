package in.pubbs.pubbsadmin.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import in.pubbs.pubbsadmin.R;

public class ManageBicycleAdapter extends RecyclerView.Adapter<ManageBicycleAdapter.HolderClass> {

    ArrayList list;
    ClickListener menuItemClick;
    int position;

    public ManageBicycleAdapter(ArrayList list) {
        this.list = list;
    }

    @NonNull
    @Override
    public HolderClass onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_manage_bicycle_row, parent, false);
        return new HolderClass(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HolderClass holder, int position) {
        String val = (String) list.get(position);
        holder.data.setText(val);
        if(val.contains("Station")){
            holder.data_icon.setImageResource(R.drawable.ic_station_black);
        }else if(val.contains("Bicycle")){
            holder.data_icon.setImageResource(R.drawable.ic_cycle_rider);
            holder.details_icon.setVisibility(View.GONE);
            if(val.contains("active")){
                holder.data_icon.setImageResource(R.drawable.ic_cycle_green);
            }
            else{
                holder.data_icon.setImageResource(R.drawable.ic_cycle_red);
            }
        }
        holder.constraintLayout.setOnClickListener((ClickListener) view -> {
            setPosition(position);
            menuItemClick.onClick(view);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class HolderClass extends RecyclerView.ViewHolder {
        TextView data;
        ImageView data_icon,details_icon;
        ConstraintLayout constraintLayout;
        public HolderClass(@NonNull View itemView) {
            super(itemView);
            data = itemView.findViewById(R.id.data);
            data_icon = itemView.findViewById(R.id.icon);
            details_icon = itemView.findViewById(R.id.details_icon);
            constraintLayout = itemView.findViewById(R.id.container);
            position = getAdapterPosition();
        }
    }
    public interface ClickListener extends View.OnClickListener {
        @Override
        void onClick(View view);
    }
    public void onMenuItemClick(ClickListener clickListener){
        this.menuItemClick = clickListener;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
