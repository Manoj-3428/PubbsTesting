package in.pubbs.pubbsadmin;

import android.content.Context;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class ReassignHelper {

    public interface Callback {
        void run();
    }

    private static final String TAG = "ReassignHelper";

    public static void reassignBicycleToStation(Context context,
                                                String org,
                                                String bicycleId,
                                                String newStationId,
                                                Callback onSuccess,
                                                Callback onFailure) {
        try {
            DatabaseReference orgRef = FirebaseDatabase.getInstance().getReference(org);
            DatabaseReference bikeRef = orgRef.child("Bicycle").child(bicycleId);

            bikeRef.get().addOnSuccessListener(bikeSnap -> {
                if (!bikeSnap.exists()) {
                    Log.e(TAG, "Bicycle not found: " + bicycleId);
                    if (onFailure != null) onFailure.run();
                    return;
                }

                String oldStationId = bikeSnap.child("inStationId").getValue(String.class);

                // Update bicycle assignment and cycleStatus
                Map<String, Object> bikeUpdates = new HashMap<>();
                bikeUpdates.put("inStationId", newStationId);
                bikeUpdates.put("cycleStatus", "active"); // Update cycleStatus to active after reassignment
                bikeRef.updateChildren(bikeUpdates);
                Log.d(TAG, "Updated bicycle " + bicycleId + " - inStationId: " + newStationId + ", cycleStatus: active");

                // Update cycleStatus in report collection
                updateCycleStatusInReports(orgRef, bicycleId, () -> {
                    // Adjust counts (best-effort)
                    adjustStationCounts(orgRef, oldStationId, newStationId, onSuccess, onFailure);
                }, onFailure);
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to fetch bicycle", e);
                if (onFailure != null) onFailure.run();
            });
        } catch (Exception e) {
            Log.e(TAG, "reassignBicycleToStation error", e);
            if (onFailure != null) onFailure.run();
        }
    }

    private static void updateCycleStatusInReports(DatabaseReference orgRef,
                                                   String bicycleId,
                                                   Runnable onComplete,
                                                   Callback onFailure) {
        Log.d(TAG, "=== Updating cycleStatus in reports for bicycle: " + bicycleId + " ===");
        
        DatabaseReference reportCollectionRef = orgRef.child("ReportCycleCollection");
        DatabaseReference reportCycleRef = orgRef.child("ReportCycle");
        
        // Try ReportCycleCollection first
        reportCollectionRef.get().addOnSuccessListener(collectionSnap -> {
            Map<String, Object> updates = new HashMap<>();
            boolean foundAny = false;
            
            if (collectionSnap.exists()) {
                for (DataSnapshot reportSnap : collectionSnap.getChildren()) {
                    String reportKey = reportSnap.getKey();
                    
                    // Check in Report child (nested structure)
                    DataSnapshot reportData = reportSnap.child("Report");
                    if (reportData.exists()) {
                        String cycleId = getStringFromSnapshot(reportData, "CycleId", "cycleId", "bicycleId", "BicycleId");
                        if (bicycleId.equalsIgnoreCase(cycleId)) {
                            // Update CycleStatus in Report (based on screenshot: ReportCycleCollection/{key}/Report/CycleStatus)
                            updates.put(reportKey + "/Report/CycleStatus", "active");
                            foundAny = true;
                            Log.d(TAG, "Found matching report in ReportCycleCollection (nested): " + reportKey);
                        }
                    } else {
                        // Check flat structure (legacy ReportCycle)
                        String cycleId = getStringFromSnapshot(reportSnap, "CycleId", "cycleId", "bicycleId", "BicycleId");
                        if (bicycleId.equalsIgnoreCase(cycleId)) {
                            // Update both for legacy structure
                            updates.put(reportKey + "/CycleStatus", "active");
                            updates.put(reportKey + "/cycleStatus", "active");
                            foundAny = true;
                            Log.d(TAG, "Found matching report in ReportCycleCollection (flat): " + reportKey);
                        }
                    }
                }
            }
            
            if (!updates.isEmpty()) {
                reportCollectionRef.updateChildren(updates)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Successfully updated " + updates.size() + " report(s) in ReportCycleCollection");
                            if (onComplete != null) onComplete.run();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to update reports in ReportCycleCollection", e);
                            // Try ReportCycle as fallback
                            tryReportCycle(reportCycleRef, bicycleId, onComplete, onFailure);
                        });
            } else {
                // No matches in ReportCycleCollection, try ReportCycle
                tryReportCycle(reportCycleRef, bicycleId, onComplete, onFailure);
            }
        }).addOnFailureListener(e -> {
            Log.w(TAG, "ReportCycleCollection not found or error, trying ReportCycle", e);
            tryReportCycle(reportCycleRef, bicycleId, onComplete, onFailure);
        });
    }

    private static void tryReportCycle(DatabaseReference reportCycleRef,
                                       String bicycleId,
                                       Runnable onComplete,
                                       Callback onFailure) {
        reportCycleRef.get().addOnSuccessListener(cycleSnap -> {
            Map<String, Object> updates = new HashMap<>();
            boolean foundAny = false;
            
            if (cycleSnap.exists()) {
                for (DataSnapshot reportSnap : cycleSnap.getChildren()) {
                    String reportKey = reportSnap.getKey();
                    String cycleId = getStringFromSnapshot(reportSnap, "CycleId", "cycleId", "bicycleId", "BicycleId");
                    if (bicycleId.equalsIgnoreCase(cycleId)) {
                        // Update both for ReportCycle (legacy structure)
                        updates.put(reportKey + "/CycleStatus", "active");
                        updates.put(reportKey + "/cycleStatus", "active");
                        foundAny = true;
                        Log.d(TAG, "Found matching report in ReportCycle: " + reportKey);
                    }
                }
            }
            
            if (!updates.isEmpty()) {
                reportCycleRef.updateChildren(updates)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Successfully updated " + updates.size() + " report(s) in ReportCycle");
                            if (onComplete != null) onComplete.run();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to update reports in ReportCycle", e);
                            // Still continue with reassignment even if report update fails
                            if (onComplete != null) onComplete.run();
                        });
            } else {
                Log.d(TAG, "No matching reports found for bicycle: " + bicycleId);
                if (onComplete != null) onComplete.run();
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to fetch ReportCycle", e);
            // Still continue with reassignment even if report update fails
            if (onComplete != null) onComplete.run();
        });
    }

    private static String getStringFromSnapshot(DataSnapshot snapshot, String... keys) {
        for (String key : keys) {
            DataSnapshot child = snapshot.child(key);
            if (child.exists()) {
                Object value = child.getValue();
                if (value != null) {
                    return String.valueOf(value);
                }
            }
        }
        return null;
    }

    private static void adjustStationCounts(DatabaseReference orgRef,
                                            String oldStationId,
                                            String newStationId,
                                            Callback onSuccess,
                                            Callback onFailure) {
        DatabaseReference stationRoot = orgRef.child("Station");

        Log.d(TAG, "=== Adjusting Station Counts ===");
        Log.d(TAG, "Old Station ID: " + (oldStationId != null ? oldStationId : "NULL"));
        Log.d(TAG, "New Station ID: " + newStationId);

        // Increment new station count
        DatabaseReference newCountRef = stationRoot.child(newStationId).child("stationCycleCount");
        Log.d(TAG, "Incrementing count for station: " + newStationId);
        
        newCountRef.get().addOnSuccessListener(snap -> {
            long current = 0L;
            if (snap.exists()) {
                try {
                    Object value = snap.getValue();
                    if (value instanceof Number) {
                        current = ((Number) value).longValue();
                    } else {
                        current = Long.parseLong(String.valueOf(value));
                    }
                    Log.d(TAG, "Current count for new station: " + current);
                } catch (Exception e) {
                    Log.w(TAG, "Failed to parse current count: " + snap.getValue(), e);
                }
            } else {
                Log.d(TAG, "stationCycleCount does not exist, starting from 0");
            }
            
            long newCount = current + 1;
            Log.d(TAG, "Setting new station count: " + current + " -> " + newCount);
            newCountRef.setValue(newCount).addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Successfully incremented new station count to: " + newCount);
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to set new station count", e);
            });

            // Decrement old station count (if exists and not same)
            if (oldStationId != null && !oldStationId.isEmpty() && !oldStationId.equals(newStationId)) {
                DatabaseReference oldCountRef = stationRoot.child(oldStationId).child("stationCycleCount");
                Log.d(TAG, "Decrementing count for old station: " + oldStationId);
                
                oldCountRef.get().addOnSuccessListener(oldSnap -> {
                    long oldCurrent = 0L;
                    if (oldSnap.exists()) {
                        try {
                            Object value = oldSnap.getValue();
                            if (value instanceof Number) {
                                oldCurrent = ((Number) value).longValue();
                            } else {
                                oldCurrent = Long.parseLong(String.valueOf(value));
                            }
                            Log.d(TAG, "Current count for old station: " + oldCurrent);
                        } catch (Exception e) {
                            Log.w(TAG, "Failed to parse old station count: " + oldSnap.getValue(), e);
                        }
                    } else {
                        Log.d(TAG, "Old station count does not exist");
                    }
                    
                    long newVal = Math.max(0L, oldCurrent - 1);
                    Log.d(TAG, "Setting old station count: " + oldCurrent + " -> " + newVal);
                    oldCountRef.setValue(newVal).addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Successfully decremented old station count to: " + newVal);
                        if (onSuccess != null) onSuccess.run();
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to set old station count", e);
                        if (onSuccess != null) onSuccess.run(); // assignment succeeded, count adjust partial
                    });
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get old station count", e);
                    if (onSuccess != null) onSuccess.run(); // assignment succeeded, count adjust partial
                });
            } else {
                Log.d(TAG, "No old station to decrement (oldStationId: " + oldStationId + ")");
                if (onSuccess != null) onSuccess.run();
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to get new station count", e);
            if (onFailure != null) onFailure.run();
        });
    }
}

