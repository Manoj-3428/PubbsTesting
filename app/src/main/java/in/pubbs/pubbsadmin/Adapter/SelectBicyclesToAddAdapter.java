package in.pubbs.pubbsadmin.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import in.pubbs.pubbsadmin.R;

/**
 * Adapter for multi-select list of unassigned bicycles (id + percentage).
 */
public class SelectBicyclesToAddAdapter extends RecyclerView.Adapter<SelectBicyclesToAddAdapter.ViewHolder> {

    public static class Item {
        public String bicycleId;
        public int percentage; // cyclebattery or 100
        public boolean selected;

        public Item(String bicycleId, int percentage) {
            this.bicycleId = bicycleId;
            this.percentage = percentage;
            this.selected = false;
        }
    }

    private final ArrayList<Item> list;
    private final Set<Integer> selectedPositions = new HashSet<>();

    public SelectBicyclesToAddAdapter(ArrayList<Item> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_select_bicycle_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = list.get(position);
        holder.tvBicycleId.setText(item.bicycleId);
        holder.tvPercentage.setText(item.percentage + "%");
        holder.checkbox.setChecked(item.selected);
        holder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.selected = isChecked;
            if (isChecked) selectedPositions.add(position);
            else selectedPositions.remove(position);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    /** Returns list of selected items (bicycleId, percentage). */
    public ArrayList<Item> getSelectedItems() {
        ArrayList<Item> selected = new ArrayList<>();
        for (Item item : list) {
            if (item.selected) selected.add(item);
        }
        return selected;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkbox;
        TextView tvBicycleId, tvPercentage;

        ViewHolder(View itemView) {
            super(itemView);
            checkbox = itemView.findViewById(R.id.checkbox);
            tvBicycleId = itemView.findViewById(R.id.tv_bicycle_id);
            tvPercentage = itemView.findViewById(R.id.tv_percentage);
        }
    }
}
