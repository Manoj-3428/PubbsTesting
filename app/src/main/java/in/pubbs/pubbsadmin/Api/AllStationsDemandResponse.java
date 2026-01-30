package in.pubbs.pubbsadmin.Api;

import java.util.Map;

/**
 * Response model for fetching all stations' cycle demand at once
 */
public class AllStationsDemandResponse {
    private Map<String, Integer> demands; // stationId -> demand value
    private boolean success;
    private String message;
    
    public AllStationsDemandResponse() {
    }
    
    public AllStationsDemandResponse(Map<String, Integer> demands, boolean success, String message) {
        this.demands = demands;
        this.success = success;
        this.message = message;
    }
    
    public Map<String, Integer> getDemands() {
        return demands;
    }
    
    public void setDemands(Map<String, Integer> demands) {
        this.demands = demands;
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
