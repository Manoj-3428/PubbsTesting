package in.pubbs.pubbsadmin.Api;

/**
 * Request model for updating cycle demand
 */
public class CycleDemandRequest {
    private int demand;
    
    public CycleDemandRequest(int demand) {
        this.demand = demand;
    }
    
    public int getDemand() {
        return demand;
    }
    
    public void setDemand(int demand) {
        this.demand = demand;
    }
}
