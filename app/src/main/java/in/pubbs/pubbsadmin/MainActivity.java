package in.pubbs.pubbsadmin;



import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.TextView;


import com.google.android.material.navigation.NavigationView;

import java.util.Objects;

import in.pubbs.pubbsadmin.Model.Operator;

/*Created by: Parita Dey*/
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    DrawerLayout drawerLayout;
    NavigationView navigationView;
    String TAG = MainActivity.class.getSimpleName();
    ConstraintLayout container;
    Boolean mLocationPermissionsGranted = false;
    MapFragment mapFragment;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    String mobile, admin_id;
    TextView phone_number, user_name, title;
    Operator operator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        Objects.requireNonNull(getSupportActionBar()).setTitle("");
        title = findViewById(R.id.toolbar_title);
        sharedPreferences = getSharedPreferences(getResources().getString(R.string.sharedPreferences), Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();//code to be deleted
        container = findViewById(R.id.container);
        drawerLayout = findViewById(R.id.drawer);
        navigationView = findViewById(R.id.navigation_view);
        View hView = navigationView.getHeaderView(0);
        phone_number = hView.findViewById(R.id.phone_number);
        user_name = hView.findViewById(R.id.user_name);
        navigationView.setNavigationItemSelectedListener(this);
        ActionBarDrawerToggle actionBarDrawerToggle
                = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open, R.string.close) {
            float scaleFactor = 8f;

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                float slideX = drawerView.getWidth() * slideOffset;
                container.setTranslationX(slideX);
                container.setScaleX(1 - (slideOffset / scaleFactor));
                container.setScaleY(1 - (slideOffset / scaleFactor));
            }
        };
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        drawerLayout.setScrimColor(getResources().getColor(android.R.color.transparent));//Removes the shadow cast by the navigation drawer
        actionBarDrawerToggle.syncState();
        if (sharedPreferences.contains("login")) {
            mobile = sharedPreferences.getString("mobileValue", null);
            admin_id = sharedPreferences.getString("admin_id", null);
            Log.d(TAG, "Mobile number and admin id:" + mobile + "\t" + admin_id);
            phone_number.setText(mobile);
            if (admin_id.contains("PUBBS")) {
                Log.d(TAG, "Admin Id:" + admin_id.contains("PUBBS"));
                title.setText("PUBBS");
                user_name.setText("PUBBS");
                navigationView.getMenu().clear();
                navigationView.inflateMenu(R.menu.drawer_menu); //drawer menu was not inflating so added this line--Parita Dey
            } else if (admin_id.equalsIgnoreCase("Regional Manager")) {
                Log.d(TAG, "Inside Regional Manager");
                Log.d(TAG, "Regional manager's area: " + sharedPreferences.getString("zone_area_id", null));
                title.setText("Regional Manager");
                user_name.setText("Regional Manager");
                navigationView.getMenu().clear();
                navigationView.inflateMenu(R.menu.regional_manager_menu);
                //operator = (Operator) getIntent().getSerializableExtra("OperatorData");
            } else if (admin_id.equalsIgnoreCase("Zone Manager")) {
                Log.d(TAG, "Inside Zone Manager");
                Log.d(TAG, "Zone manager's area: " + sharedPreferences.getString("zone_area_id", null));
                title.setText("Zone Manager");
                user_name.setText("Zone Manager");
                navigationView.getMenu().clear();
                navigationView.inflateMenu(R.menu.zone_manager_menu);
                //operator = (Operator) getIntent().getSerializableExtra("OperatorData");
            } else if (admin_id.equalsIgnoreCase("Area Manager")) {
                Log.d(TAG, "Inside Area Manager");
                title.setText("Area Manager");
                user_name.setText("Area Manager");
                navigationView.getMenu().clear();
                navigationView.inflateMenu(R.menu.area_manager_menu);
                //operator = (Operator) getIntent().getSerializableExtra("OperatorData");
            } else if (admin_id.equalsIgnoreCase("Service Manager")) {
                Log.d(TAG, "Inside Service Manager");
                title.setText("Service Manager");
                user_name.setText("Service Manager");
                navigationView.getMenu().clear();
                navigationView.inflateMenu(R.menu.service_manager_menu);
                //operator = (Operator) getIntent().getSerializableExtra("OperatorData");
            }
        }
        //Permission Check
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                        android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.SEND_SMS}, 101);
        } else {
            mLocationPermissionsGranted = true;
        }
    }

