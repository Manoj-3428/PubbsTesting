package in.pubbs.pubbsadmin;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;

import in.pubbs.pubbsadmin.Model.Bicycle;
import in.pubbs.pubbsadmin.Model.RepairBicycle;

public class AddOrRemoveBicycle extends AppCompatActivity implements View.OnClickListener {

    SurfaceView surfaceView;
    TextView barcodeText, title;
    BarcodeDetector detector;
    CameraSource cameraSource;
    String code, status, dateTime, adminId, mobile;
    SparseArray<Barcode> sparseArray;
    ImageView back, flash;
    String TAG = AddOrRemoveBicycle.class.getSimpleName();
    private Camera camera = null;
    boolean flashmode = false;
    DatabaseReference databaseReference, dbCheckBicycle, dbRemoveBicycle;
    SharedPreferences sharedPreferences;
    String stationId, stationName, areaId;
    Button addBicycleBtn;
    FusedLocationProviderClient mFusedLocationProviderClient;
    Boolean mLocationPermissionsGranted = false;
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final int MY_CAMERA_REQUEST_CODE = 100;
    double latitude, longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_bicycle);
        status = getIntent().getStringExtra("Status");
        getLocationPermission();
        init();
        getDeviceLocation();
    }

    private void init() {
        sharedPreferences = getSharedPreferences(getResources().getString(R.string.sharedPreferences), Context.MODE_PRIVATE);
        stationId = getIntent().getStringExtra("StationId") == null ? "" : getIntent().getStringExtra("StationId");
        stationName = getIntent().getStringExtra("StationName") == null ? "" : getIntent().getStringExtra("StationName");
        areaId = getIntent().getStringExtra("AreaId") == null ? "" : getIntent().getStringExtra("AreaId");
        surfaceView = findViewById(R.id.surface_view);
        barcodeText = findViewById(R.id.tv_barcode);
        title = findViewById(R.id.toolbar_title);
        title.setText(status.equals("ADD") ? "Add Bicycle" : "Repair Bicycle");
        back = findViewById(R.id.back_button);
        back.setOnClickListener(this);
        flash = findViewById(R.id.flash_button);
        flash.setVisibility(View.VISIBLE);
        flash.setOnClickListener(this);
        addBicycleBtn = findViewById(R.id.add_bicycle);
        if (status.equals("ADD")) {
            addBicycleBtn.setText("Add Bicycle");
        } else {
            addBicycleBtn.setText("Repair Bicycle");
        }
        addBicycleBtn.setOnClickListener(this);

        detector = new BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.QR_CODE).build();

        cameraSource = new CameraSource.Builder(this, detector)
                .setRequestedPreviewSize(1024, 768)
                .setRequestedFps(25f)
                .setAutoFocusEnabled(true)
                .build();

        detector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() { }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                sparseArray = detections.getDetectedItems();
                if (sparseArray.size() > 0) {
                    Log.d(TAG, "data: " + sparseArray.valueAt(0).displayValue);
                    setCode(sparseArray.valueAt(0).displayValue);
                }
            }
        });
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback2() {
            @Override public void surfaceRedrawNeeded(SurfaceHolder holder) { }
            @Override @RequiresApi(api = Build.VERSION_CODES.M)
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_REQUEST_CODE);
                    }
                    cameraSource.start(holder);
                } catch (Exception ignored) { }
            }
            @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { }
            @Override public void surfaceDestroyed(SurfaceHolder holder) { cameraSource.stop(); }
        });
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.back_button) {
            finish();
        }
        else if (v.getId() == R.id.flash_button) {
            if (!flashmode) {
                flash.setImageResource(R.drawable.ic_flash_off_white);
                flashOnButton();
            }
            else {
                flash.setImageResource(R.drawable.ic_flash_on_white);
                flashOnButton();
            }
        }
        else if (v.getId() == R.id.add_bicycle) {
            // Support both scanned value and manual entry in the EditText
            String entered = barcodeText.getText() == null ? "" : barcodeText.getText().toString().trim();
            if (!entered.isEmpty() && !entered.equalsIgnoreCase("Enter code manually")) {
                code = entered;
                if (status.equals("ADD")) {
                    Bicycle bicycle = new Bicycle(code, code.replaceAll(":", ""), areaId, stationName, stationId, "active", "100", "0", "0");
                    showDialog("Lock Type", "Please enter the type of lock that is been used");
                }
                else {
                    checkBicycle(code.replaceAll(":", ""));
                }
            }
            else {
                barcodeText.setError("Scan was not properly done");
            }
        }
    }

    private void setCode(String code) {
        this.code = code;
        barcodeText.setText(code);
    }

    private static Camera getCamera(@NonNull CameraSource cameraSource) {
        Field[] declaredFields = CameraSource.class.getDeclaredFields();
        for (Field field : declaredFields) {
            if (field.getType() == Camera.class) {
                field.setAccessible(true);
                try {
                    return (Camera) field.get(cameraSource);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        return null;
    }

    private void flashOnButton() {
        camera = getCamera(cameraSource);
        if (camera != null) {
            try {
                Camera.Parameters param = camera.getParameters();
                param.setFlashMode(!flashmode ? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF);
                camera.setParameters(param);
                flashmode = !flashmode;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void getLocationPermission() {
        Log.d(TAG, "getLocationPermission: getting location permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(), COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionsGranted = true;
            } else {
                ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mLocationPermissionsGranted = false;
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0) {
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        mLocationPermissionsGranted = false;
                        return;
                    }
                }
                mLocationPermissionsGranted = true;
            }
        }
    }

    private void getDeviceLocation() {
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
        try {
            if (mLocationPermissionsGranted) {
                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Location currentLocation = (Location) task.getResult();
                        LatLng coordinates = new LatLng(Objects.requireNonNull(currentLocation).getLatitude(), currentLocation.getLongitude());
                        latitude = coordinates.latitude;
                        longitude = coordinates.longitude;
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage());
        }
    }

    private void checkBicycle(String code) {
        dbRemoveBicycle = FirebaseDatabase.getInstance().getReference(Objects.requireNonNull(sharedPreferences.getString("organisationName", "no_data")).replaceAll(" ", "") + "/Bicycle");
        dbCheckBicycle = FirebaseDatabase.getInstance().getReference(Objects.requireNonNull(sharedPreferences.getString("organisationName", "no_data")).replaceAll(" ", "") + "/Bicycle");
        dbCheckBicycle.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot i : dataSnapshot.getChildren()) {
                    if (Objects.requireNonNull(i.getKey()).equals(code)) {
                        adminId = sharedPreferences.getString("admin_id", "null");
                        mobile = sharedPreferences.getString("mobileValue", "null");
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                        dateTime = sdf.format(new Date());
                        RepairBicycle repairBicycle = new RepairBicycle(code, adminId, mobile, code.replaceAll(":", ""), latitude, longitude, "active", "10", dateTime);
                        databaseReference = FirebaseDatabase.getInstance().getReference();
                        databaseReference.child(Objects.requireNonNull(sharedPreferences.getString("organisationName", null)).replaceAll(" ", ""))
                                .child("RepairBicycle").child(code.replaceAll(":", "")).setValue(repairBicycle);
                        dbRemoveBicycle.child(code.replaceAll(":", "")).removeValue();
                        finish();
                    } else {
                        Toast.makeText(getApplicationContext(), "Please check the Bicycle lock properly", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    private void showDialog(String heading, String details) {
        TextView title, message;
        EditText data;
        ImageButton positiveButton, negativeButton;
        final Dialog dialog = new Dialog(AddOrRemoveBicycle.this, R.style.WideDialog);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setContentView(R.layout.custom_dialog_edittext);
        dialog.setCancelable(true);
        title = dialog.findViewById(R.id.title);
        title.setText(heading);
        message = dialog.findViewById(R.id.content);
        message.setText(details);
        data = dialog.findViewById(R.id.data);
        positiveButton = dialog.findViewById(R.id.positive);
        positiveButton.setOnClickListener(v -> {
            Bicycle bicycle = new Bicycle(code, code.replaceAll(":", ""), areaId, stationName, stationId, "active","100","0","0");
            if (!TextUtils.isEmpty(data.getText().toString())) {
                bicycle.setType(data.getText().toString().trim());
                databaseReference = FirebaseDatabase.getInstance().getReference();
                databaseReference
                        .child(Objects.requireNonNull(sharedPreferences.getString("organisationName", null)).replaceAll(" ", ""))
                        .child("Bicycle")
                        .child(code.replaceAll(":", ""))
                        .setValue(bicycle).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                dialog.dismiss();
                                if (!task.isSuccessful()) {
                                    Toast.makeText(AddOrRemoveBicycle.this, "Failed to add bicycle", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                // After creating/updating bicycle, ensure it's added into Station/{stationId}/cyclesList
                                String bicycleId = code.replaceAll(":", "");
                                ensureInStationCyclesListWithConfirmation(bicycleId);
                            }
                        });
            } else {
                Toast.makeText(AddOrRemoveBicycle.this, "Please do not enter blank field", Toast.LENGTH_SHORT).show();
            }
        });
        negativeButton = dialog.findViewById(R.id.negetive);
        negativeButton.setOnClickListener(v -> {
            dialog.dismiss();
            AddOrRemoveBicycle.this.finish();
        });
        dialog.show();
    }

    /**
     * Ensures the given bicycle exists under current Station/{stationId}/cyclesList.
     * If it already exists under another station cyclesList, asks for confirmation to move it.
     */
    private void ensureInStationCyclesListWithConfirmation(@NonNull String bicycleId) {
        String orgName = Objects.requireNonNull(sharedPreferences.getString("organisationName", "no_data")).replaceAll(" ", "");
        if (stationId == null || stationId.trim().isEmpty()) {
            Toast.makeText(this, "Please select a station first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Fetch battery percentage to store in cyclesList
        DatabaseReference bikeRef = FirebaseDatabase.getInstance().getReference(orgName + "/Bicycle").child(bicycleId);
        bikeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot bikeSnap) {
                int percentage = getBatteryPercentage(bikeSnap);

                DatabaseReference stationRootRef = FirebaseDatabase.getInstance().getReference(orgName + "/Station");
                stationRootRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot stationSnap) {
                        String foundStationKey = null;
                        String foundStationName = "";

                        for (DataSnapshot stationChild : stationSnap.getChildren()) {
                            DataSnapshot cyclesListSnap = stationChild.child("cyclesList");
                            if (cyclesListSnap.exists() && cyclesListSnap.child(bicycleId).exists()) {
                                foundStationKey = stationChild.getKey();
                                Object snameObj = stationChild.child("stationName").getValue();
                                foundStationName = snameObj != null ? snameObj.toString() : "";
                                break;
                            }
                        }

                        if (foundStationKey != null && !foundStationKey.isEmpty()) {
                            if (foundStationKey.equals(stationId)) {
                                Toast.makeText(AddOrRemoveBicycle.this, "Bicycle already exists in this station", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            String oldIdFinal = foundStationKey;
                            String oldNameFinal = foundStationName;
                            new AlertDialog.Builder(AddOrRemoveBicycle.this)
                                    .setTitle("Cycle already assigned")
                                    .setMessage("This cycle is already in station \"" + oldNameFinal + "\" (" + oldIdFinal + ").\n\nMove it to \"" + stationName + "\" (" + stationId + ")?")
                                    .setPositiveButton("Yes, move", (d, which) ->
                                            upsertCyclesListAndCounts(orgName, bicycleId, percentage, oldIdFinal))
                                    .setNegativeButton("No", (d, which) -> d.dismiss())
                                    .setCancelable(true)
                                    .show();
                        } else {
                            upsertCyclesListAndCounts(orgName, bicycleId, percentage, null);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(AddOrRemoveBicycle.this, "Failed to check station cycle lists", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AddOrRemoveBicycle.this, "Failed to load bicycle data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int getBatteryPercentage(@NonNull DataSnapshot bikeSnap) {
        // Prefer cyclebattery if present; otherwise fallback to legacy battery; default 100.
        int percentage = 100;
        Object cyclebatteryObj = bikeSnap.child("cyclebattery").getValue();
        if (cyclebatteryObj != null) {
            try {
                double p = Double.parseDouble(cyclebatteryObj.toString());
                return (int) Math.round(Math.max(0, Math.min(100, p)));
            } catch (NumberFormatException ignored) { }
        }
        Object batteryObj = bikeSnap.child("battery").getValue();
        if (batteryObj != null) {
            try {
                double p = Double.parseDouble(batteryObj.toString());
                percentage = (int) Math.round(Math.max(0, Math.min(100, p)));
            } catch (NumberFormatException ignored) { }
        }
        return percentage;
    }

    /**
     * Multi-location update:
     * - optionally remove from old station cyclesList
     * - add into current station cyclesList with {id, percentage}
     * - set bicycle assignment fields (lowercase) and cyclebattery
     * - update stationCycleCount (decrement old / increment new)
     */
    private void upsertCyclesListAndCounts(@NonNull String orgName,
                                          @NonNull String bicycleId,
                                          int percentage,
                                          @Nullable String oldStationId) {
        DatabaseReference orgRef = FirebaseDatabase.getInstance().getReference(orgName);

        HashMap<String, Object> updates = new HashMap<>();
        if (oldStationId != null && !oldStationId.isEmpty()) {
            updates.put("Station/" + oldStationId + "/cyclesList/" + bicycleId, null);
        }
        updates.put("Station/" + stationId + "/cyclesList/" + bicycleId + "/id", bicycleId);
        updates.put("Station/" + stationId + "/cyclesList/" + bicycleId + "/percentage", percentage);

        // Keep these consistent for other screens that use lowercase keys
        updates.put("Bicycle/" + bicycleId + "/inStationId", stationId);
        updates.put("Bicycle/" + bicycleId + "/inStationName", stationName == null ? "" : stationName);
        updates.put("Bicycle/" + bicycleId + "/inAreaId", areaId == null ? "" : areaId);
        updates.put("Bicycle/" + bicycleId + "/cyclebattery", percentage);

        orgRef.updateChildren(updates).addOnCompleteListener(t -> {
            if (!t.isSuccessful()) {
                Toast.makeText(AddOrRemoveBicycle.this, "Failed to update station cycles list", Toast.LENGTH_SHORT).show();
                return;
            }

            // Update stationCycleCount (transactions)
            if (oldStationId != null && !oldStationId.isEmpty()) {
                updateStationCycleCount(orgRef.child("Station").child(oldStationId).child("stationCycleCount"), -1, () ->
                        updateStationCycleCount(orgRef.child("Station").child(stationId).child("stationCycleCount"), +1, () -> {
                            Toast.makeText(AddOrRemoveBicycle.this, "Bicycle moved to station", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                );
            } else {
                updateStationCycleCount(orgRef.child("Station").child(stationId).child("stationCycleCount"), +1, () -> {
                    Toast.makeText(AddOrRemoveBicycle.this, "Bicycle added successfully", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void updateStationCycleCount(@NonNull DatabaseReference countRef, int delta, @NonNull Runnable onDone) {
        countRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Integer current = currentData.getValue(Integer.class);
                if (current == null) current = 0;
                int next = current + delta;
                if (next < 0) next = 0;
                currentData.setValue(next);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                onDone.run();
            }
        });
    }
}
