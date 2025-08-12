package in.pubbs.pubbsadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.lang.reflect.Field;
import java.util.Objects;

public class ScanQRActivity extends AppCompatActivity implements View.OnClickListener {
    ImageView back, flash;
    SurfaceView surfaceView;
    TextView barcodeText, title;
    BarcodeDetector detector;
    CameraSource cameraSource;
    String code;
    SparseArray<Barcode> sparseArray;
    String TAG = ScanQRActivity.class.getSimpleName();
    private Camera camera = null;
    boolean flashmode = false;
    TextView toolbarText;
    private BluetoothAdapter mBluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 21;
    Button confirmQr;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qr);
        initView();
    }

    private void initView() {
        toolbarText = findViewById(R.id.toolbar_title);
        toolbarText.setVisibility(View.VISIBLE);
        toolbarText.setText("Scan QR");
        back = findViewById(R.id.back_button);
        back.setOnClickListener(this);
        confirmQr = findViewById(R.id.confirm);
        confirmQr.setOnClickListener(this);
        surfaceView = findViewById(R.id.surface_view);
        barcodeText = findViewById(R.id.tv_barcode);
        flash = findViewById(R.id.flash_button);
        flash.setVisibility(View.VISIBLE);
        flash.setOnClickListener(this);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            showBluetoothPermission();
        }
        //Set up Barcode Detector
        detector = new BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.QR_CODE).build();
        //Set camera source
        cameraSource = new CameraSource.Builder(this, detector).setRequestedPreviewSize(1024, 768).setRequestedFps(25f).setAutoFocusEnabled(true).build();
        //Creating Detector callback function
        detector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {

            }

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
            @Override
            public void surfaceRedrawNeeded(SurfaceHolder holder) {

            }

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    if (ActivityCompat.checkSelfPermission(ScanQRActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    cameraSource.start(holder);
                } catch (Exception ignored) {

                }

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });

    }

    public void showBluetoothPermission() {
        Typeface type4 = Typeface.createFromAsset(getAssets(), "fonts/Comfortaa-Regular.ttf");
        final AlertDialog dialogBuilder = new AlertDialog.Builder(this).create();
        LayoutInflater inflater = this.getLayoutInflater();
        Objects.requireNonNull(dialogBuilder.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        View dialogView = inflater.inflate(R.layout.custom_bluettooth_permission, null);
        final TextView content = dialogView.findViewById(R.id.content);
        content.setTypeface(type4);
        final ImageView bluetooth = dialogView.findViewById(R.id.bluetooth);
        final Button turn_on = dialogView.findViewById(R.id.turn_on);
        turn_on.setTypeface(type4);
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
        final TextView cancel = dialogView.findViewById(R.id.cancel);
        cancel.setTypeface(type4);
        cancel.setOnClickListener(view -> {
            dialogBuilder.dismiss();
            finish();
        });
        dialogBuilder.setView(dialogView);
        dialogBuilder.show();
        dialogBuilder.setCancelable(true);
    }


    private void setCode(String code) {
        this.code = code;
        barcodeText.setText(code);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.back_button) {
            finish();
        } else if (v.getId() == R.id.flash_button) {
            if (!flashmode) {
                flash.setImageResource(R.drawable.ic_flash_off_white);
                flashOnButton();
            } else {
                flash.setImageResource(R.drawable.ic_flash_on_white);
                flashOnButton();
            }
        } else if (v.getId() == R.id.confirm) {
            Log.d(TAG, "bar code data: " + code);
            // A standard BLE address has a length of 17 characters.
            if (barcodeText.getText().length() == 17) {
                Log.d(TAG, "QR code after scanning: " + code);
                Intent intent = new Intent(ScanQRActivity.this, AddLock.class);
                intent.putExtra("QrCode", code);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                this.finish();
            } else {
                barcodeText.setError("Please scan or re-enter the QR code again");
            }
        } else {
            // Handle default case or do nothing
        }

//        switch (v.getId()) {
//            case R.id.back_button:
//                finish();
//                break;
//            case R.id.flash_button:
//                if (!flashmode) {
//                    flash.setImageResource(R.drawable.ic_flash_off_white);
//                    flashOnButton();
//                } else {
//                    flash.setImageResource(R.drawable.ic_flash_on_white);
//                    flashOnButton();
//                }
//                break;
//            case R.id.confirm:
//                Log.d(TAG, "bar code data: " + code);
//                if (barcodeText.getText().length() == 17) {//A standard ble address has a length of 17 characters.
//                    Log.d(TAG, "QR code after scanning: " + code);
//                    Intent intent = new Intent(ScanQRActivity.this, AddLock.class);
//                    intent.putExtra("QrCode", code);
//                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                    startActivity(intent);
//                    this.finish();
//                } else {
//                    barcodeText.setError("Please scan or re-enter the QR code again");
//                }
//                break;
//            default:
//                break;
//        }
    }

    private static Camera getCamera(@NonNull CameraSource cameraSource) {
        Field[] declaredFields = CameraSource.class.getDeclaredFields();

        for (Field field : declaredFields) {
            if (field.getType() == Camera.class) {
                field.setAccessible(true);
                try {
                    Camera camera = (Camera) field.get(cameraSource);
                    return camera;
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
