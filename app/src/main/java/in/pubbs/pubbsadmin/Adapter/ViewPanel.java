package in.pubbs.pubbsadmin.Adapter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import in.pubbs.pubbsadmin.BottomSheet.BottomSheetAreaDetails;
import in.pubbs.pubbsadmin.Model.Area;
import in.pubbs.pubbsadmin.R;

public class ViewPanel extends RecyclerView.Adapter<ViewPanel.MyViewHolder> {
    private List<Area> areas;
    ArrayList<Map<String, Object>> arrayList;
    private String TAG = ManageArea.class.getSimpleName();
    FragmentManager fragmentManager;


    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView area_name;
        public ConstraintLayout areaListLayout;

        public MyViewHolder(View view) {
            super(view);
            area_name = view.findViewById(R.id.area_name);
            areaListLayout = view.findViewById(R.id.area_list_layout);
        }
    }

    public ViewPanel(ArrayList areas, FragmentManager fragmentManager) {
        this.arrayList = areas;
        this.fragmentManager = fragmentManager;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_area, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        String areaId = (String) arrayList.get(position).get("id");
        String areaName = (String) arrayList.get(position).get("name");
        holder.area_name.setText(areaName);
        holder.areaListLayout.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("areaId", areaId);
            bundle.putString("areaName", areaName);
            BottomSheetAreaDetails bottomsheetFragment = new BottomSheetAreaDetails();
            bottomsheetFragment.setArguments(bundle);
            bottomsheetFragment.show(fragmentManager, "dialog");
        });
    }


    @Override
    public int getItemCount() {
        return arrayList.size();
    }

}
