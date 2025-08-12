package in.pubbs.pubbsadmin.Model;

import com.google.android.gms.maps.model.LatLng;

import java.io.Serializable;
import java.util.List;

public class Station implements Serializable {
    String areaId, areaName, stationId, stationName, stationLatitude, stationLongitude;
    List<LatLng> markerList;
    String stationRadius, stationType, createDate, createdBy;
    boolean stationStatus;

    public Station() {
    }

    public Station(String stationName, String stationId, String stationLatitude, String stationLongitude, String stationRadius) {
        this.stationName = stationName;
        this.stationId = stationId;
        this.stationLatitude = stationLatitude;
        this.stationLongitude = stationLongitude;
        this.stationRadius = stationRadius;
    }

    public Station(String areaId, String areaName, List<LatLng> markerList, String stationId, String stationName, String stationLatitude, String stationLongitude,
                   String stationRadius, String stationType, boolean stationStatus, String createdBy, String createDate) {
        this.areaId = areaId;
        this.areaName = areaName;
        this.markerList = markerList;
        this.stationId = stationId;
        this.stationName = stationName;
        this.stationLatitude = stationLatitude;
        this.stationLongitude = stationLongitude;
        this.stationRadius = stationRadius;
        this.stationType = stationType;
        this.stationStatus = stationStatus;
        this.createdBy = createdBy;
        this.createDate = createDate;
    }

    public String getAreaId() {
        return areaId;
    }

    public void setAreaId(String areaId) {
        this.areaId = areaId;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public String getStationId() {
        return stationId;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public String getStationLatitude() {
        return stationLatitude;
    }

    public void setStationLatitude(String stationLatitude) {
        this.stationLatitude = stationLatitude;
    }

    public String getStationLongitude() {
        return stationLongitude;
    }

    public void setStationLongitude(String stationLongitude) {
        this.stationLongitude = stationLongitude;
    }

    public List<LatLng> getMarkerList() {
        return markerList;
    }

    public void setMarkerList(List<LatLng> markerList) {
        this.markerList = markerList;
    }

    public String getStationRadius() {
        return stationRadius;
    }

    public void setStationRadius(String stationRadius) {
        this.stationRadius = stationRadius;
    }

    public String getStationType() {
        return stationType;
    }

    public void setStationType(String stationType) {
        this.stationType = stationType;
    }

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public boolean isStationStatus() {
        return stationStatus;
    }

    public void setStationStatus(boolean stationStatus) {
        this.stationStatus = stationStatus;
    }
}
