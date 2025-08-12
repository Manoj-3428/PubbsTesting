package in.pubbs.pubbsadmin.Model;

public class Zone {
    String zoneManagerPhone;

    Zone() {

    }

    public Zone(String phone) {
        zoneManagerPhone = phone;
    }

    public String getZoneManagerPhone() {
        return zoneManagerPhone;
    }

    public void setZoneManagerPhone(String zoneManagerPhone) {
        this.zoneManagerPhone = zoneManagerPhone;
    }
}
