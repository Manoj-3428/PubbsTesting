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

import in.pubbs.pubbsadmin.MainActivity;
import in.pubbs.pubbsadmin.R;

public class ShowAllOperatorsAdapter extends RecyclerView.Adapter<ShowAllOperatorsAdapter.HolderClass> {
    ArrayList list;
    Context context;

    public ShowAllOperatorsAdapter(ArrayList list, Context context) {
        this.list = list;
        this.context = context;
    }

    public ShowAllOperatorsAdapter() {
    }

    @NonNull
    @Override
    public HolderClass onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.show_all_operator_row, parent, false);
        return new HolderClass(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HolderClass holder, int position) {
        holder.operatorName.setText(list.get(position).toString());
        holder.constraintLayout.setOnClickListener(v -> {
            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra("operator_name", list.get(position).toString());
            intent.putExtra("type", "PUBBS");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            holder.editor.putString("sa_operator_name", list.get(position).toString());
            holder.editor.commit();
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class HolderClass extends RecyclerView.ViewHolder {
        TextView operatorName;
        ConstraintLayout constraintLayout;
        SharedPreferences.Editor editor;

        public HolderClass(@NonNull View itemView) {
            super(itemView);
            operatorName = itemView.findViewById(R.id.operatorName);
            constraintLayout = itemView.findViewById(R.id.container);
            editor = context.getSharedPreferences("pubbs", Context.MODE_PRIVATE).edit();
        }
    }
}
