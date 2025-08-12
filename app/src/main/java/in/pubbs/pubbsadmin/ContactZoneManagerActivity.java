package in.pubbs.pubbsadmin;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

import in.pubbs.pubbsadmin.View.CustomLoader;

public class ContactZoneManagerActivity extends AppCompatActivity {
    private EditText subject, message;
    private Button send;
    private SharedPreferences sharedPreferences;
    private String TAG = ContactZoneManagerActivity.class.getSimpleName();
    private CustomLoader customLoader;//Loader

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_zone_manager);
        init();
        loadZonalManagerMail();

    }

    private void init() {
        findViewById(R.id.iv_back).setOnClickListener(v -> onBackPressed());
        TextView tv_title = findViewById(R.id.tv_title);
        tv_title.setText("Contact Zone Manager");
        ImageView iv_menu = findViewById(R.id.iv_menu);
        iv_menu.setVisibility(View.GONE);
        subject = findViewById(R.id.subject);
        message = findViewById(R.id.message);
        send = findViewById(R.id.send_button);
        sharedPreferences = getSharedPreferences("pubbs", Context.MODE_PRIVATE);
        customLoader = new CustomLoader(this, R.style.WideDialog);
        customLoader.show();
    }

    private void loadZonalManagerMail() {
        String path = sharedPreferences.getString("organisationName", "no_data").replace(" ", "") + swapData() + sharedPreferences.getString("mobileValue", null);
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(path);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "path: " + Objects.requireNonNull(dataSnapshot.child("createdBy").getValue()).toString());
                DatabaseReference databaseReference1 = FirebaseDatabase.getInstance().getReference(Objects.requireNonNull(dataSnapshot.child("createdBy").getValue()).toString());
                databaseReference1.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        customLoader.dismiss();
                        send.setOnClickListener(v -> {
                            if (!TextUtils.isEmpty(subject.getText()) && !TextUtils.isEmpty(message.getText())) {
                                //Fires the Email Intent
                                try {
                                    final Intent emailIntent = new Intent(Intent.ACTION_SEND);
                                    emailIntent.setType("text/plain");
                                    emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{Objects.requireNonNull(dataSnapshot.child("zoneManagerEmail").getValue()).toString()});
                                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject.getText().toString());
                                    emailIntent.putExtra(Intent.EXTRA_TEXT, message.getText().toString());
                                    emailIntent.setType("message/rfc822");
                                    startActivity(Intent.createChooser(emailIntent,
                                            "Send email using..."));
                                } catch (android.content.ActivityNotFoundException ignored) {
                                    Toast.makeText(ContactZoneManagerActivity.this,"Email cannot be sent.",Toast.LENGTH_SHORT).show();
                                }catch (Exception e){
                                    Toast.makeText(ContactZoneManagerActivity.this,"Email is not valid.",Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private String swapData() {
        if (Objects.requireNonNull(sharedPreferences.getString("admin_id", null)).equals("Area Manager"))
            return "/AM/";
        else if (Objects.requireNonNull(sharedPreferences.getString("admin_id", null)).equals("Service Manager"))
            return "/SM/";
        else return "//";
    }
}
