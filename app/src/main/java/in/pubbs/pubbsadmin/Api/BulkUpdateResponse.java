package in.pubbs.pubbsadmin.Api;

/**
 * Response model for bulk update operations
 */
public class BulkUpdateResponse {
    private boolean success;
    private String message;
    private int updatedCount;
    
    public BulkUpdateResponse() {
    }
    
    public BulkUpdateResponse(boolean success, String message, int updatedCount) {
        this.success = success;
        this.message = message;
        this.updatedCount = updatedCount;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public int getUpdatedCount() {
        return updatedCount;
    }
    
    public void setUpdatedCount(int updatedCount) {
        this.updatedCount = updatedCount;
    }
}
