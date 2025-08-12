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

public class BottomsheetReportDetails extends BottomSheetDialogFragment {
    private BottomSheetBehavior mBehavior;
    private TextView descriptionHeader, bicycle_id, report_id, date_time, user_id, problem_details, area_now;
    private ImageView showMap;
    String reportId, dateTime, userId, bicycleId, problem;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        View view = View.inflate(getContext(), R.layout.custom_reporte_details, null);
        dialog.setContentView(view);
        reportId = Objects.requireNonNull(getArguments()).getString("reportId");
        dateTime = getArguments().getString("dateTime");
        userId = getArguments().getString("userId");
        bicycleId = getArguments().getString("bicycleId");
        problem = getArguments().getString("problem");
        mBehavior = BottomSheetBehavior.from((View) view.getParent());
        descriptionHeader = view.findViewById(R.id.description_header);
        bicycle_id = view.findViewById(R.id.bicycle_id);
        bicycle_id.setText("Bicycle ID: " + bicycleId);
        report_id = view.findViewById(R.id.report_id);
        report_id.setText("Report ID: " + reportId);
        date_time = view.findViewById(R.id.date_time);
        date_time.setText("Date Time: " + dateTime);
        user_id = view.findViewById(R.id.user_id);
        user_id.setText("Complaint by: " + userId);
        problem_details = view.findViewById(R.id.problem_details);
        problem_details.setText("Problem details: \n" + problem);
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
