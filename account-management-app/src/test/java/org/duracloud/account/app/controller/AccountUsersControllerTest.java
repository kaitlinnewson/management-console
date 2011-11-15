/*
 * Copyright (c) 2009-2010 DuraSpace. All rights reserved.
 */
package org.duracloud.account.app.controller;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.duracloud.account.common.domain.DuracloudUser;
import org.duracloud.account.common.domain.Role;
import org.duracloud.account.common.domain.UserInvitation;
import org.duracloud.account.db.error.DBConcurrentUpdateException;
import org.duracloud.account.db.error.DBNotFoundException;
import org.duracloud.account.util.AccountManagerService;
import org.duracloud.account.util.AccountService;
import org.duracloud.account.util.DuracloudUserService;
import org.duracloud.account.util.error.AccountNotFoundException;
import org.duracloud.account.util.notification.NotificationMgr;
import org.duracloud.notification.Emailer;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

/**
 * @contributor "Daniel Bernstein (dbernstein@duraspace.org)"
 * 
 */
public class AccountUsersControllerTest extends AmaControllerTestBase {
    private AccountUsersController accountUsersController;
    private AccountService accountService;
    private DuracloudUserService userService;
    private NotificationMgr notificationMgr;

    @Before
    public void before() throws Exception {
        super.before();

        accountUsersController = new AccountUsersController();
        accountManagerService = EasyMock.createMock("AccountManagerService",
                                                    AccountManagerService.class);
        accountService = EasyMock.createMock("AccountService",
                                             AccountService.class);
        userService = EasyMock.createMock("DuracloudUserService",
                                          DuracloudUserService.class);
        notificationMgr = EasyMock.createMock("NotificationMgr",
                                              NotificationMgr.class);

        this.accountUsersController.setNotificationMgr(notificationMgr);
        this.accountUsersController.setAccountManagerService(
            accountManagerService);
        this.accountUsersController.setUserService(userService);

    }
    
    @After
    public void after() throws Exception {
        super.after();
        EasyMock.verify(accountManagerService,
                        accountService, 
                        userService, 
                        notificationMgr);
    }

    private void replayMocks() {
        EasyMock.replay(accountManagerService,
                        accountService, 
                        userService,
                        notificationMgr);
    }
    
    @Test
    public void testSendInvitations() throws Exception {
        setupGet(TEST_ACCOUNT_ID);
        UserInvitation ui = createUserInvitation();

        Emailer emailer = EasyMock.createMock("Emailer", Emailer.class);
        EasyMock.expect(notificationMgr.getEmailer()).andReturn(emailer);

        EasyMock.expect(accountService.inviteUser(ui.getUserEmail(), ui.getAdminUsername(), emailer))
            .andReturn(ui);

        EasyMock.expect(accountService.getPendingInvitations())
        .andReturn(new HashSet<UserInvitation>(Arrays.asList(new UserInvitation[]{
            ui})))
        .times(1);
        
        BindingResult result = EasyMock.createMock(BindingResult.class);
        EasyMock.expect(result.hasErrors()).andReturn(false);
        EasyMock.replay(result);
        EasyMock.replay(emailer);

        replayMocks();

        Model model = new ExtendedModelMap();
        InvitationForm invitationForm = new InvitationForm();
        invitationForm.setEmailAddresses("test@duracloud.org");

        // call under test.
        this.accountUsersController.sendInvitations(TEST_ACCOUNT_ID,
                                                    invitationForm,
                                                    result,
                                                    model);

        Collection<AccountUsersController.PendingAccountUser> accountUsers = (Collection<AccountUsersController.PendingAccountUser>) model.asMap()
            .get("pendingUserInvitations");
        Assert.assertNotNull(accountUsers);
        Assert.assertFalse(accountUsers.isEmpty());
        EasyMock.verify(result);
    }

    /**
     * @return
     */
    private UserInvitation createUserInvitation() {
        return new UserInvitation(2,
            TEST_ACCOUNT_ID.intValue(),
            "testuser",
            "test@duracloud.org",
            14,
            "xyz",
            0);
    }

    @Test
    public void testGet() throws Exception {
        setupGet(TEST_ACCOUNT_ID);
        EasyMock.expect(accountService.getPendingInvitations())
        .andReturn(new HashSet<UserInvitation>(Arrays.asList(new UserInvitation[0])))
        .times(1);              
        replayMocks();

        Model model = new ExtendedModelMap();
        accountUsersController.get(TEST_ACCOUNT_ID, model);
        verifyGet(model);

    }

    private void verifyGet(Model model) {
        Assert.assertTrue(model.containsAttribute("account"));
        Assert.assertTrue(model.containsAttribute(AccountUsersController.USERS_KEY));
    }

    private void setupGet(int accountId)
        throws DBConcurrentUpdateException, AccountNotFoundException, DBNotFoundException {
        EasyMock.expect(accountService.retrieveAccountInfo())
            .andReturn(createAccountInfo())
            .times(1);

        EasyMock.expect(accountService.getUsers())
            .andReturn(new HashSet<DuracloudUser>(Arrays.asList(new DuracloudUser[] { createUser() })))
            .times(1);

        EasyMock.expect(accountManagerService.getAccount(accountId))
            .andReturn(accountService)
            .once();

        EasyMock.expect(userService.loadDuracloudUserByUsername(TEST_USERNAME))
            .andReturn(createUser())
            .anyTimes();
    }
    
    @Test
    public void testDeleteUserInvitation() throws Exception {
        EasyMock.expect(accountManagerService.getAccount(TEST_ACCOUNT_ID))
            .andReturn(accountService);
        this.accountService.deleteUserInvitation(1);
        EasyMock.expectLastCall();
        replayMocks();
        
        Model model = new ExtendedModelMap();
        this.accountUsersController.deleteUserInvitation(TEST_ACCOUNT_ID, 1, model);
   }

    @Test
    public void testDeleteUser() throws Exception {
        userService.revokeUserRights(TEST_ACCOUNT_ID, 1);
        EasyMock.expectLastCall();
        
        EasyMock.expect(userService.loadDuracloudUserByUsername(TEST_USERNAME))
            .andReturn(createUser())
            .anyTimes();
        
        replayMocks();
        
        Model model = new ExtendedModelMap();
        this.accountUsersController.deleteUserFromAccount(TEST_ACCOUNT_ID,
                                                          1,
                                                          model);
    }

    @Test
    public void testEditUser() throws Exception {
        // set up mocks, and args
        int acctId = 7;
        int userId = 9;

        AccountUserEditForm acctUserEditForm = new AccountUserEditForm();
        acctUserEditForm.setRole(Role.ROLE_ADMIN.name());

        BindingResult bindingResult = EasyMock.createMock(BindingResult.class);
        EasyMock.expect(bindingResult.hasErrors()).andReturn(false);
        EasyMock.replay(bindingResult);
        Model model = new ExtendedModelMap();

        setupGet(acctId);

        EasyMock.expect(userService.setUserRights(EasyMock.eq(acctId),
                                                  EasyMock.eq(userId),
                                                  EasyMock.isA(Role.class),
                                                  EasyMock.isA(Role.class),
                                                  EasyMock.isA(Role.class)))
            .andReturn(true);

        EasyMock.expect(accountService.getPendingInvitations())
        .andReturn(new HashSet<UserInvitation>(Arrays.asList(new UserInvitation[0])))
        .times(1);              

        
        replayMocks();

        // method under test
        accountUsersController.setUserService(userService);
        accountUsersController.editUser(acctId,
                                        userId,
                                        acctUserEditForm,
                                        bindingResult,
                                        model);

    }

}
