package in.pubbs.pubbsadmin.Model;

public class AddBank {
    private String adminId, mobile, upi, bankAccountHolderPhone, organisationName;

    public AddBank() {
    }

    public AddBank(String adminId, String mobile, String upi, String bankAccountHolderPhone, String organisationName) {
        this.adminId = adminId;
        this.mobile = mobile;
        this.upi = upi;
        this.bankAccountHolderPhone = bankAccountHolderPhone;
        this.organisationName = organisationName;
    }

    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getUpi() {
        return upi;
    }

    public void setUpi(String upi) {
        this.upi = upi;
    }

    public String getBankAccountHolderPhone() {
        return bankAccountHolderPhone;
    }

    public void setBankAccountHolderPhone(String bankAccountHolderPhone) {
        this.bankAccountHolderPhone = bankAccountHolderPhone;
    }

    public String getOrganisationName() {
        return organisationName;
    }

    public void setOrganisationName(String organisationName) {
        this.organisationName = organisationName;
    }
}
