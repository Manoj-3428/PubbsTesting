package in.pubbs.pubbsadmin;

import in.pubbs.pubbsadmin.Model.StationData;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class for validating routes
 */
public class RouteValidator {
    
    private static final int MAX_VEHICLE_CAPACITY = 4;
    
    /**
     * Validate if a route satisfies vehicle capacity constraints at each step
     * 
     * @param route The route to validate
     * @return true if route is valid, false otherwise
     */
    public static boolean isValidRoute(List<StationData> route) {
        if (route.isEmpty()) return false;
        
        // Route must start with a pickup station
        if (!"pickup".equals(route.get(0).stationType)) {
            return false;
        }
        
        // Simulate the route and check capacity constraints
        int cyclesHolding = 0;
        int vehicleCapacity = MAX_VEHICLE_CAPACITY;
        
        for (StationData station : route) {
            if ("pickup".equals(station.stationType)) {
                // Check if we have enough capacity to pick cycles
                if (station.cyclesToMove > vehicleCapacity) {
                    return false; // Not enough capacity
                }
                // Update vehicle state after picking
                cyclesHolding += station.cyclesToMove;
                vehicleCapacity -= station.cyclesToMove;
            } else if ("drop".equals(station.stationType)) {
                // Check if we have enough cycles to drop
                // Condition: cyclesHolding >= cyclesToDrop
                int cyclesToDrop = station.cyclesToMove;
                if (cyclesHolding < cyclesToDrop) {
                    return false; // Not enough cycles to drop
                }
                // Update vehicle state after dropping
                cyclesHolding -= cyclesToDrop;
                vehicleCapacity += cyclesToDrop;
            }
        }
        
        return true; // All constraints satisfied
    }
    
    /**
     * Check if a route visits all required stations
     * 
     * @param route The route to check
     * @param allStations List of all stations that must be visited
     * @return true if route visits all stations, false otherwise
     */
    public static boolean visitsAllStations(List<StationData> route, List<StationData> allStations) {
        if (route.size() < allStations.size()) {
            return false; // Route has fewer stations than required
        }
        
        Set<String> visitedStationIds = new HashSet<>();
        for (StationData station : route) {
            visitedStationIds.add(station.stationId);
        }
        
        for (StationData station : allStations) {
            if (!visitedStationIds.contains(station.stationId)) {
                return false; // At least one station not visited
            }
        }
        
        return true; // All stations visited
    }
    
    public static int getMaxVehicleCapacity() {
        return MAX_VEHICLE_CAPACITY;
    }
}

