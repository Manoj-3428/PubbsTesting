package in.pubbs.pubbsadmin.Model;

public class ViewPanel {
    String maximumRideTime, maxHoldTime, customerServiceNumber, serviceStartTime, serviceEndTime;

    public ViewPanel() {
    }

    public ViewPanel(String maximumRideTime, String maxHoldTime, String customerServiceNumber, String serviceStartTime, String serviceEndTime) {
        this.maximumRideTime = maximumRideTime;
        this.maxHoldTime = maxHoldTime;
        this.customerServiceNumber = customerServiceNumber;
        this.serviceStartTime = serviceStartTime;
        this.serviceEndTime = serviceEndTime;
    }

    public String getMaximumRideTime() {
        return maximumRideTime;
    }

    public void setMaximumRideTime(String maximumRideTime) {
        this.maximumRideTime = maximumRideTime;
    }

    public String getMaxHoldTime() {
        return maxHoldTime;
    }

    public void setMaxHoldTime(String maxHoldTime) {
        this.maxHoldTime = maxHoldTime;
    }

    public String getCustomerServiceNumber() {
        return customerServiceNumber;
    }

    public void setCustomerServiceNumber(String customerServiceNumber) {
        this.customerServiceNumber = customerServiceNumber;
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
}
