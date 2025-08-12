package in.pubbs.pubbsadmin.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import in.pubbs.pubbsadmin.R;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.HolderClass> {
        ArrayList list;
public TransactionAdapter() {
        }

public TransactionAdapter(ArrayList list) {
        this.list = list;
        }

@NonNull
@Override
public HolderClass onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new HolderClass(LayoutInflater.from(parent.getContext())
        .inflate(R.layout.activity_manage_bicycle_row,parent,false));
        }

@Override
public void onBindViewHolder(@NonNull HolderClass holder, int position) {
        String val = (String) list.get(position);
        holder.data.setText(val);
        }

@Override
public int getItemCount() {
        return list.size();
        }

public class HolderClass extends RecyclerView.ViewHolder {
    TextView data;
    ImageView icon;
    public HolderClass(@NonNull View itemView) {
        super(itemView);
        data = itemView.findViewById(R.id.data);
        icon = itemView.findViewById(R.id.icon);
        icon.setImageResource(R.drawable.ic_transaction_black);
    }
}
}

