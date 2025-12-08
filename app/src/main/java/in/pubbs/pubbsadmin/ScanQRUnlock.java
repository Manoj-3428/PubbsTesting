package in.pubbs.pubbsadmin;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.journeyapps.barcodescanner.CompoundBarcodeView;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

public class ScanQRUnlock extends AppCompatActivity implements View.OnClickListener {

    private static final int CAMERA_PERMISSION_REQUEST = 101;
    private static final int REQUEST_ENABLE_BT = 20;

    private CompoundBarcodeView barcodeView;
    private EditText MacAddress;
    private Button connectBtn, openLockBtn, endRideBtn, endRideAuto;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;

    private CustomAnimationDialog cad;
    private static CustomAlertDialog2 reConnectDialog = null;

    private final String TAG = "ScanQRUnlock";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qrunlock);

        barcodeView = findViewById(R.id.compoundBarcodeView);
        MacAddress = findViewById(R.id.tv_barcode);
        connectBtn = findViewById(R.id.connectBtn);
        openLockBtn = findViewById(R.id.openLockBtn);
        endRideBtn = findViewById(R.id.endRideBtn);
        endRideAuto = findViewById(R.id.endRideAuto);

        connectBtn.setOnClickListener(this);
        openLockBtn.setOnClickListener(this);
        endRideBtn.setOnClickListener(this);
        endRideAuto.setOnClickListener(this);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST
            );
        } else {
            startQRScanner();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.connectBtn) {
            connectToLock();
        }
        else if (id == R.id.openLockBtn) {
            sendUnlockCommand();
        }
        else if (id == R.id.endRideBtn) {
            endRide();
        }
        else if (id == R.id.endRideAuto) {
            RideEndAuto();
        }
    }

    private void startQRScanner() {
        barcodeView.decodeContinuous(result -> {
            if (result != null && result.getText() != null) {
                MacAddress.setText(result.getText().trim());
            }
        });
    }

    private void connectToLock() {
        String address = MacAddress.getText().toString().trim();

        if (address.length() >= 14) {
            if (!bluetoothAdapter.isEnabled()) {
                showBluetoothPermission();
            } else {
                startBLEService(address);
            }
        } else {
            Toast.makeText(this, "MAC Address incorrect", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendUnlockCommand() {
        String address = MacAddress.getText().toString().trim();

        if (address.length() >= 14) {
            Intent intent = new Intent(Utility.OPEN_LOCK);
            intent.putExtra("address", address);
            sendBroadcast(intent);
        } else {
            Toast.makeText(this, "MAC Address incorrect", Toast.LENGTH_SHORT).show();
        }
    }

    private void RideEndAuto() {
        String address = MacAddress.getText().toString().trim();

        if (address.length() >= 14) {
            Intent intent = new Intent(Utility.END_RIDE_AUTO);
            intent.putExtra("address", address);
            sendBroadcast(intent);
        } else {
            Toast.makeText(this, "MAC Address incorrect", Toast.LENGTH_SHORT).show();
        }
    }

    private void endRide() {
        String address = MacAddress.getText().toString().trim();
        if (address.length() >= 14) {
            sendBroadcast(new Intent(Utility.RIDE_ENDED));
        } else {
            Toast.makeText(this, "MAC Address incorrect", Toast.LENGTH_SHORT).show();
        }
    }

    // -------------------------------------------------------------
    //  MAC → BICYCLE ID FETCH
    // -------------------------------------------------------------

    private void getDeviceIdFromMac(String mac, OnDeviceIdFound callback) {

        SharedPreferences sp = getSharedPreferences("pubbs", MODE_PRIVATE);
        String org = sp.getString("organisationName", "").replaceAll(" ", "");

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference(org)
                .child("Bicycle");

        ref.get().addOnSuccessListener(snapshot -> {

            for (DataSnapshot child : snapshot.getChildren()) {

                String bleMac = String.valueOf(child.child("bleaddress").getValue());

                if (bleMac.equalsIgnoreCase(mac)) {
                    callback.onFound(child.getKey());
                    return;
                }
            }
            callback.onFound(null);
        });
    }

    interface OnDeviceIdFound {
        void onFound(String deviceId);
    }

    // -------------------------------------------------------------
    // BLE SERVICE START
    // -------------------------------------------------------------

    private void startBLEService(String address) {
        try {
            stopService(new Intent(this, PubbsService.class));

            cad = new CustomAnimationDialog(this, R.style.WideDialog);
            cad.show();
            cad.setAnimation(R.raw.bluetooth);
            cad.setTitle("Connecting...");
            cad.playAnimation();

            Intent pubbsService = new Intent(this, PubbsService.class);
            pubbsService.putExtra("address", address);
            startService(pubbsService);

        } catch (Exception e) {
            Log.e(TAG, "startBLEService exception: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------
    // BLUETOOTH PERMISSION DIALOG
    // -------------------------------------------------------------

    public void showBluetoothPermission() {

        AlertDialog dialogBuilder = new AlertDialog.Builder(this).create();
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.custom_bluettooth_permission, null);

        Button turn_on = dialogView.findViewById(R.id.turn_on);
        turn_on.setOnClickListener(v -> {
            dialogBuilder.dismiss();

            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        });

        TextView cancel = dialogView.findViewById(R.id.cancel);
        cancel.setOnClickListener(view -> {
            dialogBuilder.dismiss();
            finish();
        });

        dialogBuilder.setView(dialogView);
        dialogBuilder.show();
    }

    // -------------------------------------------------------------
    // BROADCAST RECEIVER
    // -------------------------------------------------------------

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if (action == null) return;

            switch (action) {

                case Utility.GATT_CONNECTED:
                    if (cad != null) cad.setTitle("Connected!");
                    openLockBtn.setEnabled(true);
                    endRideBtn.setEnabled(true);
                    new Handler().postDelayed(() ->
                            sendBroadcast(new Intent(Utility.REQUEST_KEY)), 1000);
                    break;

                case Utility.GATT_DISCONNECTED:
                case Utility.INVALID_BLUETOOTH:
                    if (cad != null && cad.isShowing()) cad.dismiss();
                    openLockBtn.setEnabled(false);
                    endRideBtn.setEnabled(false);
                    break;

                case Utility.KEY_RECEIVED:
                    if (cad != null) cad.setTitle("Checking lock...");
                    sendBroadcast(new Intent(Utility.CHECKING_LOCK_STATUS));
                    break;

                case Utility.LOCK_OPENED:
                    Log.e("SCANQR", "🔥🔥 LOCK_OPENED RECEIVED IN ACTIVITY");

                    if (cad != null && cad.isShowing()) cad.dismiss();

                    // ✅ Always run toast on UI thread with Activity context
                    runOnUiThread(() ->
                            Toast.makeText(ScanQRUnlock.this, "Lock opened!", Toast.LENGTH_SHORT).show()
                    );

                    // ✅ Correctly call the function
                    decrementStationCycleCount();
                    break;

                case Utility.RIDE_ENDED:
                case Utility.LOCKED:
                case Utility.LOCK_ALREADY_OPENED:
                    if (cad != null && cad.isShowing()) cad.dismiss();
                    break;
            }
        }
    };

    // -------------------------------------------------------------
    //  DECREMENT STATION CYCLE COUNT (FINAL WORKING)
    // -------------------------------------------------------------

    private void decrementStationCycleCount() {
        Log.d("SCANCYCLE", "decrementStationCycleCount() CALLED");

        SharedPreferences sharedPreferences = getSharedPreferences("pubbs", MODE_PRIVATE);
        String operator = sharedPreferences.getString("organisationName", "").replaceAll(" ", "");
        String macAddress = MacAddress.getText().toString().trim();

        Log.d("SCANCYCLE", "Operator: " + operator + " | MAC RAW: " + macAddress);

        // FIX: Remove colons from MAC address
        String cleanMac = macAddress.replace(":", "");
        Log.d("SCANCYCLE", "Clean MAC: " + cleanMac);

        DatabaseReference bikeRef = FirebaseDatabase.getInstance().getReference()
                .child(operator)
                .child("Bicycle")
                .child(cleanMac);

        bikeRef.get().addOnSuccessListener(bikeSnap -> {

            if (!bikeSnap.exists()) {
                Log.e("SCANCYCLE", "❌ Bicycle node NOT FOUND for key: " + cleanMac);
                return;
            }

            String stationId = bikeSnap.child("inStationId").getValue(String.class);

            if (stationId == null) {
                Log.e("SCANCYCLE", "❌ inStationId not found in bicycle data!");
                Log.e("SCANCYCLE", "🔥 FULL bicycle DATA = " + bikeSnap.getValue());
                return;
            }

            Log.d("SCANCYCLE", "Station ID FOUND: " + stationId);

            DatabaseReference stationRef = FirebaseDatabase.getInstance().getReference()
                    .child(operator)
                    .child("Station")
                    .child(stationId)
                    .child("stationCycleCount");

            stationRef.get().addOnSuccessListener(stationSnap -> {
                if (!stationSnap.exists()) {
                    Log.e("SCANCYCLE", "❌ stationCycleCount NOT FOUND in Station node");
                    return;
                }

                int count = Integer.parseInt(stationSnap.getValue().toString());
                int newValue = Math.max(0, count - 1);

                stationRef.setValue(newValue)
                        .addOnSuccessListener(a -> {
                            Log.d("SCANCYCLE", "✅ stationCycleCount UPDATED: " + count + " → " + newValue);
                            Toast.makeText(this, "Station count updated!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e ->
                                Log.e("SCANCYCLE", "❌ Failed updating station count", e));
            });
        });
    }



    // -------------------------------------------------------------
    // LIFECYCLE
    // -------------------------------------------------------------

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        barcodeView.resume();
        registerReceiver(broadcastReceiver, Utility.mapIntentFilter());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { if (bluetoothSocket != null) bluetoothSocket.close(); }
        catch (IOException ignored) {}
    }
}
