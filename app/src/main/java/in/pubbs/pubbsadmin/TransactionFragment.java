package in.pubbs.pubbsadmin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import in.pubbs.pubbsadmin.Adapter.TransactionAdapter;

/*Created by Souvik Datta*/
public class TransactionFragment extends Fragment {
    View view;
    RecyclerView recyclerView;
    ConstraintLayout noData;
    RecyclerView.LayoutManager layoutManager;
    ArrayList arrayList = new ArrayList();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.activity_transaction, container, false);
        init();
        return view;
    }

    private void init() {
        recyclerView = view.findViewById(R.id.recycler_view);
        noData = view.findViewById(R.id.no_data_found);
        layoutManager = new LinearLayoutManager(view.getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        for (int i = 1; i < 10; i++) {

            arrayList.add("transaction_" + i);
        }
        if (arrayList.size() > 0) {
            TransactionAdapter transactionAdapter = new TransactionAdapter(arrayList);
            recyclerView.setAdapter(transactionAdapter);
            transactionAdapter.notifyDataSetChanged();
        } else {
            recyclerView.setVisibility(View.GONE);
            noData.setVisibility(View.VISIBLE);
        }
    }
    //May require to implement onResume();
}
