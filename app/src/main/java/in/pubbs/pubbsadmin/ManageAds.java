package in.pubbs.pubbsadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.TooltipCompat;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.VectorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ManageAds extends AppCompatActivity implements View.OnClickListener {
    private EditText post;
    private TextView title, countTv;
    private ImageView back;
    private String TAG = ManageAds.class.getSimpleName();
    private Button send, color;
    private final int max = 254, min = 0;
    private VectorDrawable bgShape;
    private DatabaseReference databaseReference;
    private SharedPreferences sharedPreferences;
    private int r = 255, g = 255, b = 255;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_ads);
        init();
    }

    private void init() {
        sharedPreferences = getApplication().getSharedPreferences(getResources().getString(R.string.sharedPreferences), MODE_PRIVATE);
        post = findViewById(R.id.ads_text);
        title = findViewById(R.id.toolbar_title);
        title.setText("Manage Ads");
        back = findViewById(R.id.back_button);
        back.setOnClickListener(this);
        send = findViewById(R.id.create_ads);
        send.setOnClickListener(this);
        color = findViewById(R.id.color);
        color.setOnClickListener(this);
        countTv = findViewById(R.id.textinput_counter);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        getSupportActionBar().setTitle("");
        /*GradientDrawable bgShape = (GradientDrawable) color.getBackground();
        bgShape.setColorFilter(Color.rgb(0, 0, 0), PorterDuff.Mode.SRC_ATOP);*/
        bgShape = (VectorDrawable) color.getBackground();
        bgShape.setColorFilter(Color.rgb(0, 0, 0), PorterDuff.Mode.SRC_ATOP);
        post.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                countTv.setVisibility(View.VISIBLE);
                post.setCursorVisible(true);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                countTv.setText(200 - post.getText().length() + "/200");
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s != null && s.toString().length() > 0) {
                    send.setVisibility(View.VISIBLE);
                } else {
                    send.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "Id: " + v.getId());

        if (v.getId() == R.id.back_button) {
            this.onBackPressed();
        }
        else if (v.getId() == R.id.create_ads) {
            //To Code
            //insertData();
            showDialog();
        }
        else if (v.getId() == R.id.color) {
            //To Code
            bgShape.setColorFilter(Color.rgb(255, 255, 255), PorterDuff.Mode.SRC_ATOP);
            post.setTextColor(Color.rgb(255, 255, 255));
            Random random = new Random();
            r = random.nextInt(max - min) + min;
            g = random.nextInt(max - min) + min;
            b = random.nextInt(max - min) + min;
            post.getBackground().setColorFilter(Color.rgb(r, g, b), PorterDuff.Mode.SRC_ATOP);
        }
    }


//    @Override
//    public void onClick(View v) {
//        Log.d(TAG, "Id: " + v.getId());
//        switch (v.getId()) {
//            case R.id.back_button:
//                this.onBackPressed();
//                break;
//            case R.id.create_ads:
//                //To Code
//                //insertData();
//                showDialog();
//                break;
//            case R.id.color:
//                //To Code
//                bgShape.setColorFilter(Color.rgb(255, 255, 255), PorterDuff.Mode.SRC_ATOP);
//                post.setTextColor(Color.rgb(255, 255, 255));
//                Random random = new Random();
//                r = random.nextInt(max - min) + min;
//                g = random.nextInt(max - min) + min;
//                b = random.nextInt(max - min) + min;
//                post.getBackground().setColorFilter(Color.rgb(r, g, b), PorterDuff.Mode.SRC_ATOP);
//                break;
//        }
//    }

    private void insertData(float discount, int validity, String startDate, boolean status) {
        String path = sharedPreferences.getString("organisationName", "no_data").replace(" ", "") + "/DiscountDetails";
        Log.d(TAG, "Path: " + path);
        databaseReference = FirebaseDatabase.getInstance().getReference(path);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                databaseReference.child("DSN_" + dataSnapshot.getChildrenCount()).child("text").setValue(post.getText().toString());
                databaseReference.child("DSN_" + dataSnapshot.getChildrenCount()).child("color").setValue(r + "," + g + "," + b);
                databaseReference.child("DSN_" + dataSnapshot.getChildrenCount()).child("discount").setValue(discount);
                databaseReference.child("DSN_" + dataSnapshot.getChildrenCount()).child("validity").setValue(validity);
                databaseReference.child("DSN_" + dataSnapshot.getChildrenCount()).child("startDate").setValue(startDate);
                databaseReference.child("DSN_" + dataSnapshot.getChildrenCount()).child("active").setValue(status);
                Toast.makeText(ManageAds.this,"Data inserted successfully!!",Toast.LENGTH_LONG).show();
                resetUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onBackPressed() {
        ManageAds.this.finish();
    }

    private void showDialog() {
        Button submit;
        EditText discount, validity, startDate;
        CheckBox active;
        Dialog dialog = new Dialog(ManageAds.this, R.style.WideDialog);
        dialog.setContentView(R.layout.ads_more_info);
        dialog.show();
        submit = dialog.findViewById(R.id.submit);
        discount = dialog.findViewById(R.id.discount_data);
        validity = dialog.findViewById(R.id.validity_data);
        startDate = dialog.findViewById(R.id.date_picker);
        active = dialog.findViewById(R.id.is_active);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String dis, valid, start;
                dis = discount.getText().toString();
                valid = validity.getText().toString();
                start = startDate.getText().toString();
                if (TextUtils.isEmpty(dis)) {
                    discount.setError("Field cannot be left blank.");
                }
                if (TextUtils.isEmpty(valid)) {
                    validity.setError("Field cannot be left blank.");
                }
                if (TextUtils.isEmpty(start)) {
                    startDate.setError("Field cannot be left blank.");
                }
                if (startDate.getText().toString().length() != 10) {
                    startDate.setError("Date entered in wrong format");
                }
                if (!validateDate(start)) {
                    startDate.setError("Date entered in wrong format");
                } else {
                    if (active.isChecked())
                        insertData(Float.valueOf(discount.getText().toString()), Integer.valueOf(validity.getText().toString()), startDate.getText().toString(), true);
                    else
                        insertData(Float.valueOf(discount.getText().toString()), Integer.valueOf(validity.getText().toString()), startDate.getText().toString(), false);
                    dialog.dismiss();
                }
            }
        });

    }

    public boolean validateDate(String date) {
        int dd = Integer.valueOf(date.substring(0, date.indexOf("/")));
        int mm = Integer.valueOf(date.substring(date.indexOf("/") + 1, date.lastIndexOf("/")));
        int yy = Integer.valueOf(date.lastIndexOf("/") + 1);
        if (!(dd > 0 && dd < 31)) {
            return false;
        }
        if (!(mm > 0 && mm < 13)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.ads_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.show_all_ads) {
            startActivity(new Intent(ManageAds.this, ShowAllAds.class));
        }
        else if (item.getItemId() == R.id.remove_ads) {
            // No action specified
        }

//        switch (item.getItemId()) {
//            case R.id.show_all_ads:
//                startActivity(new Intent(ManageAds.this, ShowAllAds.class));
//                break;
//            case R.id.remove_ads:
//                break;
//        }
        return true;
    }
    private void resetUI(){
        post.setText("");
        send.setVisibility(View.INVISIBLE);
        countTv.setVisibility(View.INVISIBLE);
    }
}
