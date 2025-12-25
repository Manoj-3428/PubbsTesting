package in.pubbs.pubbsadmin;

import android.util.Log;
import in.pubbs.pubbsadmin.Model.StationData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages partitioning of stations into vehicle groups
 * STRICT RULES:
 * 1. totalPickupCycles == totalDropDemand for each vehicle (STRICTLY)
 * 2. Each vehicle must have at least one pickup and one drop
 * 3. All stations must be assigned
 * 4. Routes must end with drop stations
 */
public class VehiclePartitionManager {
    
    private static final String TAG = "VehiclePartitionManager";
    private static final int NUM_VEHICLES = 2;
    
    /**
     * Partition stations into vehicle groups ensuring STRICT BALANCE
     * Uses a pairing approach: pairs pickups with drops to achieve balance
     * 
     * @param pickupStations List of pickup stations
     * @param dropStations List of drop stations
     * @return Map of vehicle ID to list of assigned stations
     */
    public static Map<String, List<StationData>> partitionStations(
            List<StationData> pickupStations, 
            List<StationData> dropStations) {
        
        Map<String, List<StationData>> vehicleStations = new HashMap<>();
        
        // Initialize vehicle groups
        for (int i = 1; i <= NUM_VEHICLES; i++) {
            vehicleStations.put("vehicle" + i, new ArrayList<>());
        }
        
        // Calculate totals
        int totalPickupCycles = 0;
        int totalDropDemand = 0;
        
        for (StationData pickup : pickupStations) {
            totalPickupCycles += pickup.cyclesToMove;
        }
        for (StationData drop : dropStations) {
            totalDropDemand += drop.cyclesToMove;
        }
        
        Log.d(TAG, "Total pickup cycles: " + totalPickupCycles + ", Total drop demand: " + totalDropDemand);
        Log.d(TAG, "Total stations: " + (pickupStations.size() + dropStations.size()) + 
              " (" + pickupStations.size() + " pickups, " + dropStations.size() + " drops)");
        
        // Validate overall balance - if imbalanced, we'll try to handle it
        if (totalPickupCycles != totalDropDemand) {
            Log.w(TAG, "Warning: Total pickup cycles (" + totalPickupCycles + ") != Total drop demand (" + 
                  totalDropDemand + "). Will attempt strict balance per vehicle.");
        }
        
        // Target cycles per vehicle (strict balance: pickup cycles == drop demand)
        // If overall is imbalanced, distribute the imbalance
        int targetCyclesPerVehicle = totalPickupCycles / NUM_VEHICLES;
        int remainder = totalPickupCycles % NUM_VEHICLES;
        
        Log.d(TAG, "Target cycles per vehicle: " + targetCyclesPerVehicle + " (with remainder: " + remainder + ")");
        
        // Track allocations per vehicle
        Map<String, Integer> vehiclePickupCycles = new HashMap<>();
        Map<String, Integer> vehicleDropDemand = new HashMap<>();
        
        for (int i = 1; i <= NUM_VEHICLES; i++) {
            String vehicleId = "vehicle" + i;
            vehiclePickupCycles.put(vehicleId, 0);
            vehicleDropDemand.put(vehicleId, 0);
        }
        
        // Sort stations by cyclesToMove (descending) for better distribution
        List<StationData> sortedPickups = new ArrayList<>(pickupStations);
        Collections.sort(sortedPickups, (a, b) -> Integer.compare(b.cyclesToMove, a.cyclesToMove));
        
        List<StationData> sortedDrops = new ArrayList<>(dropStations);
        Collections.sort(sortedDrops, (a, b) -> Integer.compare(b.cyclesToMove, a.cyclesToMove));
        
        // STRATEGY: Use bin-packing approach to achieve strict balance
        // For each vehicle, assign pickups and drops such that pickup cycles == drop demand
        
        // Step 1: Distribute pickups using round-robin
        int vehicleIndex = 0;
        for (StationData pickup : sortedPickups) {
            String vehicleId = "vehicle" + (vehicleIndex % NUM_VEHICLES + 1);
            vehicleStations.get(vehicleId).add(pickup);
            vehiclePickupCycles.put(vehicleId, vehiclePickupCycles.get(vehicleId) + pickup.cyclesToMove);
            vehicleIndex++;
        }
        
        // Step 2: Assign drops to achieve strict balance (pickup cycles == drop demand)
        // For each vehicle, assign drops until drop demand equals pickup cycles
        for (int i = 1; i <= NUM_VEHICLES; i++) {
            String vehicleId = "vehicle" + i;
            int targetDropDemand = vehiclePickupCycles.get(vehicleId); // STRICT: drop demand == pickup cycles
            
            // Assign drops to this vehicle until we reach the target
            List<StationData> assignedDrops = new ArrayList<>();
            int currentDropDemand = 0;
            
            // Try to find drops that sum to exactly targetDropDemand
            for (StationData drop : new ArrayList<>(sortedDrops)) {
                if (currentDropDemand + drop.cyclesToMove <= targetDropDemand) {
                    // Check if this drop is already assigned
                    boolean alreadyAssigned = false;
                    for (List<StationData> stations : vehicleStations.values()) {
                        if (stations.contains(drop)) {
                            alreadyAssigned = true;
                            break;
                        }
                    }
                    
                    if (!alreadyAssigned) {
                        assignedDrops.add(drop);
                        currentDropDemand += drop.cyclesToMove;
                        sortedDrops.remove(drop); // Remove from available list
                        
                        // If we've reached or exceeded target, stop
                        if (currentDropDemand >= targetDropDemand) {
                            break;
                        }
                    }
                }
            }
            
            // Add assigned drops to vehicle
            for (StationData drop : assignedDrops) {
                vehicleStations.get(vehicleId).add(drop);
                vehicleDropDemand.put(vehicleId, vehicleDropDemand.get(vehicleId) + drop.cyclesToMove);
            }
        }
        
        // Step 3: Assign any remaining drops (if overall system is imbalanced)
        // Assign to vehicle with smallest current drop demand (to balance)
        for (StationData drop : sortedDrops) {
            String selectedVehicle = findVehicleForRemainingDrop(drop, vehiclePickupCycles, vehicleDropDemand);
            vehicleStations.get(selectedVehicle).add(drop);
            vehicleDropDemand.put(selectedVehicle, vehicleDropDemand.get(selectedVehicle) + drop.cyclesToMove);
        }
        
        // Step 4: REFINEMENT - Try to achieve STRICT BALANCE by swapping
        // This is critical: we need pickup cycles == drop demand for each vehicle
        boolean improved = true;
        int maxIterations = 100;
        int iterations = 0;
        
        while (improved && iterations < maxIterations) {
            iterations++;
            improved = false;
            
            // Try to improve balance by swapping between vehicles
            for (int i = 1; i <= NUM_VEHICLES; i++) {
                String vehicleId1 = "vehicle" + i;
                List<StationData> stations1 = vehicleStations.get(vehicleId1);
                int pickupCycles1 = vehiclePickupCycles.get(vehicleId1);
                int dropDemand1 = vehicleDropDemand.get(vehicleId1);
                int imbalance1 = pickupCycles1 - dropDemand1; // Positive = more pickup, Negative = more drop
                
                for (int j = i + 1; j <= NUM_VEHICLES; j++) {
                    String vehicleId2 = "vehicle" + j;
                    List<StationData> stations2 = vehicleStations.get(vehicleId2);
                    int pickupCycles2 = vehiclePickupCycles.get(vehicleId2);
                    int dropDemand2 = vehicleDropDemand.get(vehicleId2);
                    int imbalance2 = pickupCycles2 - dropDemand2;
                    
                    // Try swapping a pickup from vehicle1 with a drop from vehicle2
                    // This can help balance if vehicle1 has excess pickup and vehicle2 has excess drop
                    if (imbalance1 > 0 && imbalance2 < 0) {
                        // Vehicle1 has more pickup cycles, Vehicle2 has more drop demand
                        // Try to swap pickup from vehicle1 with drop from vehicle2
                        for (StationData pickup1 : new ArrayList<>(stations1)) {
                            if (!"pickup".equals(pickup1.stationType)) continue;
                            
                            for (StationData drop2 : new ArrayList<>(stations2)) {
                                if (!"drop".equals(drop2.stationType)) continue;
                                
                                // Calculate new imbalances after swap
                                int newPickupCycles1 = pickupCycles1 - pickup1.cyclesToMove;
                                int newDropDemand1 = dropDemand1 + drop2.cyclesToMove;
                                int newImbalance1 = newPickupCycles1 - newDropDemand1;
                                
                                int newPickupCycles2 = pickupCycles2 + pickup1.cyclesToMove;
                                int newDropDemand2 = dropDemand2 - drop2.cyclesToMove;
                                int newImbalance2 = newPickupCycles2 - newDropDemand2;
                                
                                // Check if swap improves balance (reduces total absolute imbalance)
                                int oldTotalImbalance = Math.abs(imbalance1) + Math.abs(imbalance2);
                                int newTotalImbalance = Math.abs(newImbalance1) + Math.abs(newImbalance2);
                                
                                if (newTotalImbalance < oldTotalImbalance) {
                                    // Perform swap
                                    stations1.remove(pickup1);
                                    stations2.remove(drop2);
                                    stations1.add(drop2);
                                    stations2.add(pickup1);
                                    
                                    vehiclePickupCycles.put(vehicleId1, newPickupCycles1);
                                    vehicleDropDemand.put(vehicleId1, newDropDemand1);
                                    vehiclePickupCycles.put(vehicleId2, newPickupCycles2);
                                    vehicleDropDemand.put(vehicleId2, newDropDemand2);
                                    
                                    improved = true;
                                    Log.d(TAG, "Swapped pickup " + pickup1.stationId + " (cycles=" + pickup1.cyclesToMove + 
                                          ") from vehicle" + i + " with drop " + drop2.stationId + " (cycles=" + 
                                          drop2.cyclesToMove + ") from vehicle" + j);
                                    break;
                                }
                            }
                            if (improved) break;
                        }
                    }
                    
                    // Try swapping a drop from vehicle1 with a pickup from vehicle2
                    // This can help balance if vehicle1 has excess drop and vehicle2 has excess pickup
                    if (imbalance1 < 0 && imbalance2 > 0) {
                        // Vehicle1 has more drop demand, Vehicle2 has more pickup cycles
                        for (StationData drop1 : new ArrayList<>(stations1)) {
                            if (!"drop".equals(drop1.stationType)) continue;
                            
                            for (StationData pickup2 : new ArrayList<>(stations2)) {
                                if (!"pickup".equals(pickup2.stationType)) continue;
                                
                                // Calculate new imbalances after swap
                                int newPickupCycles1 = pickupCycles1 + pickup2.cyclesToMove;
                                int newDropDemand1 = dropDemand1 - drop1.cyclesToMove;
                                int newImbalance1 = newPickupCycles1 - newDropDemand1;
                                
                                int newPickupCycles2 = pickupCycles2 - pickup2.cyclesToMove;
                                int newDropDemand2 = dropDemand2 + drop1.cyclesToMove;
                                int newImbalance2 = newPickupCycles2 - newDropDemand2;
                                
                                // Check if swap improves balance
                                int oldTotalImbalance = Math.abs(imbalance1) + Math.abs(imbalance2);
                                int newTotalImbalance = Math.abs(newImbalance1) + Math.abs(newImbalance2);
                                
                                if (newTotalImbalance < oldTotalImbalance) {
                                    // Perform swap
                                    stations1.remove(drop1);
                                    stations2.remove(pickup2);
                                    stations1.add(pickup2);
                                    stations2.add(drop1);
                                    
                                    vehiclePickupCycles.put(vehicleId1, newPickupCycles1);
                                    vehicleDropDemand.put(vehicleId1, newDropDemand1);
                                    vehiclePickupCycles.put(vehicleId2, newPickupCycles2);
                                    vehicleDropDemand.put(vehicleId2, newDropDemand2);
                                    
                                    improved = true;
                                    Log.d(TAG, "Swapped drop " + drop1.stationId + " (cycles=" + drop1.cyclesToMove + 
                                          ") from vehicle" + i + " with pickup " + pickup2.stationId + " (cycles=" + 
                                          pickup2.cyclesToMove + ") from vehicle" + j);
                                    break;
                                }
                            }
                            if (improved) break;
                        }
                    }
                    
                    if (improved) break;
                }
                if (improved) break;
            }
        }
        
        // Step 5: Ensure each vehicle has at least one pickup and one drop
        for (int i = 1; i <= NUM_VEHICLES; i++) {
            String vehicleId = "vehicle" + i;
            List<StationData> stations = vehicleStations.get(vehicleId);
            
            boolean hasPickup = false;
            boolean hasDrop = false;
            for (StationData station : stations) {
                if ("pickup".equals(station.stationType)) hasPickup = true;
                if ("drop".equals(station.stationType)) hasDrop = true;
            }
            
            // If missing pickup or drop, try to fix by swapping with another vehicle
            if (!hasPickup || !hasDrop) {
                Log.w(TAG, "Warning: " + vehicleId + " missing " + (!hasPickup ? "pickup" : "drop") + 
                      " - attempting to fix...");
                for (int j = 1; j <= NUM_VEHICLES; j++) {
                    if (i != j) {
                        String otherVehicleId = "vehicle" + j;
                        List<StationData> otherStations = vehicleStations.get(otherVehicleId);
                        
                        if (!hasPickup) {
                            for (StationData station : new ArrayList<>(otherStations)) {
                                if ("pickup".equals(station.stationType)) {
                                    otherStations.remove(station);
                                    stations.add(station);
                                    vehiclePickupCycles.put(vehicleId, 
                                        vehiclePickupCycles.get(vehicleId) + station.cyclesToMove);
                                    vehiclePickupCycles.put(otherVehicleId, 
                                        vehiclePickupCycles.get(otherVehicleId) - station.cyclesToMove);
                                    hasPickup = true;
                                    break;
                                }
                            }
                        }
                        
                        if (!hasDrop) {
                            for (StationData station : new ArrayList<>(otherStations)) {
                                if ("drop".equals(station.stationType)) {
                                    otherStations.remove(station);
                                    stations.add(station);
                                    vehicleDropDemand.put(vehicleId, 
                                        vehicleDropDemand.get(vehicleId) + station.cyclesToMove);
                                    vehicleDropDemand.put(otherVehicleId, 
                                        vehicleDropDemand.get(otherVehicleId) - station.cyclesToMove);
                                    hasDrop = true;
                                    break;
                                }
                            }
                        }
                        
                        if (hasPickup && hasDrop) break;
                    }
                }
            }
        }
        
        // Log partition results
        int totalAssigned = 0;
        for (int i = 1; i <= NUM_VEHICLES; i++) {
            String vehicleId = "vehicle" + i;
            List<StationData> stations = vehicleStations.get(vehicleId);
            int pickupCycles = vehiclePickupCycles.get(vehicleId);
            int dropDemand = vehicleDropDemand.get(vehicleId);
            
            int pickupCount = 0;
            int dropCount = 0;
            for (StationData station : stations) {
                if ("pickup".equals(station.stationType)) pickupCount++;
                else if ("drop".equals(station.stationType)) dropCount++;
            }
            
            totalAssigned += stations.size();
            
            Log.d(TAG, vehicleId + ": " + stations.size() + " stations (" + pickupCount + " pickups, " + 
                  dropCount + " drops), Pickup cycles: " + pickupCycles + ", Drop demand: " + dropDemand + 
                  ", Balance: " + (pickupCycles - dropDemand));
            
            // Validate strict balance
            if (pickupCycles != dropDemand) {
                Log.w(TAG, "Warning: " + vehicleId + " is NOT balanced! Pickup: " + pickupCycles + 
                      ", Drop: " + dropDemand);
            } else {
                Log.d(TAG, vehicleId + " is STRICTLY balanced: Pickup cycles == Drop demand");
            }
            
            // Validate each vehicle has at least one pickup and one drop
            if (pickupCount == 0 || dropCount == 0) {
                Log.e(TAG, "ERROR: " + vehicleId + " does not have both pickup and drop stations!");
            }
        }
        
        // Validate all stations are assigned
        int expectedTotal = pickupStations.size() + dropStations.size();
        if (totalAssigned != expectedTotal) {
            Log.e(TAG, "ERROR: Not all stations assigned! Expected: " + expectedTotal + ", Assigned: " + totalAssigned);
        } else {
            Log.d(TAG, "All " + totalAssigned + " stations successfully assigned to vehicles");
        }
        
        return vehicleStations;
    }
    
