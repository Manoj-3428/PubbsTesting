package in.pubbs.pubbsadmin.Model;
//Created By Souvik
public class AreaManager {
    private String areaManagerName, areaManagerEmail, areaManagerPhone, areaManagerKey, active, areaManagerPassword, areaManagerAreaId, areaManagerAreaName,areaManagerOrganisation, operatorDesignation, createdBy;

    public AreaManager() {
    }

    public AreaManager(String areaManagerName, String areaManagerEmail, String areaManagerPhone, String areaManagerKey, String active, String areaManagerPassword, String areaManagerAreaId, String areaManagerAreaName, String areaManagerOrganisation, String operatorDesignation, String createdBy) {
        this.areaManagerName = areaManagerName;
        this.areaManagerEmail = areaManagerEmail;
        this.areaManagerPhone = areaManagerPhone;
        this.areaManagerKey = areaManagerKey;
        this.active = active;
        this.areaManagerPassword = areaManagerPassword;
        this.areaManagerAreaId = areaManagerAreaId;
        this.areaManagerAreaName = areaManagerAreaName;
        this.areaManagerOrganisation = areaManagerOrganisation;
        this.operatorDesignation = operatorDesignation;
        this.createdBy = createdBy;
    }

    public String getAreaManagerName() {
        return areaManagerName;
    }

    public void setAreaManagerName(String areaManagerName) {
        this.areaManagerName = areaManagerName;
    }

    public String getAreaManagerEmail() {
        return areaManagerEmail;
    }

    public void setAreaManagerEmail(String areaManagerEmail) {
        this.areaManagerEmail = areaManagerEmail;
    }

    public String getAreaManagerPhone() {
        return areaManagerPhone;
    }

    public void setAreaManagerPhone(String areaManagerPhone) {
        this.areaManagerPhone = areaManagerPhone;
    }

    public String getAreaManagerKey() {
        return areaManagerKey;
    }

    public void setAreaManagerKey(String areaManagerKey) {
        this.areaManagerKey = areaManagerKey;
    }

    public String getActive() {
        return active;
    }

    public void setActive(String active) {
        this.active = active;
    }

    public String getAreaManagerPassword() {
        return areaManagerPassword;
    }

    public void setAreaManagerPassword(String areaManagerPassword) {
        this.areaManagerPassword = areaManagerPassword;
    }

    public String getAreaManagerAreaId() {
        return areaManagerAreaId;
    }

    public void setAreaManagerAreaId(String areaManagerAreaId) {
        this.areaManagerAreaId = areaManagerAreaId;
    }

    public String getAreaManagerAreaName() {
        return areaManagerAreaName;
    }

    public void setAreaManagerAreaName(String areaManagerAreaName) {
        this.areaManagerAreaName = areaManagerAreaName;
    }

    public String getAreaManagerOrganisation() {
        return areaManagerOrganisation;
    }

    public void setAreaManagerOrganisation(String areaManagerOrganisation) {
        this.areaManagerOrganisation = areaManagerOrganisation;
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
}
