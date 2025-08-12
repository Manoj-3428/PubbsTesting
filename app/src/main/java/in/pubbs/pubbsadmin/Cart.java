package in.pubbs.pubbsadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

import in.pubbs.pubbsadmin.View.CustomAlertDialog;
import in.pubbs.pubbsadmin.View.CustomLoader;

public class Cart extends AppCompatActivity {
    private ImageView back;
    private TextView title, cartAtBle, cartAtBleGsm, cartNrBle, cartNrBleGsm, cartQtBle, cartQtGsm, cartQtBleGsm, orgName;
    private String organisationName, email, phone, atBleQuant, atBleGsmQuant, nrBleQuant, nrBleGsmQuant, qtBleQuant, qtGsmQuant, qtBleGsmQuant, orderID, orderDate, invoiceNo, path;
    private String TAG = Cart.class.getSimpleName();
    private ConstraintLayout containerATBLE, containerATBLEGSM, containerNRBLE, containerNRBLESMS, containerQTBLE, containerQTGSM, containerQTBLEGSM;
    SharedPreferences sharedPreferences;
    Map<String, Object> atbleMap, atblegsmMap, nrbleMap, nrblegsmMap, qtbleMap, qtgsmMap, qtblegsmMap;
    private ArrayList<Map<String, Object>> nrbleList;
    DatabaseReference databaseOrderHistory, databasePurchasedOrderDetails, databasePurchaseHistory, databaseInventoryData, databaseCycleList;
    private Button checkoutLock;
    private CustomLoader customLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);
        init();
    }

    private void init() {
        organisationName = getIntent().getStringExtra("org_name").replace(" ", "");
        email = getIntent().getStringExtra("email");
        phone = getIntent().getStringExtra("phone");
        atBleQuant = getIntent().getStringExtra("at_ble");
        atBleGsmQuant = getIntent().getStringExtra("at_ble_gsm");
        nrBleQuant = getIntent().getStringExtra("nr_ble");
        nrBleGsmQuant = getIntent().getStringExtra("nr_ble_gsm");
        qtBleQuant = getIntent().getStringExtra("qt_ble");
        qtGsmQuant = getIntent().getStringExtra("qt_gsm");
        qtBleGsmQuant = getIntent().getStringExtra("qt_ble_gsm");
        Log.d(TAG, "Organisation details: " + email + "\t" + phone + "\t" + organisationName);
        Log.d(TAG, "Lock details: " + atBleQuant + "\t" + atBleGsmQuant + "\t" + nrBleQuant + "\t" + nrBleGsmQuant + "\t" + qtBleQuant + "\t" + qtGsmQuant + "\t" + qtBleGsmQuant);
        databaseOrderHistory = FirebaseDatabase.getInstance().getReference("/SuperAdmin/9433944708/OrderHistory");
        databasePurchasedOrderDetails = FirebaseDatabase.getInstance().getReference("/SuperAdmin/9433944708/PurchasedOrderDetails");
        databasePurchaseHistory = FirebaseDatabase.getInstance().getReference(organisationName + "/PurchaseHistory");
        databaseCycleList = FirebaseDatabase.getInstance().getReference("SuperAdmin/cycleList");
        orderID = generateOrderID();
        invoiceNo = generateInvoiceNo();
        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        orderDate = df.format(c);
        Log.d(TAG, "Order Date: " + orderDate);
        sharedPreferences = getSharedPreferences("pubbs", Context.MODE_PRIVATE);
        title = findViewById(R.id.toolbar_title);
        title.setText("Cart");
        back = findViewById(R.id.back_button);
        back.setOnClickListener(v -> {
            Log.d(TAG, "Yaay");
            finish();
        });
        checkoutLock = findViewById(R.id.checkoutLock);
        orgName = findViewById(R.id.organisation_name);
        cartAtBle = findViewById(R.id.cart_AT_BLE_1);
        cartAtBleGsm = findViewById(R.id.cart_AT_BLE_GSM);
        cartNrBle = findViewById(R.id.cart_NR_BLE);
        cartNrBleGsm = findViewById(R.id.cart_NR_BLE_SMS);
        cartQtBle = findViewById(R.id.cart_QT_BLE);
        cartQtGsm = findViewById(R.id.cart_QT_GSM);
        cartQtBleGsm = findViewById(R.id.cart_QT_BLE_GSM);
        containerATBLE = findViewById(R.id.container_AT_BLE_1);
        containerATBLEGSM = findViewById(R.id.container_AT_BLE_GSM);
        containerNRBLE = findViewById(R.id.container_NR_BLE);
        containerNRBLESMS = findViewById(R.id.container_NR_BLE_SMS);
        containerQTBLE = findViewById(R.id.container_QT_BLE);
        containerQTGSM = findViewById(R.id.container_QT_GSM);
        containerQTBLEGSM = findViewById(R.id.container_QT_BLE_GSM);
        customLoader = new CustomLoader(this, R.style.WideDialog);//Loader
        nrbleList = new ArrayList<>();
        orgName.append(" : " + organisationName);
        if (Integer.valueOf(atBleQuant) > 0) {
            containerATBLE.setVisibility(View.VISIBLE);
            cartAtBle.append(atBleQuant);
        }
        if (Integer.valueOf(atBleGsmQuant) > 0) {
            containerATBLEGSM.setVisibility(View.VISIBLE);
            cartAtBleGsm.append(atBleGsmQuant);
        }
        if (Integer.valueOf(nrBleQuant) > 0) {
            containerNRBLE.setVisibility(View.VISIBLE);
            cartNrBle.append(nrBleQuant);
        }
        if (Integer.valueOf(nrBleGsmQuant) > 0) {
            containerNRBLESMS.setVisibility(View.VISIBLE);
            cartNrBleGsm.append(nrBleGsmQuant);
        }
        if (Integer.valueOf(qtBleQuant) > 0) {
            containerQTBLE.setVisibility(View.VISIBLE);
            cartQtBle.append(qtBleQuant);
        }
        if (Integer.valueOf(qtGsmQuant) > 0) {
            containerQTGSM.setVisibility(View.VISIBLE);
            cartQtGsm.append(qtGsmQuant);
        }
        if (Integer.valueOf(qtBleGsmQuant) > 0) {
            containerQTBLEGSM.setVisibility(View.VISIBLE);
            cartQtBleGsm.append(qtBleGsmQuant);
        }
        checkoutLock.setOnClickListener(v -> loadData(Integer.valueOf(atBleQuant), Integer.valueOf(atBleGsmQuant), Integer.valueOf(nrBleQuant), Integer.valueOf(nrBleGsmQuant), Integer.valueOf(qtBleQuant), Integer.valueOf(qtGsmQuant),
                Integer.valueOf(qtBleGsmQuant)));

    }

    private void loadData(int atBleQuant, int atBleGsmQuant, int nrBleQuant, int nrBleGsmQuant, int qtBleQuant, int qtGsmQuant, int qtBleGsmQuant) {
        customLoader.show();
        path = "/SuperAdmin/9433944708/Inventory/";
        if (atBleQuant <= 0 && atBleGsmQuant <= 0 && nrBleQuant <= 0 && nrBleGsmQuant <= 0 && qtBleQuant <= 0 && qtGsmQuant <= 0 && qtBleGsmQuant <= 0) {
            Toast.makeText(getApplicationContext(), "There are no locks present in the inventory", Toast.LENGTH_SHORT).show();
        }
        if (atBleQuant > 0) {
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(path + "AT_BLE");
            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.getChildrenCount() >= atBleQuant) {
                        int count = 0;
                        Log.d(TAG, "lock count: " + dataSnapshot.getChildrenCount());
                        for (DataSnapshot i : dataSnapshot.getChildren()) {
                            count++;
                            if (count > atBleQuant) {
                                break;
                            } else {
                                atbleMap = (Map<String, Object>) i.getValue();
                                nrbleList.add(atbleMap);
                                Log.d(TAG, "Lock details: " + atbleMap);
                            }
                        }
                        updateDatabase(nrbleList);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
        if (atBleGsmQuant > 0) {
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(path + "AT_BLE_GSM");
            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.getChildrenCount() >= atBleGsmQuant) {
                        int count = 0;
                        Log.d(TAG, "lock count: " + dataSnapshot.getChildrenCount());
                        for (DataSnapshot i : dataSnapshot.getChildren()) {
                            count++;
                            if (count > atBleGsmQuant) {
                                break;
                            } else {
                                atblegsmMap = (Map<String, Object>) i.getValue();
                                nrbleList.add(atblegsmMap);
                                Log.d(TAG, "Lock details: " + atblegsmMap);
                            }
                        }
                        updateDatabase(nrbleList);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
        if (nrBleQuant > 0) {
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(path + "NR_BLE");
            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.getChildrenCount() >= nrBleQuant) {
                        int count = 0;
                        Log.d(TAG, "lock count: " + dataSnapshot.getChildrenCount());
                        for (DataSnapshot i : dataSnapshot.getChildren()) {
                            count++;
                            if (count > nrBleQuant) {
                                break;
                            } else {
                                nrbleMap = (Map<String, Object>) i.getValue();
                                nrbleList.add(nrbleMap);
                                Log.d(TAG, "Lock details: " + nrbleMap);
                                Log.d(TAG, "Lock Data: " + nrbleList.get(count - 1).get("lockId"));
                            }
                        }
                        updateDatabase(nrbleList);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
        if (nrBleGsmQuant > 0) {
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(path + "NR_BLE_GSM");
            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.getChildrenCount() >= nrBleGsmQuant) {
                        int count = 0;
                        Log.d(TAG, "lock count: " + dataSnapshot.getChildrenCount());
                        for (DataSnapshot i : dataSnapshot.getChildren()) {
                            count++;
                            if (count > nrBleGsmQuant) {
                                break;
                            } else {
                                nrblegsmMap = (Map<String, Object>) i.getValue();
                                nrbleList.add(nrblegsmMap);
                                Log.d(TAG, "Lock details: " + nrblegsmMap);
                            }
                        }
                        updateDatabase(nrbleList);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
        if (qtBleQuant > 0) {
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(path + "QT_BLE");
            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.getChildrenCount() >= qtBleQuant) {
                        int count = 0;
                        Log.d(TAG, "lock count: " + dataSnapshot.getChildrenCount());
                        for (DataSnapshot i : dataSnapshot.getChildren()) {
                            count++;
                            if (count > qtBleQuant) {
                                break;
                            } else {
                                qtbleMap = (Map<String, Object>) i.getValue();
                                nrbleList.add(qtbleMap);
                                Log.d(TAG, "Lock details: " + qtbleMap);
                            }
                        }
                        updateDatabase(nrbleList);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
        if (qtGsmQuant > 0) {
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(path + "QT_GSM");
            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.getChildrenCount() >= qtGsmQuant) {
                        int count = 0;
                        Log.d(TAG, "lock count: " + dataSnapshot.getChildrenCount());
                        for (DataSnapshot i : dataSnapshot.getChildren()) {
                            count++;
                            if (count > qtGsmQuant) {
                                break;
                            } else {
                                qtgsmMap = (Map<String, Object>) i.getValue();
                                nrbleList.add(qtgsmMap);
                                Log.d(TAG, "Lock details: " + qtgsmMap);
                            }
                        }
                        updateDatabase(nrbleList);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
        if (qtBleGsmQuant > 0) {
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(path + "QT_BLE_GSM");
            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.getChildrenCount() >= qtBleGsmQuant) {
                        int count = 0;
                        Log.d(TAG, "lock count: " + dataSnapshot.getChildrenCount());
                        for (DataSnapshot i : dataSnapshot.getChildren()) {
                            count++;
                            if (count > qtBleGsmQuant) {
                                break;
                            } else {
                                qtblegsmMap = (Map<String, Object>) i.getValue();
                                nrbleList.add(qtblegsmMap);
                                Log.d(TAG, "Lock details: " + qtblegsmMap);
                            }
                        }
                        updateDatabase(nrbleList);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }

    private void updateDatabase(ArrayList<Map<String, Object>> lockList) {
        customLoader.dismiss();
        ArrayList arrayList = new ArrayList();
        ArrayList bleAddress = new ArrayList();
        ArrayList<Map<String, Object>> atble = new ArrayList<>();
        ArrayList<Map<String, Object>> atblegsm = new ArrayList<>();
        ArrayList<Map<String, Object>> nrble = new ArrayList<>();
        ArrayList<Map<String, Object>> nrblegsm = new ArrayList<>();
        ArrayList<Map<String, Object>> qtble = new ArrayList<>();
        ArrayList<Map<String, Object>> qtgsm = new ArrayList<>();
        ArrayList<Map<String, Object>> qtblegsm = new ArrayList<>();
        for (int i = 0; i < lockList.size(); i++) {
            Log.d("TAG", "Lock ID: " + lockList.get(i).get("lockId") + "\n" + lockList.get(i).get("bleAddress"));
            bleAddress.add(Objects.requireNonNull(lockList.get(i).get("bleAddress")).toString().replace(":", ""));
            arrayList.add(lockList.get(i).get("lockId"));
        }
        for (int i = 0; i < bleAddress.size(); i++) {
            Log.d(TAG, "Arraylist of lock ble address: " + bleAddress.get(i));
            databaseCycleList.child(bleAddress.get(i).toString()).child("customerOrganisation").setValue(organisationName);
            databaseCycleList.child(bleAddress.get(i).toString()).child("orderDate").setValue(orderDate);
        }
        for (int i = 0; i < lockList.size(); i++) {
            if (Objects.requireNonNull(lockList.get(i).get("lockId")).toString().contains("ATBLEGSM")) {
                atblegsm.add(lockList.get(i));
                Log.d(TAG, "Locks: " + atblegsm);
            } else if (Objects.requireNonNull(lockList.get(i).get("lockId")).toString().contains("ATBLE")) {
                atble.add(lockList.get(i));
                Log.d(TAG, "Locks: " + atble);
            } else if (Objects.requireNonNull(lockList.get(i).get("lockId")).toString().contains("NRBLEGSM")) {
                nrblegsm.add(lockList.get(i));
                Log.d(TAG, "Locks: " + nrblegsm);
            } else if (Objects.requireNonNull(lockList.get(i).get("lockId")).toString().contains("NRBLE")) {
                nrble.add(lockList.get(i));
                Log.d(TAG, "Locks: " + nrble);
            } else if (Objects.requireNonNull(lockList.get(i).get("lockId")).toString().contains("QTBLE")) {
                qtble.add(lockList.get(i));
                Log.d(TAG, "Locks: " + qtble);
            } else if (Objects.requireNonNull(lockList.get(i).get("lockId")).toString().contains("QTGSM")) {
                qtgsm.add(lockList.get(i));
                Log.d(TAG, "Locks: " + qtgsm);
            } else if (Objects.requireNonNull(lockList.get(i).get("lockId")).toString().contains("QTBLEGSM")) {
                qtblegsm.add(lockList.get(i));
                Log.d(TAG, "Locks: " + qtblegsm);
            }
        }
        for (int j = 0; j < lockList.size(); j++) {
            if (Objects.requireNonNull(lockList.get(j).get("lockId")).toString().contains("ATBLEGSM")) {
                path = "SuperAdmin/9433944708/Inventory/AT_BLE_GSM";
                databaseInventoryData = FirebaseDatabase.getInstance().getReference(path);
                databaseInventoryData.child(Objects.requireNonNull(lockList.get(j).get("lockId")).toString()).removeValue();
            } else if (Objects.requireNonNull(lockList.get(j).get("lockId")).toString().contains("ATBLE")) {
                path = "SuperAdmin/9433944708/Inventory/AT_BLE";
                databaseInventoryData = FirebaseDatabase.getInstance().getReference(path);
                databaseInventoryData.child(Objects.requireNonNull(lockList.get(j).get("lockId")).toString()).removeValue();
            } else if (Objects.requireNonNull(lockList.get(j).get("lockId")).toString().contains("NRBLEGSM")) {
                path = "SuperAdmin/9433944708/Inventory/NR_BLE_GSM";
                databaseInventoryData = FirebaseDatabase.getInstance().getReference(path);
                databaseInventoryData.child(Objects.requireNonNull(lockList.get(j).get("lockId")).toString()).removeValue();
            } else if (Objects.requireNonNull(lockList.get(j).get("lockId")).toString().contains("NRBLE")) {
                path = "SuperAdmin/9433944708/Inventory/NR_BLE";
                databaseInventoryData = FirebaseDatabase.getInstance().getReference(path);
                databaseInventoryData.child(Objects.requireNonNull(lockList.get(j).get("lockId")).toString()).removeValue();
            } else if (Objects.requireNonNull(lockList.get(j).get("lockId")).toString().contains("QTBLEGSM")) {
                path = "SuperAdmin/9433944708/Inventory/QT_BLE_GSM";
                databaseInventoryData = FirebaseDatabase.getInstance().getReference(path);
                databaseInventoryData.child(Objects.requireNonNull(lockList.get(j).get("lockId")).toString()).removeValue();
            } else if (Objects.requireNonNull(lockList.get(j).get("lockId")).toString().contains("QTBLE")) {
                path = "SuperAdmin/9433944708/QT_BLE";
                databaseInventoryData = FirebaseDatabase.getInstance().getReference(path);
                databaseInventoryData.child(Objects.requireNonNull(lockList.get(j).get("lockId")).toString()).removeValue();
            } else if (Objects.requireNonNull(lockList.get(j).get("lockId")).toString().contains("QTGSM")) {
                path = "SuperAdmin/9433944708/QT_GSM";
                databaseInventoryData = FirebaseDatabase.getInstance().getReference(path);
                databaseInventoryData.child(Objects.requireNonNull(lockList.get(j).get("lockId")).toString()).removeValue();
            }
        }
        databaseOrderHistory.child(orderID).child("Locks").setValue(arrayList);
        databaseOrderHistory.child(orderID).child("orderId").setValue(orderID);
        databaseOrderHistory.child(orderID).child("invoiceNo").setValue(invoiceNo);
        databaseOrderHistory.child(orderID).child("customerOrganisation").setValue(organisationName);
        databaseOrderHistory.child(orderID).child("customerEmail").setValue(email);
        databaseOrderHistory.child(orderID).child("customerPhone").setValue(phone);
        databaseOrderHistory.child(orderID).child("orderDate").setValue(orderDate);
        databaseOrderHistory.child(orderID).child("shippingAddress").setValue("Kolkata");
        databaseOrderHistory.child(orderID).child("billingAddress").setValue("Kolkata");
        databaseOrderHistory.child(orderID).child("GSTN").setValue("18%");
        databaseOrderHistory.child(orderID).child("TAX").setValue("5%");
        databaseOrderHistory.child(orderID).child("discount").setValue("3%");
        databaseOrderHistory.child(orderID).child("quantity").setValue(lockList.size());
        databaseOrderHistory.child(orderID).child("totalPrice").setValue(1000);
        databaseOrderHistory.child(orderID).child("soldBy").setValue("IIT Kharagpur");

        databasePurchasedOrderDetails.child(orderID).child("customerOrganisation").setValue(organisationName);
        databasePurchasedOrderDetails.child(orderID).child("orderId").setValue(orderID);
        databasePurchasedOrderDetails.child(orderID).child("LockList").child("ATBLE").setValue(atble);
        databasePurchasedOrderDetails.child(orderID).child("LockList").child("ATBLEGSM").setValue(atblegsm);
        databasePurchasedOrderDetails.child(orderID).child("LockList").child("NRBLE").setValue(nrble);
        databasePurchasedOrderDetails.child(orderID).child("LockList").child("NRBLEGSM").setValue(nrblegsm);
        databasePurchasedOrderDetails.child(orderID).child("LockList").child("QTBLE").setValue(qtble);
        databasePurchasedOrderDetails.child(orderID).child("LockList").child("QTGSM").setValue(qtgsm);
        databasePurchasedOrderDetails.child(orderID).child("LockList").child("QTBLEGSM").setValue(qtblegsm);
        databasePurchasedOrderDetails.child(orderID).child("customerEmail").setValue(email);
        databasePurchasedOrderDetails.child(orderID).child("customerPhone").setValue(phone);
        databasePurchasedOrderDetails.child(orderID).child("orderDate").setValue(orderDate);
        databasePurchasedOrderDetails.child(orderID).child("invoiceNo").setValue(invoiceNo);
        databasePurchasedOrderDetails.child(orderID).child("shippingAddress").setValue("Kolkata");
        databasePurchasedOrderDetails.child(orderID).child("billingAddress").setValue("Kolkata");
        databasePurchasedOrderDetails.child(orderID).child("GSTN").setValue("18%");
        databasePurchasedOrderDetails.child(orderID).child("TAX").setValue("5%");
        databasePurchasedOrderDetails.child(orderID).child("discount").setValue("3%");
        databasePurchasedOrderDetails.child(orderID).child("quantity").setValue(lockList.size());
        databasePurchasedOrderDetails.child(orderID).child("totalPrice").setValue(1000);
        databasePurchasedOrderDetails.child(orderID).child("soldBy").setValue("IIT Kharagpur");

        databasePurchaseHistory.child(orderID).child("LockList").child("ATBLE").setValue(atble);
        databasePurchaseHistory.child(orderID).child("LockList").child("ATBLEGSM").setValue(atblegsm);
        databasePurchaseHistory.child(orderID).child("LockList").child("NRBLE").setValue(nrble);
        databasePurchaseHistory.child(orderID).child("LockList").child("NRBLEGSM").setValue(nrblegsm);
        databasePurchaseHistory.child(orderID).child("LockList").child("QTBLE").setValue(qtble);
        databasePurchaseHistory.child(orderID).child("LockList").child("QTGSM").setValue(qtgsm);
        databasePurchaseHistory.child(orderID).child("LockList").child("QTBLEGSM").setValue(qtblegsm);
        databasePurchaseHistory.child(orderID).child("orderId").setValue(orderID);
        databasePurchaseHistory.child(orderID).child("customerEmail").setValue(email);
        databasePurchaseHistory.child(orderID).child("customerPhone").setValue(phone);
        databasePurchaseHistory.child(orderID).child("orderDate").setValue(orderDate);
        databasePurchaseHistory.child(orderID).child("invoiceNo").setValue(invoiceNo);
        databasePurchaseHistory.child(orderID).child("shippingAddress").setValue("Kolkata");
        databasePurchaseHistory.child(orderID).child("billingAddress").setValue("Kolkata");
        databasePurchaseHistory.child(orderID).child("GSTN").setValue("18%");
        databasePurchaseHistory.child(orderID).child("TAX").setValue("5%");
        databasePurchaseHistory.child(orderID).child("discount").setValue("3%");
        databasePurchaseHistory.child(orderID).child("quantity").setValue(lockList.size());
        databasePurchaseHistory.child(orderID).child("totalPrice").setValue(1000);
        databasePurchaseHistory.child(orderID).child("soldBy").setValue("IIT Kharagpur");
        alertDialog("Pubbs Purchase Details", "Lock purchase data is successfully stored");
    }


    public String generateOrderID() {
        String orderNumber = "PUBBSOD_"; // OD stands for Order
        String order;
        int max = 999;
        int min = 1;
        int randomNum = (int) (Math.random() * (max - min)) + min;
        order = orderNumber + randomNum;
        Log.d(TAG, "Order Number: " + order);
        return order;
    }

    public String generateInvoiceNo() {
        String invoiceNumber = "PUBBSOD_INVO_"; // OD stands for Order and INVO stands for Invoice
        String invoice;
        int max = 999;
        int min = 1;
        int randomNum = (int) (Math.random() * (max - min)) + min;
        invoice = invoiceNumber + randomNum;
        Log.d(TAG, "invoice number: " + invoice);
        return invoice;
    }


    @Override
    public void onBackPressed() {
        finish();
    }

    private void alertDialog(String title, String message) {
        final CustomAlertDialog dialog = new CustomAlertDialog(this,
                R.style.WideDialog, title, message);
        dialog.show();
        dialog.onPositiveButton(view -> {
            dialog.dismiss();
            Intent intent = new Intent(Cart.this, ManageOperator.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

        });
    }
}
