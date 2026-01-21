package in.pubbs.pubbsadmin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SelectStationAdapter extends RecyclerView.Adapter<SelectStationAdapter.VH> {

    public interface OnStationClick {
        void onClick(SelectStationActivity.StationRow row);
    }

    private final List<SelectStationActivity.StationRow> items;
    private final OnStationClick onStationClick;

    public SelectStationAdapter(List<SelectStationActivity.StationRow> items, OnStationClick onStationClick) {
        this.items = items;
        this.onStationClick = onStationClick;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_station_select, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        SelectStationActivity.StationRow row = items.get(position);
        holder.name.setText(row.stationName == null ? row.stationId : row.stationName);
        holder.itemView.setOnClickListener(v -> onStationClick.onClick(row));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView name;
        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.station_name);
        }
    }
}

