package in.pubbs.pubbsadmin;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import in.pubbs.pubbsadmin.Adapter.UserMessageListAdapter;

public class UserManageListActivity extends AppCompatActivity {

    private UserMessageListAdapter userMessageListAdapter;
    private RecyclerView rv_user_message_list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_manage_list);

        findViewById(R.id.iv_back).setOnClickListener(v -> onBackPressed());
        TextView tv_title = findViewById(R.id.tv_title);
        tv_title.setText("User Message List");
        ImageView iv_menu = findViewById(R.id.iv_menu);
        iv_menu.setVisibility(View.GONE);


        rv_user_message_list = findViewById(R.id.rv_user_message_list);
        rv_user_message_list.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));

        userMessageListAdapter = new UserMessageListAdapter(this, (o, position) -> {

        });

        rv_user_message_list.setAdapter(userMessageListAdapter);

    }

}
