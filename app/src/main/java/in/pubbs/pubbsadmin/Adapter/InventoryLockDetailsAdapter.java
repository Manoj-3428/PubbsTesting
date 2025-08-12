package in.pubbs.pubbsadmin.Adapter;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import in.pubbs.pubbsadmin.Model.Lock;
import in.pubbs.pubbsadmin.R;

//Created By Souvik
public class InventoryLockDetailsAdapter extends RecyclerView.Adapter<InventoryLockDetailsAdapter.HolderClass> {
    private List<Lock> dataSet;
    private String lockType;

    public InventoryLockDetailsAdapter(List<Lock> dataSet, String lockType) {
        this.dataSet = dataSet;
        this.lockType = lockType;
    }

    @NonNull
    @Override
    public HolderClass onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_lock_row, parent, false);
        return new HolderClass(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HolderClass holder, int position) {
        Lock lock = dataSet.get(position);
        holder.lockId.setText("Lock ID: " + lock.getLockId());
        holder.simNo.setText("Sim Number: " + lock.getSimId());
        holder.bleAddress.setText("Ble Address: " + lock.getBleAddress());
        holder.batteryData.setText("Battery percentage: " + lock.getBatteryValue() + "" + "%");
    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    public class HolderClass extends RecyclerView.ViewHolder {
        TextView lockId, bleAddress, simNo, batteryData;

        public HolderClass(@NonNull View itemView) {
            super(itemView);
            lockId = itemView.findViewById(R.id.lock_id);
            bleAddress = itemView.findViewById(R.id.lock_ble);
            simNo = itemView.findViewById(R.id.lock_sim);
            batteryData = itemView.findViewById(R.id.lock_battery);
            /*if (lockType.contains("AT_BLE") || lockType.contains("NR_BLE") || lockType.contains("QT_BLE")) {
                bleAddress.setVisibility(View.VISIBLE);
                simNo.setVisibility(View.GONE);
            } else {
                simNo.setVisibility(View.VISIBLE);
                bleAddress.setVisibility(View.VISIBLE);
            }*/
        }
    }
}
