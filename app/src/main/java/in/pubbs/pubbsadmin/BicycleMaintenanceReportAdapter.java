package in.pubbs.pubbsadmin;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class BicycleMaintenanceReportAdapter extends RecyclerView.Adapter<BicycleMaintenanceReportAdapter.VH> {

    public interface OnViewOnMapClick {
        void onClick(ReportNotificationsActivity.ReportItem item);
    }

    private final List<ReportNotificationsActivity.ReportItem> items;
    private final OnViewOnMapClick onViewOnMapClick;

    public BicycleMaintenanceReportAdapter(List<ReportNotificationsActivity.ReportItem> items,
                                          OnViewOnMapClick onViewOnMapClick) {
        this.items = items;
        this.onViewOnMapClick = onViewOnMapClick;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_report_notification_with_status, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ReportNotificationsActivity.ReportItem item = items.get(position);
        holder.bicycle.setText("Issue reported for Bicycle " + item.bicycleId);
        holder.issueType.setText("Issue Type : " + item.issue);
        holder.viewOnMap.setOnClickListener(v -> onViewOnMapClick.onClick(item));

        // Status chip - use same logic as ReportNotificationsAdapter
        updateStatusChip(holder.statusChip, item.cycleStatus, item.bicycleId);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView bicycle, issueType, statusChip;
        Button viewOnMap;

        VH(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.icon);
            bicycle = itemView.findViewById(R.id.tv_bicycle);
            issueType = itemView.findViewById(R.id.tv_issue_type);
            statusChip = itemView.findViewById(R.id.tv_status_chip);
            viewOnMap = itemView.findViewById(R.id.btn_view_on_map);
        }
    }
    
    private void updateStatusChip(TextView statusChip, String reportStatus, String bicycleId) {
        android.util.Log.d("BicycleMaintenanceAdapter", "=== updateStatusChip ===");
        android.util.Log.d("BicycleMaintenanceAdapter", "reportStatus: " + reportStatus);
        android.util.Log.d("BicycleMaintenanceAdapter", "bicycleId: " + bicycleId);
        
        // First check report status
        String status = reportStatus != null ? reportStatus.trim() : null;
        android.util.Log.d("BicycleMaintenanceAdapter", "trimmed status: " + status);
        
        if (status != null && !status.isEmpty()) {
            // Normalize status for comparison (remove spaces, lowercase)
            String normalizedStatus = status.toLowerCase().replaceAll("\\s+", "");
            android.util.Log.d("BicycleMaintenanceAdapter", "normalized status: " + normalizedStatus);
            
            if (normalizedStatus.contains("inrepair") || normalizedStatus.equals("inrepair")) {
                android.util.Log.d("BicycleMaintenanceAdapter", "MATCH: InRepair - showing RED chip");
                statusChip.setVisibility(View.VISIBLE);
                statusChip.setBackgroundResource(R.drawable.status_chip_red);
                statusChip.setText("In Repair");
                return;
            } else if (normalizedStatus.equals("active")) {
                android.util.Log.d("BicycleMaintenanceAdapter", "MATCH: Active - showing GREEN chip");
                statusChip.setVisibility(View.VISIBLE);
                statusChip.setBackgroundResource(R.drawable.status_chip_active);
                statusChip.setText("Active");
                return;
            } else {
                android.util.Log.d("BicycleMaintenanceAdapter", "Report status doesn't match InRepair/Active: " + normalizedStatus);
            }
        } else {
            android.util.Log.d("BicycleMaintenanceAdapter", "Report status is null or empty, fetching bicycle status");
        }
        
        // If report status doesn't match, fetch bicycle status as fallback
        if (bicycleId != null && !bicycleId.isEmpty()) {
            android.util.Log.d("BicycleMaintenanceAdapter", "Fetching bicycle status for: " + bicycleId);
            fetchBicycleStatus(statusChip, bicycleId);
        } else {
            android.util.Log.d("BicycleMaintenanceAdapter", "bicycleId is null/empty, hiding chip");
            statusChip.setVisibility(View.GONE);
        }
    }
    
    private void fetchBicycleStatus(TextView statusChip, String bicycleId) {
        try {
            Context context = statusChip.getContext();
            SharedPreferences sp = context.getSharedPreferences("pubbs", Context.MODE_PRIVATE);
            String org = sp.getString("organisationName", "");
            if (org == null || org.isEmpty()) {
                android.util.Log.d("BicycleMaintenanceAdapter", "Org name is empty, hiding chip");
                statusChip.setVisibility(View.GONE);
                return;
            }
            org = org.replaceAll(" ", "");
            android.util.Log.d("BicycleMaintenanceAdapter", "Fetching from org: " + org + ", bicycleId: " + bicycleId);
            
            DatabaseReference bikeRef = FirebaseDatabase.getInstance()
                    .getReference(org)
                    .child("Bicycle")
                    .child(bicycleId)
                    .child("cycleStatus");
            
            bikeRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    android.util.Log.d("BicycleMaintenanceAdapter", "Bicycle status snapshot exists: " + snapshot.exists());
                    if (snapshot.exists()) {
                        Object value = snapshot.getValue();
                        android.util.Log.d("BicycleMaintenanceAdapter", "Bicycle cycleStatus value: " + value);
                        if (value != null) {
                            String bikeStatus = String.valueOf(value).trim();
                            String normalizedBikeStatus = bikeStatus.toLowerCase().replaceAll("\\s+", "");
                            android.util.Log.d("BicycleMaintenanceAdapter", "Normalized bicycle status: " + normalizedBikeStatus);
                            
                            if (normalizedBikeStatus.contains("inrepair") || normalizedBikeStatus.equals("inrepair")) {
                                android.util.Log.d("BicycleMaintenanceAdapter", "BICYCLE STATUS MATCH: InRepair - showing RED chip");
                                statusChip.setVisibility(View.VISIBLE);
                                statusChip.setBackgroundResource(R.drawable.status_chip_red);
                                statusChip.setText("In Repair");
                            } else if (normalizedBikeStatus.equals("active")) {
                                android.util.Log.d("BicycleMaintenanceAdapter", "BICYCLE STATUS MATCH: Active - showing GREEN chip");
                                statusChip.setVisibility(View.VISIBLE);
                                statusChip.setBackgroundResource(R.drawable.status_chip_active);
                                statusChip.setText("Active");
                            } else {
                                android.util.Log.d("BicycleMaintenanceAdapter", "Bicycle status doesn't match: " + normalizedBikeStatus);
                                statusChip.setVisibility(View.GONE);
                            }
                        } else {
                            android.util.Log.d("BicycleMaintenanceAdapter", "Bicycle status value is null");
                            statusChip.setVisibility(View.GONE);
                        }
                    } else {
                        android.util.Log.d("BicycleMaintenanceAdapter", "Bicycle status snapshot doesn't exist");
                        statusChip.setVisibility(View.GONE);
                    }
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    android.util.Log.e("BicycleMaintenanceAdapter", "Error fetching bicycle status: " + error.getMessage());
                    statusChip.setVisibility(View.GONE);
                }
            });
        } catch (Exception e) {
            android.util.Log.e("BicycleMaintenanceAdapter", "Exception fetching bicycle status", e);
            statusChip.setVisibility(View.GONE);
        }
    }
}

