package in.pubbs.pubbsadmin;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;

public class PubbsService extends Service {
    private final static String TAG = "PUBBS_BLE_SERVICE";
    private NotificationManager notificationManager;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothDevice device;
    private String bluetoothAddress;
    private static byte[] communicationKey = new byte[4];
    private boolean manualDisconnect = false;
    private boolean selfDistract = false;
    private String bookingId = null;

    private Intent mapIntent;

    public final static UUID SERVICE_UUID = UUID.fromString(Utility.SERVICE);
    public final static UUID READ_UUID = UUID.fromString(Utility.NOTIFY);
    public final static UUID WRITE_UUID = UUID.fromString(Utility.WRITE);
    public final static UUID NOTIFICATION_UUID = UUID.fromString(Utility.NOTIFICATION);
    /*public final static UUID BATTERY_SERVICE_UUID = UUID.fromString(Utility.BATTERY_SERVICE);
    public final static UUID BATTERY_LEVEL_UUID = UUID.fromString(Utility.BATTERY_LEVEL);
*/
    private LocationManager mLocationManager = null;
    private static int LOCATION_INTERVAL = 3000;
    private static int LOCATION_FAST_INTERVAL = 1000;
    private static float LOCATION_DISTANCE = 1.00f;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Location lastLocation = null;


    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mapIntent = new Intent(this, AddLock.class);
        mapIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        initialize();
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(LOCATION_INTERVAL);
        mLocationRequest.setFastestInterval(LOCATION_FAST_INTERVAL);
        mLocationRequest.setSmallestDisplacement(LOCATION_DISTANCE);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        EventBus.getDefault().register(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        initialize();
        registerReceiver(broadcastReceiver, Utility.serviceIntentFilter());
        bluetoothAddress = intent.getStringExtra("address");
        // modify by dipankar
        ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT);

        if (mBluetoothGatt != null)  mBluetoothGatt.close();
        connect(bluetoothAddress);
        mapIntent.putExtra("status", 1);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(Utility.NOTIFICATION_ID, Utility.rideNotificationSDK26(this, mapIntent, "Connecting...", "Connecting bike lock"));
        } else {
            startForeground(Utility.NOTIFICATION_ID, Utility.rideNotification(this, mapIntent, "Connecting...", "Connecting bike lock"));
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        notificationManager.cancel(Utility.NOTIFICATION_ID);
        unregisterReceiver(broadcastReceiver);
        disconnect();
        if (mBluetoothGatt == null) {
            return;
        }
        EventBus.getDefault().unregister(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Subscribe
    public void onMessageEvent(CloseGatt event) {
        /* Do something */
        disconnect();

        Log.d(TAG, "CloseGattt **********");
    }


    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.d(TAG, "BluetoothAdapter not initialized");
            return;
        }
        refreshDeviceCache();
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
        mBluetoothGatt.disconnect();
        mBluetoothGatt.close();
        mBluetoothGatt = null;
        if (selfDistract) stopSelf();

    }


    private void initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) Log.d(TAG, "Unable to initialize BluetoothManager.");
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) Log.d(TAG, "Unable to obtain a BluetoothAdapter.");
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void connect(final String address) {
        Log.d(TAG, "Bluetooth is connecting: " + address);
        manualDisconnect = false;
        Log.d(TAG, address);
        if (mBluetoothAdapter == null || address == null) {
            if (address == null) {
                Log.d(TAG, "unspecified address.");
            } else {
                Log.d(TAG, "BluetoothAdapter not initialized");
            }
            return;
        }

        // Previously connected device.  Try to reconnect.
      /*  if (address.equals(bluetoothAddress) && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            mBluetoothGatt.connect();
            return;
        }*/
        try {
            device = mBluetoothAdapter.getRemoteDevice(address);
        } catch (IllegalArgumentException e) {
            Intent intent = new Intent(Utility.INVALID_BLUETOOTH);
            sendBroadcast(intent);
            stopSelf();
        }

        if (device == null) {
            Log.d(TAG, "Device not found.  Unable to connect.");
            return;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.


        refreshDeviceCache();

//         mBluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback);
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
        mBluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);


        Log.d(TAG, "Trying to create a new connection.");
        bluetoothAddress = address;
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private boolean isConnected(String address) {

        // Check Bluetooth permission for Android 12+ (API 31 and above)
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
        }



        Log.d(TAG, "My address: isConnected ********" + address);
        for (BluetoothDevice device : mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT)) {
            Log.d(TAG, address);
            if (device.getAddress().equals(address)) {
                return true;
            }
        }
        return false;
    }


