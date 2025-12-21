package in.pubbs.pubbsadmin.Adapter;

import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import in.pubbs.pubbsadmin.R;

public class CycleDemandAdapter extends RecyclerView.Adapter<CycleDemandAdapter.ViewHolder> {

    private final List<StationDemandItem> stationList;
    // Map to store demand values: stationId -> demand value
    private final Map<String, Integer> demandMap = new HashMap<>();
    
    // Maximum allowed demand value (configurable, default 15)
    public static final int MAX_DEMAND_VALUE = 15;

    public CycleDemandAdapter(List<StationDemandItem> stationList) {
        this.stationList = stationList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cycle_demand_station, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StationDemandItem item = stationList.get(position);
        
        holder.stationName.setText(item.getStationName());
        holder.stationId.setText(item.getStationId());
        
        // Display cycle count
        Integer cycleCount = item.getCycleCount();
        if (cycleCount != null) {
            holder.cycleCount.setText("Cycle Count: " + cycleCount);
        } else {
            holder.cycleCount.setText("Cycle Count: 0");
        }
        
        // Remove any existing TextWatcher to avoid memory leaks
        if (holder.textWatcher != null) {
            holder.demandInput.removeTextChangedListener(holder.textWatcher);
        }
        
        // Set existing demand if any (from Firebase - field may not exist yet)
        // stationCycleDemand field will be created when user saves for first time
        // Don't clamp existing values - show them as-is, validation happens on save
        // IMPORTANT: Check demandMap first (user-entered values) before Firebase values
        // This prevents values from resetting when RecyclerView recycles views
        String stationId = item.getStationId();
        if (demandMap.containsKey(stationId)) {
            // User has already entered a value - preserve it (don't reset on scroll)
            holder.demandInput.setText(String.valueOf(demandMap.get(stationId)));
        } else if (item.getCurrentDemand() != null && item.getCurrentDemand() > 0) {
            // No user input yet - use Firebase value
            holder.demandInput.setText(String.valueOf(item.getCurrentDemand()));
            demandMap.put(stationId, item.getCurrentDemand());
        } else {
            // Field doesn't exist yet - leave empty for user to enter
            holder.demandInput.setText("");
            holder.demandInput.setHint("Max " + MAX_DEMAND_VALUE);
            demandMap.remove(stationId);
        }
        
        // Create and add TextWatcher to track input changes (NO automatic clamping)
        // Validation will only happen when user tries to save (in confirmation dialog)
        holder.textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString().trim();
                if (!text.isEmpty()) {
                    try {
                        int demand = Integer.parseInt(text);
                        // Store the value as-is (validation happens only on save)
                        // Don't clamp or change the value - let user enter what they want
                        demandMap.put(item.getStationId(), demand);
                    } catch (NumberFormatException e) {
                        demandMap.remove(item.getStationId());
                    }
                } else {
                    demandMap.remove(item.getStationId());
                }
            }
        };
        
        // Set max length to prevent entering numbers > 99 (which would be > 15 anyway)
        // This helps prevent typing very large numbers
        holder.demandInput.setFilters(new InputFilter[] {
            new InputFilter.LengthFilter(2) // Max 2 digits (0-15)
        });
        
        holder.demandInput.addTextChangedListener(holder.textWatcher);
    }

    @Override
    public int getItemCount() {
        return stationList.size();
    }

    // Method to get all demand values
    public Map<String, Integer> getDemandMap() {
        return new HashMap<>(demandMap);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView stationPinIcon;
        TextView stationName;
        TextView stationId;
        TextView cycleCount;
        EditText demandInput;
        TextWatcher textWatcher;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            stationPinIcon = itemView.findViewById(R.id.station_pin_icon);
            stationName = itemView.findViewById(R.id.station_name);
            stationId = itemView.findViewById(R.id.station_id);
            cycleCount = itemView.findViewById(R.id.cycle_count);
            demandInput = itemView.findViewById(R.id.demand_input);
        }
    }

    // Helper class to hold station data for the adapter
    public static class StationDemandItem {
        private String stationId;
        private String stationName;
        private Integer currentDemand;
        private Integer cycleCount;

        public StationDemandItem(String stationId, String stationName, Integer currentDemand, Integer cycleCount) {
            this.stationId = stationId;
            this.stationName = stationName;
            this.currentDemand = currentDemand;
            this.cycleCount = cycleCount;
        }

        public String getStationId() {
            return stationId;
        }

        public String getStationName() {
            return stationName;
        }

        public Integer getCurrentDemand() {
            return currentDemand;
        }

        public Integer getCycleCount() {
            return cycleCount;
        }
    }
}

