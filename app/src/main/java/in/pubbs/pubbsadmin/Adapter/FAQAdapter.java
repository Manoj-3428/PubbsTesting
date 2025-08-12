package in.pubbs.pubbsadmin.Adapter;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import in.pubbs.pubbsadmin.BottomSheet.BottomsheetFragmentSingleFAQ;
import in.pubbs.pubbsadmin.Model.FAQList;
import in.pubbs.pubbsadmin.R;

public class FAQAdapter extends RecyclerView.Adapter<FAQAdapter.MyViewHolder> {

    private List<FAQList> questionList;
    private String TAG = FAQAdapter.class.getSimpleName();
    private Context mContext;
    private LayoutInflater inflater;
    private FragmentManager fragmentManager;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView question;
        ImageView details;
        ConstraintLayout container;

        public MyViewHolder(View view) {
            super(view);
            question = itemView.findViewById(R.id.data);
            container = itemView.findViewById(R.id.container);
            details = itemView.findViewById(R.id.details_icon);
            details.setVisibility(View.GONE);
        }
    }

    public FAQAdapter(List<FAQList> questionList, FragmentManager fragmentManager) {
        this.questionList = questionList;
        this.fragmentManager = fragmentManager;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_recycler_row, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        FAQList faqList = questionList.get(position);
        holder.question.setText(faqList.getQuestion());
        holder.container.setOnClickListener(v -> {
            Log.d(TAG, "Question clicked: " + faqList.getQuestion() + "\t" + faqList.getAnswer());
            Bundle bundle = new Bundle();
            bundle.putString("question", faqList.getQuestion());
            bundle.putString("answer", faqList.getAnswer());
            BottomsheetFragmentSingleFAQ bottomsheetFragment = new BottomsheetFragmentSingleFAQ();
            bottomsheetFragment.setArguments(bundle);
            bottomsheetFragment.show(fragmentManager, "dialog");
        });

    }

    @Override
    public int getItemCount() {
        return questionList.size();
    }



}
