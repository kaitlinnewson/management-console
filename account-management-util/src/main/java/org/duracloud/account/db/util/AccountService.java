/*
 * Copyright (c) 2009-2010 DuraSpace. All rights reserved.
 */
package org.duracloud.account.db.util;

import org.duracloud.account.db.model.AccountInfo;
import org.duracloud.account.db.model.ComputeProviderAccount;
import org.duracloud.account.db.model.DuracloudUser;
import org.duracloud.account.db.model.ServerDetails;
import org.duracloud.account.db.model.StorageProviderAccount;
import org.duracloud.account.db.model.UserInvitation;
import org.duracloud.notification.Emailer;
import org.duracloud.storage.domain.StorageProviderType;
import org.springframework.security.access.annotation.Secured;

import java.util.Set;

/**
 * An interface for manipulating account data.
 * 
 * @author "Daniel Bernstein (dbernstein@duraspace.org)"
 */
public interface AccountService {

    /**
     * This method returns the id of account
     *
     * @return account id
     */
    @Secured("role:ROLE_ANONYMOUS, scope:ANY")
    public Long getAccountId();

    /**
     * @return
     */
    @Secured({"role:ROLE_ADMIN, scope:SELF_ACCT"})
    public AccountInfo retrieveAccountInfo();

    /**
     * @param acctName
     * @param orgName
     * @param department
     */
    @Secured({"role:ROLE_OWNER, scope:SELF_ACCT"})
    public void storeAccountInfo(String acctName,
                                 String orgName,
                                 String department);

    /**
     * @param status
     */
    @Secured({"role:ROLE_OWNER, scope:SELF_ACCT"})
    public void storeAccountStatus(AccountInfo.AccountStatus status);

    /**
     * Retrieves ServerDetails if they are available (if this account type
     * makes user of ServerDetails).
     *
     * @return ServerDetails if available, otherwise null
     */
    @Secured({"role:ROLE_ADMIN, scope:SELF_ACCT"})
    public ServerDetails retrieveServerDetails();

    /**
     * @param serverDetails
     */
    @Secured({"role:ROLE_OWNER, scope:SELF_ACCT"})
    public void storeServerDetails(ServerDetails serverDetails);

    /**
     * @param subdomain
     */
    @Secured({"role:ROLE_OWNER, scope:SELF_ACCT"})
    public void storeSubdomain(String subdomain);

    /**
     * This method returns the subdomain associated with this account.
     *
     * @return subdomain
     */
    @Secured({"role:ROLE_USER, scope:SELF_ACCT"})
    public String getSubdomain();

    /**
     * Retrieves the primary storage provider account info
     *
     * @return
     */
    @Secured({"role:ROLE_ADMIN, scope:SELF_ACCT"})
    public StorageProviderAccount getPrimaryStorageProvider();

    /**
     * Retrieves the primary storage provider account info
     *
     * @return
     */
    @Secured({"role:ROLE_ADMIN, scope:SELF_ACCT"})
    public ComputeProviderAccount getComputeProvider();

    /**
     * Retrieves the info for all secondary storage provider accounts
     *
     * @return
     */
    @Secured({"role:ROLE_ADMIN, scope:SELF_ACCT"})
    public Set<StorageProviderAccount> getSecondaryStorageProviders();

    /**
     * Adds a new secondary storage provider to this account.
     *
     * @param storageProviderType
     */
    @Secured({"role:ROLE_OWNER, scope:SELF_ACCT"})
    public void addStorageProvider(StorageProviderType storageProviderType);

    /**
     * Sets the primary storage provider to RRS or not
     *
     * @param rrs
     */
    @Secured({"role:ROLE_OWNER, scope:SELF_ACCT"})
    public void setPrimaryStorageProviderRrs(boolean rrs);

    /**
     * Removes a storage provider from the list of secondary storage providers
     * for this account. The primary storage provider cannot be removed.
     *
     * @param storageProviderId
     */
    @Secured({"role:ROLE_OWNER, scope:SELF_ACCT"})
    public void removeStorageProvider(Long storageProviderId);

    /**
     * @return empty list
     */
    @Secured({"role:ROLE_ANONYMOUS, scope:ANY"})
    public Set<DuracloudUser> getUsers();

    /**
     * Invites a user to join this account by sending a notification to the
     * provided email address. The invitation sent to the user is also
     * returned by this method, as it may be useful to an administrator.
     *
     * @param emailAddress address at which to invite user
     * @param emailer      utility for sending mail
     * @return UserInvitation
     */
    @Secured({"role:ROLE_ADMIN, scope:SELF_ACCT"})
    public UserInvitation inviteUser(String emailAddress, String adminUsername, Emailer emailer);

    /**
     * Gets a listing of the user invitations which are associated with this
     * account.
     *
     * @return UserInvitation set
     */
    @Secured({"role:ROLE_ADMIN, scope:SELF_ACCT"})
    public Set<UserInvitation> getPendingInvitations();

    /**
     * Deletes an invitation to this account.
     * @param invitationId
     */
    @Secured({"role:ROLE_ADMIN, scope:SELF_ACCT"})
    public void deleteUserInvitation(Long invitationId);

}
