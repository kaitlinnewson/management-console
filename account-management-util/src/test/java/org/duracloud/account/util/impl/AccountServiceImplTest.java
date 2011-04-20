/*
 * Copyright (c) 2009-2010 DuraSpace. All rights reserved.
 */
package org.duracloud.account.util.impl;

import org.duracloud.account.common.domain.AccountInfo;
import org.duracloud.account.common.domain.AccountRights;
import org.duracloud.account.common.domain.DuracloudUser;
import org.duracloud.account.common.domain.Role;
import org.duracloud.account.common.domain.StorageProviderAccount;
import org.duracloud.account.common.domain.UserInvitation;
import org.duracloud.account.util.error.DuracloudProviderAccountNotAvailableException;
import org.duracloud.notification.Emailer;
import org.duracloud.storage.domain.StorageProviderType;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Andrew Woods Date: Dec 10, 2010
 */
public class AccountServiceImplTest extends DuracloudServiceTestBase {

    private AccountServiceImpl acctService;
    private AccountInfo acctInfo;

    private final int acctId = 1;
    private final String subdomain = "sub-domain";
    private Set<StorageProviderType> storageProviders;

    @Before
    public void before() throws Exception {
        super.before();

        storageProviders = new HashSet<StorageProviderType>();
        for (StorageProviderType provider : StorageProviderType.values()) {
            storageProviders.add(provider);
        }

        acctInfo = newAccountInfo(acctId, subdomain);
        acctService =
            new AccountServiceImpl(acctInfo, repoMgr, providerAccountUtil);

    }

    @Test
    public void testGetPrimaryStorageProvider() throws Exception {
        StorageProviderAccount storageAcct = setUpGetStorageProviders();
        StorageProviderAccount retVal = acctService.getPrimaryStorageProvider();
        Assert.assertNotNull(retVal);
        Assert.assertEquals(storageAcct, retVal);
    }

    @Test
    public void testGetSecondaryStorageProviders() throws Exception {
        StorageProviderAccount storageAcct = setUpGetStorageProviders();
        Set<StorageProviderAccount> retVals = acctService.getSecondaryStorageProviders();
        Assert.assertNotNull(retVals);
        Assert.assertEquals(1, retVals.size());
        Assert.assertEquals(storageAcct, retVals.iterator().next());
    }

    private StorageProviderAccount setUpGetStorageProviders() throws Exception {
        EasyMock.expect(repoMgr.getStorageProviderAccountRepo())
            .andReturn(storageProviderAcctRepo)
            .times(1);

        StorageProviderAccount storageAcct =
            new StorageProviderAccount(1, StorageProviderType.AMAZON_S3,
                                       "username", "password", false);
        EasyMock.expect(storageProviderAcctRepo.findById(EasyMock.anyInt()))
            .andReturn(storageAcct)
            .times(1);

        replayMocks();

        return storageAcct;
    }

    @Test
    public void testAddStorageProvider() throws Exception {
        StorageProviderType typeToAdd = StorageProviderType.MICROSOFT_AZURE;

        EasyMock.expect(providerAccountUtil.
            createEmptyStorageProviderAccount(typeToAdd))
            .andReturn(0)
            .times(1);

        accountRepo.save(EasyMock.isA(AccountInfo.class));
        EasyMock.expectLastCall()
            .times(1);

        replayMocks();

        acctService.addStorageProvider(typeToAdd);
    }

    @Test
    public void testRemoveStorageProvider() throws Exception {
        accountRepo.save(EasyMock.isA(AccountInfo.class));
        EasyMock.expectLastCall()
            .times(1);

        EasyMock.expect(repoMgr.getStorageProviderAccountRepo())
            .andReturn(storageProviderAcctRepo)
            .times(1);

        storageProviderAcctRepo.delete(0);
        EasyMock.expectLastCall()
            .times(1);

        replayMocks();

        acctService.removeStorageProvider(0);

        // Item has been removed, should fail a subsequent request
        try {
            acctService.removeStorageProvider(0);
            Assert.fail("Exception expected");
        } catch(DuracloudProviderAccountNotAvailableException e) {
            Assert.assertNotNull(e);
        }
    }

    @Test
    public void testGetUsers() throws Exception {
        Set<AccountRights> rights = new HashSet<AccountRights>();
        Set<Role> roles = new HashSet<Role>();
        roles.add(Role.ROLE_OWNER);
        roles.add(Role.ROLE_ADMIN);
        roles.add(Role.ROLE_USER);
        int userId = 1;
        rights.add(new AccountRights(1, acctId, userId, roles));
        EasyMock.expect(this.rightsRepo.findByAccountId(acctId)).andReturn(
            rights);
        EasyMock.expect(this.userRepo.findById(userId)).andReturn(
            newDuracloudUser(userId, "test"));
        EasyMock.expectLastCall();
        replayMocks();
        Set<DuracloudUser> users = this.acctService.getUsers();
        Assert.assertNotNull(users);
        Assert.assertTrue(users.size() == 1);
    }

    @Test
    public void testDeleteUserInvitation() throws Exception {
        this.invitationRepo.delete(1);
        EasyMock.expectLastCall();
        replayMocks();
        this.acctService.deleteUserInvitation(1);

    }

    @Test
    public void testGetPendingUserInvitations() throws Exception {
        Set<UserInvitation> userInvitations = new HashSet<UserInvitation>();
        userInvitations.add(createUserInvite());
        EasyMock.expect(this.invitationRepo.findByAccountId(acctId)).andReturn(userInvitations);
        replayMocks();
        Set<UserInvitation> invites = this.acctService.getPendingInvitations();
        Assert.assertTrue(invites.size() > 0);
    }

    private UserInvitation createUserInvite() {
        return new UserInvitation(1, acctId, "test@duracloud.org", 1, "xyz");
   }

    @Test
    public void testInviteUser() throws Exception {
        String email = "test@duracloud.org";
        EasyMock.expect(this.idUtil.newUserInvitationId()).andReturn(1);

        this.invitationRepo.save(EasyMock.isA(UserInvitation.class));
        EasyMock.expectLastCall();

        Emailer emailer = EasyMock.createMock("Emailer", Emailer.class);

        Capture<String> capturedBody = new Capture<String>();
        emailer.send(EasyMock.isA(String.class),
                     EasyMock.capture(capturedBody),
                     EasyMock.eq(email));
        EasyMock.expectLastCall();

        EasyMock.replay(emailer);
        replayMocks();

        UserInvitation ui = this.acctService.inviteUser(email, emailer);
        Assert.assertTrue(ui.getId() == 1);
        Assert.assertTrue(ui.getRedemptionCode().length() > 3);

        String body = capturedBody.getValue();
        Assert.assertNotNull(body);
        Assert.assertTrue("Email body !contain the redemption code: " + body,
                          body.contains(ui.getRedemptionCode()));

        EasyMock.verify(emailer);
    }

    
    @Test
    public void testStoreRetrieveAccountInfo() throws Exception {
        // FIXME: implement
        replayMocks();
    }

    @Test
    public void testStoreRetrievePaymentInfo() throws Exception {
        // FIXME: implement
        replayMocks();
    }

    @Test
    public void testStoreSubdomain() throws Exception {
        // FIXME: implement
        replayMocks();
    }
}