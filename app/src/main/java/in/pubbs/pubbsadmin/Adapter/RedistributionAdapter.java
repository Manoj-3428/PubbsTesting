 package in.pubbs.pubbsadmin.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import in.pubbs.pubbsadmin.R;

public class RedistributionAdapter extends RecyclerView.Adapter<RedistributionAdapter.ViewHolder> {

    private final List<String> planList;

    public RedistributionAdapter(List<String> planList) {
        this.planList = planList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_plan, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.planText.setText(planList.get(position));
    }

    @Override
    public int getItemCount() {
        return planList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView planText;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            planText = itemView.findViewById(R.id.plan_text);
        }
    }
}