//    @Override
//    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
//        switch (menuItem.getItemId()) {
//            case R.id.manage_operator:
//                startActivity(new Intent(MainActivity.this, ManageOperator.class));
//                break;
//            case R.id.show_inventor:
//                startActivity(new Intent(MainActivity.this, ShowInventory.class));
//                break;
//            case R.id.show_report:
//                // startActivity(new Intent(MainActivity.this, ShowReport.class));
//                break;
//            case R.id.show_profile:
//                startActivity(new Intent(MainActivity.this, Profile.class));
//                break;
//            case R.id.logout:
//                editor.clear();
//                editor.commit();
//                startActivity(new Intent(MainActivity.this, SplashScreen.class));
//                finish();
//                break;
//            case R.id.nav_manage_zone_manger:
//                Intent intent = new Intent(MainActivity.this, ShowZoneManager.class);
//                startActivity(intent);
//                break;
//            case R.id.nav_view_panel:
//                startActivity(new Intent(MainActivity.this, ViewPanel.class));
//                break;
//            case R.id.nav_subscription:
//                startActivity(new Intent(MainActivity.this, ManageSubscription.class));
//                break;
//         /*   case R.id.nav_feedback:
//                break;*/
//            case R.id.nav_profile:
//                startActivity(new Intent(MainActivity.this, Profile.class));
//                break;
//            case R.id.zm_manage_area:
//                startActivity(new Intent(MainActivity.this, ManageArea.class));
//                break;
//            case R.id.zm_manage_bicycle:
//                startActivity(new Intent(MainActivity.this, ManageBicycle.class));
//                break;
//            case R.id.zm_manage_user:
//                startActivity(new Intent(MainActivity.this, ManageUser.class));
//                break;
//            case R.id.zm_admin_employee:
//                startActivity(new Intent(MainActivity.this, ManageEmployee.class));
//                break;
//            case R.id.zm_admin_ads:
//                startActivity(new Intent(MainActivity.this, ManageAds.class));
//                break;
//            case R.id.zm_live:
//                startActivity(new Intent(MainActivity.this, LiveTrackActivity.class));
//                break;
//            case R.id.zm_profile:
//                startActivity(new Intent(MainActivity.this, Profile.class));
//                break;
//            case R.id.am_live_track:
//                startActivity(new Intent(MainActivity.this, LiveTrackActivity.class));
//                break;
//            case R.id.am_manage_cycle:
//                startActivity(new Intent(MainActivity.this, BicycleListActivity.class));
//                break;
//            case R.id.am_manage_ads:
//                startActivity(new Intent(MainActivity.this, ManageAds.class));
//                break;
//           /* case R.id.am_user_support:
//                startActivity(new Intent(MainActivity.this, UserChatActivity.class));
//                break;*/
//            case R.id.am_contact_zone_manager:
//                startActivity(new Intent(MainActivity.this, ContactZoneManagerActivity.class));
//                break;
//            case R.id.am_area_manager_profile:
//                startActivity(new Intent(MainActivity.this, Profile.class));
//                break;
//            case R.id.sm_live_track:
//                startActivity(new Intent(MainActivity.this, LiveTrackActivity.class));
//                break;
//            case R.id.sm_manage_cycle:
//                startActivity(new Intent(MainActivity.this, BicycleListActivity.class));
//                break;
//            case R.id.sm_redistribution:
//                startActivity(new Intent(MainActivity.this, Redistribution.class));
//                break;
//            case R.id.sm_user_support:
//                startActivity(new Intent(MainActivity.this, UserMessage.class));
//                break;
//            case R.id.sm_contact_zone_manager:
//                startActivity(new Intent(MainActivity.this, ContactZoneManagerActivity.class));
//                break;
//            case R.id.sm_show_profile:
//                startActivity(new Intent(MainActivity.this, Profile.class));
//                break;
//            case R.id.nav_inventory:
//                startActivity(new Intent(MainActivity.this, Inventory.class));
//                break;
//        }
//        drawerLayout.closeDrawer(GravityCompat.START);
//        return true;
//    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        int itemId = menuItem.getItemId();  // Get ID once for optimization

        if (itemId == R.id.manage_operator) {
            startActivity(new Intent(MainActivity.this, ManageOperator.class));
        }
        else if (itemId == R.id.show_inventor) {
            startActivity(new Intent(MainActivity.this, ShowInventory.class));
        }
        else if (itemId == R.id.show_report) {
            // startActivity(new Intent(MainActivity.this, ShowReport.class));
        }
        else if (itemId == R.id.show_profile) {
            startActivity(new Intent(MainActivity.this, Profile.class));
        }
        else if (itemId == R.id.logout) {
            editor.clear();
            editor.commit();
            startActivity(new Intent(MainActivity.this, SplashScreen.class));
            finish();
        }
        else if (itemId == R.id.nav_manage_zone_manger) {
            Intent intent = new Intent(MainActivity.this, ShowZoneManager.class);
            startActivity(intent);
        }
        else if (itemId == R.id.nav_view_panel) {
            startActivity(new Intent(MainActivity.this, ViewPanel.class));
        }
        else if (itemId == R.id.nav_subscription) {
            startActivity(new Intent(MainActivity.this, ManageSubscription.class));
        }
        else if (itemId == R.id.nav_profile) {
            startActivity(new Intent(MainActivity.this, Profile.class));
        }
        else if (itemId == R.id.zm_manage_area) {
            startActivity(new Intent(MainActivity.this, ManageArea.class));
        }
        else if (itemId == R.id.zm_manage_bicycle) {
            startActivity(new Intent(MainActivity.this, ManageBicycle.class));
        }
        else if (itemId == R.id.zm_manage_user) {
            startActivity(new Intent(MainActivity.this, ManageUser.class));
        }
        else if (itemId == R.id.zm_admin_employee) {
            startActivity(new Intent(MainActivity.this, ManageEmployee.class));
        }
        else if (itemId == R.id.zm_admin_ads) {
            startActivity(new Intent(MainActivity.this, ManageAds.class));
        }
        else if (itemId == R.id.zm_live) {
            startActivity(new Intent(MainActivity.this, LiveTrackActivity.class));
        }
        else if (itemId == R.id.zm_profile) {
            startActivity(new Intent(MainActivity.this, Profile.class));
        }
        else if (itemId == R.id.am_live_track) {
            startActivity(new Intent(MainActivity.this, LiveTrackActivity.class));
        }
        else if (itemId == R.id.am_manage_cycle) {
            startActivity(new Intent(MainActivity.this, BicycleListActivity.class));
        }
        else if (itemId == R.id.am_manage_ads) {
            startActivity(new Intent(MainActivity.this, ManageAds.class));
        }
        else if (itemId == R.id.am_contact_zone_manager) {
            startActivity(new Intent(MainActivity.this, ContactZoneManagerActivity.class));
        }
        else if (itemId == R.id.am_area_manager_profile) {
            startActivity(new Intent(MainActivity.this, Profile.class));
        }
        else if (itemId == R.id.sm_live_track) {
            startActivity(new Intent(MainActivity.this, LiveTrackActivity.class));
        }
        else if (itemId == R.id.sm_manage_cycle) {
            startActivity(new Intent(MainActivity.this, BicycleListActivity.class));
        }
        else if (itemId == R.id.sm_redistribution) {
            startActivity(new Intent(MainActivity.this, Redistribution.class));
        }
