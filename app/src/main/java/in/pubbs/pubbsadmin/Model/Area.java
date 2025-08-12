package in.pubbs.pubbsadmin.Model;
/*Created by: Parita Dey*/

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class Area {
    public String areaId, areaName, maximumRideTime, maxRideTimeExceedingFine, maxHoldTime;
    public String maxHoldTimeExceedingFine, serviceStartTime, serviceEndTime, serviceHourExceedingFine, trackBicycle;
    public String geofencingFine, customerServiceNumber, geofencingCondition, baseFareCondition, serviceCondition, subscriptionCondition, areaCondition, createdBy, createDate;
    public boolean areaStatus;
    List<LatLng> markerList;

    public Area() {
    }

    public Area(String areaId){
        this.areaId = areaId;
    }
    //for area node
    public Area(String areaId, String areaName,  List<LatLng> markerList, String maximumRideTime, String maxRideTimeExceedingFine, String maxHoldTime, String maxHoldTimeExceedingFine,
                String serviceStartTime, String serviceEndTime, String serviceHourExceedingFine, String geofencingFine, String customerServiceNumber,
                String geofencingCondition, String baseFareCondition, String serviceCondition, String subscriptionCondition, String areaCondition, boolean areaStatus, String createdBy, String createDate, String trackBicycle) {
        this.areaId = areaId;
        this.areaName = areaName;
        this.markerList = markerList;
        this.maximumRideTime = maximumRideTime;
        this.maxRideTimeExceedingFine = maxRideTimeExceedingFine;
        this.maxHoldTime = maxHoldTime;
        this.maxHoldTimeExceedingFine = maxHoldTimeExceedingFine;
        this.serviceStartTime = serviceStartTime;
        this.serviceEndTime = serviceEndTime;
        this.serviceHourExceedingFine = serviceHourExceedingFine;
        this.geofencingFine = geofencingFine;
        this.customerServiceNumber = customerServiceNumber;
        this.geofencingCondition = geofencingCondition;
        this.baseFareCondition = baseFareCondition;
        this.serviceCondition = serviceCondition;
        this.subscriptionCondition = subscriptionCondition;
        this.areaCondition = areaCondition;
        this.areaStatus = areaStatus;
        this.createdBy = createdBy;
        this.createDate = createDate;
        this.trackBicycle = trackBicycle;
    }

    public String getTrackBicycle() {
        return trackBicycle;
    }

    public void setTrackBicycle(String trackBicycle) {
        this.trackBicycle = trackBicycle;
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

    public String getMaximumRideTime() {
        return maximumRideTime;
    }

    public void setMaximumRideTime(String maximumRideTime) {
        this.maximumRideTime = maximumRideTime;
    }

    public String getMaxRideTimeExceedingFine() {
        return maxRideTimeExceedingFine;
    }

    public void setMaxRideTimeExceedingFine(String maxRideTimeExceedingFine) {
        this.maxRideTimeExceedingFine = maxRideTimeExceedingFine;
    }

    public String getMaxHoldTime() {
        return maxHoldTime;
    }

    public void setMaxHoldTime(String maxHoldTime) {
        this.maxHoldTime = maxHoldTime;
    }

    public String getMaxHoldTimeExceedingFine() {
        return maxHoldTimeExceedingFine;
    }

    public void setMaxHoldTimeExceedingFine(String maxHoldTimeExceedingFine) {
        this.maxHoldTimeExceedingFine = maxHoldTimeExceedingFine;
    }

    public String getServiceStartTime() {
        return serviceStartTime;
    }

    public void setServiceStartTime(String serviceStartTime) {
        this.serviceStartTime = serviceStartTime;
    }

    public String getServiceEndTime() {
        return serviceEndTime;
    }

    public void setServiceEndTime(String serviceEndTime) {
        this.serviceEndTime = serviceEndTime;
    }

    public String getServiceHourExceedingFine() {
        return serviceHourExceedingFine;
    }

    public void setServiceHourExceedingFine(String serviceHourExceedingFine) {
        this.serviceHourExceedingFine = serviceHourExceedingFine;
    }

    public String getGeofencingFine() {
        return geofencingFine;
    }

    public void setGeofencingFine(String geofencingFine) {
        this.geofencingFine = geofencingFine;
    }

    public String getCustomerServiceNumber() {
        return customerServiceNumber;
    }

    public void setCustomerServiceNumber(String customerServiceNumber) {
        this.customerServiceNumber = customerServiceNumber;
    }

    public String getGeofencingCondition() {
        return geofencingCondition;
    }

    public void setGeofencingCondition(String geofencingCondition) {
        this.geofencingCondition = geofencingCondition;
    }

    public String getBaseFareCondition() {
        return baseFareCondition;
    }

    public void setBaseFareCondition(String baseFareCondition) {
        this.baseFareCondition = baseFareCondition;
    }

    public String getServiceCondition() {
        return serviceCondition;
    }

    public void setServiceCondition(String serviceCondition) {
        this.serviceCondition = serviceCondition;
    }

    public String getSubscriptionCondition() {
        return subscriptionCondition;
    }

    public void setSubscriptionCondition(String subscriptionCondition) {
        this.subscriptionCondition = subscriptionCondition;
    }

    public String getAreaCondition() {
        return areaCondition;
    }

    public void setAreaCondition(String areaCondition) {
        this.areaCondition = areaCondition;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }

    public boolean isAreaStatus() {
        return areaStatus;
    }

    public void setAreaStatus(boolean areaStatus) {
        this.areaStatus = areaStatus;
    }

    public List<LatLng> getMarkerList() {
        return markerList;
    }

    public void setMarkerList(List<LatLng> markerList) {
        this.markerList = markerList;
    }
}
