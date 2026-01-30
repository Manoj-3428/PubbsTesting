package in.pubbs.pubbsadmin.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import in.pubbs.pubbsadmin.R;

public class ManageCycleListAdapter extends RecyclerView.Adapter<ManageCycleListAdapter.VH> {

    public interface OnItemClick {
        void onClick(Item item);
    }

    public static class Item {
        public final String id;
        public final int batteryPercent;
        public final boolean isEBike;

        public Item(@NonNull String id, int batteryPercent, boolean isEBike) {
            this.id = id;
            this.batteryPercent = batteryPercent;
            this.isEBike = isEBike;
        }
    }

    private final List<Item> items;
    private final OnItemClick onItemClick;

    public ManageCycleListAdapter(@NonNull List<Item> items, @NonNull OnItemClick onItemClick) {
        this.items = items;
        this.onItemClick = onItemClick;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_manage_cycle_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Item item = items.get(position);
        holder.id.setText(item.id);
        holder.battery.setText(item.batteryPercent + "%");
        holder.ebikeBadge.setVisibility(item.isEBike ? View.VISIBLE : View.GONE);
        holder.itemView.setOnClickListener(v -> onItemClick.onClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView id;
        TextView battery;
        LinearLayout ebikeBadge;

        VH(@NonNull View itemView) {
            super(itemView);
            id = itemView.findViewById(R.id.tv_lock_id);
            battery = itemView.findViewById(R.id.battery_value);
            ebikeBadge = itemView.findViewById(R.id.ebike_badge);
        }
    }
}

