package in.pubbs.pubbsadmin.Adapter;

import android.content.Context;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import in.pubbs.pubbsadmin.Model.ZoneManager;
import in.pubbs.pubbsadmin.R;

//Created By Souvik
public class ZoneManagerAdapter extends RecyclerView.Adapter<ZoneManagerAdapter.HolderClass> {
    private ArrayList<ZoneManager> dataSet;
    private Context context;
    private int position;
    private ClickListener active,deactivate;

    public ZoneManagerAdapter(ArrayList<ZoneManager> list, Context context) {
        dataSet = list;
        this.context = context;
    }

    @NonNull
    @Override
    public ZoneManagerAdapter.HolderClass onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.zone_manager_list_row, parent, false);
        return new HolderClass(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ZoneManagerAdapter.HolderClass holder, int position) {
        holder.operatorName.append(dataSet.get(position).getZoneManagerName());
        holder.key.append(dataSet.get(position).getZoneManagerKey());
        holder.mobile.append(dataSet.get(position).getZoneManagerPhone());
        if (dataSet.get(position).getActive()) {
            holder.status.setChecked(true);
        } else {
            holder.status.setChecked(false);
        }
        holder.status.setOnClickListener(v -> {
            if(holder.status.isChecked()){
                setPosition(position);
                active.onClick(v);
            }else{
                setPosition(position);
                deactivate.onClick(v);
            }
        });
    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    public class HolderClass extends RecyclerView.ViewHolder {
        TextView operatorName, key, mobile;
        Switch status;

        public HolderClass(@NonNull View itemView) {
            super(itemView);
            operatorName = itemView.findViewById(R.id.operator_name);
            mobile = itemView.findViewById(R.id.mobile_number);
            key = itemView.findViewById(R.id.key);
            status = itemView.findViewById(R.id.user_status);
            status.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    status.getTrackDrawable().setColorFilter(ContextCompat.getColor(context, R.color.green_300), PorterDuff.Mode.SRC_IN);
                    status.getThumbDrawable().setColorFilter(ContextCompat.getColor(context, R.color.green_400), PorterDuff.Mode.SRC_IN);
                } else {
                    status.getTrackDrawable().setColorFilter(ContextCompat.getColor(context, R.color.red_300), PorterDuff.Mode.SRC_IN);
                    status.getThumbDrawable().setColorFilter(ContextCompat.getColor(context, R.color.red_400), PorterDuff.Mode.SRC_IN);
                }
            });
        }
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
    public interface ClickListener extends View.OnClickListener {
        @Override
        void onClick(View view);
    }
    public void activateUser(ClickListener active){
        this.active = active;
    }
    public void deactivateUser(ClickListener deactivate){
        this.deactivate = deactivate;
    }
}
