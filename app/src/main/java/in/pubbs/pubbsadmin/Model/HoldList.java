package in.pubbs.pubbsadmin.Model;

public class HoldList {
    String bicycle, status,rideStartTime,excessElapsed,bookingId, actualRidetime;

    public HoldList() {
    }

    public HoldList(String bicycle) {
        this.bicycle = bicycle;
    }
    public HoldList(String bicycle, String status) {
        this.bicycle = bicycle;
        this.status = status;
    }

    public String getBicycle() {
        return bicycle;
    }

    public String getStatus() {
        return status;
    }

    public void setBicycle(String bicycle) {
        this.bicycle = bicycle;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setRideStartTime(String rideStartTime) {
        this.rideStartTime = rideStartTime;
    }

    public String getRideStartTime() {
        return rideStartTime;
    }

    public void setExcessElapsed(String excessElapsed) {
        this.excessElapsed = excessElapsed;
    }

    public String getExcessElapsed() {
        return excessElapsed;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getActualRidetime() {
        return actualRidetime;
    }

    public void setActualRidetime(String actualRidetime) {
        this.actualRidetime = actualRidetime;
    }
}
