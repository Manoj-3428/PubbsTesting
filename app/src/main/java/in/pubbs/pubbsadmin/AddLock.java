package in.pubbs.pubbsadmin;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;

import in.pubbs.pubbsadmin.Model.Lock;
import in.pubbs.pubbsadmin.View.CustomAlertDialog;
import in.pubbs.pubbsadmin.View.CustomAnimationDialog;

public class AddLock extends AppCompatActivity implements View.OnClickListener {

    private TextView title;
    private ImageView back;
    RadioGroup optionGrp;
    RadioButton optionAtble, optionAtbleGsm, optionNrble, optionNrbleGsm, optionQtble, optionQtbleGsm, optionQtgsm, optionNrbleMsh, optionNrnbIot;
    EditText lockId, bleAddress, simNumber;
    Button addLockButton;
    ConstraintLayout container;
    String TAG = AddLock.class.getSimpleName();
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference, atBleReference, atBleGsmReference, nrBleReference, nrBleGsmReference, qtBleReference, qtBleGsmReference, qtGsmReference, nrBleMshReference, nrNbIotReference;
    RadioButton radioPressed;
    Lock lock;
    SharedPreferences sharedPreferences;
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private static final int REQUEST_ENABLE_BT = 21;
    CustomAnimationDialog cad;
    String scanResult;
    private static String batteryData = "";
    TextView battery_data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_lock);
        Intent intent = getIntent();
        scanResult = intent.getStringExtra("QrCode");
        Log.d(TAG, "Scan result from QR activity: " + scanResult);
        init();
    }

    private void init() {
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();
        atBleReference = firebaseDatabase.getReference("SuperAdmin/9433944708/Inventory/" + getResources().getString(R.string.at_ble));
        atBleGsmReference = firebaseDatabase.getReference("SuperAdmin/9433944708/Inventory/" + getResources().getString(R.string.at_ble_gsm));
        nrBleReference = firebaseDatabase.getReference("SuperAdmin/9433944708/Inventory/" + getResources().getString(R.string.nr_ble));
        nrBleGsmReference = firebaseDatabase.getReference("SuperAdmin/9433944708/Inventory/" + getResources().getString(R.string.nr_ble_gsm));
        qtBleReference = firebaseDatabase.getReference("SuperAdmin/9433944708/Inventory/" + getResources().getString(R.string.qt_ble));
        qtBleGsmReference = firebaseDatabase.getReference("SuperAdmin/9433944708/Inventory/" + getResources().getString(R.string.qt_ble_gsm));
        qtGsmReference = firebaseDatabase.getReference("SuperAdmin/9433944708/Inventory/" + getResources().getString(R.string.qt_gsm));
        nrBleMshReference = firebaseDatabase.getReference("SuperAdmin/9433944708/Inventory/" + getResources().getString(R.string.nr_ble_msh));
        nrNbIotReference = firebaseDatabase.getReference("SuperAdmin/9433944708/Inventory/" + getResources().getString(R.string.nr_nb_iot));

        sharedPreferences = getSharedPreferences(getResources().getString(R.string.sharedPreferences), Context.MODE_PRIVATE);

        title = findViewById(R.id.toolbar_title);
        title.setText("Add Lock");
        back = findViewById(R.id.back_button);
        back.setOnClickListener(this);
        optionAtble = findViewById(R.id.option_at_ble);
        optionAtble.setOnClickListener(this);
        optionAtbleGsm = findViewById(R.id.option_at_ble_gsm);
        optionAtbleGsm.setOnClickListener(this);
        optionNrble = findViewById(R.id.option_nr_ble);
        optionNrble.setOnClickListener(this);
        optionNrbleGsm = findViewById(R.id.option_nr_ble_gsm);
        optionNrbleGsm.setOnClickListener(this);
        optionQtble = findViewById(R.id.option_qt_ble);
        optionQtble.setOnClickListener(this);
        optionQtbleGsm = findViewById(R.id.option_qt_ble_gsm);
        optionQtbleGsm.setOnClickListener(this);
        optionQtgsm = findViewById(R.id.option_qt_gsm);
        optionQtgsm.setOnClickListener(this);
        optionNrbleMsh = findViewById(R.id.option_nr_ble_msh);
        optionNrbleMsh.setOnClickListener(this);
        optionNrbleMsh.setEnabled(false);
        optionNrbleMsh.setTextColor(getResources().getColor(R.color.grey_400));
        optionNrnbIot = findViewById(R.id.option_nr_nb_iot);
        optionNrnbIot.setOnClickListener(this);
        optionNrnbIot.setEnabled(false);
        optionNrnbIot.setTextColor(getResources().getColor(R.color.grey_400));
        lockId = findViewById(R.id.lock_id);
        bleAddress = findViewById(R.id.ble_address);
        simNumber = findViewById(R.id.sim_number);
        battery_data = findViewById(R.id.battery_data);
        addLockButton = findViewById(R.id.add_lock);
        addLockButton.setOnClickListener(this);
        optionGrp = findViewById(R.id.option_group);
        container = findViewById(R.id.container);
    }

    @Override
    public void onClick(View v) {
//        switch (v.getId()) {
//            case R.id.back_button:
//                finish();
//                break;
//            case R.id.lock_inventory:
//                startActivity(new Intent(this, AddLock.class));
//                break;
//            case R.id.option_at_ble:
//            case R.id.option_nr_ble:
//            case R.id.option_qt_ble:
//                lockId.setVisibility(View.VISIBLE);
//                bleAddress.setVisibility(View.VISIBLE);
//                simNumber.setVisibility(View.GONE);
//                bleAddress.setText(scanResult);
//                simNumber.setText("NULL");
//                battery_data.setVisibility(View.VISIBLE);
//                battery_data.setText("Battery percentage: " + batteryData);
//                break;
//            case R.id.option_at_ble_gsm:
//            case R.id.option_nr_ble_gsm:
//            case R.id.option_qt_ble_gsm:
//                lockId.setVisibility(View.VISIBLE);
//                bleAddress.setVisibility(View.VISIBLE);
//                simNumber.setVisibility(View.VISIBLE);
//                simNumber.setText("");
//                bleAddress.setText(scanResult);
//                battery_data.setVisibility(View.VISIBLE);
//                battery_data.setText("Battery percentage: " + batteryData);
//                break;
//            case R.id.option_qt_gsm:
//                lockId.setVisibility(View.VISIBLE);
//                bleAddress.setVisibility(View.GONE);
//                simNumber.setVisibility(View.VISIBLE);
//                simNumber.setText("");
//                bleAddress.setText("NULL");
//                battery_data.setVisibility(View.VISIBLE);
//                battery_data.setText("Battery percentage: " + batteryData);
//                break;
//            case R.id.add_lock:
//                if (conditionCheck()) {
//                    addLock(radioPressed.getText().toString());
//                }
//                break;
//            case R.id.option_nr_ble_msh:
//                break;
//            case R.id.option_nr_nb_iot:
//                break;
//        }
        if (v.getId() == R.id.back_button) {
            finish();
        }
        else if (v.getId() == R.id.lock_inventory) {
            startActivity(new Intent(this, AddLock.class));
        }
        else if (v.getId() == R.id.option_at_ble || v.getId() == R.id.option_nr_ble || v.getId() == R.id.option_qt_ble) {
            lockId.setVisibility(View.VISIBLE);
            bleAddress.setVisibility(View.VISIBLE);
            simNumber.setVisibility(View.GONE);
            bleAddress.setText(scanResult);
            simNumber.setText("NULL");
            battery_data.setVisibility(View.VISIBLE);
            battery_data.setText("Battery percentage: " + batteryData);
        }
        else if (v.getId() == R.id.option_at_ble_gsm || v.getId() == R.id.option_nr_ble_gsm || v.getId() == R.id.option_qt_ble_gsm) {
            lockId.setVisibility(View.VISIBLE);
            bleAddress.setVisibility(View.VISIBLE);
            simNumber.setVisibility(View.VISIBLE);
            simNumber.setText("");
            bleAddress.setText(scanResult);
            battery_data.setVisibility(View.VISIBLE);
            battery_data.setText("Battery percentage: " + batteryData);
        }
        else if (v.getId() == R.id.option_qt_gsm) {
            lockId.setVisibility(View.VISIBLE);
            bleAddress.setVisibility(View.GONE);
            simNumber.setVisibility(View.VISIBLE);
            simNumber.setText("");
            bleAddress.setText("NULL");
            battery_data.setVisibility(View.VISIBLE);
            battery_data.setText("Battery percentage: " + batteryData);
        }
        else if (v.getId() == R.id.add_lock) {
            if (conditionCheck()) {
                addLock(radioPressed.getText().toString());
            }
        }
        else if (v.getId() == R.id.option_nr_ble_msh) {
            // Do nothing (empty block)
        }
        else if (v.getId() == R.id.option_nr_nb_iot) {
            // Do nothing (empty block)
        }
        else {
            // Optional: Handle unknown view clicks if needed
        }


    }

    private void addLock(String lockType) {
        String title = "Successfully added lock to the inventory",
                message = "Lock ID: " + lockId.getText().toString() + "\n\nBLE Address: " + bleAddress.getText().toString() +
                        "\n\nSIM Number: " + simNumber.getText().toString();
        lock = new Lock(createLockId(bleAddress.getText().toString(), lockType), simNumber.getText().toString(), bleAddress.getText().toString(), batteryData,
                sharedPreferences.getString("mobileValue", null));
        if (lockType.equals(getResources().getString(R.string.at_ble))) {
            atBleReference.child(lock.getLockId()).setValue(lock);
            displayDialogMessage(title, message);
        } else if (lockType.equals(getResources().getString(R.string.at_ble_gsm))) {
            atBleGsmReference.child(lock.getLockId()).setValue(lock);
            displayDialogMessage(title, message);
        } else if (lockType.equals(getResources().getString(R.string.nr_ble))) {
            nrBleReference.child(lock.getLockId()).setValue(lock);
            displayDialogMessage(title, message);
        } else if (lockType.equals(getResources().getString(R.string.nr_ble_gsm))) {
            nrBleGsmReference.child(lock.getLockId()).setValue(lock);
            displayDialogMessage(title, message);
        } else if (lockType.equals(getResources().getString(R.string.qt_ble))) {
            qtBleReference.child(lock.getLockId()).setValue(lock);
            displayDialogMessage(title, message);
        } else if (lockType.equals(getResources().getString(R.string.qt_ble_gsm))) {
            qtBleGsmReference.child(lock.getLockId()).setValue(lock);
            displayDialogMessage(title, message);
        } else if (lockType.equals(getResources().getString(R.string.qt_gsm))) {
            qtGsmReference.child(lock.getLockId()).setValue(lock);
            displayDialogMessage(title, message);
        } else if (lockType.equals(getResources().getString(R.string.nr_ble_msh))) {
            nrBleMshReference.child(lock.getLockId()).setValue(lock);
            displayDialogMessage(title, message);
        } else if (lockType.equals(getResources().getString(R.string.nr_nb_iot))) {
            nrNbIotReference.child(lock.getLockId()).setValue(lock);
            displayDialogMessage(title, message);
        }
    }

    public boolean conditionCheck() {
        if (optionGrp.getCheckedRadioButtonId() == -1) {
            Snackbar.make(container, "Please select one of the options", Snackbar.LENGTH_LONG).show();
            return false;
        }
        if (TextUtils.isEmpty(lockId.getText())) {
            lockId.setError("Field cannot be left empty");
            return false;
        }
        if (TextUtils.isEmpty(bleAddress.getText())) {
            bleAddress.setError("Field cannot be left empty");
            return false;
        }
        if (TextUtils.isEmpty(simNumber.getText())) {
            simNumber.setError("Field cannot be left empty");
            return false;
        }
        radioPressed = optionGrp.findViewById(optionGrp.getCheckedRadioButtonId());
        Log.d(TAG, "Lock Option: " + radioPressed.getText()
                + " Lock Id: " + lockId.getText() + " bleAddress: "
                + bleAddress.getText() + " simNumber: " + simNumber.getText());

        return true;
    }

    public String createLockId(String lockAddress, String lockType) {
        lockAddress = lockAddress.replaceAll(":", "");
        lockType = lockType.replaceAll("_", "");
        return lockType + lockAddress;
    }

    public void displayDialogMessage(String title, String message) {
        CustomAlertDialog dialog;
        dialog = new CustomAlertDialog(this, R.style.WideDialog, title,
                message);
        dialog.show();
        dialog.onPositiveButton(view -> {
            dialog.dismiss();
            finish();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(broadcastReceiver, Utility.mapIntentFilter());
        if (!Utility.isMyServiceRunning(this, PubbsService.class)) {
            reConnectDialog("Lock status", getString(R.string.reconnect));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            byte[] data = null;
            Intent intent1 = null;
            if (intent.hasExtra(Utility.INTENT_DATA)) {
                data = intent.getByteArrayExtra(Utility.INTENT_DATA);
            }
            Log.d(TAG, "Service action received:" + action);
            switch (Objects.requireNonNull(action)) {
                case Utility.GATT_CONNECTED:
                    cad.setTitle("Connected!");
                    new Handler().postDelayed(() -> {
                        Intent intent11 = new Intent(Utility.REQUEST_KEY);
                        sendBroadcast(intent11);
                    }, 2000);
                    break;
                case Utility.GATT_DISCONNECTED:
                    reConnectDialog("Connection Failed", "Do you want to reconnect with the lock ?");
                    break;
                case Utility.INVALID_BLUETOOTH:
                    alertDialog("Invalid Bluetooth Addrese", "The bluetooth mac address is not valid.");
                    break;
                case Utility.KEY_RECEIVED:
                    cad.setTitle("Checking lock battery details...");
                    intent1 = new Intent(Utility.CHECKING_LOCK_STATUS);
                    sendBroadcast(intent1);
                    break;
                case Utility.LOCK_OPENED:
                    Log.d(TAG, "lock opened");
                    break;
                case Utility.LOCK_ALREADY_OPENED:
                    cad.dismiss();
                    intent1 = new Intent(Utility.CLEAR_ALL_DATA);
                    sendBroadcast(intent1);
                    Log.d(TAG, "Lock is already opened");
                    Intent intent2 = new Intent(Utility.DISCONNECT_LOCK);
                    intent2.putExtra(Utility.INTENT_DATA_SELF_DISTRACT, true);
                    sendBroadcast(intent2);
                    break;
                case Utility.LOCKED:
                    cad.dismiss();
                    intent1 = new Intent(Utility.CHECK_BATTERY_STATUS);
                    sendBroadcast(intent1);
                    break;
                case Utility.MANUAL_LOCKED:
                    Log.d(TAG, "Manually Locked");
                    break;
                case Utility.BATTERY_STATUS_RECEIVED:
                    for (byte i : Objects.requireNonNull(data)) {
                        batteryData = batteryData + i;
                    }
                    Log.d(TAG, "Battery Data: " + batteryData);
                    intent1 = new Intent(Utility.CLEAR_ALL_DATA);
                    sendBroadcast(intent1);
                    intent1 = new Intent(Utility.DISCONNECT_LOCK);
                    intent1.putExtra(Utility.INTENT_DATA_SELF_DISTRACT, true);
                    sendBroadcast(intent1);
                    // mBluetoothAdapter.disable();
                    break;
                case Utility.DATA_CLEARED:
                    break;
            }
        }
    };

    private void reConnectDialog(String titleString, String contentString) {
        final CustomAlertDialog dialog = new CustomAlertDialog(this, R.style.WideDialog, titleString, contentString);
        dialog.show();
        dialog.onPositiveButton(view -> {
            dialog.dismiss();
            if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
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
            } else {
                startBLEService(sharedPreferences.getString("scanResult", scanResult));
            }
        });
        dialog.onNegativeButton(view -> {
            dialog.dismiss();
            Intent pubbsService = new Intent(this, PubbsService.class);
            stopService(pubbsService);
        });
    }

    //Custom Alert Dialog
    private void alertDialog(String title, String message) {
        final CustomAlertDialog dialog = new CustomAlertDialog(this,
                R.style.WideDialog, title, message);
        dialog.show();
        dialog.onPositiveButton(view -> dialog.dismiss());
    }

    private void startBLEService(String address) {
        Intent intent = new Intent(this, PubbsService.class);
        stopService(intent);
        cad = new CustomAnimationDialog(this, R.style.WideDialog);
        cad.show();
        cad.setAnimation(R.raw.bluetooth);
        cad.setTitle("Connecting...");
        cad.playAnimation();
        Intent pubbsService = new Intent(this, PubbsService.class);
        pubbsService.putExtra("address", address);
        startService(pubbsService);
    }
}
