package in.pubbs.pubbsadmin;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class UserChatActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_chat);

        findViewById(R.id.iv_back).setOnClickListener(v -> onBackPressed());
        TextView tv_title = findViewById(R.id.tv_title);
        tv_title.setText("User Chat");
        ImageView iv_menu = findViewById(R.id.iv_menu);
        iv_menu.setVisibility(View.GONE);

    }
}
