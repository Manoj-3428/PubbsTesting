package in.pubbs.pubbsadmin.BottomSheet;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Objects;

import in.pubbs.pubbsadmin.R;

public class BottomsheetFragmentSingleFAQ extends BottomSheetDialogFragment {
    private BottomSheetBehavior mBehavior;
    private TextView descriptionHeader, description_, areaNow, description;
    private String question, answer, TAG = BottomsheetFragmentFAQ.class.getSimpleName();


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        View view = View.inflate(getContext(), R.layout.bottom_sheet_single_faq, null);
        dialog.setContentView(view);
        question = Objects.requireNonNull(getArguments()).getString("question");
        answer = getArguments().getString("answer");
        Log.d(TAG, "Bundle data: " + question + "\t" + answer);
        mBehavior = BottomSheetBehavior.from((View) view.getParent());
        descriptionHeader = view.findViewById(R.id.description_header);
        description_ = view.findViewById(R.id.description_);
        description_.setText(question);
        description = view.findViewById(R.id.description);
        description.setText(answer);
        areaNow = view.findViewById(R.id.area_now);
        areaNow.setText("Go back To FAQ List");

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
