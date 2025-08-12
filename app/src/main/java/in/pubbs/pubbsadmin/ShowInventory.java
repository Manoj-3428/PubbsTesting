package in.pubbs.pubbsadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import in.pubbs.pubbsadmin.Model.Lock;
import in.pubbs.pubbsadmin.View.CustomLoader;

public class ShowInventory extends AppCompatActivity implements View.OnClickListener, ValueEventListener {

    private TextView title;
    private ImageView back;
    private Button nextPage;
    private TextView InvAtBle, InvAtBleGsm, InvNrBle, InvNrBleGsm, InvQtBle, InvQtBleGsm, InvQtGsm, InvNrBleMsh, InvNrNbIot;
    private ImageButton btnAtBle, btnAtBleGsm, btnNrBle, btnNrBleGsm, btnQtBle, btnQtBleGsm, btnQtGsm, btnNrBleMsh, btnNrNbIot;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference, atBleReference, atBleGsmReference, nrBleReference, nrBleGsmReference, qtBleReference, qtBleGsmReference, qtGsmReference, nrBleMshReference, nrNbIotReference;
    private ArrayList<Lock> lockList, atBleList, atBleGsmList, nrBleList, nrBleGsmList, qtBleList, qtBleGsmList, qtGsmList, nrBleMshList, nrNbIotList;
    private String TAG = ShowInventory.class.getSimpleName();
    private CustomLoader customLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_inventory);
        init();
        loadData();
    }

    private void loadData() {
        customLoader.show();
        atBleReference.addListenerForSingleValueEvent(this);
        atBleGsmReference.addListenerForSingleValueEvent(this);
        nrBleReference.addListenerForSingleValueEvent(this);
        nrBleGsmReference.addListenerForSingleValueEvent(this);
        qtBleReference.addListenerForSingleValueEvent(this);
        qtBleGsmReference.addListenerForSingleValueEvent(this);
        qtGsmReference.addListenerForSingleValueEvent(this);
        nrBleMshReference.addListenerForSingleValueEvent(this);
        nrNbIotReference.addListenerForSingleValueEvent(this);
    }

    private void init() {
        lockList = new ArrayList<>();
        atBleList = new ArrayList<>();
        atBleGsmList = new ArrayList<>();
        nrBleList = new ArrayList<>();
        nrBleGsmList = new ArrayList<>();
        qtBleList = new ArrayList<>();
        qtBleGsmList = new ArrayList<>();
        qtGsmList = new ArrayList<>();
        nrBleMshList = new ArrayList<>();
        nrNbIotList = new ArrayList<>();
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


        title = findViewById(R.id.toolbar_title);
        title.setText("Inventory");
        back = findViewById(R.id.back_button);
        back.setOnClickListener(this);
        nextPage = findViewById(R.id.lock_inventory);
        nextPage.setOnClickListener(this);
        InvAtBle = findViewById(R.id.cart_AT_BLE);
        InvAtBleGsm = findViewById(R.id.cart_AT_BLE_GSM);
        InvNrBle = findViewById(R.id.cart_NR_BLE);
        InvNrBleGsm = findViewById(R.id.cart_NR_BLE_GSM);
        InvQtBle = findViewById(R.id.cart_QT_BLE);
        InvQtBleGsm = findViewById(R.id.cart_QT_BLE_GSM);
        InvQtGsm = findViewById(R.id.cart_QT_GSM);
        InvNrBleMsh = findViewById(R.id.cart_NR_BLE_MSH);
        InvNrNbIot = findViewById(R.id.cart_NR_NB_IOT);
        btnAtBle = findViewById(R.id.down_AT_BLE);
        btnAtBle.setOnClickListener(this);
        btnAtBleGsm = findViewById(R.id.down_AT_BLE_GSM);
        btnAtBleGsm.setOnClickListener(this);
        btnNrBle = findViewById(R.id.down_NR_BLE);
        btnNrBle.setOnClickListener(this);
        btnNrBleGsm = findViewById(R.id.down_NR_BLE_GSM);
        btnNrBleGsm.setOnClickListener(this);
        btnQtBle = findViewById(R.id.down_QT_BLE);
        btnQtBle.setOnClickListener(this);
        btnQtBleGsm = findViewById(R.id.down_QT_BLE_GSM);
        btnQtBleGsm.setOnClickListener(this);
        btnQtGsm = findViewById(R.id.down_QT_GSM);
        btnQtGsm.setOnClickListener(this);
        btnNrBleMsh = findViewById(R.id.down_NR_BLE_MSH);
        btnNrBleMsh.setOnClickListener(this);
        btnNrNbIot = findViewById(R.id.down_NR_NB_IOT);
        btnNrNbIot.setOnClickListener(this);
        customLoader = new CustomLoader(this, R.style.WideDialog);//Loader
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        if (v.getId() == R.id.back_button) {
            startActivity(new Intent(this, MainActivity.class));
        } else if (v.getId() == R.id.lock_inventory) {
            // startActivity(new Intent(this, AddLock.class));
            startActivity(new Intent(this, ScanQRActivity.class));
        } else if (v.getId() == R.id.down_AT_BLE) {
            intent = new Intent(ShowInventory.this, InventoryLockDetails.class);
            intent.putExtra("dataSet", atBleList);
            intent.putExtra("lockType", "AT_BLE");
            startActivity(intent);
        } else if (v.getId() == R.id.down_AT_BLE_GSM) {
            intent = new Intent(ShowInventory.this, InventoryLockDetails.class);
            intent.putExtra("dataSet", atBleGsmList);
            intent.putExtra("lockType", "AT_BLE_GSM");
            startActivity(intent);
        } else if (v.getId() == R.id.down_NR_BLE) {
            intent = new Intent(ShowInventory.this, InventoryLockDetails.class);
            intent.putExtra("dataSet", nrBleList);
            intent.putExtra("lockType", "NR_BLE");
            startActivity(intent);
        } else if (v.getId() == R.id.down_NR_BLE_GSM) {
            intent = new Intent(ShowInventory.this, InventoryLockDetails.class);
            intent.putExtra("dataSet", nrBleGsmList);
            intent.putExtra("lockType", "NR_BLE_GSM");
            startActivity(intent);
        } else if (v.getId() == R.id.down_QT_BLE) {
            intent = new Intent(ShowInventory.this, InventoryLockDetails.class);
            intent.putExtra("dataSet", qtBleList);
            intent.putExtra("lockType", "QT_BLE");
            startActivity(intent);
        } else if (v.getId() == R.id.down_QT_BLE_GSM) {
            intent = new Intent(ShowInventory.this, InventoryLockDetails.class);
            intent.putExtra("dataSet", qtBleGsmList);
            intent.putExtra("lockType", "QT_BLE_GSM");
            startActivity(intent);
        } else if (v.getId() == R.id.down_QT_GSM) {
            intent = new Intent(ShowInventory.this, InventoryLockDetails.class);
            intent.putExtra("dataSet", qtGsmList);
            intent.putExtra("lockType", "QT_GSM");
            startActivity(intent);
        } else if (v.getId() == R.id.down_NR_BLE_MSH) {
    /*intent = new Intent(ShowInventory.this, InventoryLockDetails.class);
    intent.putExtra("dataSet", nrBleMshList);
    intent.putExtra("lockType", "QT_SMS");
    startActivity(intent);*/
        } else if (v.getId() == R.id.down_NR_NB_IOT) {
    /*intent = new Intent(ShowInventory.this, InventoryLockDetails.class);
    intent.putExtra("dataSet", nrNbIotList);
    intent.putExtra("lockType", "QT_SMS");
    startActivity(intent);*/
        }

//        switch (v.getId()) {
//            case R.id.back_button:
//                startActivity(new Intent(this, MainActivity.class));
//                break;
//            case R.id.lock_inventory:
//                // startActivity(new Intent(this, AddLock.class));
//                startActivity(new Intent(this, ScanQRActivity.class));
//                break;
//            case R.id.down_AT_BLE:
//                intent = new Intent(ShowInventory.this, InventoryLockDetails.class);
//                intent.putExtra("dataSet", atBleList);
//                intent.putExtra("lockType", "AT_BLE");
//                startActivity(intent);
//                break;
//            case R.id.down_AT_BLE_GSM:
//                intent = new Intent(ShowInventory.this, InventoryLockDetails.class);
//                intent.putExtra("dataSet", atBleGsmList);
//                intent.putExtra("lockType", "AT_BLE_GSM");
//                startActivity(intent);
//                break;
//            case R.id.down_NR_BLE:
//                intent = new Intent(ShowInventory.this, InventoryLockDetails.class);
//                intent.putExtra("dataSet", nrBleList);
//                intent.putExtra("lockType", "NR_BLE");
//                startActivity(intent);
//                break;
//            case R.id.down_NR_BLE_GSM:
//                intent = new Intent(ShowInventory.this, InventoryLockDetails.class);
//                intent.putExtra("dataSet", nrBleGsmList);
//                intent.putExtra("lockType", "NR_BLE_GSM");
//                startActivity(intent);
//                break;
//            case R.id.down_QT_BLE:
//                intent = new Intent(ShowInventory.this, InventoryLockDetails.class);
//                intent.putExtra("dataSet", qtBleList);
//                intent.putExtra("lockType", "QT_BLE");
//                startActivity(intent);
//                break;
//            case R.id.down_QT_BLE_GSM:
//                intent = new Intent(ShowInventory.this, InventoryLockDetails.class);
//                intent.putExtra("dataSet", qtBleGsmList);
//                intent.putExtra("lockType", "QT_BLE_GSM");
//                startActivity(intent);
//                break;
//            case R.id.down_QT_GSM:
//                intent = new Intent(ShowInventory.this, InventoryLockDetails.class);
//                intent.putExtra("dataSet", qtGsmList);
//                intent.putExtra("lockType", "QT_GSM");
//                startActivity(intent);
//                break;
//            case R.id.down_NR_BLE_MSH:
//                /*intent = new Intent(ShowInventory.this, InventoryLockDetails.class);
//                intent.putExtra("dataSet", nrBleMshList);
//                intent.putExtra("lockType", "QT_SMS");
//                startActivity(intent);*/
//                break;
//            case R.id.down_NR_NB_IOT:
//                /*intent = new Intent(ShowInventory.this, InventoryLockDetails.class);
//                intent.putExtra("dataSet", nrNbIotList);
//                intent.putExtra("lockType", "QT_SMS");
//                startActivity(intent);*/
//                break;
//        }
    }

    @Override
    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
        int j = 0;
        for (DataSnapshot i : dataSnapshot.getChildren()) {
            Lock lock = i.getValue(Lock.class);
            if (lock.getSimId().equals("NULL") && !lock.getBleAddress().equals("NULL")) {
                if (lock.lockId.substring(0, 5).equalsIgnoreCase("ATBLE")) {
                    atBleList.add(lock);
                } else if (lock.lockId.substring(0, 5).equalsIgnoreCase("NRBLE")) {
                    nrBleList.add(lock);
                } else if (lock.lockId.substring(0, 5).equalsIgnoreCase("QTBLE")) {
                    qtBleList.add(lock);
                }
            } else if (!lock.getSimId().equals("NULL") && !lock.getBleAddress().equals("NULL")) {
                if (lock.lockId.substring(0, 8).equalsIgnoreCase("NRBLEGSM")) {
                    nrBleGsmList.add(lock);
                } else if (lock.lockId.substring(0, 8).equalsIgnoreCase("ATBLEGSM")) {
                    atBleGsmList.add(lock);
                } else if (lock.lockId.substring(0, 8).equalsIgnoreCase("QTBLEGSM")) {
                    qtBleGsmList.add(lock);
                }
            } else if (!lock.getSimId().equals("NULL") && lock.getBleAddress().equals("NULL")) {
                if (lock.getLockId().substring(0, 5).equals("QTGSM")) {
                    qtGsmList.add(lock);
                }
            }
        }
        updateUI();
    }

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {

    }

    private void updateUI() {
        customLoader.dismiss();
        InvAtBle.setText("In Cart : " + atBleList.size());
        InvAtBleGsm.setText("In Cart : " + atBleGsmList.size());
        InvNrBle.setText("In Cart : " + nrBleList.size());
        InvNrBleGsm.setText("In Cart : " + nrBleGsmList.size());
        InvQtBle.setText("In Cart : " + qtBleList.size());
        InvQtBleGsm.setText("In Cart : " + qtBleGsmList.size());
        InvQtGsm.setText("In Cart : " + qtGsmList.size());
        InvNrBleMsh.setText("In Cart : " + nrBleMshList.size());
        InvNrNbIot.setText("In Cart : " + nrNbIotList.size());
    }
}