//        else if (itemId == R.id.sm_user_support) {
//            startActivity(new Intent(MainActivity.this, UserMessage.class));
//        }
        else if (itemId == R.id.sm_contact_zone_manager) {
            startActivity(new Intent(MainActivity.this, ContactZoneManagerActivity.class));
        }
        else if (itemId == R.id.sm_show_profile) {
            startActivity(new Intent(MainActivity.this, Profile.class));
        }
        else if (itemId == R.id.nav_inventory) {
            startActivity(new Intent(MainActivity.this, Inventory.class));
        }

        // Close the drawer after selection
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }


    @Override
    protected void onResume() {
        super.onResume();
        //mapView.onResume();
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, 101);

        } else {
            mLocationPermissionsGranted = true;
        }

        Bundle bundle = new Bundle();
        bundle.putBoolean("LocationPermissionsGranted", mLocationPermissionsGranted);
        mapFragment = new MapFragment();
        mapFragment.setArguments(bundle);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_container, mapFragment)
                .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        if (admin_id.equalsIgnoreCase("Area Manager") || admin_id.equalsIgnoreCase("Service Manager")) {
            //menu item will not inflate
        } else if (admin_id.equalsIgnoreCase("PUBBS")) {
            inflater.inflate(R.menu.super_admin_option_menu, menu);
            Typeface type = Typeface.createFromAsset(getAssets(), "fonts/Comfortaa-Regular.ttf");
            for (int i = 0; i < menu.size(); i++) {
                MenuItem menuItem = menu.getItem(i);
                SubMenu subMenu = menuItem.getSubMenu();
                if (subMenu != null && subMenu.size() > 0) {
                    for (int j = 0; j < subMenu.size(); j++) {
                        MenuItem subMenuItem = subMenu.getItem(j);
                        applyFont(subMenuItem, type);
                    }
                }
                applyFont(menuItem, type);
            }
        } else {
            inflater.inflate(R.menu.option_menu, menu);
            Typeface type = Typeface.createFromAsset(getAssets(), "fonts/Comfortaa-Regular.ttf");
            for (int i = 0; i < menu.size(); i++) {
                MenuItem menuItem = menu.getItem(i);
                SubMenu subMenu = menuItem.getSubMenu();
                if (subMenu != null && subMenu.size() > 0) {
                    for (int j = 0; j < subMenu.size(); j++) {
                        MenuItem subMenuItem = subMenu.getItem(j);
                        applyFont(subMenuItem, type);
                    }
                }
                applyFont(menuItem, type);
            }
        }
        return true;
    }

