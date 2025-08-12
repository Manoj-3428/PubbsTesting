package in.pubbs.pubbsadmin.View;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.Objects;

import in.pubbs.pubbsadmin.R;

/*Created by: Parita Dey*/
public class CustomAlertDialog extends Dialog {

    private TextView titleText, content;
    private ImageButton positiveButton, negativeButton;
    private ClickListener positiveClickListener, negativeClickListener;
    private String title, msg;

    public CustomAlertDialog(@NonNull Context context, int themeResId, String title, String msg) {
        super(context, themeResId);
        this.title = title;
        this.msg = msg;
    }
    public CustomAlertDialog(@NonNull Context context, String title, String msg) {
        super(context);
        this.title = title;
        this.msg = msg;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        Window window = getWindow();
        Objects.requireNonNull(window).setBackgroundDrawableResource(android.R.color.transparent);
        setContentView(R.layout.custom_dialog);
        titleText = findViewById(R.id.title);
        content = findViewById(R.id.content);
        positiveButton = findViewById(R.id.positive);
        negativeButton = findViewById(R.id.negetive);
        positiveButton.setOnClickListener(v -> positiveClickListener.onClick(v));
        negativeButton.setOnClickListener(v -> negativeClickListener.onClick(v));
        titleText.setText(title);
        content.setText(msg);
    }


    public void onPositiveButton(ClickListener clickListener) {
        this.positiveClickListener = clickListener;
    }

    public void onNegativeButton(ClickListener clickListener) {
        negativeButton.setVisibility(View.VISIBLE);
        this.negativeClickListener = clickListener;
    }

    public interface ClickListener extends View.OnClickListener {
        @Override
        void onClick(View view);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
