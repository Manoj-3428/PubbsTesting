package in.pubbs.pubbsadmin;

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

public class ReportNotificationsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    public interface OnViewOnMapClick {
        void onClick(ReportNotificationsActivity.ReportItem item);
    }

    private final List<Object> items;
    private final OnViewOnMapClick onViewOnMapClick;

    public ReportNotificationsAdapter(List<Object> items, OnViewOnMapClick onViewOnMapClick) {
        this.items = items;
        this.onViewOnMapClick = onViewOnMapClick;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof String ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.row_report_section_header, parent, false);
            return new HeaderVH(v);
        } else {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.row_report_notification, parent, false);
            return new ItemVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object obj = items.get(position);
        if (holder instanceof HeaderVH) {
            ((HeaderVH) holder).title.setText(String.valueOf(obj));
        } else if (holder instanceof ItemVH) {
            ReportNotificationsActivity.ReportItem item = (ReportNotificationsActivity.ReportItem) obj;
            ItemVH vh = (ItemVH) holder;
            vh.bicycleId.setText("Issue reported for Bicycle " + item.bicycleId);
            vh.issueType.setText("Issue Type : " + item.issue);
            vh.viewOnMap.setOnClickListener(v -> onViewOnMapClick.onClick(item));
            
            // Update status chip - check both report status and fetch bicycle status as fallback
            updateStatusChip(vh.statusChip, item.cycleStatus, item.bicycleId);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderVH extends RecyclerView.ViewHolder {
        TextView title;
        HeaderVH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.section_title);
        }
    }

    static class ItemVH extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView bicycleId, issueType, statusChip;
        Button viewOnMap;
        ItemVH(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.icon);
            bicycleId = itemView.findViewById(R.id.tv_bicycle);
            issueType = itemView.findViewById(R.id.tv_issue_type);
            viewOnMap = itemView.findViewById(R.id.btn_view_on_map);
            statusChip = itemView.findViewById(R.id.tv_status_chip);
        }
    }
    
    private void updateStatusChip(TextView statusChip, String reportStatus, String bicycleId) {
        android.util.Log.d("ReportAdapter", "=== updateStatusChip ===");
        android.util.Log.d("ReportAdapter", "reportStatus: " + reportStatus);
        android.util.Log.d("ReportAdapter", "bicycleId: " + bicycleId);
        
        // First check report status
        String status = reportStatus != null ? reportStatus.trim() : null;
        android.util.Log.d("ReportAdapter", "trimmed status: " + status);
        
        if (status != null && !status.isEmpty()) {
            // Normalize status for comparison
            String normalizedStatus = status.toLowerCase().replaceAll("\\s+", "");
            android.util.Log.d("ReportAdapter", "normalized status: " + normalizedStatus);
            
            if (normalizedStatus.contains("inrepair") || normalizedStatus.equals("inrepair")) {
                android.util.Log.d("ReportAdapter", "MATCH: InRepair - showing RED chip");
                statusChip.setVisibility(View.VISIBLE);
                statusChip.setBackgroundResource(R.drawable.status_chip_red);
                statusChip.setText("In Repair");
                return;
            } else if (normalizedStatus.equals("active")) {
                android.util.Log.d("ReportAdapter", "MATCH: Active - showing GREEN chip");
                statusChip.setVisibility(View.VISIBLE);
                statusChip.setBackgroundResource(R.drawable.status_chip_active);
                statusChip.setText("Active");
                return;
            } else {
                android.util.Log.d("ReportAdapter", "Report status doesn't match InRepair/Active: " + normalizedStatus);
            }
        } else {
            android.util.Log.d("ReportAdapter", "Report status is null or empty, fetching bicycle status");
        }
        
        // If report status doesn't match, fetch bicycle status as fallback
        if (bicycleId != null && !bicycleId.isEmpty()) {
            android.util.Log.d("ReportAdapter", "Fetching bicycle status for: " + bicycleId);
            fetchBicycleStatus(statusChip, bicycleId);
        } else {
            android.util.Log.d("ReportAdapter", "bicycleId is null/empty, hiding chip");
            statusChip.setVisibility(View.GONE);
        }
    }
    
    private void fetchBicycleStatus(TextView statusChip, String bicycleId) {
        try {
            android.content.Context context = statusChip.getContext();
            android.content.SharedPreferences sp = context.getSharedPreferences("pubbs", android.content.Context.MODE_PRIVATE);
            String org = sp.getString("organisationName", "");
            if (org == null || org.isEmpty()) {
                android.util.Log.d("ReportAdapter", "Org name is empty, hiding chip");
                statusChip.setVisibility(View.GONE);
                return;
            }
            org = org.replaceAll(" ", "");
            android.util.Log.d("ReportAdapter", "Fetching from org: " + org + ", bicycleId: " + bicycleId);
            
            DatabaseReference bikeRef = FirebaseDatabase.getInstance()
                    .getReference(org)
                    .child("Bicycle")
                    .child(bicycleId)
                    .child("cycleStatus");
            
            bikeRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    android.util.Log.d("ReportAdapter", "Bicycle status snapshot exists: " + snapshot.exists());
                    if (snapshot.exists()) {
                        Object value = snapshot.getValue();
                        android.util.Log.d("ReportAdapter", "Bicycle cycleStatus value: " + value);
                        if (value != null) {
                            String bikeStatus = String.valueOf(value).trim();
                            String normalizedBikeStatus = bikeStatus.toLowerCase().replaceAll("\\s+", "");
                            android.util.Log.d("ReportAdapter", "Normalized bicycle status: " + normalizedBikeStatus);
                            
                            if (normalizedBikeStatus.contains("inrepair") || normalizedBikeStatus.equals("inrepair")) {
                                android.util.Log.d("ReportAdapter", "BICYCLE STATUS MATCH: InRepair - showing RED chip");
                                statusChip.setVisibility(View.VISIBLE);
                                statusChip.setBackgroundResource(R.drawable.status_chip_red);
                                statusChip.setText("In Repair");
                            } else if (normalizedBikeStatus.equals("active")) {
                                android.util.Log.d("ReportAdapter", "BICYCLE STATUS MATCH: Active - showing GREEN chip");
                                statusChip.setVisibility(View.VISIBLE);
                                statusChip.setBackgroundResource(R.drawable.status_chip_active);
                                statusChip.setText("Active");
                            } else {
                                android.util.Log.d("ReportAdapter", "Bicycle status doesn't match: " + normalizedBikeStatus);
                                statusChip.setVisibility(View.GONE);
                            }
                        } else {
                            android.util.Log.d("ReportAdapter", "Bicycle status value is null");
                            statusChip.setVisibility(View.GONE);
                        }
                    } else {
                        android.util.Log.d("ReportAdapter", "Bicycle status snapshot doesn't exist");
                        statusChip.setVisibility(View.GONE);
                    }
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    android.util.Log.e("ReportAdapter", "Error fetching bicycle status: " + error.getMessage());
                    statusChip.setVisibility(View.GONE);
                }
            });
        } catch (Exception e) {
            android.util.Log.e("ReportAdapter", "Exception fetching bicycle status", e);
            statusChip.setVisibility(View.GONE);
        }
    }
}

