package in.pubbs.pubbsadmin.Model;

public class ReportList {
    public String bicycleId, dateTime, reportId, problem, userId;

    public ReportList() {
    }

    public ReportList(String bicycleId, String dateTime, String reportId, String problem, String userId) {
        this.bicycleId = bicycleId;
        this.dateTime = dateTime;
        this.reportId = reportId;
        this.problem = problem;
        this.userId = userId;
    }

    public String getBicycleId() {
        return bicycleId;
    }

    public void setBicycleId(String bicycleId) {
        this.bicycleId = bicycleId;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getProblem() {
        return problem;
    }

    public void setProblem(String problem) {
        this.problem = problem;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
