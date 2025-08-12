package in.pubbs.pubbsadmin.Adapter;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import in.pubbs.pubbsadmin.Model.Operator;
import in.pubbs.pubbsadmin.OperatorDetails;
import in.pubbs.pubbsadmin.R;

public class ManageOperator extends RecyclerView.Adapter<ManageOperator.MyViewHolder> {
    private List<Operator> operators;
    private String TAG = ManageOperator.class.getSimpleName();

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView operator_id;
        public ConstraintLayout operatorListLayout;

        public MyViewHolder(View view) {
            super(view);
            operator_id = view.findViewById(R.id.operator_id);
            operatorListLayout = view.findViewById(R.id.operator_list_layout);
        }
    }

    public ManageOperator(List<Operator> operators) {
        this.operators = operators;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.operator_list, parent, false);

        return new ManageOperator.MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Operator operator = operators.get(position);
        holder.operator_id.setText(operator.getOperatorOrganisation());
        holder.operatorListLayout.setOnClickListener(v -> {
            Log.d(TAG, "Operator's name:" + operator.getOperatorOrganisation() + "\t" + operator.getOperatorName() + "\t" + operator.getOperatorMobile() + "\t"
                    + operator.getOperatorEmail() + "\t" + operator.getOperatorKey());
            Intent intent = new Intent(v.getContext(), OperatorDetails.class);
            intent.putExtra("operatorOrganisation", operator.getOperatorOrganisation());
            intent.putExtra("operatorName", operator.getOperatorName());
            intent.putExtra("operatorMobile", operator.getOperatorMobile());
            intent.putExtra("operatorEmail", operator.getOperatorEmail());
            intent.putExtra("operatorKey", operator.getOperatorKey());
            v.getContext().startActivity(intent);
        });
    }
    

    @Override
    public int getItemCount() {
        return operators.size();
    }

}
