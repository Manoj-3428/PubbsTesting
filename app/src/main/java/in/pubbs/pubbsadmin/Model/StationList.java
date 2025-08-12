package in.pubbs.pubbsadmin.Model;

public class StationList {
    public String stationId;
    public boolean stationStatus;

    public StationList(String stationId, boolean stationStatus) {
        this.stationId = stationId;
        this.stationStatus = stationStatus;
    }

    public String getStationId() {
        return stationId;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId;
    }

    public boolean getStationStatus() {
        return stationStatus;
    }

    public void setStationStatus(boolean stationStatus) {
        this.stationStatus = stationStatus;
    }
}
