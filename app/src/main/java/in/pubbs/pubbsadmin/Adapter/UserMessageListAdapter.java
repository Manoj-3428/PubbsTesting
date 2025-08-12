package in.pubbs.pubbsadmin.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import in.pubbs.pubbsadmin.R;

public class UserMessageListAdapter extends RecyclerView.Adapter<UserMessageListAdapter.MyViewHolder> {

    private OnClickItems mInterface;
    private Context mContext;



    public UserMessageListAdapter(Context context, OnClickItems interfaces) {
        this.mContext=context;
        this.mInterface = interfaces;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_user_message_list_adapter_items, parent, false);

        return new MyViewHolder(itemView);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull final MyViewHolder holder, final int position) {



    }

    @Override
    public int getItemCount() {
        return 30;
    }

    class MyViewHolder extends RecyclerView.ViewHolder {


        MyViewHolder(View view) {
            super(view);


        }
    }
    public interface OnClickItems{
        void  details(Object o, int position);
    }

}
