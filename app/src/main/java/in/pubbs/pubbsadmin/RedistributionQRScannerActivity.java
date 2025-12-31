package in.pubbs.pubbsadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.graphics.Color;
import androidx.cardview.widget.CardView;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import in.pubbs.pubbsadmin.Model.StationData;
import in.pubbs.pubbsadmin.View.CustomAnimationDialog;

public class RedistributionQRScannerActivity extends AppCompatActivity {

    private static final String TAG = "RedistQRScanner";
    private static final int CAMERA_PERMISSION_REQUEST = 101;
    private static final int REQUEST_ENABLE_BT = 20;

    private SurfaceView surfaceView;
    private BarcodeDetector barcodeDetector;
    private CameraSource cameraSource;
    private SparseArray<Barcode> barcodes;
    
    // UI elements
    private TextView tvStationName;
    private TextView tvStationTypeText;
    private ImageView ivStationTypeIcon;
    private ImageView ivStationTypeArrow;
    private TextView tvCyclesCount;
    private TextView tvCyclesLabel;
    private CardView cardStationType;
    private QRScannerOverlayView qrOverlay;
    
    private StationData stationData;
    private boolean isPickup;
    private String currentStationId;
    private String currentStationName;
    private int cyclesToMove;
    
    // Bluetooth and unlocking (RESTORED - using actual QR code scanning)
    private BluetoothAdapter bluetoothAdapter;
    private CustomAnimationDialog cad;
    private BroadcastReceiver broadcastReceiver;
    private String scannedQRCode; // Store scanned QR code (MAC address)
    private boolean isProcessingUnlock = false; // Prevent multiple unlock attempts
    
