package in.pubbs.pubbsadmin.Model;

public class RepairBicycle {
    String BLEAddress, id, status, battery, dateTime, mobile, adminId;
    double latitude, longitude;

    public RepairBicycle() {
    }

    public RepairBicycle(String BLEAddress,  String adminId,  String mobile,  String id, double latitude, double longitude, String status, String battery, String dateTime) {
        this.BLEAddress = BLEAddress;
        this.adminId = adminId;
        this.mobile = mobile;
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status;
        this.battery = battery;
        this.dateTime = dateTime;
    }

    public String getBLEAddress() {
        return BLEAddress;
    }

    public void setBLEAddress(String BLEAddress) {
        this.BLEAddress = BLEAddress;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBattery() {
        return battery;
    }

    public void setBattery(String battery) {
        this.battery = battery;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
