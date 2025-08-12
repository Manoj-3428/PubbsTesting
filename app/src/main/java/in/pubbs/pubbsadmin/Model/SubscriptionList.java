package in.pubbs.pubbsadmin.Model;

public class SubscriptionList {
    public String areaId, areaName, subscriptionId, subscriptionDescription, planName, suscriptionStatus;

    public SubscriptionList() {
    }

    public SubscriptionList(String areaId, String areaName, String subscriptionId, String subscriptionDescription,
                            String planName, String suscriptionStatus) {
        this.areaId = areaId;
        this.areaName = areaName;
        this.subscriptionId = subscriptionId;
        this.subscriptionDescription = subscriptionDescription;
        this.planName = planName;
        this.suscriptionStatus = suscriptionStatus;
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

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getSubscriptionDescription() {
        return subscriptionDescription;
    }

    public void setSubscriptionDescription(String subscriptionDescription) {
        this.subscriptionDescription = subscriptionDescription;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }


    public String getSuscriptionStatus() {
        return suscriptionStatus;
    }

    public void setSuscriptionStatus(String suscriptionStatus) {
        this.suscriptionStatus = suscriptionStatus;
    }

}
