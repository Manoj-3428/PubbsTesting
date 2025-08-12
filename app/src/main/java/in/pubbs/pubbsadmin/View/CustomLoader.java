package in.pubbs.pubbsadmin.View;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

import in.pubbs.pubbsadmin.R;

public class CustomLoader extends Dialog {
    public CustomLoader(@NonNull Context context) {
        super(context);
    }

    public CustomLoader(@NonNull Context context, int themeResId) {
        super(context, themeResId);
    }

    protected CustomLoader(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        Window window = getWindow();
        Objects.requireNonNull(window).setBackgroundDrawableResource(android.R.color.transparent);
        setContentView(R.layout.custom_loader);
    }
}
