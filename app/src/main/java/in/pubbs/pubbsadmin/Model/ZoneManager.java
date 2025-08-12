package in.pubbs.pubbsadmin.Model;

//Created By Souvik
public class ZoneManager {
    private String zoneManagerName, zoneManagerEmail, zoneManagerPhone, zoneManagerKey, zoneManagerPassword, zoneManagerCity, zoneManagerOrganisation, operatorDesignation, createdBy;
    private boolean active;

    public ZoneManager() {
    }

    public ZoneManager(String name, String email, String phone, String key, String password, String organisation, String parent, String city, boolean active) {
        zoneManagerName = name;
        zoneManagerEmail = email;
        zoneManagerPhone = phone;
        zoneManagerKey = key;
        this.active = active;
        zoneManagerPassword = password;
        operatorDesignation = "Zone Manager";
        zoneManagerOrganisation = organisation;
        createdBy = parent;
        zoneManagerCity = city;
    }

    public String getZoneManagerName() {
        return zoneManagerName;
    }

    public void setZoneManagerName(String zoneManagerName) {
        this.zoneManagerName = zoneManagerName;
    }

    public String getZoneManagerEmail() {
        return zoneManagerEmail;
    }

    public void setZoneManagerEmail(String zoneManagerEmail) {
        this.zoneManagerEmail = zoneManagerEmail;
    }

    public String getZoneManagerPhone() {
        return zoneManagerPhone;
    }

    public void setZoneManagerPhone(String zoneManagerPhone) {
        this.zoneManagerPhone = zoneManagerPhone;
    }

    public String getZoneManagerKey() {
        return zoneManagerKey;
    }

    public void setZoneManagerKey(String zoneManagerKey) {
        this.zoneManagerKey = zoneManagerKey;
    }

    public String getZoneManagerPassword() {
        return zoneManagerPassword;
    }

    public void setZoneManagerPassword(String zoneManagerPassword) {
        this.zoneManagerPassword = zoneManagerPassword;
    }

    public String getZoneManagerCity() {
        return zoneManagerCity;
    }

    public void setZoneManagerCity(String zoneManagerCity) {
        this.zoneManagerCity = zoneManagerCity;
    }

    public String getZoneManagerOrganisation() {
        return zoneManagerOrganisation;
    }

    public void setZoneManagerOrganisation(String zoneManagerOrganisation) {
        this.zoneManagerOrganisation = zoneManagerOrganisation;
    }

    public String getOperatorDesignation() {
        return operatorDesignation;
    }

    public void setOperatorDesignation(String operatorDesignation) {
        this.operatorDesignation = operatorDesignation;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public boolean getActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
