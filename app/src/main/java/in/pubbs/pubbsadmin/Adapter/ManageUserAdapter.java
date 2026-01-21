package in.pubbs.pubbsadmin.Adapter;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Map;

import in.pubbs.pubbsadmin.R;
import in.pubbs.pubbsadmin.UserDetailActivity;

public class ManageUserAdapter extends RecyclerView.Adapter<ManageUserAdapter.HolderClass> {
    ArrayList<Map<String, Object>> list;
    Context context;

    public ManageUserAdapter(ArrayList list, Context context) {
        this.list = list;
        this.context = context;
    }
    
    public void updateList(ArrayList<Map<String, Object>> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ManageUserAdapter.HolderClass onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_manage_user_row, parent, false);
        Log.d("ManageUserAdapter", "Inflated view type: " + view.getClass().getSimpleName());
        return new ManageUserAdapter.HolderClass(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ManageUserAdapter.HolderClass holder, int position) {
        String userName = (String) list.get(position).get("name");
        String id = (String) list.get(position).get("user_id");
        String phone = (String) list.get(position).get("mobile");
        holder.name.setText("Name: " + userName);
        holder.id.setText("Id: " + id);
        holder.phone.setText("Phone: " + phone);

        // Ensure toggle is found (try again if not found in constructor)
        // Use post to ensure view is fully laid out
        holder.itemView.post(() -> {
            if (holder.blockToggle == null) {
                holder.findBlockToggle();
                
                // If still not found, try one more time with more aggressive search
                if (holder.blockToggle == null) {
                    Log.w("ManageUserAdapter", "Toggle still not found, trying aggressive search");
                    // Try finding from the absolute root
                    View root = holder.itemView.getRootView();
                    if (root != null) {
                        holder.blockToggle = root.findViewById(R.id.block_toggle);
                    }
                    // Last resort: find any Switch in the itemView
                    if (holder.blockToggle == null) {
                        View switchView = holder.findViewByType(holder.itemView, Switch.class);
                        if (switchView instanceof Switch) {
                            holder.blockToggle = (Switch) switchView;
                            Log.d("ManageUserAdapter", "Found toggle as last resort");
                        }
                    }
                }
            }
            
            // Now set up the toggle if found
            if (holder.blockToggle != null) {
                setupToggleForUser(holder, phone);
            } else {
                Log.e("ManageUserAdapter", "CRITICAL: block_toggle not found after all attempts!");
            }
        });
        
        // Also try immediately (might work if view is already laid out)
        if (holder.blockToggle == null) {
            holder.findBlockToggle();
        }

        // Fetch and set block value - check if toggle exists
        if (holder.blockToggle != null) {
            setupToggleForUser(holder, phone);
        }
    }
    
