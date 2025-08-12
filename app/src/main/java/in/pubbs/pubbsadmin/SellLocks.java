package in.pubbs.pubbsadmin;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.animation.Animator;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Objects;

import in.pubbs.pubbsadmin.Adapter.SellLockDetailsAdapter;
import in.pubbs.pubbsadmin.Model.Lock;

//Created By Souvik
public class SellLocks extends AppCompatActivity implements View.OnClickListener, ValueEventListener, Animator.AnimatorListener {

    private EditText atBleQuant, atBleGsmQuant, nrBleQuant, nrBleGsmQuant, qtBleQuant, qtGsmQuant, qtBleGsmQuant;
    private ImageButton atBleInc, atBleDec, atBleGsmInc, atBleGsmDec, nrBleInc, nrBleDec, nrBleGsmInc, nrBleGsmDec, qtBleInc, qtBleDec, qtGsmInc, qtGsmDec, qtBleGsmInc, qtBleGsmDec;
    private Button addToCart;
    private TextView title, phone, organisationName, email;
    private final int REQUEST_CODE = 99;
    private String num = "";
    private ImageView back_button;
    private static String organisation, phoneNo, emailId;
    private CardView cardAtble, cardAtbleGsm, cardNrble, cardNrbleGsm, cardQtble, cardQtGsm, cardQtbleGsm;
    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference, atBleReference, atBleGsmReference, nrBleReference, nrBleGsmReference, qtBleReference, qtBleGsmReference, qtGsmReference, nrBleMshReference, nrNbIotReference;
    private ArrayList<Lock> lockList, atBleList, atBleGsmList, nrBleList, nrBleGsmList, qtBleList, qtBleGsmList, qtGsmList, nrBleMshList, nrNbIotList;
    private RecyclerView.Adapter mAdapter;
    private ConstraintLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sell_lock);
        init();
    }

    private void init() {
        organisation = getIntent().getStringExtra("operatorOrganisation") == null ? "" : getIntent().getStringExtra("operatorOrganisation");
        phoneNo = getIntent().getStringExtra("operatorMobile") == null ? "" : getIntent().getStringExtra("operatorMobile");
        emailId = getIntent().getStringExtra("operatorEmail") == null ? "" : getIntent().getStringExtra("operatorEmail");
        title = findViewById(R.id.toolbar_title);
        title.setText("Sell lock");
        organisationName = findViewById(R.id.organisation_name);
        organisationName.setText(organisation);
        email = findViewById(R.id.email_id);
        email.setText(phoneNo);
        phone = findViewById(R.id.phone_number);
        phone.setText(emailId);

        atBleQuant = findViewById(R.id.value_at_ble1);
        atBleGsmQuant = findViewById(R.id.value_at_ble_gsm);
        nrBleQuant = findViewById(R.id.value_nr_ble);
        nrBleGsmQuant = findViewById(R.id.value_nr_ble_gsm);
        qtBleQuant = findViewById(R.id.value_qt_ble);
        qtGsmQuant = findViewById(R.id.value_qt_gsm);
        qtBleGsmQuant = findViewById(R.id.value_qt_ble_gsm);

        atBleInc = findViewById(R.id.increment_at_ble1);
        atBleDec = findViewById(R.id.decrement_at_ble1);
        atBleGsmInc = findViewById(R.id.increment_at_ble_gsm);
        atBleGsmDec = findViewById(R.id.decrement_at_ble_gsm);
        nrBleInc = findViewById(R.id.increment_nr_ble);
        nrBleDec = findViewById(R.id.decrement_nr_ble);
        nrBleGsmInc = findViewById(R.id.increment_nr_ble_gsm);
        nrBleGsmDec = findViewById(R.id.decrement_nr_ble_gsm);
        qtBleInc = findViewById(R.id.increment_qt_ble);
        qtBleDec = findViewById(R.id.decrement_qt_ble);
        qtGsmInc = findViewById(R.id.increment_qt_gsm);
        qtGsmDec = findViewById(R.id.decrement_qt_gsm);
        qtBleGsmInc = findViewById(R.id.increment_qt_ble_gps);
        qtBleGsmDec = findViewById(R.id.decrement_qt_ble_gsm);

        addToCart = findViewById(R.id.checkout);
        back_button = findViewById(R.id.back_button);

        atBleInc.setOnClickListener(this);
        atBleDec.setOnClickListener(this);
        atBleGsmInc.setOnClickListener(this);
        atBleGsmDec.setOnClickListener(this);
        nrBleInc.setOnClickListener(this);
        nrBleDec.setOnClickListener(this);
        nrBleGsmInc.setOnClickListener(this);
        nrBleGsmDec.setOnClickListener(this);
        qtBleInc.setOnClickListener(this);
        qtBleDec.setOnClickListener(this);
        qtGsmInc.setOnClickListener(this);
        qtGsmDec.setOnClickListener(this);
        qtBleGsmInc.setOnClickListener(this);
        qtBleGsmDec.setOnClickListener(this);

        phone.setOnClickListener(this);
        addToCart.setOnClickListener(this);
        back_button.setOnClickListener(this);

        cardAtble = findViewById(R.id.card1);
        cardAtbleGsm = findViewById(R.id.card2);
        cardNrble = findViewById(R.id.card3);
        cardNrbleGsm = findViewById(R.id.card4);
        cardQtble = findViewById(R.id.card6);
        cardQtGsm = findViewById(R.id.card7);
        cardQtbleGsm = findViewById(R.id.card8);

        cardAtble.setOnClickListener(this);
        cardAtbleGsm.setOnClickListener(this);
        cardNrble.setOnClickListener(this);
        cardNrbleGsm.setOnClickListener(this);
        cardQtble.setOnClickListener(this);
        cardQtGsm.setOnClickListener(this);
        cardQtbleGsm.setOnClickListener(this);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setAlpha(0.0f);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);

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

        atBleReference.addListenerForSingleValueEvent(this);
        atBleGsmReference.addListenerForSingleValueEvent(this);
        nrBleReference.addListenerForSingleValueEvent(this);
        nrBleGsmReference.addListenerForSingleValueEvent(this);
        qtBleReference.addListenerForSingleValueEvent(this);
        qtBleGsmReference.addListenerForSingleValueEvent(this);
        qtGsmReference.addListenerForSingleValueEvent(this);
        nrBleMshReference.addListenerForSingleValueEvent(this);
        nrNbIotReference.addListenerForSingleValueEvent(this);

        container = findViewById(R.id.sell_locks_container);

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.phone_number) {
            Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
            startActivityForResult(intent, REQUEST_CODE);
        } else if (v.getId() == R.id.card1) {
            if (Integer.valueOf(atBleQuant.getText().toString()) != 0) {
                container.animate().alpha(0.0f).setDuration(1000).setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        container.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        recyclerView.animate().alpha(1.0f).setDuration(1000).setListener(this);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                title.setText(getResources().getString(R.string.at_ble));
                mAdapter = new SellLockDetailsAdapter(atBleList, getResources().getString(R.string.at_ble), Integer.valueOf(atBleQuant.getText().toString()));
                recyclerView.setAdapter(mAdapter);
                mAdapter.notifyDataSetChanged();
            }
        } else if (v.getId() == R.id.card2) {
            if (Integer.valueOf(atBleGsmQuant.getText().toString()) != 0) {
                container.animate().alpha(0.0f).setDuration(1000).setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        container.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        recyclerView.animate().alpha(1.0f).setDuration(1000).setListener(this);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                title.setText(getResources().getString(R.string.at_ble_gsm));
                mAdapter = new SellLockDetailsAdapter(atBleGsmList, getResources().getString(R.string.at_ble_gsm), Integer.valueOf(atBleGsmQuant.getText().toString()));
                recyclerView.setAdapter(mAdapter);
            }
        } else if (v.getId() == R.id.card3) {
            if (Integer.valueOf(nrBleQuant.getText().toString()) != 0) {
                container.animate().alpha(0.0f).setDuration(1000).setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        container.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        recyclerView.animate().alpha(1.0f).setDuration(1000).setListener(this);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                title.setText(getResources().getString(R.string.nr_ble));
                mAdapter = new SellLockDetailsAdapter(nrBleList, getResources().getString(R.string.nr_ble), Integer.valueOf(nrBleQuant.getText().toString()));
                recyclerView.setAdapter(mAdapter);
            }
        } else if (v.getId() == R.id.card4) {
            if (Integer.valueOf(nrBleGsmQuant.getText().toString()) != 0) {
                container.animate().alpha(0.0f).setDuration(1000).setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        container.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        recyclerView.animate().alpha(1.0f).setDuration(1000).setListener(this);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                title.setText(getResources().getString(R.string.nr_ble_gsm));
                mAdapter = new SellLockDetailsAdapter(nrBleGsmList, getResources().getString(R.string.nr_ble_gsm), Integer.valueOf(nrBleGsmQuant.getText().toString()));
                recyclerView.setAdapter(mAdapter);
            }
        } else if (v.getId() == R.id.card6) {
            if (Integer.valueOf(qtBleQuant.getText().toString()) != 0) {
                container.animate().alpha(0.0f).setDuration(1000).setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        container.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        recyclerView.animate().alpha(1.0f).setDuration(1000).setListener(this);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                title.setText(getResources().getString(R.string.qt_ble));
                mAdapter = new SellLockDetailsAdapter(qtBleList, getResources().getString(R.string.qt_ble), Integer.valueOf(qtBleQuant.getText().toString()));
                recyclerView.setAdapter(mAdapter);
            }
        } else if (v.getId() == R.id.card7) {
            if (Integer.valueOf(qtGsmQuant.getText().toString()) != 0) {
                container.animate().alpha(0.0f).setDuration(1000).setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        container.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        recyclerView.animate().alpha(1.0f).setDuration(1000).setListener(this);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                title.setText(getResources().getString(R.string.qt_gsm));
                mAdapter = new SellLockDetailsAdapter(qtGsmList, getResources().getString(R.string.qt_gsm), Integer.valueOf(qtGsmQuant.getText().toString()));
                recyclerView.setAdapter(mAdapter);
            }
        } else if (v.getId() == R.id.card8) {
            if (Integer.valueOf(qtBleGsmQuant.getText().toString()) != 0) {
                container.animate().alpha(0.0f).setDuration(1000).setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        container.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        recyclerView.animate().alpha(1.0f).setDuration(1000).setListener(this);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                title.setText(getResources().getString(R.string.qt_ble_gsm));
                mAdapter = new SellLockDetailsAdapter(qtBleGsmList, getResources().getString(R.string.qt_ble_gsm), Integer.valueOf(qtBleGsmQuant.getText().toString()));
                recyclerView.setAdapter(mAdapter);
            }
        } else if (v.getId() == R.id.increment_at_ble1) {
            increment(atBleQuant);
        } else if (v.getId() == R.id.decrement_at_ble1) {
            decrement(atBleQuant);
        } else if (v.getId() == R.id.increment_at_ble_gsm) {
            increment(atBleGsmQuant);
        } else if (v.getId() == R.id.decrement_at_ble_gsm) {
            decrement(atBleGsmQuant);
        } else if (v.getId() == R.id.increment_nr_ble) {
            increment(nrBleQuant);
        } else if (v.getId() == R.id.decrement_nr_ble) {
            decrement(nrBleQuant);
        } else if (v.getId() == R.id.increment_nr_ble_gsm) {
            increment(nrBleGsmQuant);
        } else if (v.getId() == R.id.decrement_nr_ble_gsm) {
            decrement(nrBleGsmQuant);
        } else if (v.getId() == R.id.increment_qt_ble) {
            increment(qtBleQuant);
        } else if (v.getId() == R.id.decrement_qt_ble) {
            decrement(qtBleQuant);
        } else if (v.getId() == R.id.increment_qt_gsm) {
            increment(qtGsmQuant);
        } else if (v.getId() == R.id.decrement_qt_gsm) {
            decrement(qtGsmQuant);
        } else if (v.getId() == R.id.increment_qt_ble_gps) {
            increment(qtBleGsmQuant);
        } else if (v.getId() == R.id.decrement_qt_ble_gsm) {
            decrement(qtBleGsmQuant);
        } else if (v.getId() == R.id.checkout) {
            sentToCart();
            // Need to code regarding checkout.
        } else if (v.getId() == R.id.back_button) {
            onClickOfBackButton();
        }


