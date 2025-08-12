package in.pubbs.pubbsadmin;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/*Created by Souvik Datta*/
public class UserChat extends AppCompatActivity implements View.OnClickListener {
    TextView title, userID, replyBtn, email, dateTime, messageReceive;
    EditText replyMessage;
    Button send;
    ImageView back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_chat);
        init();
    }

    private void init() {
        title = findViewById(R.id.toolbar_title);
        title.setText("User Chat");
        back = findViewById(R.id.back_button);
        back.setOnClickListener(this);
        userID = findViewById(R.id.user_id);
        email = findViewById(R.id.email_id);
        dateTime = findViewById(R.id.date_time);
        messageReceive = findViewById(R.id.user_message);
        replyMessage = findViewById(R.id.reply_message);
        replyBtn = findViewById(R.id.reply_button);
        replyBtn.setOnClickListener(this);
        send = findViewById(R.id.send_button);
        send.setOnClickListener(this);
        replyMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.length()>0){
                    send.setEnabled(true);
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();

        if (viewId == R.id.reply_button) {
            replyMessage.setVisibility(View.VISIBLE);
            send.setVisibility(View.VISIBLE);
            send.setEnabled(false);
        } else if (viewId == R.id.back_button) {
            finish();
        } else if (viewId == R.id.send_button) {
            // Code to be written
            Toast.makeText(UserChat.this, "Send button clicked", Toast.LENGTH_LONG).show();
        }

//        switch (v.getId()) {
//            case R.id.reply_button:
//                replyMessage.setVisibility(View.VISIBLE);
//                send.setVisibility(View.VISIBLE);
//                send.setEnabled(false);
//                break;
//            case R.id.back_button:
//                finish();
//                break;
//            case R.id.send_button:
//                //code to be written
//                Toast.makeText(UserChat.this,"Send button clicked",Toast.LENGTH_LONG).show();
//                break;
//        }
    }
}
