package in.pubbs.pubbsadmin.Model;

public class ServiceManager {
    private String serviceManagerName, serviceManagerEmail, serviceManagerPhone, serviceManagerKey, active, serviceManagerPassword, serviceManagerAreaId, serviceManagerAreaName,serviceManagerOrganisation, operatorDesignation, createdBy;

    public ServiceManager() {
    }

    public ServiceManager(String serviceManagerName, String serviceManagerEmail, String serviceManagerPhone, String serviceManagerKey, String active, String serviceManagerPassword, String serviceManagerAreaId, String serviceManagerAreaName, String serviceManagerOrganisation, String operatorDesignation, String createdBy) {
        this.serviceManagerName = serviceManagerName;
        this.serviceManagerEmail = serviceManagerEmail;
        this.serviceManagerPhone = serviceManagerPhone;
        this.serviceManagerKey = serviceManagerKey;
        this.active = active;
        this.serviceManagerPassword = serviceManagerPassword;
        this.serviceManagerAreaId = serviceManagerAreaId;
        this.serviceManagerAreaName = serviceManagerAreaName;
        this.serviceManagerOrganisation = serviceManagerOrganisation;
        this.operatorDesignation = operatorDesignation;
        this.createdBy = createdBy;
    }

    public String getServiceManagerName() {
        return serviceManagerName;
    }

    public void setServiceManagerName(String serviceManagerName) {
        this.serviceManagerName = serviceManagerName;
    }

    public String getServiceManagerEmail() {
        return serviceManagerEmail;
    }

    public void setServiceManagerEmail(String serviceManagerEmail) {
        this.serviceManagerEmail = serviceManagerEmail;
    }

    public String getServiceManagerPhone() {
        return serviceManagerPhone;
    }

    public void setServiceManagerPhone(String serviceManagerPhone) {
        this.serviceManagerPhone = serviceManagerPhone;
    }

    public String getServiceManagerKey() {
        return serviceManagerKey;
    }

    public void setServiceManagerKey(String serviceManagerKey) {
        this.serviceManagerKey = serviceManagerKey;
    }

    public String getActive() {
        return active;
    }

    public void setActive(String active) {
        this.active = active;
    }

    public String getServiceManagerPassword() {
        return serviceManagerPassword;
    }

    public void setServiceManagerPassword(String serviceManagerPassword) {
        this.serviceManagerPassword = serviceManagerPassword;
    }

    public String getServiceManagerAreaId() {
        return serviceManagerAreaId;
    }

    public void setServiceManagerAreaId(String serviceManagerAreaId) {
        this.serviceManagerAreaId = serviceManagerAreaId;
    }

    public String getServiceManagerAreaName() {
        return serviceManagerAreaName;
    }

    public void setServiceManagerAreaName(String serviceManagerAreaName) {
        this.serviceManagerAreaName = serviceManagerAreaName;
    }

    public String getServiceManagerOrganisation() {
        return serviceManagerOrganisation;
    }

    public void setServiceManagerOrganisation(String serviceManagerOrganisation) {
        this.serviceManagerOrganisation = serviceManagerOrganisation;
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
