package in.pubbs.pubbsadmin;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.util.concurrent.atomic.AtomicBoolean;

public class MaintenanceQRScannerActivity extends AppCompatActivity {

    public static final int REQ_REASSIGN_SCAN = 2003;
    private static final int REQ_CAMERA = 2004;
    private static final String TAG = "MaintenanceQRScanner";

    private SurfaceView surfaceView;
    private QRScannerOverlayView qrOverlay;
    private BarcodeDetector barcodeDetector;
    private CameraSource cameraSource;
    private Handler animationHandler;
    private Runnable animationRunnable;
    private final AtomicBoolean handled = new AtomicBoolean(false);
    private SparseArray<Barcode> barcodes;

    private String mode;
    private String stationId;
    private String stationName;
    private String areaId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maintenance_qr_scanner);

        mode = getIntent().getStringExtra("mode");
        stationId = getIntent().getStringExtra("stationId");
        stationName = getIntent().getStringExtra("stationName");
        areaId = getIntent().getStringExtra("areaId");

        TextView toolbarTitle = findViewById(R.id.tv_title);
        if (toolbarTitle != null) {
            toolbarTitle.setText(R.string.scan_qr_code);
        }

        ImageView backButton = findViewById(R.id.iv_back);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        ImageView menuButton = findViewById(R.id.iv_menu);
        if (menuButton != null) {
            menuButton.setVisibility(android.view.View.GONE);
        }

        surfaceView = findViewById(R.id.surface_view);
        qrOverlay = findViewById(R.id.qr_overlay);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
        } else {
            initCamera();
        }
    }

    private void initCamera() {
        barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();

        if (!barcodeDetector.isOperational()) {
            Log.w(TAG, "Barcode detector dependencies are not yet available.");
            return;
        }

        cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setRequestedPreviewSize(1024, 768)
                .setRequestedFps(25.0f)
                .setAutoFocusEnabled(true)
                .build();

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
                // Cleanup
            }

            @Override
            public void receiveDetections(@NonNull Detector.Detections<Barcode> detections) {
                barcodes = detections.getDetectedItems();
                if (barcodes.size() > 0 && !handled.get()) {
                    Barcode barcode = barcodes.valueAt(0);
                    String qrCode = barcode.displayValue;
                    Log.d(TAG, "QR Code scanned: " + qrCode);

                    runOnUiThread(() -> handleQRCodeScanned(qrCode));
                }
            }
        });

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                try {
                    if (ActivityCompat.checkSelfPermission(MaintenanceQRScannerActivity.this,
                            Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        cameraSource.start(holder);
                        startScanningLineAnimation();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error starting camera source", e);
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
                // Handle surface changes
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                cameraSource.stop();
                stopScanningLineAnimation();
            }
        });

        if (qrOverlay != null) {
            qrOverlay.setWillNotDraw(false);
        }
    }

    private void handleQRCodeScanned(String qrCode) {
        if (handled.get()) return;
        handled.set(true);

        String raw = qrCode.trim();
        String bicycleId = raw.replace(":", "");

        Intent data = new Intent();
        data.putExtra("bicycleId", bicycleId);
        // Echo station context back (useful for add/reassign flows)
        if (stationId != null) data.putExtra("stationId", stationId);
        if (stationName != null) data.putExtra("stationName", stationName);
        if (areaId != null) data.putExtra("areaId", areaId);
        setResult(RESULT_OK, data);
        finish();
    }

    private void startScanningLineAnimation() {
        if (qrOverlay == null) return;

        animationHandler = new Handler();
        animationRunnable = new Runnable() {
            @Override
            public void run() {
                if (qrOverlay != null) {
                    int maxPosition = qrOverlay.getScanningAreaSize();
                    if (maxPosition == 0) {
                        maxPosition = (int) (qrOverlay.getWidth() * 0.75f);
                    }

                    int currentPosition = qrOverlay.getScanningLinePosition();
                    boolean direction = qrOverlay.getScanningDirection();

                    if (direction) {
                        currentPosition += 8;
                        if (currentPosition >= maxPosition) {
                            currentPosition = maxPosition;
                            direction = false;
                        }
                    } else {
                        currentPosition -= 8;
                        if (currentPosition <= 0) {
                            currentPosition = 0;
                            direction = true;
                        }
                    }

                    qrOverlay.updateScanningLine(currentPosition);
                    qrOverlay.setScanningDirection(direction);

                    animationHandler.postDelayed(this, 50);
                }
            }
        };
        animationHandler.post(animationRunnable);
    }

    private void stopScanningLineAnimation() {
        if (animationHandler != null && animationRunnable != null) {
            animationHandler.removeCallbacks(animationRunnable);
        }
        if (qrOverlay != null) {
            qrOverlay.resetScanningLine();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initCamera();
            } else {
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopScanningLineAnimation();
        if (cameraSource != null) {
            cameraSource.stop();
        }
        if (barcodeDetector != null) {
            barcodeDetector.release();
        }
    }
}

