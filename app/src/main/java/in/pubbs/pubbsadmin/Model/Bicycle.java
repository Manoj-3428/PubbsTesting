package in.pubbs.pubbsadmin.Model;

public class Bicycle {
    String BLEAddress, id, InAreaId, InStationName, InStationId, status, battery, type, Operation, theft;
     //int Operation;


    public Bicycle(String BLEAddress, String id, String inAreaId, String inStationName, String inStationId, String status, String battery, String Operation, String theft) {// int cycleOperation) {
        this.BLEAddress = BLEAddress;
        this.id = id;
        InAreaId = inAreaId;
        InStationName = inStationName;
        InStationId = inStationId;
        this.status = status;
        this.battery = battery;
        this.Operation = Operation;
        this.theft = theft;
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

    public String getInAreaId() {
        return InAreaId;
    }

    public void setInAreaId(String inAreaId) {
        InAreaId = inAreaId;
    }

    public String getInStationName() {
        return InStationName;
    }

    public void setInStationName(String inStationName) {
        InStationName = inStationName;
    }

    public String getInStationId() {
        return InStationId;
    }

    public void setInStationId(String inStationId) {
        InStationId = inStationId;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

 public String getOperation() {
        return Operation;
    }

    public void setOperation(String Operation) {
        this.Operation = Operation;
    }

 public String gettheft(){
        return theft;

 }
 public void setTheft(String theft){
        this.theft = theft;
 }

}
