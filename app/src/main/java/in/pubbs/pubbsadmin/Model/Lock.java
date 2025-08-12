package in.pubbs.pubbsadmin.Model;

import java.io.Serializable;

//Created By Souvik
public class Lock implements Serializable {
    public String lockId, simId, bleAddress, operator, batteryValue;

    public Lock() {
    }

    public Lock(String lockId, String simId, String bleAddress, String batteryValue, String operator) {
        this.lockId = lockId;
        this.simId = simId;
        this.bleAddress = bleAddress;
        this.batteryValue = batteryValue;
        this.operator = operator;
    }

    public String getLockId() {
        return lockId;
    }

    public void setLockId(String lockId) {
        this.lockId = lockId;
    }

    public String getSimId() {
        return simId;
    }

    public void setSimId(String simId) {
        this.simId = simId;
    }

    public String getBleAddress() {
        return bleAddress;
    }

    public void setBleAddress(String bleAddress) {
        this.bleAddress = bleAddress;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getBatteryValue() {
        return batteryValue;
    }

    public void setBatteryValue(String batteryValue) {
        this.batteryValue = batteryValue;
    }

}
