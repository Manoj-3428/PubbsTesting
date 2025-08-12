package in.pubbs.pubbsadmin.Model;

public class AreaRate {
    String rateId, createdBy, createDate;
    int rateMoney, rateTime;

    public AreaRate() {

    }

    public AreaRate(String rateId, int rateMoney, int rateTime, String createdBy, String createDate) {
        this.rateId = rateId;
        this.rateMoney = rateMoney;
        this.rateTime = rateTime;
        this.createdBy = createdBy;
        this.createDate = createDate;
    }

    public String getRateId() {
        return rateId;
    }

    public void setRateId(String rateId) {
        this.rateId = rateId;
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

    public int getRateMoney() {
        return rateMoney;
    }

    public void setRateMoney(int rateMoney) {
        this.rateMoney = rateMoney;
    }

    public int getRateTime() {
        return rateTime;
    }

    public void setRateTime(int rateTime) {
        this.rateTime = rateTime;
    }
}
