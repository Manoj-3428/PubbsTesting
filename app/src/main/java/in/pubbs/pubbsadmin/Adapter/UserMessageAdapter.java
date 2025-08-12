package in.pubbs.pubbsadmin.Adapter;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import in.pubbs.pubbsadmin.BottomSheet.BottomsheetReportDetails;
import in.pubbs.pubbsadmin.Model.ReportList;
import in.pubbs.pubbsadmin.R;

public class UserMessageAdapter extends RecyclerView.Adapter<UserMessageAdapter.HolderClass> {
    private List<ReportList> reportLists;
    ArrayList<Map<String, Object>> arrayList;
    private String TAG = UserMessageAdapter.class.getSimpleName();
    FragmentManager fragmentManager;

    public UserMessageAdapter(ArrayList reportLists, FragmentManager fragmentManager) {
        this.arrayList = reportLists;
        this.fragmentManager = fragmentManager;
    }

    public UserMessageAdapter() {
    }

    @NonNull
    @Override
    public HolderClass onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new HolderClass(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_user_message_row, parent, false));
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onBindViewHolder(@NonNull HolderClass holder, int position) {
        String reportId = (String) arrayList.get(position).get("reportId");
        String dateTime = (String) arrayList.get(position).get("dateTime");
        String userId = (String) arrayList.get(position).get("userId");
        String bicycleId = (String) arrayList.get(position).get("bicycleId");
        String problem = (String) arrayList.get(position).get("problem");
        holder.reportId.setText(reportId);
        holder.dateTime.setText("Date and Time: " + dateTime);
        holder.container.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("reportId", reportId);
            bundle.putString("dateTime", dateTime);
            bundle.putString("userId", userId);
            bundle.putString("bicycleId", bicycleId);
            bundle.putString("problem", problem);
            BottomsheetReportDetails bottomsheetFragment = new BottomsheetReportDetails();
            bottomsheetFragment.setArguments(bundle);
            bottomsheetFragment.show(fragmentManager, "dialog");
        });
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public class HolderClass extends RecyclerView.ViewHolder {
        TextView reportId, dateTime;
        ImageView icon;
        ConstraintLayout container;

        public HolderClass(@NonNull View itemView) {
            super(itemView);
            reportId = itemView.findViewById(R.id.reportId);
            dateTime = itemView.findViewById(R.id.dateTime);
            icon = itemView.findViewById(R.id.icon);
            container = itemView.findViewById(R.id.container);
        }
    }
}
