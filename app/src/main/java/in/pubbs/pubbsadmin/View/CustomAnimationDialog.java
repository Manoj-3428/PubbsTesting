package in.pubbs.pubbsadmin.View;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.view.animation.Animation;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.airbnb.lottie.LottieAnimationView;

import java.util.Objects;

import in.pubbs.pubbsadmin.R;

public class CustomAnimationDialog extends Dialog {
    LottieAnimationView lottieAnimationView;
    int animation=0;
    TextView title;


    public CustomAnimationDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lottie_view_layout);
        setCancelable(false);
        Window window = getWindow();
        Objects.requireNonNull(window).setBackgroundDrawableResource(android.R.color.transparent);
        lottieAnimationView=findViewById(R.id.animation_view);
        lottieAnimationView.setRepeatCount(Animation.INFINITE);
        title=findViewById(R.id.title_text);
    }

    public void setAnimation(int animation) {
        this.animation = animation;
        lottieAnimationView.setAnimation(animation);
    }

    public void setTitle(String title) {
        this.title.setText(title);
    }

    public void playAnimation(){
        lottieAnimationView.playAnimation();
    }


}