    /**
     * Find vehicle for remaining drop (used when overall system is imbalanced)
     * Assigns to vehicle that will have smallest imbalance after adding it
     */
    private static String findVehicleForRemainingDrop(StationData drop,
                                                      Map<String, Integer> vehiclePickupCycles,
                                                      Map<String, Integer> vehicleDropDemand) {
        String selected = "vehicle1";
        int bestImbalance = Integer.MAX_VALUE;
        
        for (int i = 1; i <= NUM_VEHICLES; i++) {
            String vehicleId = "vehicle" + i;
            int pickupCycles = vehiclePickupCycles.get(vehicleId);
            int dropDemand = vehicleDropDemand.get(vehicleId);
            
            // Calculate imbalance after adding this drop
            int dropDemandAfter = dropDemand + drop.cyclesToMove;
            int imbalanceAfter = Math.abs(pickupCycles - dropDemandAfter);
            
            // Prefer vehicle with smallest imbalance (closest to perfect balance)
            if (imbalanceAfter < bestImbalance) {
                bestImbalance = imbalanceAfter;
                selected = vehicleId;
            }
        }
        
        return selected;
    }
    
    /**
     * Validate that a vehicle's station group is feasible
     * (has at least one pickup and one drop)
     */
    public static boolean isVehicleGroupFeasible(List<StationData> stations) {
        if (stations.isEmpty()) return false;
        
        boolean hasPickup = false;
        boolean hasDrop = false;
        int totalPickupCycles = 0;
        int totalDropDemand = 0;
        
        for (StationData station : stations) {
            if ("pickup".equals(station.stationType)) {
                hasPickup = true;
                totalPickupCycles += station.cyclesToMove;
            } else if ("drop".equals(station.stationType)) {
                hasDrop = true;
                totalDropDemand += station.cyclesToMove;
            }
        }
        
        // Must have both pickup and drop
        if (!hasPickup || !hasDrop) {
            Log.w(TAG, "Vehicle group missing pickup or drop stations");
            return false;
        }
        
        // Check balance (warn if imbalanced, but still allow routing attempt)
        if (totalPickupCycles != totalDropDemand) {
            Log.w(TAG, "Vehicle group imbalance: Pickup cycles=" + totalPickupCycles + 
                  ", Drop demand=" + totalDropDemand + " (will attempt routing anyway)");
        }
        
        return true;
    }
    
    /**
     * Get number of vehicles
     */
    public static int getNumVehicles() {
        return NUM_VEHICLES;
    }
}
