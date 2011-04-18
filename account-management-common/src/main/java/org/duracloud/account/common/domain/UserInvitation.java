/*
 * Copyright (c) 2009-2010 DuraSpace. All rights reserved.
 */
package org.duracloud.account.common.domain;

import java.util.Date;

/**
 * @author: Bill Branan Date: Dec 2, 2010
 */
public class UserInvitation extends BaseDomainData {

    private int accountId;
    private String userEmail;
    private Date creationDate;
    private Date expirationDate;
    private String redemptionCode;

    public UserInvitation(
        int id, int accountId, String userEmail, int expirationDays,
        String redemptionCode) {
        this(id, accountId, userEmail, expirationDays, redemptionCode, 0);
    }

    public UserInvitation(
        int id, int accountId, String userEmail, int expirationDays,
        String redemptionCode, int counter) {
        this.id = id;
        this.accountId = accountId;
        this.userEmail = userEmail;

        this.creationDate = new Date();

        // milliseconds until expiration (days * millis in a day)
        long expMillis = expirationDays * 86400000;
        this.expirationDate = new Date(creationDate.getTime() + expMillis);

        this.redemptionCode = redemptionCode;
        this.counter = counter;
    }

    public UserInvitation(
        int id, int accountId, String userEmail, Date creationDate,
        Date expirationDate, String redemptionCode, int counter) {
        this.id = id;
        this.accountId = accountId;
        this.userEmail = userEmail;
        this.creationDate = creationDate;
        this.expirationDate = expirationDate;
        this.redemptionCode = redemptionCode;
        this.counter = counter;
    }

    public int getAccountId() {
        return accountId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public String getRedemptionCode() {
        return redemptionCode;
    }

    public String getRedemptionURL() {
        return AmaEndpoint.getUrl() + "/users/redeem/" + getRedemptionCode();
    }



    public String getSubject() {
        return "DuraCloud Account Invitation";
    }

    public String getBody() {
        StringBuilder sb = new StringBuilder();
        sb.append("You are being invited as a member of a DuraCloud account.");
        sb.append("\n");
        sb.append("Please follow the link to join.");
        sb.append("\n");
        sb.append(getRedemptionURL());
        sb.append("\n");
        sb.append("\n");
        sb.append("DuraCloud Admin");

        return sb.toString();
    }

    /*
    * Generated by IntelliJ
    */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UserInvitation that = (UserInvitation) o;

        if (accountId != that.accountId) {
            return false;
        }
        if (creationDate != null ? !creationDate.equals(that.creationDate) :
            that.creationDate != null) {
            return false;
        }
        if (expirationDate != null ? !expirationDate
            .equals(that.expirationDate) : that.expirationDate != null) {
            return false;
        }
        if (redemptionCode != null ? !redemptionCode
            .equals(that.redemptionCode) : that.redemptionCode != null) {
            return false;
        }
        if (userEmail != null ? !userEmail.equals(that.userEmail) :
            that.userEmail != null) {
            return false;
        }

        return true;
    }

    /*
     * Generated by IntelliJ
     */    
    @Override
    public int hashCode() {
        int result = accountId;
        result = 31 * result + (userEmail != null ? userEmail.hashCode() : 0);
        result =
            31 * result + (creationDate != null ? creationDate.hashCode() : 0);
        result = 31 * result +
            (expirationDate != null ? expirationDate.hashCode() : 0);
        result = 31 * result +
            (redemptionCode != null ? redemptionCode.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "UserInvitation[id="
            + id + ", accountId=" + accountId + ", userEmail=" + userEmail
            + ", creationDate=" + creationDate + ", expirationDate="
            + expirationDate + ", redemptionCode=" + redemptionCode
            + ", counter=" + counter + "]";

    }
}