//        switch (v.getId()) {
//            case R.id.phone_number:
//                Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
//                startActivityForResult(intent, REQUEST_CODE);
//                break;
//            case R.id.card1:
//                if(Integer.valueOf(atBleQuant.getText().toString())!=0) {
//                    container.animate().alpha(0.0f).setDuration(1000).setListener(new Animator.AnimatorListener() {
//                        @Override
//                        public void onAnimationStart(Animator animation) {
//
//                        }
//
//                        @Override
//                        public void onAnimationEnd(Animator animation) {
//                            container.setVisibility(View.GONE);
//                            recyclerView.setVisibility(View.VISIBLE);
//                            recyclerView.animate().alpha(1.0f).setDuration(1000).setListener(this);
//                        }
//
//                        @Override
//                        public void onAnimationCancel(Animator animation) {
//
//                        }
//
//                        @Override
//                        public void onAnimationRepeat(Animator animation) {
//
//                        }
//                    });
//                    title.setText(getResources().getString(R.string.at_ble));
//                    mAdapter = new SellLockDetailsAdapter(atBleList, getResources().getString(R.string.at_ble), Integer.valueOf(atBleQuant.getText().toString()));
//                    recyclerView.setAdapter(mAdapter);
//                    mAdapter.notifyDataSetChanged();
//                }
//                break;
//            case R.id.card2:
//                if(Integer.valueOf(atBleGsmQuant.getText().toString())!=0) {
//                    container.animate().alpha(0.0f).setDuration(1000).setListener(new Animator.AnimatorListener() {
//                        @Override
//                        public void onAnimationStart(Animator animation) {
//
//                        }
//
//                        @Override
//                        public void onAnimationEnd(Animator animation) {
//                            container.setVisibility(View.GONE);
//                            recyclerView.setVisibility(View.VISIBLE);
//                            recyclerView.animate().alpha(1.0f).setDuration(1000).setListener(this);
//                        }
//
//                        @Override
//                        public void onAnimationCancel(Animator animation) {
//
//                        }
//
//                        @Override
//                        public void onAnimationRepeat(Animator animation) {
//
//                        }
//                    });
//                    title.setText(getResources().getString(R.string.at_ble_gsm));
//                    mAdapter = new SellLockDetailsAdapter(atBleGsmList, getResources().getString(R.string.at_ble_gsm), Integer.valueOf(atBleGsmQuant.getText().toString()));
//                    recyclerView.setAdapter(mAdapter);
//                }
//                break;
//            case R.id.card3:
//                if(Integer.valueOf(nrBleQuant.getText().toString())!=0) {
//                    container.animate().alpha(0.0f).setDuration(1000).setListener(new Animator.AnimatorListener() {
//                        @Override
//                        public void onAnimationStart(Animator animation) {
//
//                        }
//
//                        @Override
//                        public void onAnimationEnd(Animator animation) {
//                            container.setVisibility(View.GONE);
//                            recyclerView.setVisibility(View.VISIBLE);
//                            recyclerView.animate().alpha(1.0f).setDuration(1000).setListener(this);
//                        }
//
//                        @Override
//                        public void onAnimationCancel(Animator animation) {
//
//                        }
//
//                        @Override
//                        public void onAnimationRepeat(Animator animation) {
//
//                        }
//                    });
//                    title.setText(getResources().getString(R.string.nr_ble));
//                    mAdapter = new SellLockDetailsAdapter(nrBleList, getResources().getString(R.string.nr_ble), Integer.valueOf(nrBleQuant.getText().toString()));
//                    recyclerView.setAdapter(mAdapter);
//                }
//                break;
//            case R.id.card4:
//                if(Integer.valueOf(nrBleGsmQuant.getText().toString())!=0) {
//                    container.animate().alpha(0.0f).setDuration(1000).setListener(new Animator.AnimatorListener() {
//                        @Override
//                        public void onAnimationStart(Animator animation) {
//
//                        }
//
//                        @Override
//                        public void onAnimationEnd(Animator animation) {
//                            container.setVisibility(View.GONE);
//                            recyclerView.setVisibility(View.VISIBLE);
//                            recyclerView.animate().alpha(1.0f).setDuration(1000).setListener(this);
//                        }
//
//                        @Override
//                        public void onAnimationCancel(Animator animation) {
//
//                        }
//
//                        @Override
//                        public void onAnimationRepeat(Animator animation) {
//
//                        }
//                    });
//                    title.setText(getResources().getString(R.string.nr_ble_gsm));
//                    mAdapter = new SellLockDetailsAdapter(nrBleGsmList, getResources().getString(R.string.nr_ble_gsm), Integer.valueOf(nrBleGsmQuant.getText().toString()));
//                    recyclerView.setAdapter(mAdapter);
//                }
//                break;
//            case R.id.card6:
//                if(Integer.valueOf(qtBleQuant.getText().toString())!=0) {
//                    container.animate().alpha(0.0f).setDuration(1000).setListener(new Animator.AnimatorListener() {
//                        @Override
//                        public void onAnimationStart(Animator animation) {
//
//                        }
//
//                        @Override
//                        public void onAnimationEnd(Animator animation) {
//                            container.setVisibility(View.GONE);
//                            recyclerView.setVisibility(View.VISIBLE);
//                            recyclerView.animate().alpha(1.0f).setDuration(1000).setListener(this);
//                        }
//
//                        @Override
//                        public void onAnimationCancel(Animator animation) {
//
//                        }
//
//                        @Override
//                        public void onAnimationRepeat(Animator animation) {
//
//                        }
//                    });
//                    title.setText(getResources().getString(R.string.qt_ble));
//                    mAdapter = new SellLockDetailsAdapter(qtBleList, getResources().getString(R.string.qt_ble), Integer.valueOf(qtBleQuant.getText().toString()));
//                    recyclerView.setAdapter(mAdapter);
//                }
//                break;
//            case R.id.card7:
//                if(Integer.valueOf(qtGsmQuant.getText().toString())!=0) {
//                    container.animate().alpha(0.0f).setDuration(1000).setListener(new Animator.AnimatorListener() {
//                        @Override
//                        public void onAnimationStart(Animator animation) {
//
//                        }
//
//                        @Override
//                        public void onAnimationEnd(Animator animation) {
//                            container.setVisibility(View.GONE);
//                            recyclerView.setVisibility(View.VISIBLE);
//                            recyclerView.animate().alpha(1.0f).setDuration(1000).setListener(this);
//                        }
//
//                        @Override
//                        public void onAnimationCancel(Animator animation) {
//
//                        }
//
//                        @Override
//                        public void onAnimationRepeat(Animator animation) {
//
//                        }
//                    });
//                    title.setText(getResources().getString(R.string.qt_gsm));
//                    mAdapter = new SellLockDetailsAdapter(qtGsmList, getResources().getString(R.string.qt_gsm), Integer.valueOf(qtGsmQuant.getText().toString()));
//                    recyclerView.setAdapter(mAdapter);
//                }
//                break;
//            case R.id.card8:
//                if(Integer.valueOf(qtBleGsmQuant.getText().toString())!=0) {
//                    container.animate().alpha(0.0f).setDuration(1000).setListener(new Animator.AnimatorListener() {
//                        @Override
//                        public void onAnimationStart(Animator animation) {
//
//                        }
//
//                        @Override
//                        public void onAnimationEnd(Animator animation) {
//                            container.setVisibility(View.GONE);
//                            recyclerView.setVisibility(View.VISIBLE);
//                            recyclerView.animate().alpha(1.0f).setDuration(1000).setListener(this);
//                        }
//
//                        @Override
//                        public void onAnimationCancel(Animator animation) {
//
//                        }
//
//                        @Override
//                        public void onAnimationRepeat(Animator animation) {
//
//                        }
//                    });
//                    title.setText(getResources().getString(R.string.qt_ble_gsm));
//                    mAdapter = new SellLockDetailsAdapter(qtBleGsmList, getResources().getString(R.string.qt_ble_gsm), Integer.valueOf(qtBleGsmQuant.getText().toString()));
//                    recyclerView.setAdapter(mAdapter);
//                }
//                break;
//            case R.id.increment_at_ble1:
//                increment(atBleQuant);
//                break;
//            case R.id.decrement_at_ble1:
//                decrement(atBleQuant);
//                break;
//            case R.id.increment_at_ble_gsm:
//                increment(atBleGsmQuant);
//                break;
//            case R.id.decrement_at_ble_gsm:
//                decrement(atBleGsmQuant);
//                break;
//            case R.id.increment_nr_ble:
//                increment(nrBleQuant);
//                break;
//            case R.id.decrement_nr_ble:
//                decrement(nrBleQuant);
//                break;
//            case R.id.increment_nr_ble_gsm:
//                increment(nrBleGsmQuant);
//                break;
//            case R.id.decrement_nr_ble_gsm:
//                decrement(nrBleGsmQuant);
//                break;
//            case R.id.increment_qt_ble:
//                increment(qtBleQuant);
//                break;
//            case R.id.decrement_qt_ble:
//                decrement(qtBleQuant);
//                break;
//            case R.id.increment_qt_gsm:
//                increment(qtGsmQuant);
//                break;
//            case R.id.decrement_qt_gsm:
//                decrement(qtGsmQuant);
//                break;
//            case R.id.increment_qt_ble_gps:
//                increment(qtBleGsmQuant);
//                break;
//            case R.id.decrement_qt_ble_gsm:
//                decrement(qtBleGsmQuant);
//                break;
//            case R.id.checkout:
//                sentToCart();
//                //Need to code regarding checkout.
//                break;
//            case R.id.back_button:
//                onClickOfBackButton();
//                break;
//        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri contactData = Objects.requireNonNull(data).getData();
                Cursor c = getContentResolver().query(Objects.requireNonNull(contactData), null, null, null, null);
                if (Objects.requireNonNull(c).moveToFirst()) {
                    String contactId = c.getString(c.getColumnIndex(ContactsContract.Contacts._ID));
                    String name = c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));//Display name
                    //phone.setText(name);
                    String hasNumber = c.getString(c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));
                    if (Integer.valueOf(hasNumber) == 1) {
                        Cursor numbers = getContentResolver().
                                query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId, null, null);
                        while (Objects.requireNonNull(numbers).moveToNext()) {
                            num = numbers.getString(numbers.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        }
                        phone.setText(num);
                    }

                }
            }
        }
    }

    private void increment(EditText view) {
        String value;
        int val = 0;
        value = view.getText().toString();
        if (!value.equals("")) {
            val = Integer.valueOf(value);
            val++;
        }
        view.setText(String.valueOf(val));
    }

    private void decrement(EditText view) {
        String value;
        int val = 0;
        value = view.getText().toString();
        if (!value.equals("")) {
            val = Integer.valueOf(value);
            if (val > 0)
                val--;
        }
        view.setText(String.valueOf(val));
    }

    private void sentToCart() {
        Intent intent1 = new Intent(this, Cart.class);
        if (TextUtils.isEmpty(organisationName.getText())) {
            organisationName.setError("Field cannot be left blank");
            return;
        } else {
            intent1.putExtra("org_name", organisationName.getText().toString());
        }
        if (TextUtils.isEmpty(email.getText())) {
            email.setError("Field cannot be left blank");
            return;
        } else {
            intent1.putExtra("email", email.getText().toString());
        }
        if (TextUtils.isEmpty(phone.getText())) {
            phone.setError("Field cannot be left blank");
            return;
        } else {
            intent1.putExtra("phone", phone.getText().toString());
        }
        intent1.putExtra("at_ble", atBleQuant.getText()
                .toString().equals("") ? "0" : atBleQuant.getText().toString());
        intent1.putExtra("at_ble_gsm", atBleGsmQuant.getText()
                .toString().equals("") ? "0" : atBleGsmQuant.getText().toString());
        intent1.putExtra("nr_ble", nrBleQuant.getText()
                .toString().equals("") ? "0" : nrBleQuant.getText().toString());
        intent1.putExtra("nr_ble_gsm", nrBleGsmQuant.getText()
                .toString().equals("") ? "0" : nrBleGsmQuant.getText().toString());
        intent1.putExtra("qt_ble", qtBleQuant.getText()
                .toString().equals("") ? "0" : qtBleQuant.getText().toString());
        intent1.putExtra("qt_gsm", qtGsmQuant.getText()
                .toString().equals("") ? "0" : qtGsmQuant.getText().toString());
        intent1.putExtra("qt_ble_gsm", qtBleGsmQuant.getText()
                .toString().equals("") ? "0" : qtBleGsmQuant.getText().toString());
        startActivity(intent1);
    }

    @Override
    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
        int j = 0;
        for (DataSnapshot i : dataSnapshot.getChildren()) {
            Lock lock = i.getValue(Lock.class);
            if (Objects.requireNonNull(lock).getSimId().equals("NULL") && !lock.getBleAddress().equals("NULL")) {

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
    }

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {

    }

    private void onClickOfBackButton(){
        if(title.getText().equals("Sell lock"))
            finish();
        else{
            title.setText("Sell lock");
            recyclerView.animate().alpha(0.0f).setDuration(1000).setListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    recyclerView.setVisibility(View.GONE);
                    container.setVisibility(View.VISIBLE);
                    container.animate().alpha(1.0f).setListener(this);
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        onClickOfBackButton();
    }

    @Override
    public void onAnimationStart(Animator animation) {

    }

    @Override
    public void onAnimationEnd(Animator animation) {
        recyclerView.setAlpha(1.0f);
        container.setAlpha(1.0f);
    }

    @Override
    public void onAnimationCancel(Animator animation) {

    }

    @Override
    public void onAnimationRepeat(Animator animation) {

    }
}
