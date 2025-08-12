package in.pubbs.pubbsadmin.Adapter;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import in.pubbs.pubbsadmin.BottomSheet.BottomSheetSubscriptionDetails;
import in.pubbs.pubbsadmin.Model.SubscriptionList;
import in.pubbs.pubbsadmin.R;

public class ManageSubscriptionAdapter extends RecyclerView.Adapter<ManageSubscriptionAdapter.MyViewHolder> {
    private List<SubscriptionList> areas;
    ArrayList<Map<String, Object>> arrayList;
    Context mContext;
    FragmentManager fragmentManager;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView data;
        public ImageView icon, details_icon;
        public ConstraintLayout subscriptionLayout;

        public MyViewHolder(View view) {
            super(view);
            data = view.findViewById(R.id.data);
            icon = view.findViewById(R.id.icon);
            details_icon = view.findViewById(R.id.details_icon);
            subscriptionLayout = view.findViewById(R.id.subscription_list_layout);
        }
    }

    public ManageSubscriptionAdapter(ArrayList areas, FragmentManager fragmentManager) {
        this.arrayList = areas;
        this.fragmentManager = fragmentManager;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_manage_subscription_row, parent, false);

        return new ManageSubscriptionAdapter.MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        String subscriptionName = (String) arrayList.get(position).get("subscriptionPlanName");
        String subscriptionId = (String) arrayList.get(position).get("subscriptionId");
        String areaId = (String) arrayList.get(position).get("areaId");
        String areaName = (String) arrayList.get(position).get("areaName");
        Boolean subscriptionStatus = (Boolean)arrayList.get(position).get("subscriptionStatus");
        String subscriptionDescription = (String) arrayList.get(position).get("subscriptionDescription");
        holder.data.setText("Subscription Name: "+subscriptionName);
        holder.subscriptionLayout.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("subscriptionName", subscriptionName);
            bundle.putString("subscriptionId", subscriptionId);
            bundle.putString("areaId", areaId);
            bundle.putString("areaName", areaName);
            bundle.putBoolean("subscriptionStatus", subscriptionStatus);
            bundle.putString("subscriptionDescription", subscriptionDescription);
            BottomSheetSubscriptionDetails bottomsheetFragment = new BottomSheetSubscriptionDetails();
            bottomsheetFragment.setArguments(bundle);
            bottomsheetFragment.show(fragmentManager, "dialog");
        });
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }



}
