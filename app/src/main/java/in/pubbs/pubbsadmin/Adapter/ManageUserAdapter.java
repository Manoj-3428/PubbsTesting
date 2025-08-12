package in.pubbs.pubbsadmin.Adapter;

import android.content.Context;
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

public class ManageUserAdapter extends RecyclerView.Adapter<ManageUserAdapter.HolderClass> {
    ArrayList<Map<String, Object>> list;
    Context context;

    public ManageUserAdapter(ArrayList list, Context context) {
        this.list = list;
        this.context = context;
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
           /* Intent intent = new Intent(context, ManageUserDetails.class);
            intent.putExtra("user_name", userName);
            intent.putExtra("id", id);
            intent.putExtra("mobile", phone);
            context.startActivity(intent);
            //Code to be added.*/
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