package in.pubbs.pubbsadmin;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.widget.Toast;

import java.util.Objects;

import in.pubbs.pubbsadmin.View.CustomAlertDialog;

/*Created by: Parita Dey*/
public class SplashScreen extends AppCompatActivity {
    SharedPreferences sharedPreferences;
    boolean internet, gps, location;
    Context context;
    private String TAG = SplashScreen.class.getSimpleName();
    LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        sharedPreferences = getSharedPreferences(getResources().getString(R.string.sharedPreferences), Context.MODE_PRIVATE);
        internet = isConnectingToInternet(context);
        location = isLocationOn();
        if (internet && location) {
            //if sharedpreference stores the user's mobile number, password then will directly go to the MainActivity class
            //otherwise hit Login class
            if (sharedPreferences.contains("login")) {
                goDashboard();
            } else {
                goLogin();
            }
        } else {
            if (internet != true) {
                alertDialog("Network Message", "Please connect to the internet");
            } else if (location != true) {
                alertLocationDialog("Location Error", "Please enable GPS location");
            } else {
                alertDialog("Pubbs Error", "Please check internet and GPS connection");
            }
        }
    }

    private void goLogin() {
        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashScreen.this, Login.class));
            finish();
        }, 2000);
    }

    private void goDashboard() {
        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashScreen.this, MainActivity.class));
            finish();
        }, 2000);
    }

    //checking if the app is connected with internet
    private boolean isConnectingToInternet(Context applicationContext) {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = Objects.requireNonNull(cm).getActiveNetworkInfo();
        if (ni == null) {
            Toast.makeText(getApplicationContext(), "Please connect to the internet", Toast.LENGTH_SHORT).show();
            return false;
        } else
            return true;
    }

    private void alertDialog(String title, String message) {
        final CustomAlertDialog dialog = new CustomAlertDialog(this,
                R.style.WideDialog, title, message);
        dialog.show();
        dialog.onPositiveButton(view -> {
            dialog.dismiss();
            finish();
        });
    }

    private void alertLocationDialog(String title, String message) {
        final CustomAlertDialog dialog = new CustomAlertDialog(this,
                R.style.WideDialog, title, message);
        dialog.show();
        dialog.onPositiveButton(view -> {
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            dialog.dismiss();
            if (sharedPreferences.contains("login")) {
                goDashboard();
            } else {
                goLogin();
            }
        });
    }

    //check whether android device has GPS on or not, otherwise turn on the GPS
    private boolean isLocationOn() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (Objects.requireNonNull(locationManager).isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            gps = true;
            return true;
        } else {
            gps = false;
            return false;
        }
    }
}