    // Scanning line animation (KEPT - for visual feedback during QR scanning)
    private Handler animationHandler = new Handler();
    private Runnable animationRunnable;
    private int scanningLinePosition = 0;
    private boolean scanningDirection = true; // true = down, false = up

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_redistribution_qr_scanner);
        
        // Get station data from intent
        currentStationId = getIntent().getStringExtra("station_id");
        currentStationName = getIntent().getStringExtra("station_name");
        String stationType = getIntent().getStringExtra("station_type");
        cyclesToMove = getIntent().getIntExtra("cycles_to_move", 0);
        
        isPickup = "pickup".equals(stationType);
        
        // Initialize Bluetooth (RESTORED - using actual QR code scanning)
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        // Initialize UI
        initViews();
        
        // Update UI with station data
        updateUI(currentStationName, stationType, cyclesToMove);
        
        // Initialize toolbar
        TextView toolbarTitle = findViewById(R.id.tv_title);
        if (toolbarTitle != null) {
            toolbarTitle.setText("Area Manager");
        }
        
        ImageView backButton = findViewById(R.id.iv_back);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }
        
        ImageView menuButton = findViewById(R.id.iv_menu);
        if (menuButton != null) {
            menuButton.setVisibility(View.GONE);
        }
        
        // Initialize camera and QR scanner
        initCamera();
        
        // Register broadcast receiver for lock events (RESTORED)
        registerBroadcastReceiver();
    }
    
    private void initViews() {
        tvStationName = findViewById(R.id.tv_station_name);
        tvStationTypeText = findViewById(R.id.tv_station_type_text);
        ivStationTypeIcon = findViewById(R.id.iv_station_type_icon);
        ivStationTypeArrow = findViewById(R.id.iv_station_type_arrow);
        tvCyclesCount = findViewById(R.id.tv_cycles_count);
        tvCyclesLabel = findViewById(R.id.tv_cycles_label);
        cardStationType = findViewById(R.id.card_station_type);
        surfaceView = findViewById(R.id.surface_view);
        qrOverlay = findViewById(R.id.qr_overlay);
    }
    
    private void updateUI(String stationName, String stationType, int cyclesCount) {
        if (tvStationName != null) {
            tvStationName.setText(stationName != null ? stationName : "");
        }
        
        boolean isPickupStation = "pickup".equals(stationType);
        
        // Update station type button
        if (tvStationTypeText != null) {
            tvStationTypeText.setText(isPickupStation ? "Pick Up Station" : "Drop Off Station");
        }
        
        // Update button background color (light blue for pickup, red for drop)
        if (cardStationType != null) {
            int backgroundColor = isPickupStation ? Color.parseColor("#2196F3") : Color.parseColor("#8B0000");
            cardStationType.setCardBackgroundColor(backgroundColor);
        }
        
        // Update arrow icon (up for pickup, down for drop)
        if (ivStationTypeArrow != null) {
            try {
                int arrowRes = isPickupStation ? R.drawable.up_arrow : R.drawable.down_arrow;
                ivStationTypeArrow.setImageResource(arrowRes);
            } catch (Exception e) {
                // If drawable doesn't exist, try alternative or hide
                Log.w(TAG, "Arrow drawable not found, hiding arrow icon");
                ivStationTypeArrow.setVisibility(View.GONE);
            }
        }
        
        // Update cycles count
        if (tvCyclesCount != null) {
            tvCyclesCount.setText(String.valueOf(cyclesCount));
        }
        
        // Update cycles label
        if (tvCyclesLabel != null) {
            tvCyclesLabel.setText(isPickupStation ? "Cycles To Pick Up" : "Cycles To Drop Off");
        }
        
        // Update instruction text based on station type
        TextView tvInstruction = findViewById(R.id.tv_instruction);
        if (tvInstruction != null) {
            tvInstruction.setText(isPickupStation ? "Scan QR Code to unlock the bicycle" : "Scan QR Code to lock the bicycle");
        }
    }
    
    private void initCamera() {
        // Check camera permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
            return;
        }
        
        // Initialize barcode detector
        barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();
        
        if (!barcodeDetector.isOperational()) {
            Log.w(TAG, "Barcode detector dependencies are not yet available.");
            return;
        }
        
        // Initialize camera source
        cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setRequestedPreviewSize(1024, 768)
                .setRequestedFps(25.0f)
                .setAutoFocusEnabled(true)
                .build();
        
        // Set up barcode detector processor (RESTORED - using actual QR code scanning)
        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
                // Cleanup
            }
            
            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                barcodes = detections.getDetectedItems();
                if (barcodes.size() > 0 && !isProcessingUnlock) {
                    Barcode barcode = barcodes.valueAt(0);
                    String qrCode = barcode.displayValue;
                    Log.d(TAG, "QR Code scanned: " + qrCode);
                    
                    // Handle QR code scan (process on main thread)
                    runOnUiThread(() -> handleQRCodeScanned(qrCode));
                }
            }
        });
        
        // Set up surface view callback
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                try {
                    if (ActivityCompat.checkSelfPermission(RedistributionQRScannerActivity.this, 
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
        
        // Create custom overlay view for QR scanner frame
        if (qrOverlay != null) {
            qrOverlay.setWillNotDraw(false);
        }
    }
    
    // Scanning line animation (KEPT - for visual feedback during QR scanning)
    private void startScanningLineAnimation() {
        if (qrOverlay == null) {
            return;
        }
        
        // Animate scanning line position (visual feedback only, not used for cycle tracking)
        animationRunnable = new Runnable() {
            @Override
            public void run() {
                if (qrOverlay != null) {
                    int maxPosition = qrOverlay.getScanningAreaSize();
                    if (maxPosition == 0) {
                        // Calculate based on overlay width if not yet set
                        maxPosition = (int) (qrOverlay.getWidth() * 0.75f);
                    }
                    
                    if (scanningDirection) {
                        // Moving down
                        scanningLinePosition += 3;
                        if (scanningLinePosition >= maxPosition) {
                            scanningDirection = false;
                        }
                    } else {
                        // Moving up
                        scanningLinePosition -= 3;
                        if (scanningLinePosition <= 0) {
                            scanningDirection = true;
                        }
                    }
                    qrOverlay.updateScanningLine(scanningLinePosition);
                    animationHandler.postDelayed(this, 16); // ~60 FPS
                }
            }
        };
        animationHandler.post(animationRunnable);
    }
    
    private void stopScanningLineAnimation() {
        if (animationRunnable != null) {
            animationHandler.removeCallbacks(animationRunnable);
            animationRunnable = null;
        }
    }
    
    // RESTORED - Unlock logic (using actual QR code scanning)
    private void handleQRCodeScanned(String qrCode) {
        if (isProcessingUnlock) {
            Log.d(TAG, "Already processing unlock, ignoring duplicate scan");
            return;
        }
        
        Log.d(TAG, "Processing scanned QR code: " + qrCode);
        
        // Validate QR code (should be MAC address, typically 17 characters with colons or 12 without)
        String cleanQR = qrCode.trim().replace(":", "");
        if (cleanQR.length() < 12) {
            Toast.makeText(this, "Invalid QR code. Please scan a valid bicycle QR code.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Store the scanned QR code
        scannedQRCode = qrCode.trim();
        
        // Start unlock process
        connectToLock();
    }
    
    // Connect to lock via Bluetooth
    private void connectToLock() {
        if (scannedQRCode == null || scannedQRCode.length() < 12) {
            Toast.makeText(this, "Invalid MAC Address", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!bluetoothAdapter.isEnabled()) {
            showBluetoothPermission();
        } else {
            isProcessingUnlock = true;
            startBLEService(scannedQRCode);
        }
    }
    
    // Start BLE service to connect to lock
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
            isProcessingUnlock = false;
            if (cad != null && cad.isShowing()) {
                cad.dismiss();
            }
            Toast.makeText(this, "Failed to connect to lock", Toast.LENGTH_SHORT).show();
        }
    }
    
    // Send lock/unlock command to connected lock based on station type
    private void sendUnlockCommand() {
        if (scannedQRCode == null || scannedQRCode.length() < 12) {
            Toast.makeText(this, "MAC Address incorrect", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // For pickup stations: unlock, for drop stations: lock
        Intent intent = new Intent(isPickup ? Utility.OPEN_LOCK : Utility.CLOSE_LOCK);
        intent.putExtra("address", scannedQRCode);
        sendBroadcast(intent);
        
        if (cad != null) {
            cad.setTitle(isPickup ? "Unlocking..." : "Locking...");
        }
    }
    
    // Show Bluetooth permission dialog
    private void showBluetoothPermission() {
        AlertDialog dialogBuilder = new AlertDialog.Builder(this).create();
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.custom_bluettooth_permission, null);
        
        Button turnOn = dialogView.findViewById(R.id.turn_on);
        turnOn.setOnClickListener(v -> {
            dialogBuilder.dismiss();
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_ENABLE_BT);
                return;
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        });
        
        TextView cancel = dialogView.findViewById(R.id.cancel);
        cancel.setOnClickListener(view -> {
            dialogBuilder.dismiss();
            isProcessingUnlock = false;
        });
        
        dialogBuilder.setView(dialogView);
        dialogBuilder.show();
    }
    
    // Register broadcast receiver for lock events
    private void registerBroadcastReceiver() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action == null) return;
                
                switch (action) {
                    case Utility.GATT_CONNECTED:
                        if (cad != null) cad.setTitle("Connected!");
                        // Request key first, then unlock
                        new Handler().postDelayed(() -> {
                            sendBroadcast(new Intent(Utility.REQUEST_KEY));
                            // Wait a bit more, then send unlock command
                            new Handler().postDelayed(() -> sendUnlockCommand(), 1500);
                        }, 1000);
                        break;
                        
                    case Utility.GATT_DISCONNECTED:
                    case Utility.INVALID_BLUETOOTH:
                        if (cad != null && cad.isShowing()) cad.dismiss();
                        isProcessingUnlock = false;
                        Toast.makeText(RedistributionQRScannerActivity.this, 
                            "Connection failed", Toast.LENGTH_SHORT).show();
                        break;
                        
                    case Utility.KEY_RECEIVED:
                        if (cad != null) cad.setTitle("Checking lock...");
                        sendBroadcast(new Intent(Utility.CHECKING_LOCK_STATUS));
                        break;
                        
                    case Utility.LOCK_OPENED:
                        // Handle unlock success (for pickup stations)
                        if (isPickup) {
                            Log.d(TAG, "Lock opened successfully!");
                            
                            if (cad != null && cad.isShowing()) {
                                cad.dismiss();
                            }
                            
                            runOnUiThread(() -> {
                                Toast.makeText(RedistributionQRScannerActivity.this, 
                                    "Lock opened!", Toast.LENGTH_SHORT).show();
                                
                                // Update station cycle count in Firebase
                                updateStationCycleCount();
                                
                                // Reset processing flag
                                isProcessingUnlock = false;
                                scannedQRCode = null;
                                
                                // Update cycles count in UI
                                if (tvCyclesCount != null && cyclesToMove > 0) {
                                    cyclesToMove--;
                                    tvCyclesCount.setText(String.valueOf(cyclesToMove));
                                }
                            });
                        }
                        break;
                        
                    case Utility.MANUAL_LOCKED:
                        // Handle lock success (for drop stations)
                        if (!isPickup) {
                            Log.d(TAG, "Lock closed successfully!");
                            
                            if (cad != null && cad.isShowing()) {
                                cad.dismiss();
                            }
                            
                            runOnUiThread(() -> {
                                Toast.makeText(RedistributionQRScannerActivity.this, 
                                    "Lock closed!", Toast.LENGTH_SHORT).show();
                                
                                // Update station cycle count in Firebase
                                updateStationCycleCount();
                                
                                // Reset processing flag
                                isProcessingUnlock = false;
                                scannedQRCode = null;
                                
                                // Update cycles count in UI
                                if (tvCyclesCount != null && cyclesToMove > 0) {
                                    cyclesToMove--;
                                    tvCyclesCount.setText(String.valueOf(cyclesToMove));
                                }
                            });
                        }
                        break;
                        
                    case Utility.RIDE_ENDED:
                    case Utility.LOCKED:
                    case Utility.LOCK_ALREADY_OPENED:
                        if (cad != null && cad.isShowing()) {
                            cad.dismiss();
                        }
                        isProcessingUnlock = false;
                        break;
                }
            }
        };
        
        // Register receiver with appropriate flag for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(broadcastReceiver, Utility.mapIntentFilter(), Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(broadcastReceiver, Utility.mapIntentFilter());
        }
    }
    
    // Update station cycle count in Firebase after successful unlock
    private void updateStationCycleCount() {
        if (scannedQRCode == null) {
            Log.e(TAG, "No QR code to process");
            return;
        }
        
        Log.d(TAG, "Updating station cycle count for QR: " + scannedQRCode);
        
        SharedPreferences sharedPreferences = getSharedPreferences("pubbs", MODE_PRIVATE);
        String operator = sharedPreferences.getString("organisationName", "").replaceAll(" ", "");
        String macAddress = scannedQRCode.trim();
        
        // Remove colons from MAC address for Firebase key
        String cleanMac = macAddress.replace(":", "");
        Log.d(TAG, "Operator: " + operator + " | Clean MAC: " + cleanMac);
        
        DatabaseReference bikeRef = FirebaseDatabase.getInstance().getReference()
                .child(operator)
                .child("Bicycle")
                .child(cleanMac);
        
        bikeRef.get().addOnSuccessListener(bikeSnap -> {
            if (!bikeSnap.exists()) {
                Log.e(TAG, "Bicycle node NOT FOUND for key: " + cleanMac);
                Toast.makeText(this, "Bicycle not found in database", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String stationId = bikeSnap.child("inStationId").getValue(String.class);
            
            if (stationId == null) {
                Log.e(TAG, "inStationId not found in bicycle data!");
                return;
            }
            
            Log.d(TAG, "Station ID found: " + stationId);
            
            DatabaseReference stationRef = FirebaseDatabase.getInstance().getReference()
                    .child(operator)
                    .child("Station")
                    .child(stationId)
                    .child("stationCycleCount");
            
            stationRef.get().addOnSuccessListener(stationSnap -> {
                if (!stationSnap.exists()) {
                    Log.e(TAG, "stationCycleCount NOT FOUND in Station node");
                    return;
                }
                
                int count = Integer.parseInt(stationSnap.getValue().toString());
                
                // For pickup: decrement count (taking cycle from station)
                // For drop: increment count (adding cycle to station)
                int newValue;
                if (isPickup) {
                    newValue = Math.max(0, count - 1);
                } else {
                    newValue = count + 1;
                }
                
                stationRef.setValue(newValue)
                        .addOnSuccessListener(a -> {
                            Log.d(TAG, "Station cycle count updated: " + count + " → " + newValue);
                            Toast.makeText(this, "Station count updated!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed updating station count", e);
                            Toast.makeText(this, "Failed to update station count", Toast.LENGTH_SHORT).show();
                        });
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to fetch bicycle data", e);
            Toast.makeText(this, "Failed to fetch bicycle data", Toast.LENGTH_SHORT).show();
        });
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initCamera();
            } else {
                Log.e(TAG, "Camera permission denied");
                finish();
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraSource != null) {
            cameraSource.stop();
        }
        if (barcodeDetector != null) {
            barcodeDetector.release();
        }
        stopScanningLineAnimation();
        
        // RESTORED - Unlock logic (using actual QR code scanning)
        // Unregister broadcast receiver
        if (broadcastReceiver != null) {
            try {
                unregisterReceiver(broadcastReceiver);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering receiver", e);
            }
        }
        
        // Stop BLE service
        if (scannedQRCode != null) {
            stopService(new Intent(this, PubbsService.class));
        }
        
        // Dismiss dialog if showing
        if (cad != null && cad.isShowing()) {
            cad.dismiss();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (cameraSource != null) {
            cameraSource.stop();
        }
        stopScanningLineAnimation();
        
        // RESTORED - Unlock logic (using actual QR code scanning)
        // Unregister receiver when paused
        if (broadcastReceiver != null) {
            try {
                unregisterReceiver(broadcastReceiver);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering receiver in onPause", e);
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // RESTORED - Unlock logic (using actual QR code scanning)
        // Re-register broadcast receiver
        if (broadcastReceiver != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(broadcastReceiver, Utility.mapIntentFilter(), Context.RECEIVER_NOT_EXPORTED);
                } else {
                    registerReceiver(broadcastReceiver, Utility.mapIntentFilter());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error registering receiver in onResume", e);
            }
        }
        
        if (cameraSource != null && surfaceView.getHolder().getSurface().isValid()) {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    cameraSource.start(surfaceView.getHolder());
                    startScanningLineAnimation();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error resuming camera", e);
            }
        }
    }
    
    // RESTORED - Unlock logic (using actual QR code scanning)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                // Bluetooth enabled, proceed with connection
                if (scannedQRCode != null) {
                    connectToLock();
                }
            } else {
                // Bluetooth not enabled
                isProcessingUnlock = false;
                Toast.makeText(this, "Bluetooth is required to unlock the bicycle", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

