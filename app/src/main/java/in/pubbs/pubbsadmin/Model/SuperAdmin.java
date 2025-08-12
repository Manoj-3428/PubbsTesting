package in.pubbs.pubbsadmin.Model;

/*Created by: Parita Dey*/
public class SuperAdmin {
    String admin_id, mobile, password;

    public SuperAdmin() {

    }

    public SuperAdmin(String admin_id, String mobile, String password) {
        this.admin_id = admin_id;
        this.mobile = mobile;
        this.password = password;
    }

    public String getAdmin_id() {
        return admin_id;
    }

    public void setAdmin_id(String admin_id) {
        this.admin_id = admin_id;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getMobile() {
        return mobile;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }
}
