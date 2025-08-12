package in.pubbs.pubbsadmin.BottomSheet;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Objects;

import in.pubbs.pubbsadmin.R;

public class BottomSheetSubscriptionDetails extends BottomSheetDialogFragment {
    private BottomSheetBehavior mBehavior;
    private TextView descriptionHeader, subscription_id, subscription_name, area_id, area_name, subscription_status, subscription_description, area_now;
    private ImageView showMap;
    String subscriptionId, subscriptionName, areaId, areaName, subscriptionDescription;
    boolean subscriptionStatus;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        View view = View.inflate(getContext(), R.layout.custom_subscription_details, null);
        dialog.setContentView(view);
        subscriptionName = Objects.requireNonNull(getArguments()).getString("subscriptionName");
        subscriptionId = getArguments().getString("subscriptionId");
        areaId = getArguments().getString("areaId");
        areaName = getArguments().getString("areaName");
        subscriptionStatus = getArguments().getBoolean("subscriptionStatus");
        subscriptionDescription = getArguments().getString("subscriptionDescription");
        mBehavior = BottomSheetBehavior.from((View) view.getParent());
        descriptionHeader = view.findViewById(R.id.description_header);
        subscription_id = view.findViewById(R.id.subscription_id);
        subscription_id.setText("Subscription ID: "+subscriptionId);
        subscription_name = view.findViewById(R.id.subscription_name);
        subscription_name.setText("Subscription Name: "+subscriptionName);
        area_id = view.findViewById(R.id.area_id);
        area_id.setText("Area ID: "+areaId);
        area_name = view.findViewById(R.id.area_name);
        area_name.setText("Area Name: "+areaName);
        subscription_status = view.findViewById(R.id.subscription_status);
        subscription_status.setText("Active Status: "+ subscriptionStatus);
        subscription_description = view.findViewById(R.id.subscription_description);
        subscription_description.setText(subscriptionDescription);
        area_now = view.findViewById(R.id.area_now);
        showMap = view.findViewById(R.id.show_map);
        showMap.setOnClickListener(v -> dismiss());
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }
}
