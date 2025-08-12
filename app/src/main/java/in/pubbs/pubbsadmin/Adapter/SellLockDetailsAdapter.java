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

public class SellLockDetailsAdapter extends RecyclerView.Adapter<SellLockDetailsAdapter.HolderClass> {
    private List<Lock> dataSet;
    private String lockType;
    private int Quant;

    public SellLockDetailsAdapter() {
    }

    public SellLockDetailsAdapter(List<Lock> dataSet, String lockType,int quant) {
        this.dataSet = dataSet;
        this.lockType = lockType;
        this.Quant = quant;
    }

    @NonNull
    @Override
    public HolderClass onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.sell_lock_row, parent, false);
        return new SellLockDetailsAdapter.HolderClass(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HolderClass holder, int position) {
        Lock lock = dataSet.get(position);
        holder.lockId.setText("Lock ID: " + lock.getLockId());
        holder.simNo.setText("Sim Number: " + lock.getSimId());
        holder.bleAddress.setText("Ble Address" + lock.getBleAddress());
    }

    @Override
    public int getItemCount() {
        if(Quant>dataSet.size())
        return dataSet.size();
        else return Quant;
    }

    @SuppressWarnings("ConstantConditions")
    public class HolderClass extends RecyclerView.ViewHolder {
        TextView lockId, bleAddress, simNo;

        public HolderClass(@NonNull View itemView) {
            super(itemView);
            lockId = itemView.findViewById(R.id.lock_id);
            bleAddress = itemView.findViewById(R.id.lock_ble);
            simNo = itemView.findViewById(R.id.lock_sim);
            if (!(lockType.equals("AT_BLE") || lockType.equals("QT_BLE") || lockType.equals("NR_BLE"))) {
                bleAddress.setVisibility(View.GONE);
                simNo.setVisibility(View.VISIBLE);
            } else if(lockType.equals("AT_BLE_GSM") || lockType.equals("QT_BLE_GSM") || lockType.equals("NR_BLE_GSM")){
                simNo.setVisibility(View.VISIBLE);
                bleAddress.setVisibility(View.VISIBLE);
            }
            else{
                simNo.setVisibility(View.GONE);
                bleAddress.setVisibility(View.VISIBLE);
            }
        }
    }
}
