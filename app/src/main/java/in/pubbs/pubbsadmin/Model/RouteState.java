package in.pubbs.pubbsadmin.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks vehicle capacity state during route construction
 */
public class RouteState {
    public List<StationData> route;
    public int cyclesHolding; // Current cycles in vehicle
    public int vehicleCapacity; // Remaining space in vehicle (MAX_CAPACITY - cyclesHolding)
    
    private static final int MAX_VEHICLE_CAPACITY = 4;
    
    public RouteState() {
        this.route = new ArrayList<>();
        this.cyclesHolding = 0;
        this.vehicleCapacity = MAX_VEHICLE_CAPACITY;
    }
    
    public RouteState(RouteState other) {
        this.route = new ArrayList<>(other.route);
        this.cyclesHolding = other.cyclesHolding;
        this.vehicleCapacity = other.vehicleCapacity;
    }
    
    // Check if we can visit a pickup station (enough space)
    public boolean canVisitPickup(StationData station) {
        return station.cyclesToMove <= vehicleCapacity;
    }
    
    // Check if we can visit a drop station (enough cycles to drop)
    // Condition: cyclesHolding >= cyclesToDrop
    public boolean canVisitDrop(StationData station) {
        int cyclesToDrop = station.cyclesToMove;
        return cyclesHolding >= cyclesToDrop;
    }
    
    // Visit a station and update vehicle state
    public void visitStation(StationData station) {
        route.add(station);
        if ("pickup".equals(station.stationType)) {
            // Pick cycles: increase cyclesHolding, decrease capacity
            cyclesHolding += station.cyclesToMove;
            vehicleCapacity -= station.cyclesToMove;
        } else if ("drop".equals(station.stationType)) {
            // Drop cycles: decrease cyclesHolding, increase capacity
            cyclesHolding -= station.cyclesToMove;
            vehicleCapacity += station.cyclesToMove;
        }
    }
    
    public static int getMaxVehicleCapacity() {
        return MAX_VEHICLE_CAPACITY;
    }
}

