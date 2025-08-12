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

import in.pubbs.pubbsadmin.R;

public class BottomSheetStationFragment extends BottomSheetDialogFragment {
    private BottomSheetBehavior mBehavior;
    private TextView descriptionHeader, description_, areaNow, description;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        View view = View.inflate(getContext(), R.layout.bottom_sheet_description, null);
        dialog.setContentView(view);
        mBehavior = BottomSheetBehavior.from((View) view.getParent());
        descriptionHeader = view.findViewById(R.id.description_header);
        descriptionHeader.setText(R.string.add_station_help);
        description_ = view.findViewById(R.id.description_);
        description_.setText(R.string.add_station_header);
        description = view.findViewById(R.id.description);
        description.setText(R.string.adding_station_help);
        areaNow = view.findViewById(R.id.area_now);
        areaNow.setText(R.string.select_station);
        ImageView showMap = view.findViewById(R.id.show_map);
        showMap.setOnClickListener(v -> dismiss());
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }
}
