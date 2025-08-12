package in.pubbs.pubbsadmin.Adapter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

import in.pubbs.pubbsadmin.ManageEmployeeDetails;
import in.pubbs.pubbsadmin.R;

public class ManageEmployeeAdapter extends RecyclerView.Adapter<ManageEmployeeAdapter.HolderClass> {
    ArrayList<Map<String, Object>> list;
    Context context;

    public ManageEmployeeAdapter() {
    }

    public ManageEmployeeAdapter(ArrayList<Map<String, Object>> list, Context context) {
        this.list = list;
        this.context = context;
    }

    @NonNull
    @Override
    public HolderClass onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.manage_employee_row, parent, false);
        return new HolderClass(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HolderClass holder, int position) {
        Log.d("ManageEmployeeAdapter", list.get(position).toString());
        holder.id.setText(Objects.requireNonNull(list.get(position).get("Mobile")).toString());
        holder.name.setText(Objects.requireNonNull(list.get(position).get("Name")).toString());
        holder.designation.setText(Objects.requireNonNull(list.get(position).get("Designation")).toString());
        if (Objects.requireNonNull(list.get(position).get("Status")).toString().equals("true")) {
            holder.status.setText("Active");
            holder.status.setTextColor(context.getResources().getColor(R.color.green_200));
        } else if (Objects.requireNonNull(list.get(position).get("Status")).toString().equals("false")) {
            holder.status.setText("Deactivated");
            holder.status.setTextColor(context.getResources().getColor(R.color.red_200));
        }
        holder.container.setOnClickListener(v -> {
            Intent intent;
            if (Objects.requireNonNull(list.get(position).get("Designation")).equals("Area Manager")) {
                intent = new Intent(context, ManageEmployeeDetails.class);
                intent.putExtra("action", "show");
                intent.putExtra("path", Objects.requireNonNull(holder.sharedPreferences.getString("organisationName", null)).replace(" ","") + "/AM/" + list.get(position).get("Mobile"));
                context.startActivity(intent);
            } else if (Objects.requireNonNull(list.get(position).get("Designation")).equals("Service Manager")) {
                intent = new Intent(context, ManageEmployeeDetails.class);
                intent.putExtra("action", "show");
                intent.putExtra("path", Objects.requireNonNull(holder.sharedPreferences.getString("organisationName", null)).replace(" ","") + "/SM/" + list.get(position).get("Mobile"));
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class HolderClass extends RecyclerView.ViewHolder {
        TextView name, id, designation, status;
        ConstraintLayout container;
        SharedPreferences sharedPreferences;
        ImageView picIcon;

        public HolderClass(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.user_name);
            id = itemView.findViewById(R.id.user_id);
            designation = itemView.findViewById(R.id.designation);
            container = itemView.findViewById(R.id.container);
            status = itemView.findViewById(R.id.status);
            picIcon = itemView.findViewById(R.id.icon_user);
            picIcon.setVisibility(View.GONE);
            sharedPreferences = context.getSharedPreferences(context.getResources().getString(R.string.sharedPreferences), Context.MODE_PRIVATE);
        }
    }
}
