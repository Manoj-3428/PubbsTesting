package in.pubbs.pubbsadmin.Adapter;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

import in.pubbs.pubbsadmin.ManageAds;
import in.pubbs.pubbsadmin.Model.DiscountDetails;
import in.pubbs.pubbsadmin.R;
import in.pubbs.pubbsadmin.ShowAllAds;

public class ShowAllAdsAdapter extends RecyclerView.Adapter<ShowAllAdsAdapter.HolderClass> {
    private ArrayList<DiscountDetails> list;
    private ShowAllAds context;
    private HashMap<String, DiscountDetails> selectedList;
    private boolean longClicked;
    private ArrayList<String> selectedId;

    public ShowAllAdsAdapter(ArrayList<DiscountDetails> list, ShowAllAds context) {
        this.list = list;
        this.context = context;
        selectedList = new HashMap<>();
        selectedId = new ArrayList<>();
    }

    @NonNull
    @Override
    public HolderClass onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_card_layout, parent, false);
        return new ShowAllAdsAdapter.HolderClass(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HolderClass holder, int position) {
        holder.textView.setText(list.get(position).getText());
        String array[] = (list.get(position).getColor() + ",").split("[,]+");
        if (longClicked) {//If long clicked then a hollow circle will appear at the top left corner of the card.
            holder.circle.setVisibility(View.VISIBLE);
        } else {//If not long clicked then a hollow circle will be invisible.
            holder.circle.setVisibility(View.INVISIBLE);
            holder.cardView.setAlpha(1);
        }
        if (Integer.valueOf(array[0]) == 255 && Integer.valueOf(array[1]) == 255 && Integer.valueOf(array[2]) == 255) { //When the background is white the text color is changed to black else the text color is white.
            holder.textView.setTextColor(Color.rgb(0, 0, 0));
        }
        holder.cardView.getBackground().setColorFilter(Color.rgb(Integer.valueOf(array[0]), Integer.valueOf(array[1]), Integer.valueOf(array[2])), PorterDuff.Mode.SRC_IN);
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (longClicked) {
                    holder.cardView.setAlpha(0.5f);
                    if (!selectedList.containsKey(list.get(position).getId())) {//If list.get(position).getId() is not present in selectedList then we insert it, update the alpha of the card and change the background of the circle at the upper left corner.
                        selectedList.put(list.get(position).getId(), list.get(position));
                        selectedId.add(list.get(position).getId());
                        holder.circle.getBackground().setColorFilter(Color.rgb(255, 255, 255), PorterDuff.Mode.SRC_ATOP);
                    } else {//If list.get(position).getId() is present in selectedList then we remove it, update the alpha of the card and change the background of the circle at the upper left corner.
                        holder.cardView.setAlpha(1);
                        selectedList.remove(list.get(position).getId());
                        selectedId.remove(list.get(position).getId());
                        holder.circle.setBackground(context.getDrawable(R.drawable.ring));
                    }
                    context.removeAds.setVisibility(View.VISIBLE);
                } else {//If the card is not long pressed then we pop up a dialog.
                    showDialog(list.get(position));
                }
            }
        });
        holder.cardView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(context, "Please click on the card to select for removal.", Toast.LENGTH_LONG).show();
                longClicked = true;
                notifyDataSetChanged();
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class HolderClass extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView textView;
        ConstraintLayout constraintLayout;
        ImageView circle;

        public HolderClass(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card);
            textView = itemView.findViewById(R.id.text);
            constraintLayout = itemView.findViewById(R.id.container);
            circle = itemView.findViewById(R.id.circle);
        }
    }

    //Dialog that pop when the card is pressed
    private void showDialog(DiscountDetails obj) {
        Button submit;
        EditText discount, validity, startDate;
        CheckBox active;
        Dialog dialog = new Dialog(context, R.style.WideDialog);
        dialog.setContentView(R.layout.ads_more_info);
        dialog.show();
        submit = dialog.findViewById(R.id.submit);
        discount = dialog.findViewById(R.id.discount_data);
        discount.setText(obj.getDiscount() + "");
        validity = dialog.findViewById(R.id.validity_data);
        validity.setText(obj.getValidity() + "");
        startDate = dialog.findViewById(R.id.date_picker);
        startDate.setText(obj.getStartDate());
        active = dialog.findViewById(R.id.is_active);
        if (obj.isActive()) active.setChecked(true);
        else active.setChecked(false);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String dis, valid, start;
                dis = discount.getText().toString();
                valid = validity.getText().toString();
                start = startDate.getText().toString();
                if (TextUtils.isEmpty(dis)) {
                    discount.setError("Field cannot be left blank.");
                }
                if (TextUtils.isEmpty(valid)) {
                    validity.setError("Field cannot be left blank.");
                }
                if (TextUtils.isEmpty(start)) {
                    startDate.setError("Field cannot be left blank.");
                }
                if (startDate.getText().toString().length() != 10) {
                    startDate.setError("Date entered in wrong format");
                }
                if (!validateDate(start)) {
                    startDate.setError("Date entered in wrong format");
                } else {
                    if (active.isChecked()) {
                        obj.setActive(true);
                        insertData(obj);
                    } else {
                        obj.setActive(false);
                        insertData(obj);
                    }
                    dialog.dismiss();
                }
            }
        });

    }

    //Date validation
    public boolean validateDate(String date) {
        int dd = Integer.valueOf(date.substring(0, date.indexOf("/")));
        int mm = Integer.valueOf(date.substring(date.indexOf("/") + 1, date.lastIndexOf("/")));
        int yy = Integer.valueOf(date.lastIndexOf("/") + 1);
        if (!(dd > 0 && dd < 31)) {
            return false;
        }
        if (!(mm > 0 && mm < 13)) {
            return false;
        }
        return true;
    }

    //This function is called when the card view is clicked and a dialog view is popped up and data need to be changed in the database.
    private void insertData(DiscountDetails obj) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getResources().getString(R.string.sharedPreferences), Context.MODE_PRIVATE);
        DatabaseReference databaseReference;
        String path = sharedPreferences.getString("organisationName", "no_data").replace(" ", "") + "/DiscountDetails/" + obj.getId();
        //Log.d(TAG, "Path: " + path);
        databaseReference = FirebaseDatabase.getInstance().getReference(path);
        databaseReference.child("text").setValue(obj.getText());
        databaseReference.child("color").setValue(obj.getColor());
        databaseReference.child("discount").setValue(obj.getDiscount());
        databaseReference.child("validity").setValue(obj.getValidity());
        databaseReference.child("startDate").setValue(obj.getStartDate());
        databaseReference.child("active").setValue(obj.isActive());
        Toast.makeText(context, "Data updated successfully!!", Toast.LENGTH_LONG).show();
    }

    //check for card long click.
    public boolean isLongClicked() {
        return longClicked;
    }

    public void updateUi() {
        longClicked = false;
        context.removeAds.setVisibility(View.INVISIBLE);
        notifyDataSetChanged();
    }

    //getter function to get a list of all the selected items.
    public ArrayList<String> getSelectedId() {
        return selectedId;
    }
}
