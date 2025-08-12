package in.pubbs.pubbsadmin.Adapter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import in.pubbs.pubbsadmin.MainActivity;
import in.pubbs.pubbsadmin.Model.Area;
import in.pubbs.pubbsadmin.R;

import static android.content.Context.MODE_PRIVATE;

public class ShowMyAreaAdapter extends RecyclerView.Adapter<ShowMyAreaAdapter.MyViewHolder> {
    private List<Area> areas;
    ArrayList<Map<String, Object>> arrayList;
    private String TAG = ShowMyAreaAdapter.class.getSimpleName();
    Context context;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView area_name;
        public ConstraintLayout areaListLayout;
        SharedPreferences sharedPreferences;
        SharedPreferences.Editor editor;

        public MyViewHolder(View view) {
            super(view);
            area_name = view.findViewById(R.id.area_name);
            areaListLayout = view.findViewById(R.id.area_list_layout);
            sharedPreferences = context.getSharedPreferences("pubbs", MODE_PRIVATE);
            editor = sharedPreferences.edit();
        }
    }

    public ShowMyAreaAdapter(ArrayList areas, Context context) {
        this.arrayList = areas;
        this.context = context;
    }

    @NonNull
    @Override
    public ShowMyAreaAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.area, parent, false);
        return new ShowMyAreaAdapter.MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ShowMyAreaAdapter.MyViewHolder holder, int position) {
        String areaId = (String) arrayList.get(position).get("areaId");
        String areaName = (String) arrayList.get(position).get("areaName");
        holder.area_name.setText(areaName);
        holder.areaListLayout.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), MainActivity.class);
            holder.editor.putBoolean("area_allocated", true);
            holder.editor.putString("zone_area_id", areaId);
            holder.editor.commit();
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

}
