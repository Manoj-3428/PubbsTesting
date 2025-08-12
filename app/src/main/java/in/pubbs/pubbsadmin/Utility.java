package in.pubbs.pubbsadmin;

import android.app.ActivityManager;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Environment;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utility extends Application {
    public static final int NOTIFICATION_ID = 65;
    public static final String NOTIFICATION_CHANNEL_ID = "PUBBS_CH_01";
    public static final String PUBBS_TAG = "PUBBS_LOG";

    //For Nordic-Parita Dey
    public static final String SERVICE = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String NOTIFY = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String WRITE = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String NOTIFICATION = "00002902-0000-1000-8000-00805f9b34fb";

    //For Quectel-Parita Dey
   /* public static final String SERVICE = "00003412-0000-1000-8000-00805f9b34fb";
    public static final String NOTIFY = "000001c0-0000-1000-8000-00805f9b34fb";
    public static final String WRITE = "000001c0-0000-1000-8000-00805f9b34fb";
    public static final String NOTIFICATION = "000001d0-0000-1000-8000-00805f9b34fb";*/

    public static final int appid = 345678;
    public static final byte[] COMMUNICATION_KEY_COMMAND = {1, 1};
    public static final byte[] UNLOCK_COMMAND = {2, 1};
    public static final byte[] LOCK_COMMAND = {2, 2};
    public static final byte[] LOCK_STATUS_COMMAND = {3, 1};
    public static final byte[] BATTERY_STATUS_COMMAND = {4, 1};
    public static final byte[] CLEAR_LOCK_DATA_COMMAND = {5, 2};
    public static final byte[] HOLD_STOP_COMMAND = {6, 1};

    public final static String INTENT_DATA = "in.pubbs.app.INTENT_DATA";
    public final static String INTENT_DATA_SELF_DISTRACT = "in.pubbs.app.INTENT_DATA_SELF_DISTRACT";
    public final static String GATT_CONNECTED = "in.pubbs.app.GATT_CONNECTED";
    public final static String GATT_DISCONNECTED = "in.pubbs.app.GATT_DISCONNECTED";
    public final static String GATT_SERVICES_DISCOVERED = "in.pubbs.app.GATT_SERVICES_DISCOVERED";
    public static final String INVALID_BLUETOOTH = "in.pubbs.app.INVALID_BLUETOOTH";
    public final static String REQUEST_KEY = "in.pubbs.app.REQUEST_KEY";
    public final static String KEY_RECEIVED = "in.pubbs.app.KEY_RECEIVED";
    public final static String CHECKING_LOCK_STATUS = "in.pubbs.app.CHECKING_LOCK_STATUS";
    public final static String CHECK_BATTERY_STATUS = "in.pubbs.app.CHECK_BATTERY_STATUS";
    public final static String LOCK_OPENED = "in.pubbs.app.LOCK_OPENED";
    public final static String LOCK_ALREADY_OPENED = "in.pubbs.app.LOCK_ALREADY_OPENED";
    public final static String LOCK_ON_HOLD = "in.pubbs.app.LOCK_ON_HOLD";
    public final static String LOCKED = "in.pubbs.app.RIDE_ON_HOLD";
    public final static String RIDE_ENDED = "in.pubbs.app.RIDE_ENDED";
    public final static String HOLD_RIDE = "in.pubbs.app.HOLD_RIDE";
    public final static String STOP_RIDE = "in.pubbs.app.STOP_RIDE";
    public final static String OPEN_LOCK = "in.pubbs.app.OPEN_LOCK";
    public final static String BATTERY_STATUS_RECEIVED = "in.pubbs.app.BATTERY_STATUS_RECEIVED";
    public final static String CLEAR_ALL_DATA = "in.pubbs.app.CLEAR_ALL_DATA";
    public final static String DATA_CLEARED = "in.pubbs.app.DATA_CLEARED";
    public final static String MANUAL_LOCKED = "in.pubbs.app.MANUAL_LOCKED";
    public final static String DISCONNECT_LOCK = "in.pubbs.app.DISCONNECT_LOCK";
    public final static String CHECK_CONNECTION = "in.pubbs.app.CHECK_CONNECTION";
    public static final String BOOKING_CONFIRM = "in.pubbs.app.BOOKING_CONFIRM";
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 14;
    public static final String LOCATION_SERVICE = "in.pubbs.app.LOCATION_SERVICE";

    private static String areaCode;
    private static String baseFare;
    private static String areaName;

    @Override
    public void onCreate() {
        super.onCreate();
        writeLog();
    }

    public static byte[] prepareBytes(byte[] communicationKey, int appid, byte[] command, byte[] data) {
        byte[] instruction = new byte[16];
        instruction[0] = (byte) 0xF;
        instruction[1] = (byte) 8;
        int j = 2;
        byte[] app_id = ByteBuffer.allocate(6).putInt(appid).array();
        for (int i = 0; i < 6; i++) {
            instruction[j] = app_id[i];
            j++;
        }
        for (byte b : communicationKey) {
            instruction[j] = b;
            j++;
        }
        for (byte b : command) {
            instruction[j] = b;
            j++;
        }
        if (data != null) {
            for (byte b : data) {
                instruction[j] = b;
                j++;
            }
        } else {
            instruction[j] = (byte) 0;
            instruction[j + 1] = (byte) 0;
        }
        return instruction;
    }

    public static byte[] getByte(byte[] dataByte, int byteLength, int startPosition) {
        byte[] response = new byte[byteLength];
        int endPosition = startPosition + byteLength;
        int j = 0;
        for (int i = startPosition; i < endPosition; i++) {
            response[j] = dataByte[i];
            j++;
        }
        return response;
    }

    public static int byteArrayToInt(byte[] b) {
        if (b.length == 4)
            return b[0] << 24 | (b[1] & 0xff) << 16 | (b[2] & 0xff) << 8
                    | (b[3] & 0xff);
        else if (b.length == 2)
            return 0x00 << 24 | 0x00 << 16 | (b[0] & 0xff) << 8 | (b[1] & 0xff);

        return 0;
    }

    public static IntentFilter serviceIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(HOLD_RIDE);
        intentFilter.addAction(STOP_RIDE);
        intentFilter.addAction(REQUEST_KEY);
        intentFilter.addAction(CHECKING_LOCK_STATUS);
        intentFilter.addAction(OPEN_LOCK);
        intentFilter.addAction(CHECK_BATTERY_STATUS);
        intentFilter.addAction(CLEAR_ALL_DATA);
        intentFilter.addAction(DISCONNECT_LOCK);
        intentFilter.addAction(CHECK_CONNECTION);
        intentFilter.addAction(BOOKING_CONFIRM);
        return intentFilter;
    }

    public static IntentFilter mapIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(GATT_CONNECTED);
        intentFilter.addAction(GATT_DISCONNECTED);
        intentFilter.addAction(INVALID_BLUETOOTH);
        intentFilter.addAction(GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(LOCK_ALREADY_OPENED);
        intentFilter.addAction(LOCK_ON_HOLD);
        intentFilter.addAction(RIDE_ENDED);
        intentFilter.addAction(BATTERY_STATUS_RECEIVED);
        intentFilter.addAction(KEY_RECEIVED);
        intentFilter.addAction(LOCKED);
        intentFilter.addAction(DATA_CLEARED);
        intentFilter.addAction(MANUAL_LOCKED);
        intentFilter.addAction(LOCATION_SERVICE);
        return intentFilter;
    }

    public static Notification rideNotification(Context context, Intent intent, String title, String message) {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);
        PendingIntent pIntent = PendingIntent.getActivity(context, NOTIFICATION_ID, intent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        notificationBuilder
                .setAutoCancel(false)
                .setSmallIcon(R.drawable.admin_app_logo)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.admin_app_logo))
                .setContentIntent(pIntent)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationManager.IMPORTANCE_HIGH);
        Notification notification = notificationBuilder.build();
        notification.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
        return notification;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static Notification rideNotificationSDK26(Context context, Intent intent, String title, String message) {
        PendingIntent pIntent = PendingIntent.getActivity(context, NOTIFICATION_ID, intent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String NOTIFICATION_CHANNEL_ID = context.getPackageName();
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_ID, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);
        notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.admin_app_logo) // change the notification's app logo
                .setContentIntent(pIntent)
                .setContentTitle(title)
                .setContentText(message)
                .setContentTitle(message)
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE);
        Notification notification = notificationBuilder.build();
        notification.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
        return notification;
    }


    public static void updateNotification(String title, String message, Intent intent, Context context) {
        Notification notification = rideNotification(context, intent, title, message);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, notification);
    }

    public static boolean isMyServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static String parseDate(Date date, String outputPattern) {
        DateFormat df = new SimpleDateFormat(outputPattern);
        return df.format(date);
    }

    public static Date toDate(String date, String inputPattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(inputPattern);
        try {
            return sdf.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int daysBetween(Date d1, Date d2) {
        return (int) ((d2.getTime() - d1.getTime()) / (1000 * 60 * 60 * 24));
    }


    private void writeLog() {
        if (isExternalStorageWritable()) {
            File appDirectory = new File(Environment.getExternalStorageDirectory() + "/pubbs");
            File logDirectory = new File(appDirectory + "/log");
            File logFile = new File(logDirectory, "logcat" + System.currentTimeMillis() + ".txt");

            // create app folder
            if (!appDirectory.exists()) {
                appDirectory.mkdir();
            }

            // create log folder
            if (!logDirectory.exists()) {
                logDirectory.mkdir();
            }

            // clear the previous logcat and then write the new one to the file
            try {
                Process process = Runtime.getRuntime().exec("logcat -c");
                process = Runtime.getRuntime().exec("logcat -f " + logFile);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else if (isExternalStorageReadable()) {
            // only readable
        } else {
            // not accessible
        }
    }


    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }
}
