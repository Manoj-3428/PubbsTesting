package in.pubbs.pubbsadmin.Model;

/**
 * Model class representing a station with its location, cycle count, and demand
 */
public class StationData {
    public String stationId;
    public String stationName;
    public double latitude;
    public double longitude;
    public int cycleCount;
    public int cycleDemand;
    public String stationType; // "pickup", "drop", or "none"
    public int cyclesToMove; // positive for pickup, negative for drop
    
    public StationData(String stationId, String stationName, double latitude, double longitude, 
                       int cycleCount, int cycleDemand) {
        this.stationId = stationId;
        this.stationName = stationName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.cycleCount = cycleCount;
        this.cycleDemand = cycleDemand;
        
        // Classify station type
        if (cycleCount > cycleDemand) {
            this.stationType = "pickup";
            this.cyclesToMove = cycleCount - cycleDemand;
        } else if (cycleCount < cycleDemand) {
            this.stationType = "drop";
            this.cyclesToMove = cycleDemand - cycleCount;
        } else {
            this.stationType = "none";
            this.cyclesToMove = 0;
        }
    }
}

