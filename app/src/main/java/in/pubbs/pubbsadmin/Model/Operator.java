package in.pubbs.pubbsadmin.Model;

import java.io.Serializable;

/*Created by: Parita Dey*/
public class Operator implements Serializable {
    String operatorOrganisation, operatorName, operatorMobile, operatorEmail, /*operatorCity,*/ operatorKey, operatorPassword,operatorDesignation;
    public Operator(){

    }

    public Operator(String operatorOrganisation, String operatorName, String operatorMobile, String operatorEmail, /*String operatorCity,*/ String operatorKey, String operatorPassword) {
        this.operatorOrganisation = operatorOrganisation;
        this.operatorName = operatorName;
        this.operatorMobile = operatorMobile;
        this.operatorEmail = operatorEmail;
        //this.operatorCity = operatorCity;
        this.operatorKey = operatorKey;
        this.operatorPassword = operatorPassword;
        operatorDesignation = "Regional Manager";
    }

    public Operator(String operatorOrganisation, String operatorName, String operatorMobile, String operatorEmail, String operatorKey){
        this.operatorOrganisation = operatorOrganisation;
        this.operatorName = operatorName;
        this.operatorMobile = operatorMobile;
        this.operatorEmail = operatorEmail;
        this.operatorKey = operatorKey;
        operatorDesignation = "Regional Manager";
    }
    public String getOperatorOrganisation() {
        return operatorOrganisation;
    }

    public void setOperatorOrganisation(String operatorOrganisation) {
        this.operatorOrganisation = operatorOrganisation;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public String getOperatorMobile() {
        return operatorMobile;
    }

    public void setOperatorMobile(String operatorMobile) {
        this.operatorMobile = operatorMobile;
    }

    public String getOperatorEmail() {
        return operatorEmail;
    }

    public void setOperatorEmail(String operatorEmail) {
        this.operatorEmail = operatorEmail;
    }

    /*public String getOperatorCity() {
        return operatorCity;
    }

    public void setOperatorCity(String operatorCity) {
        this.operatorCity = operatorCity;
    }
*/
    public String getOperatorKey() {
        return operatorKey;
    }

    public void setOperatorKey(String operatorKey) {
        this.operatorKey = operatorKey;
    }

    public String getOperatorPassword() {
        return operatorPassword;
    }

    public void setOperatorPassword(String operatorPassword) {
        this.operatorPassword = operatorPassword;
    }

    public String getOperatorDesignation() {
        return operatorDesignation;
    }

    public void setOperatorDesignation(String operatorDesignation) {
        this.operatorDesignation = operatorDesignation;
    }
}