    private void setupToggleForUser(HolderClass holder, String phone) {
        if (holder.blockToggle == null) {
            Log.w("ManageUserAdapter", "Cannot setup toggle - blockToggle is null");
            return;
        }
        
        // Use final variable for lambda to ensure proper capture
        final String phoneNumber = phone;
        
        // Set up the toggle listener (will be temporarily removed during load)
        CompoundButton.OnCheckedChangeListener toggleListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Update colors immediately for better UX
                updateSwitchColors(holder.blockToggle, isChecked);
                
                // Update database
                if (phoneNumber != null && !phoneNumber.isEmpty()) {
                    updateBlockValue(phoneNumber, isChecked);
                } else {
                    Toast.makeText(context, "Cannot update: User phone number is missing", Toast.LENGTH_SHORT).show();
                    // Revert the toggle
                    holder.blockToggle.setOnCheckedChangeListener(null);
                    holder.blockToggle.setChecked(!isChecked);
                    updateSwitchColors(holder.blockToggle, !isChecked);
                    // Re-set listener
                    holder.blockToggle.setOnCheckedChangeListener(this);
                }
            }
        };
        
        // Load initial block value from database
        if (phone != null && !phone.isEmpty()) {
            loadBlockValue(phone, holder.blockToggle, toggleListener);
        } else {
            // No phone number, default to unblocked
            holder.blockToggle.setOnCheckedChangeListener(null);
            holder.blockToggle.setChecked(false);
            updateSwitchColors(holder.blockToggle, false);
            // Set listener for future changes
            holder.blockToggle.setOnCheckedChangeListener(toggleListener);
        }

        // Prevent card click when clicking toggle
        holder.blockToggle.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });

        // Card click listener - navigate to user details
        holder.container.setOnClickListener(v -> {
            // Don't navigate if user clicked on toggle
            if (holder.blockToggle != null && holder.blockToggle.isPressed()) {
                return;
            }
            // Navigate to UserDetailActivity with user mobile as identifier
            if (phone != null && !phone.isEmpty()) {
                Intent intent = new Intent(context, UserDetailActivity.class);
                intent.putExtra("USER_MOBILE", phone);
                context.startActivity(intent);
            }
        });
    }

    private void loadBlockValue(String userMobile, Switch blockToggle, CompoundButton.OnCheckedChangeListener listener) {
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("Users/" + userMobile + "/block");
        
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String blockValue = snapshot.getValue() != null ? snapshot.getValue().toString().trim() : "no";
                    boolean isBlocked = blockValue.equalsIgnoreCase("yes");
                    
                    // Temporarily remove listener to avoid triggering during programmatic change
                    blockToggle.setOnCheckedChangeListener(null);
                    blockToggle.setChecked(isBlocked);
                    updateSwitchColors(blockToggle, isBlocked);
                    // Re-attach listener after setting the value
                    if (listener != null) {
                        blockToggle.setOnCheckedChangeListener(listener);
                    }
                    Log.d("ManageUserAdapter", "Loaded block value for " + userMobile + ": " + blockValue + " (isBlocked: " + isBlocked + ")");
                } else {
                    // If block field doesn't exist, default to false (not blocked)
                    blockToggle.setOnCheckedChangeListener(null);
                    blockToggle.setChecked(false);
                    updateSwitchColors(blockToggle, false);
                    // Re-attach listener after setting the value
                    if (listener != null) {
                        blockToggle.setOnCheckedChangeListener(listener);
                    }
                    Log.d("ManageUserAdapter", "No block value found for " + userMobile + ", defaulting to unblocked");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ManageUserAdapter", "Error loading block value: " + error.getMessage());
                blockToggle.setOnCheckedChangeListener(null);
                blockToggle.setChecked(false);
                updateSwitchColors(blockToggle, false);
                // Re-attach listener after setting the value
                if (listener != null) {
                    blockToggle.setOnCheckedChangeListener(listener);
                }
            }
        });
    }

    private void updateSwitchColors(Switch blockToggle, boolean isBlocked) {
        if (isBlocked) {
            // Blue when blocked (on)
            int thumbColor = ContextCompat.getColor(context, R.color.blue_500);
            int trackColor = ContextCompat.getColor(context, R.color.blue_300);
            blockToggle.setThumbTintList(ColorStateList.valueOf(thumbColor));
            blockToggle.setTrackTintList(ColorStateList.valueOf(trackColor));
        } else {
            // Gray when not blocked (off)
            int thumbColor = ContextCompat.getColor(context, android.R.color.white);
            int trackColor = ContextCompat.getColor(context, R.color.grey_400);
            blockToggle.setThumbTintList(ColorStateList.valueOf(thumbColor));
            blockToggle.setTrackTintList(ColorStateList.valueOf(trackColor));
        }
    }

    private void updateBlockValue(String userMobile, boolean isBlocked) {
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("Users/" + userMobile + "/block");
        
        String blockValue = isBlocked ? "yes" : "no";
        
        Log.d("ManageUserAdapter", "Updating block value for " + userMobile + " to: " + blockValue);
        
        userRef.setValue(blockValue)
                .addOnSuccessListener(aVoid -> {
                    Log.d("ManageUserAdapter", "Successfully updated block value to: " + blockValue);
                    // Show toast with clear feedback about what was updated in database
                    String message = isBlocked 
                        ? "User blocked successfully. Database updated: block = yes" 
                        : "User unblocked successfully. Database updated: block = no";
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("ManageUserAdapter", "Error updating block value: " + e.getMessage());
                    Toast.makeText(context, "Failed to update block status. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class HolderClass extends RecyclerView.ViewHolder {
        TextView name, phone, id;
        ConstraintLayout container;
        Switch blockToggle;

        public HolderClass(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.user_name);
            phone = itemView.findViewById(R.id.user_phone);
            id = itemView.findViewById(R.id.user_id);
            container = itemView.findViewById(R.id.container);
            
            // Try to find toggle initially
            findBlockToggle(itemView);
        }
        
        void findBlockToggle() {
            findBlockToggle(itemView);
        }
        
        private void findBlockToggle(View rootView) {
            // Debug: List all children in container
            if (container != null && container instanceof ViewGroup) {
                ViewGroup containerGroup = (ViewGroup) container;
                int childCount = containerGroup.getChildCount();
                Log.d("ManageUserAdapter", "Container has " + childCount + " children:");
                for (int i = 0; i < childCount; i++) {
                    View child = containerGroup.getChildAt(i);
                    String childId = "NO_ID";
                    try {
                        if (child.getId() != View.NO_ID) {
                            childId = rootView.getResources().getResourceEntryName(child.getId());
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                    Log.d("ManageUserAdapter", "  Child " + i + ": " + child.getClass().getSimpleName() + " (id: " + childId + ")");
                }
            }
            
            // 1. Direct search from root
            try {
                blockToggle = rootView.findViewById(R.id.block_toggle);
                if (blockToggle != null) {
                    Log.d("ManageUserAdapter", "Found toggle from root view");
                    return;
                }
            } catch (Exception e) {
                Log.e("ManageUserAdapter", "Error finding toggle from root: " + e.getMessage());
            }
            
            // 2. Search from container if not found
            if (blockToggle == null && container != null) {
                try {
                    blockToggle = container.findViewById(R.id.block_toggle);
                    if (blockToggle != null) {
                        Log.d("ManageUserAdapter", "Found toggle from container");
                        return;
                    }
                } catch (Exception e) {
                    Log.e("ManageUserAdapter", "Error finding toggle from container: " + e.getMessage());
                }
            }
            
            // 3. Try finding by ID using getResources (bypass R class)
            if (blockToggle == null) {
                try {
                    int toggleId = rootView.getResources().getIdentifier("block_toggle", "id", 
                        rootView.getContext().getPackageName());
                    Log.d("ManageUserAdapter", "getIdentifier returned ID: " + toggleId);
                    if (toggleId != 0) {
                        blockToggle = rootView.findViewById(toggleId);
                        if (blockToggle != null) {
                            Log.d("ManageUserAdapter", "Found toggle using getIdentifier");
                            return;
                        }
                    } else {
                        Log.e("ManageUserAdapter", "getIdentifier returned 0 - R.id.block_toggle not found in resources!");
                    }
                } catch (Exception e) {
                    Log.e("ManageUserAdapter", "Error finding toggle by identifier: " + e.getMessage());
                }
            }
            
            // 4. Traverse entire view hierarchy as last resort
            if (blockToggle == null) {
                View switchView = findViewByType(rootView, Switch.class);
                if (switchView instanceof Switch) {
                    blockToggle = (Switch) switchView;
                    Log.d("ManageUserAdapter", "Found toggle using view traversal");
                }
            }
            
            // Log result
            if (blockToggle != null) {
                Log.d("ManageUserAdapter", "Successfully found block_toggle - " + blockToggle.getClass().getSimpleName());
            } else {
                Log.e("ManageUserAdapter", "block_toggle view not found! Root: " + 
                    (rootView != null ? rootView.getClass().getSimpleName() : "null") +
                    ", Container: " + (container != null ? container.getClass().getSimpleName() : "null"));
                
                // Last resort: Create toggle programmatically if container exists
                if (container != null) {
                    Log.w("ManageUserAdapter", "Creating toggle programmatically as fallback");
                    try {
                        blockToggle = new Switch(rootView.getContext());
                        blockToggle.setId(R.id.block_toggle);
                        blockToggle.setMinWidth((int) (56 * rootView.getResources().getDisplayMetrics().density));
                        blockToggle.setMinHeight((int) (48 * rootView.getResources().getDisplayMetrics().density));
                        blockToggle.setClickable(true);
                        blockToggle.setFocusable(true);
                        
                        // Add to container with constraints
                        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                            ConstraintLayout.LayoutParams.WRAP_CONTENT,
                            ConstraintLayout.LayoutParams.WRAP_CONTENT
                        );
                        params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
                        params.topToBottom = R.id.user_name;
                        params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
                        params.setMargins(0, (int)(4 * rootView.getResources().getDisplayMetrics().density), 
                            (int)(8 * rootView.getResources().getDisplayMetrics().density), 0);
                        blockToggle.setLayoutParams(params);
                        container.addView(blockToggle);
                        Log.d("ManageUserAdapter", "Successfully created toggle programmatically");
                    } catch (Exception e) {
                        Log.e("ManageUserAdapter", "Failed to create toggle programmatically: " + e.getMessage());
                    }
                }
            }
        }
        
        View findViewByType(View view, Class<?> type) {
            if (view == null) {
                return null;
            }
            
            // Check if this view matches
            if (type.isInstance(view)) {
                // Check if it has the correct ID
                try {
                    if (view.getId() != View.NO_ID) {
                        String resourceName = view.getResources().getResourceEntryName(view.getId());
                        if ("block_toggle".equals(resourceName)) {
                            Log.d("ManageUserAdapter", "Found block_toggle by ID: " + resourceName);
                            return view;
                        }
                    }
                } catch (Exception e) {
                    // If ID check fails, continue
                }
                // If no ID match but it's a Switch, check if it's the only one or return it
                // For now, return any Switch found
                Log.d("ManageUserAdapter", "Found Switch view (may not be block_toggle)");
                return view;
            }
            
            // Recursively search children
            if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                int childCount = group.getChildCount();
                Log.d("ManageUserAdapter", "Searching in " + view.getClass().getSimpleName() + " with " + childCount + " children");
                for (int i = 0; i < childCount; i++) {
                    View child = group.getChildAt(i);
                    View result = findViewByType(child, type);
                    if (result != null) {
                        return result;
                    }
                }
            }
            return null;
        }
    }
}