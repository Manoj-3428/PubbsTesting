package in.pubbs.pubbsadmin.Model;

public class Subscription {
    public String areaId,areaName, subscriptionId, subscriptionPlanName, subscriptionValidityTime, subscriptionPlanPrice;
    public String subscriptionMaxFreeRide, subscriptionDescription;
    public int subscriptionCarryForward;
    public boolean subscriptionStatus;
    public String createdBy, createDate;
    public Subscription(){}

    public Subscription(String areaId, String areaName, String subscriptionId, String subscriptionPlanName, String subscriptionValidityTime, String subscriptionPlanPrice,
                String subscriptionMaxFreeRide, String subscriptionDescription, int subscriptionCarryForward, boolean subscriptionStatus, String createdBy, String createDate) {
        this.areaId = areaId;
        this.areaName = areaName;
        this.subscriptionId = subscriptionId;
        this.subscriptionPlanName = subscriptionPlanName;
        this.subscriptionValidityTime = subscriptionValidityTime;
        this.subscriptionPlanPrice = subscriptionPlanPrice;
        this.subscriptionMaxFreeRide = subscriptionMaxFreeRide;
        this.subscriptionDescription = subscriptionDescription;
        this.subscriptionCarryForward = subscriptionCarryForward;
        this.subscriptionStatus = subscriptionStatus;
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

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getSubscriptionPlanName() {
        return subscriptionPlanName;
    }

    public void setSubscriptionPlanName(String subscriptionPlanName) {
        this.subscriptionPlanName = subscriptionPlanName;
    }

    public String getSubscriptionValidityTime() {
        return subscriptionValidityTime;
    }

    public void setSubscriptionValidityTime(String subscriptionValidityTime) {
        this.subscriptionValidityTime = subscriptionValidityTime;
    }

    public String getSubscriptionPlanPrice() {
        return subscriptionPlanPrice;
    }

    public void setSubscriptionPlanPrice(String subscriptionPlanPrice) {
        this.subscriptionPlanPrice = subscriptionPlanPrice;
    }


    public String getSubscriptionMaxFreeRide() {
        return subscriptionMaxFreeRide;
    }

    public void setSubscriptionMaxFreeRide(String subscriptionMaxFreeRide) {
        this.subscriptionMaxFreeRide = subscriptionMaxFreeRide;
    }

    public String getSubscriptionDescription() {
        return subscriptionDescription;
    }

    public void setSubscriptionDescription(String subscriptionDescription) {
        this.subscriptionDescription = subscriptionDescription;
    }

    public int getSubscriptionCarryForward() {
        return subscriptionCarryForward;
    }

    public void setSubscriptionCarryForward(int subscriptionCarryForward) {
        this.subscriptionCarryForward = subscriptionCarryForward;
    }


    public boolean isSubscriptionStatus() {
        return subscriptionStatus;
    }

    public void setSubscriptionStatus(boolean subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
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
}
