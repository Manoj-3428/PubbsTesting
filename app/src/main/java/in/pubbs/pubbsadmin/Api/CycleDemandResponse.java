package in.pubbs.pubbsadmin.Api;

/**
 * Response model for cycle demand API calls
 */
public class CycleDemandResponse {
    private String stationId;
    private int demand;
    private boolean success;
    private String message;
    
    public CycleDemandResponse() {
    }
    
    public CycleDemandResponse(String stationId, int demand, boolean success, String message) {
        this.stationId = stationId;
        this.demand = demand;
        this.success = success;
        this.message = message;
    }
    
    public String getStationId() {
        return stationId;
    }
    
    public void setStationId(String stationId) {
        this.stationId = stationId;
    }
    
    public int getDemand() {
        return demand;
    }
    
    public void setDemand(int demand) {
        this.demand = demand;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