//Rahul Pathak -- Releasing the cache


    private void refreshDeviceCache() {
        try {
            mBluetoothGatt.getClass().getMethod("refresh").invoke(mBluetoothGatt);
        } catch (Exception localException) {
            Log.d(TAG, "An exception occured while refreshing device");
        }
    }

    private void writeDataToLock(byte[] bytes) {
        BluetoothGattService mCustomService = mBluetoothGatt.getService(SERVICE_UUID);
        if (mCustomService == null) {
            Log.d(TAG, "Custom BLE Service not found");
            return;
        }
        Log.d(TAG, "getByte method is called in writeDataToLock");
        byte[] cmd = Utility.getByte(bytes, 2, 12);
        StringBuilder builder = new StringBuilder();
        for (byte b : cmd) {
            builder.append(String.format("%x ", b));
        }
        Log.d(TAG, "BLUETOOTH_WRITE_CMD: " + builder.toString());
        //final byte[] encryptedByte=CryptAES.encode(AppConfig.ENCRYPTION_KEY,AppConfig.IV,bytes);
        /*get the read characteristic from the service*/
        BluetoothGattCharacteristic mWriteCharacteristic = mCustomService.getCharacteristic(WRITE_UUID);
        mWriteCharacteristic.setValue(bytes);
        //mWriteCharacteristic.setValue(encryptedByte);
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
        mBluetoothGatt.writeCharacteristic(mWriteCharacteristic);

    }

    private void onReceivedData(final BluetoothGattCharacteristic characteristic) {
        final byte[] responseByte = characteristic.getValue();
        if (responseByte.length == 16) {
            Log.d(TAG, "getByte method is called in onReceivedData");
            final byte[] cmd = Utility.getByte(responseByte, 2, 12);
            byte[] data = Utility.getByte(responseByte, 2, 14);
            StringBuilder builder = new StringBuilder();
            for (byte b : cmd) {
                builder.append(String.format("%x", b));
            }
            Log.d(TAG, "BLUETOOTH_CMD_RECEIVED: " + builder.toString());
            builder = new StringBuilder();
            for (byte datum : data) {
                builder.append(String.format("%x", datum));
            }
            Log.d(TAG, "BLUETOOTH_DATA_RECEIVED: " + builder.toString());
            if (Arrays.equals(cmd, Utility.COMMUNICATION_KEY_COMMAND)) {
                communicationKey = Utility.getByte(responseByte, 4, 8);
                Intent intent = new Intent(Utility.KEY_RECEIVED);
                sendBroadcast(intent);
                return;
            }
            if (Arrays.equals(cmd, Utility.LOCK_STATUS_COMMAND)) {
                Log.d(TAG, "Response after scaning: " + Utility.byteArrayToInt(data));
                if (Utility.byteArrayToInt(data) == 0) {
                    Intent intent = new Intent(Utility.LOCK_ALREADY_OPENED);
                    sendBroadcast(intent);
                } else if (Utility.byteArrayToInt(data) == 1) {
                    Intent intent = new Intent(Utility.LOCKED);
                    sendBroadcast(intent);
                } else {
                    Intent intent = new Intent(Utility.LOCK_ON_HOLD);
                    sendBroadcast(intent);
                }
                return;
            }
            if (Arrays.equals(cmd, Utility.LOCK_COMMAND)) {
                mapIntent.putExtra("status", 4);
                Utility.updateNotification("Pubbs says", "Bicycle is locked", mapIntent, PubbsService.this);
                Intent intent = new Intent(Utility.MANUAL_LOCKED);
                sendBroadcast(intent);
                return;
            }
            if (Arrays.equals(cmd, Utility.UNLOCK_COMMAND)) {
                Intent intent = new Intent(Utility.LOCK_OPENED);
                sendBroadcast(intent);
            }
            if (Arrays.equals(cmd, Utility.BATTERY_STATUS_COMMAND)) {
                Intent intent = new Intent(Utility.BATTERY_STATUS_RECEIVED);
                intent.putExtra(Utility.INTENT_DATA, data);
                sendBroadcast(intent);
                return;
            }
            if (Arrays.equals(cmd, Utility.CLEAR_LOCK_DATA_COMMAND)) {
                Intent intent = new Intent(Utility.DATA_CLEARED);
                sendBroadcast(intent);
                return;
            }
        }
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.d(TAG, "BluetoothAdapter not initialized");
            return;
        }
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
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        Log.d(TAG, "Inside setCharacteristicsNotification set characteristics: " + characteristic.getUuid());
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(NOTIFICATION_UUID);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
        Log.d(TAG, "write descriptor set");
    }


    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Connected to GATT server.");

                Log.d(TAG, "Attempting to start service discovery");

                if (ActivityCompat.checkSelfPermission(PubbsService.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                mBluetoothGatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED && !manualDisconnect) {

                Log.d(TAG, "GATT DISCONNECTED");
                Intent intent = new Intent(Utility.GATT_DISCONNECTED);
                sendBroadcast(intent);
                mapIntent.putExtra("status", 2);
                Utility.updateNotification("Pubbs says lock is disconnected", "Communication has been lost", mapIntent, PubbsService.this);
                selfDistract = true;
                disconnect();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == GATT_SUCCESS) {
                Log.d(TAG, "Inside onServicesDiscovered gatt connected successfully");
                BluetoothGattService mCustomService = gatt.getService(SERVICE_UUID);
                if (mCustomService == null) {
                    Log.d(TAG, "Custom BLE Service not found");
                    return;
                }
                BluetoothGattCharacteristic characteristic = mCustomService.getCharacteristic(READ_UUID);
                if (characteristic == null) {
                    Log.d(TAG, "Read characteristic not found");
                    return;
                }
                setCharacteristicNotification(characteristic, true);
                Intent intent = new Intent(Utility.GATT_CONNECTED);
                sendBroadcast(intent);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.d(TAG, Arrays.toString(descriptor.getCharacteristic().getValue()) + "   onDescriptorWrite******* " + status);
        }


//        @Override
//        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
//            super.onCharacteristicChanged(gatt, characteristic);
//            Log.d(TAG, "onCharacteristicChanged");
//            mBluetoothGatt.readCharacteristic(characteristic);
//            onReceivedData(characteristic);
//        }
            // modify by dipankar
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.d(TAG, "onCharacteristicChanged");

            // Call custom method to handle received data
            onReceivedData(characteristic);
        }


        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            onReceivedData(characteristic);
        }
    };

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            byte[] data = null;
            if (intent.hasExtra(Utility.INTENT_DATA)) {
                data = intent.getByteArrayExtra(Utility.INTENT_DATA);
            }
            Log.d(TAG, "service action received:" + action);
            switch (Objects.requireNonNull(action)) {
                case Utility.REQUEST_KEY:
                    Log.d(TAG, "REQUESTING KEY");
                    communicationKey[0] = (byte) 0;
                    communicationKey[1] = (byte) 0;
                    communicationKey[2] = (byte) 0;
                    communicationKey[3] = (byte) 0;
                    writeDataToLock(Utility.prepareBytes(communicationKey, Utility.appid, Utility.COMMUNICATION_KEY_COMMAND, data));
                    break;
                case Utility.CHECKING_LOCK_STATUS:
                    Log.d(TAG, "communication key: " + Arrays.toString(communicationKey));
                    writeDataToLock(Utility.prepareBytes(communicationKey, Utility.appid, Utility.LOCK_STATUS_COMMAND, data));
                    break;
                case Utility.OPEN_LOCK:
                    writeDataToLock(Utility.prepareBytes(communicationKey, Utility.appid, Utility.UNLOCK_COMMAND, data));
                    break;
                case Utility.CHECK_BATTERY_STATUS:
                    writeDataToLock(Utility.prepareBytes(communicationKey, Utility.appid, Utility.BATTERY_STATUS_COMMAND, data));
                    break;
                case Utility.CLEAR_ALL_DATA:
                    writeDataToLock(Utility.prepareBytes(communicationKey, Utility.appid, Utility.CLEAR_LOCK_DATA_COMMAND, data));
                    break;
                case Utility.DISCONNECT_LOCK:
                    manualDisconnect = true;
                    if (intent.hasExtra(Utility.INTENT_DATA_SELF_DISTRACT)) {
                        selfDistract = intent.getBooleanExtra(Utility.INTENT_DATA_SELF_DISTRACT, false);
                    }
                    disconnect();
                    break;
                case Utility.CHECK_CONNECTION:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (!isConnected(intent.getStringExtra("address"))) {
                            Intent intent1 = new Intent(Utility.GATT_DISCONNECTED);
                            sendBroadcast(intent1);
                        } else {
                            Intent intent1 = new Intent(Utility.GATT_CONNECTED);
                            sendBroadcast(intent1);
                        }
                    }
                    break;
            }
        }
    };

}
