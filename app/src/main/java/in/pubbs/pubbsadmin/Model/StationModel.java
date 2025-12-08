package in.pubbs.pubbsadmin.Model;

public class StationModel {
    private String stationName;
    private int stationCycleCount;
    private int stationCycleDemand;

    public StationModel() {
        // Needed for Firebase
    }

    public StationModel(String stationName, int stationCycleCount, int stationCycleDemand) {
        this.stationName = stationName;
        this.stationCycleCount = stationCycleCount;
        this.stationCycleDemand = stationCycleDemand;
    }

    public String getStationName() {
        return stationName;
    }

    public int getStationCycleCount() {
        return stationCycleCount;
    }

    public int getStationCycleDemand() {
        return stationCycleDemand;
    }

    public void setStationCycleCount(int count) {
        this.stationCycleCount = count;
    }
}
