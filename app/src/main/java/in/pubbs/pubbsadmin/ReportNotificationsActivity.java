package in.pubbs.pubbsadmin;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import in.pubbs.pubbsadmin.View.CustomLoader;

public class ReportNotificationsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private View noData;
    private CustomLoader loader;
    private SharedPreferences sp;
    private String adminId;
    private String org;

    private DatabaseReference reportNodeRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_notifications);

        sp = getSharedPreferences("pubbs", Context.MODE_PRIVATE);
        adminId = sp.getString("admin_id", "");
        org = Objects.requireNonNull(sp.getString("organisationName", "")).replaceAll(" ", "");

        ImageView back = findViewById(R.id.back_button);
        back.setOnClickListener(v -> finish());

        TextView title = findViewById(R.id.toolbar_title);
        title.setText("Reports");

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        noData = findViewById(R.id.no_data_found);

        loader = new CustomLoader(this, R.style.WideDialog);
        loader.show();

        loadReports();
    }

    private void loadReports() {
        DatabaseReference orgRef = FirebaseDatabase.getInstance().getReference(org);
        orgRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                DataSnapshot reportSnap = snapshot.child("ReportCycleCollection");
                if (!reportSnap.exists()) reportSnap = snapshot.child("ReportCycle");

                if (!reportSnap.exists() || reportSnap.getChildrenCount() == 0) {
                    loader.dismiss();
                    noData.setVisibility(View.VISIBLE);
                    return;
                }

                List<ReportItem> items = new ArrayList<>();
                int totalReports = 0;
                int parsedReports = 0;
                for (DataSnapshot child : reportSnap.getChildren()) {
                    totalReports++;
                    Log.d("ReportNotifications", "Processing report #" + totalReports + " with key: " + child.getKey());
                    ReportItem item = ReportItem.fromSnapshot(child);
                    if (item != null) {
                        parsedReports++;
                        items.add(item);
                        if (item.lat == 0d && item.lng == 0d) {
                            Log.w("ReportNotifications", "Report " + item.reportKey + " has no location data (0,0)");
                        }
                    } else {
                        Log.w("ReportNotifications", "Failed to parse report with key: " + child.getKey());
                    }
                }
                Log.d("ReportNotifications", "Loaded " + parsedReports + " out of " + totalReports + " reports");

                // Sort newest first (best-effort)
                items.sort((a, b) -> Long.compare(b.timestampMs, a.timestampMs));

                // Build sectioned list (Today / Yesterday / Older)
                List<Object> sectioned = buildSectioned(items);
                ReportNotificationsAdapter adapter = new ReportNotificationsAdapter(sectioned, reportItem -> {
                    Log.d("ReportNotifications", "=== Report item clicked ===");
                    Log.d("ReportNotifications", "reportKey: " + reportItem.reportKey);
                    Log.d("ReportNotifications", "bicycleId: '" + (reportItem.bicycleId != null ? reportItem.bicycleId : "NULL") + "' (empty: " + (reportItem.bicycleId == null || reportItem.bicycleId.isEmpty()) + ")");
                    Log.d("ReportNotifications", "issue: '" + (reportItem.issue != null ? reportItem.issue : "NULL") + "' (empty: " + (reportItem.issue == null || reportItem.issue.isEmpty()) + ")");
                    Log.d("ReportNotifications", "dateTimeRaw: '" + (reportItem.dateTimeRaw != null ? reportItem.dateTimeRaw : "NULL") + "' (empty: " + (reportItem.dateTimeRaw == null || reportItem.dateTimeRaw.isEmpty()) + ")");
                    Log.d("ReportNotifications", "lat: " + reportItem.lat);
                    Log.d("ReportNotifications", "lng: " + reportItem.lng);
                    
                    Intent intent = new Intent(ReportNotificationsActivity.this, ReportMapActivity.class);
                    intent.putExtra("reportKey", reportItem.reportKey);
                    // Only put extras if they're not empty
                    if (reportItem.bicycleId != null && !reportItem.bicycleId.isEmpty()) {
                        intent.putExtra("bicycleId", reportItem.bicycleId);
                    }
                    if (reportItem.issue != null && !reportItem.issue.isEmpty()) {
                        intent.putExtra("issue", reportItem.issue);
                    }
                    if (reportItem.dateTimeRaw != null && !reportItem.dateTimeRaw.isEmpty()) {
                        intent.putExtra("dateTime", reportItem.dateTimeRaw);
                    }
                    intent.putExtra("lat", reportItem.lat);
                    intent.putExtra("lng", reportItem.lng);
                    
                    Log.d("ReportNotifications", "Starting ReportMapActivity with intent extras");
                    Log.d("ReportNotifications", "Intent extras being passed:");
                    Bundle bundle = intent.getExtras();
                    if (bundle != null) {
                        for (String key : bundle.keySet()) {
                            Object value = bundle.get(key);
                            Log.d("ReportNotifications", "  " + key + " = " + (value != null ? value.toString() : "NULL"));
                        }
                    }
                    startActivity(intent);
                });
                recyclerView.setAdapter(adapter);

                markAsSeen(reportSnap, items);

                loader.dismiss();
                noData.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loader.dismiss();
                noData.setVisibility(View.VISIBLE);
            }
        });
    }

    private List<Object> buildSectioned(List<ReportItem> items) {
        List<Object> out = new ArrayList<>();
        String currentHeader = null;
        for (ReportItem item : items) {
            String header = sectionHeaderFor(item.timestampMs);
            if (!TextUtils.equals(currentHeader, header)) {
                currentHeader = header;
                out.add(header);
            }
            out.add(item);
        }
        return out;
    }

    private String sectionHeaderFor(long ts) {
        if (ts <= 0) return "Older";
        long now = System.currentTimeMillis();
        long days = (now - ts) / (24L * 60L * 60L * 1000L);
        if (days == 0) return "Today";
        if (days == 1) return "Yesterday";
        return "Older";
    }

    private void markAsSeen(DataSnapshot reportSnap, List<ReportItem> items) {
        if (!(adminId.equalsIgnoreCase("Area Manager") || adminId.equalsIgnoreCase("Zone Manager"))) return;

        Map<String, Object> updates = new HashMap<>();
        for (ReportItem item : items) {
            if (!item.isUnseenForRole(adminId)) continue;
            if (adminId.equalsIgnoreCase("Area Manager")) {
                updates.put(item.reportKey + "/seenByAreaManager", true);
            } else {
                updates.put(item.reportKey + "/seenByZoneManager", true);
            }
        }
        if (updates.isEmpty()) return;

        reportSnap.getRef().updateChildren(updates);
    }

    static class ReportItem {
        final String reportKey;
        final String bicycleId;
        final String issue;
        final String cycleStatus;
        final String dateTimeRaw;
        final long timestampMs;
        final double lat;
        final double lng;
        final Boolean seenByAreaManager;
        final Boolean seenByZoneManager;

        ReportItem(String reportKey,
                   String bicycleId,
                   String issue,
                   String cycleStatus,
                   String dateTimeRaw,
                   long timestampMs,
                   double lat,
                   double lng,
                   Boolean seenByAreaManager,
                   Boolean seenByZoneManager) {
            this.reportKey = reportKey;
            this.bicycleId = bicycleId;
            this.issue = issue;
            this.cycleStatus = cycleStatus;
            this.dateTimeRaw = dateTimeRaw;
            this.timestampMs = timestampMs;
            this.lat = lat;
            this.lng = lng;
            this.seenByAreaManager = seenByAreaManager;
            this.seenByZoneManager = seenByZoneManager;
        }

        boolean isUnseenForRole(String role) {
            if ("Area Manager".equalsIgnoreCase(role)) return seenByAreaManager == null || !seenByAreaManager;
            if ("Zone Manager".equalsIgnoreCase(role)) return seenByZoneManager == null || !seenByZoneManager;
            return false;
        }

        static ReportItem fromSnapshot(DataSnapshot snap) {
            try {
                String key = snap.getKey();
                if (key == null) return null;

                Log.d("ReportNotifications", "=== Parsing ReportItem from snapshot ===");
                Log.d("ReportNotifications", "Key: " + key);
                
                // Log all children to see what's actually in the snapshot
                Log.d("ReportNotifications", "Snapshot children (top level):");
                for (DataSnapshot child : snap.getChildren()) {
                    Log.d("ReportNotifications", "  " + child.getKey() + " (type: " + (child.getValue() != null ? child.getValue().getClass().getSimpleName() : "null") + ")");
                    if (child.hasChildren()) {
                        Log.d("ReportNotifications", "    Has children:");
                        for (DataSnapshot grandChild : child.getChildren()) {
                            Log.d("ReportNotifications", "      " + grandChild.getKey() + " = " + grandChild.getValue());
                        }
                    } else {
                        Log.d("ReportNotifications", "    Value: " + child.getValue());
                    }
                }

                // Check if data is nested under "Report" child (ReportCycleCollection structure)
                DataSnapshot reportData = snap.child("Report");
                DataSnapshot dataSource;
                if (reportData.exists()) {
                    // Data is nested under Report child
                    Log.d("ReportNotifications", "Found 'Report' child, using nested structure");
                    dataSource = reportData;
                } else {
                    // Legacy flat structure (ReportCycle)
                    Log.d("ReportNotifications", "No 'Report' child found, using flat structure");
                    dataSource = snap;
                }

                String bicycleId = getStringAny(dataSource, "CycleId", "cycleId", "bicycleId", "BicycleId");
                String issue = getStringAny(dataSource, "issue", "Issue", "problem", "Problem");
                String cycleStatus = getStringAny(dataSource, "CycleStatus", "cycleStatus", "status");
                String dateTime = getStringAny(dataSource, "DateTime", "dateTime", "datetime");

                Log.d("ReportNotifications", "Parsed values - bicycleId: " + (bicycleId != null && !bicycleId.isEmpty() ? bicycleId : "NULL/EMPTY"));
                Log.d("ReportNotifications", "Parsed values - issue: " + (issue != null && !issue.isEmpty() ? issue : "NULL/EMPTY"));
                Log.d("ReportNotifications", "Parsed values - cycleStatus: " + (cycleStatus != null && !cycleStatus.isEmpty() ? cycleStatus : "NULL/EMPTY"));
                Log.d("ReportNotifications", "Parsed values - dateTime: " + (dateTime != null && !dateTime.isEmpty() ? dateTime : "NULL/EMPTY"));

                double lat = 0d;
                double lng = 0d;
                
                // Check for location at snap level (same level as Report)
                // Try both lowercase and uppercase (case sensitivity issue)
                DataSnapshot loc = snap.child("location");
                if (!loc.exists()) {
                    loc = snap.child("Location"); // Try with capital L
                }
                
                Log.d("ReportNotifications", "=== Parsing Location ===");
                Log.d("ReportNotifications", "Checking snap.child('location').exists(): " + snap.child("location").exists());
                Log.d("ReportNotifications", "Checking snap.child('Location').exists(): " + snap.child("Location").exists());
                Log.d("ReportNotifications", "Final loc.exists(): " + loc.exists());
                
                if (loc.exists()) {
                    Log.d("ReportNotifications", "Found 'location' child, parsing coordinates...");
                    // Log all location child keys and values
                    Log.d("ReportNotifications", "Location child keys and values:");
                    for (DataSnapshot locChild : loc.getChildren()) {
                        Object value = locChild.getValue();
                        Log.d("ReportNotifications", "  " + locChild.getKey() + " = " + value + " (type: " + (value != null ? value.getClass().getSimpleName() : "null") + ")");
                    }
                    
                    // Try direct access first
                    Object latObj = loc.child("latitude").getValue();
                    Object lngObj = loc.child("longitude").getValue();
                    Log.d("ReportNotifications", "Direct access - latitude: " + latObj + ", longitude: " + lngObj);
                    
                    double rawLat = getDoubleAny(loc, "latitude", "lat");
                    double rawLng = getDoubleAny(loc, "longitude", "lng");
                    
                    Log.d("ReportNotifications", "Raw parsed - lat: " + rawLat + ", lng: " + rawLng);
                    
                    // Validate and potentially swap coordinates
                    // Latitude should be between -90 and 90
                    // Longitude should be between -180 and 180
                    if (Math.abs(rawLat) <= 90 && Math.abs(rawLng) <= 180) {
                        // Valid range, use as is
                        lat = rawLat;
                        lng = rawLng;
                        Log.d("ReportNotifications", "Coordinates in valid range - lat: " + lat + ", lng: " + lng);
                    } else if (Math.abs(rawLng) <= 90 && Math.abs(rawLat) <= 180) {
                        // Likely swapped - swap them
                        lat = rawLng;
                        lng = rawLat;
                        Log.w("ReportNotifications", "Coordinates appear swapped! Swapped - lat: " + lat + ", lng: " + lng);
                    } else {
                        // Use as is but log warning
                        lat = rawLat;
                        lng = rawLng;
                        Log.w("ReportNotifications", "Coordinates outside normal range - lat: " + lat + ", lng: " + lng);
                    }
                } else {
                    Log.d("ReportNotifications", "No 'location' child, checking flat structure...");
                    // legacy flat - check in dataSource too
                    double rawLat = getDoubleAny(dataSource, "latitude", "lat");
                    double rawLng = getDoubleAny(dataSource, "longitude", "lng");
                    if (rawLat == 0d && rawLng == 0d) {
                        // Try at snap level
                        rawLat = getDoubleAny(snap, "latitude", "lat");
                        rawLng = getDoubleAny(snap, "longitude", "lng");
                    }
                    
                    Log.d("ReportNotifications", "Raw parsed from flat - lat: " + rawLat + ", lng: " + rawLng);
                    
                    // Validate and potentially swap
                    if (Math.abs(rawLat) <= 90 && Math.abs(rawLng) <= 180) {
                        lat = rawLat;
                        lng = rawLng;
                        Log.d("ReportNotifications", "Coordinates in valid range - lat: " + lat + ", lng: " + lng);
                    } else if (Math.abs(rawLng) <= 90 && Math.abs(rawLat) <= 180) {
                        lat = rawLng;
                        lng = rawLat;
                        Log.w("ReportNotifications", "Coordinates appear swapped! Swapped - lat: " + lat + ", lng: " + lng);
                    } else {
                        lat = rawLat;
                        lng = rawLng;
                        Log.w("ReportNotifications", "Coordinates outside normal range - lat: " + lat + ", lng: " + lng);
                    }
                }
                
                Log.d("ReportNotifications", "Final coordinates - lat: " + lat + ", lng: " + lng);

                Boolean seenAm = snap.child("seenByAreaManager").getValue(Boolean.class);
                if (seenAm == null) seenAm = snap.child("seen_area_manager").getValue(Boolean.class);
                Boolean seenZm = snap.child("seenByZoneManager").getValue(Boolean.class);
                if (seenZm == null) seenZm = snap.child("seen_zone_manager").getValue(Boolean.class);

                long ts = parseTimestamp(dateTime);
                ReportItem item = new ReportItem(key, bicycleId, issue, cycleStatus, dateTime, ts, lat, lng, seenAm, seenZm);
                Log.d("ReportNotifications", "Created ReportItem - bicycleId: " + item.bicycleId + ", issue: " + item.issue + ", dateTimeRaw: " + item.dateTimeRaw + ", lat: " + item.lat + ", lng: " + item.lng);
                
                if (item.lat == 0d && item.lng == 0d) {
                    Log.e("ReportNotifications", "WARNING: ReportItem created with 0,0 coordinates! Location data is missing for report: " + key);
                }
                
                return item;
            } catch (Exception e) {
                Log.e("ReportNotifications", "ERROR parsing ReportItem from snapshot: " + e.getMessage(), e);
                return null;
            }
        }

        private static String getStringAny(DataSnapshot snap, String... keys) {
            for (String k : keys) {
                Object v = snap.child(k).getValue();
                if (v != null) {
                    String s = String.valueOf(v);
                    if (!TextUtils.isEmpty(s) && !"null".equalsIgnoreCase(s)) return s;
                }
            }
            return "";
        }

        private static double getDoubleAny(DataSnapshot snap, String... keys) {
            for (String k : keys) {
                DataSnapshot child = snap.child(k);
                if (child.exists()) {
                    Object v = child.getValue();
                    Log.d("ReportNotifications", "getDoubleAny checking key '" + k + "': exists=true, value=" + v + ", type=" + (v != null ? v.getClass().getSimpleName() : "null"));
                    if (v instanceof Number) {
                        double result = ((Number) v).doubleValue();
                        Log.d("ReportNotifications", "getDoubleAny found '" + k + "' as Number = " + result);
                        return result;
                    }
                    if (v != null) {
                        try {
                            double result = Double.parseDouble(String.valueOf(v));
                            Log.d("ReportNotifications", "getDoubleAny parsed '" + k + "' = " + result);
                            return result;
                        } catch (Exception e) {
                            Log.w("ReportNotifications", "getDoubleAny failed to parse '" + k + "' = " + v + ": " + e.getMessage());
                        }
                    } else {
                        Log.d("ReportNotifications", "getDoubleAny: '" + k + "' exists but value is null");
                    }
                } else {
                    Log.d("ReportNotifications", "getDoubleAny: '" + k + "' does not exist");
                }
            }
            Log.w("ReportNotifications", "getDoubleAny: No valid double found for keys: " + java.util.Arrays.toString(keys));
            return 0d;
        }

        private static long parseTimestamp(String raw) {
            if (TextUtils.isEmpty(raw)) return 0L;
            // screenshot format: 2026-01-20 16:04:55
            List<SimpleDateFormat> formats = new ArrayList<>();
            formats.add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US));
            formats.add(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US));
            formats.add(new SimpleDateFormat("dd MMMM yyyy", Locale.US));
            for (SimpleDateFormat f : formats) {
                try {
                    Date d = f.parse(raw);
                    if (d != null) return d.getTime();
                } catch (ParseException ignored) {
                }
            }
            return 0L;
        }
    }
}

