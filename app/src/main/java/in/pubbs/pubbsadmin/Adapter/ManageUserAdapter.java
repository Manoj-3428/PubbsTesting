package in.pubbs.pubbsadmin.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Map;

import in.pubbs.pubbsadmin.R;
import in.pubbs.pubbsadmin.UserDetailActivity;

public class ManageUserAdapter extends RecyclerView.Adapter<ManageUserAdapter.HolderClass> {
    ArrayList<Map<String, Object>> list;
    Context context;

    public ManageUserAdapter(ArrayList list, Context context) {
        this.list = list;
        this.context = context;
    }
    
    public void updateList(ArrayList<Map<String, Object>> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ManageUserAdapter.HolderClass onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_manage_user_row, parent, false);
        return new ManageUserAdapter.HolderClass(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ManageUserAdapter.HolderClass holder, int position) {
        String userName = (String) list.get(position).get("name");
        String id = (String) list.get(position).get("user_id");
        String phone = (String) list.get(position).get("mobile");
        holder.name.setText("Name: " + userName);
        holder.id.setText("Id: " + id);
        holder.phone.setText("Phone: " + phone);

        holder.container.setOnClickListener(v -> {
            // Navigate to UserDetailActivity with user mobile as identifier
            if (phone != null && !phone.isEmpty()) {
                Intent intent = new Intent(context, UserDetailActivity.class);
                intent.putExtra("USER_MOBILE", phone);
            context.startActivity(intent);
            }
        });

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class HolderClass extends RecyclerView.ViewHolder {
        TextView name, phone, id;
        ConstraintLayout container;

        public HolderClass(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.user_name);
            phone = itemView.findViewById(R.id.user_phone);
            id = itemView.findViewById(R.id.user_id);
            container = itemView.findViewById(R.id.container);
        }
    }
}