//    @Override
//    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
//        Intent intent;
//        switch (item.getItemId()) {
//            case R.id.change_area:
//                intent = new Intent(MainActivity.this, ShowMyArea.class);
//                startActivity(intent);
//                Log.d(TAG, "Change Operator");
//                return true;
//            case R.id.sa_change_operator:
//                intent = new Intent(MainActivity.this, ShowAllOperators.class);
//                startActivity(intent);
//                Log.d(TAG, "Show Operator");
//                return true;
//            case R.id.tutorial:
//                //Code to be written
//                Log.d(TAG, "Tutorial");
//                startActivity(new Intent(MainActivity.this, Tutorial.class));
//                return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();  // Get the ID once to avoid multiple calls

        if (itemId == R.id.change_area) {
            Intent intent = new Intent(MainActivity.this, ShowMyArea.class);
            startActivity(intent);
            Log.d(TAG, "Change Operator");
            return true;
        }
        else if (itemId == R.id.sa_change_operator) {
            Intent intent = new Intent(MainActivity.this, ShowAllOperators.class);
            startActivity(intent);
            Log.d(TAG, "Show Operator");
            return true;
        }
        else if (itemId == R.id.tutorial) {
            // Code to be written
            Log.d(TAG, "Tutorial");
            startActivity(new Intent(MainActivity.this, Tutorial.class));
            return true;
        }
        else {
            return super.onOptionsItemSelected(item);
        }
    }


    //setting the font size and font style
    private void applyFont(MenuItem menuItem, Typeface font) {
        SpannableString spannableString = new SpannableString(menuItem.getTitle());
        spannableString.setSpan(new CustomTypeFace("", font), 0, spannableString.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        menuItem.setTitle(spannableString);
    }

    private class CustomTypeFace extends TypefaceSpan {
        private final Typeface typeface;

        public CustomTypeFace(String family, Typeface type) {
            super(family);
            typeface = type;
        }

        @Override
        public void updateDrawState(@NonNull TextPaint textPaint) {
            applyCustomTypeFace(textPaint, typeface);
        }

        @Override
        public void updateMeasureState(@NonNull TextPaint textPaint) {
            applyCustomTypeFace(textPaint, typeface);
        }

        private void applyCustomTypeFace(Paint paint, Typeface typeface) {
            int oldStyle;
            Typeface old = paint.getTypeface();
            if (old == null) {
                oldStyle = 0;
            } else {
                oldStyle = old.getStyle();
            }

            int fake = oldStyle & ~typeface.getStyle();
            if ((fake & Typeface.BOLD) != 0) {
                paint.setFakeBoldText(true);
            }

            if ((fake & Typeface.ITALIC) != 0) {
                paint.setTextSkewX(-0.25f);
            }
            paint.setTextSize(36);
            paint.setTypeface(typeface);
        }
    }
}
