/*
 * Copyright (c) 2009-2010 DuraSpace. All rights reserved.
 */
package org.duracloud.account.app.controller;

import org.duracloud.account.common.domain.AccountCreationInfo;
import org.duracloud.account.common.domain.AccountInfo;
import org.duracloud.account.common.domain.DuracloudInstance;
import org.duracloud.account.common.domain.DuracloudUser;
import org.duracloud.account.db.IdUtil;
import org.duracloud.account.db.error.DBNotFoundException;
import org.duracloud.account.util.AccountManagerService;
import org.duracloud.account.util.AccountService;
import org.duracloud.account.util.DuracloudInstanceManagerService;
import org.duracloud.account.util.DuracloudInstanceService;
import org.duracloud.account.util.DuracloudUserService;
import org.duracloud.account.util.error.AccountNotFoundException;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import java.util.HashSet;
import java.util.Set;

/**
 * @contributor "Daniel Bernstein (dbernstein@duraspace.org)"
 * 
 */
public class AccountControllerTest extends AmaControllerTestBase {
    private AccountController accountController;
    private DuracloudUserService userService;
    private DuracloudInstanceManagerService instanceManagerService;
    private DuracloudInstanceService instanceService;

    @Before
    public void before() throws Exception {
        super.before();

        setupSimpleAccountManagerService();
        
        userService = EasyMock.createMock("DuracloudUserService",
                                          DuracloudUserService.class);

        accountController = new AccountController();
        accountController.setAccountManagerService(this.accountManagerService);
    }

    /* FIXME: Properly set up mock account controller to pass verification - bb
    @After
    public void after() throws Exception {
        EasyMock.verify(accountController);
    }
    */

    /**
     * Test method for org.duracloud.account.app.controller.AccountController
     * 
     * @throws AccountNotFoundException
     */
    @Test
    public void testGetHome() throws AccountNotFoundException {
        Model model = new ExtendedModelMap();
        String view = accountController.getHome(TEST_ACCOUNT_ID, model);
        Assert.assertEquals(AccountController.ACCOUNT_HOME, view);
        Assert.assertTrue(model.containsAttribute(AccountController.ACCOUNT_INFO_KEY));
    }

    @Test
    public void testActivate() throws Exception {
        this.accountManagerService =
            EasyMock.createMock(AccountManagerService.class);
        AccountService as = EasyMock.createMock(AccountService.class);

        as.storeAccountStatus(AccountInfo.AccountStatus.ACTIVE);
        EasyMock.expectLastCall();

        EasyMock.expect(accountManagerService.getAccount(TEST_ACCOUNT_ID)).andReturn(as);
        EasyMock.replay(accountManagerService, as);

        accountController.setAccountManagerService(accountManagerService);

        Model model = new ExtendedModelMap();
        accountController.activate(TEST_ACCOUNT_ID, model);

        EasyMock.verify();
    }

    @Test
    public void testDeactivate() throws Exception {
        this.accountManagerService =
            EasyMock.createMock(AccountManagerService.class);
        AccountService as = EasyMock.createMock(AccountService.class);

        as.storeAccountStatus(AccountInfo.AccountStatus.INACTIVE);
        EasyMock.expectLastCall();

        EasyMock.expect(accountManagerService.getAccount(TEST_ACCOUNT_ID)).andReturn(as);
        EasyMock.replay(accountManagerService, as);

        accountController.setAccountManagerService(accountManagerService);

        Model model = new ExtendedModelMap();
        accountController.deactivate(TEST_ACCOUNT_ID, model);

        EasyMock.verify();
    }

    @Test
    public void testGetInstance() throws Exception {
        initializeMockUserServiceLoadUser();
        initializeMockInstanceManagerService();
        replayMocks();

        accountController.setInstanceManagerService(this.instanceManagerService);

        Model model = new ExtendedModelMap();
        accountController.getInstance(TEST_ACCOUNT_ID, model);
        Assert.assertTrue(model.containsAttribute(AccountController.ACCOUNT_INFO_KEY));

        EasyMock.verify(instanceManagerService, instanceService);
    }

    private void initializeMockUserServiceLoadUser() throws DBNotFoundException {
        userService =
            EasyMock.createMock(DuracloudUserService.class);
        EasyMock.expect(userService.loadDuracloudUserByUsername(TEST_USERNAME))
            .andReturn(createUser())
            .anyTimes();
        accountController.setUserService(userService);
    }

    @Test
    public void testRestartInstance() throws Exception {
        initializeMockInstanceManagerService();
        initRestart(TEST_ACCOUNT_ID);
        replayMocks();
        accountController.setInstanceManagerService(this.instanceManagerService);

        Model model = new ExtendedModelMap();
        accountController.restartInstance(TEST_ACCOUNT_ID,
                                          TEST_INSTANCE_ID,
                                          model);
        verifyResult(model);
    }

    private void verifyResult(Model model) {
        Assert.assertTrue(model.containsAttribute(AccountController.ACCOUNT_INFO_KEY));
        Assert.assertTrue(model.containsAttribute(AccountController.ACTION_STATUS));

        EasyMock.verify(instanceManagerService, instanceService);
    }

    @Test
    public void testStartInstance() throws Exception {
        String version = "1.0";
        initializeMockInstanceManagerService();
        initStart(TEST_ACCOUNT_ID, version);
        replayMocks();
        accountController.setInstanceManagerService(this.instanceManagerService);

        Model model = new ExtendedModelMap();
        AccountInstanceForm instanceForm = new AccountInstanceForm();
        instanceForm.setVersion(version);
        accountController.startInstance(TEST_ACCOUNT_ID,
                                        instanceForm,
                                        model);
        verifyResult(model);
    }

    @Test
    public void testReInitUsers() throws Exception {
        boolean initUsers = true;
        createReInitInstanceMocks(initUsers);
        replayMocks();
        accountController.setInstanceManagerService(this.instanceManagerService);

        Model model = new ExtendedModelMap();
        accountController.reInitializeUserRoles(TEST_ACCOUNT_ID,
                                                TEST_INSTANCE_ID,
                                                model);
        verifyResult(model);
    }

    @Test
    public void testReInitInstance() throws Exception {
        boolean initUsers = false;
        createReInitInstanceMocks(initUsers);
        replayMocks();
        accountController.setInstanceManagerService(this.instanceManagerService);

        Model model = new ExtendedModelMap();
        accountController.reInitialize(TEST_ACCOUNT_ID, TEST_INSTANCE_ID, model);
        verifyResult(model);
    }

    private void createReInitInstanceMocks(boolean initUsers) throws Exception {
        instanceManagerService = EasyMock.createMock(
            "DuracloudInstanceManagerService",
            DuracloudInstanceManagerService.class);

        instanceService = EasyMock.createMock("DuracloudInstanceService",
                                              DuracloudInstanceService.class);
        Set<DuracloudInstanceService> instanceServices = new HashSet<DuracloudInstanceService>();
        instanceServices.add(instanceService);

        EasyMock.expect(instanceManagerService.getInstanceServices(EasyMock.anyInt()))
            .andReturn(instanceServices);

        EasyMock.expect(instanceManagerService.getInstanceService(EasyMock.anyInt()))
            .andReturn(instanceService);

        if (initUsers) {
            instanceService.reInitializeUserRoles();
        } else {
            instanceService.reInitialize();
        }
        EasyMock.expectLastCall();

        EasyMock.expect(instanceService.getStatus()).andReturn("status");

        DuracloudInstance instance = new DuracloudInstance(0,
                                                           0,
                                                           0,
                                                           "host",
                                                           "providerInstanceId");
        EasyMock.expect(instanceService.getInstanceInfo()).andReturn(instance);
    }

    @Test
    public void testStopInstance() throws Exception {
        initializeMockInstanceManagerService();
        initStop(TEST_ACCOUNT_ID);
        replayMocks();
        accountController.setInstanceManagerService(this.instanceManagerService);

        Model model = new ExtendedModelMap();
        accountController.stopInstance(TEST_ACCOUNT_ID,
                                       TEST_INSTANCE_ID,
                                       model);
        verifyResult(model);
    }

    @Test
    public void testUpgradeInstance() throws Exception {
        initializeMockInstanceManagerService();
        initStop(TEST_ACCOUNT_ID);

        String version = "1.0";
        EasyMock.expect(instanceManagerService.getLatestVersion())
            .andReturn(version);

        initStart(TEST_ACCOUNT_ID, version);

        replayMocks();
        accountController.setInstanceManagerService(this.instanceManagerService);

        Model model = new ExtendedModelMap();
        accountController.upgradeInstance(TEST_ACCOUNT_ID,
                                          TEST_INSTANCE_ID,
                                          model);
        verifyResult(model);
    }

    @Test
    public void testGetStatement() throws AccountNotFoundException {
        Model model = new ExtendedModelMap();
        accountController.getStatement(TEST_ACCOUNT_ID, model);
        Assert.assertTrue(model.containsAttribute(AccountController.ACCOUNT_INFO_KEY));
    }

    /**
     * Test method for
     * {@link org.duracloud.account.app.controller.AccountController #getNewForm()}
     * .
     */
    @Test
    public void testOpenAddForm() throws Exception {
        accountController.setAuthenticationManager(authenticationManager);
        initializeMockUserService();

        Model model = new ExtendedModelMap();
        String view = accountController.openAddForm(model);
        Assert.assertEquals(AccountController.NEW_ACCOUNT_VIEW, view);
        Assert.assertNotNull(model.asMap()
            .get(AccountController.NEW_ACCOUNT_FORM_KEY));
    }

    @Test
    public void testAdd() throws Exception {
        accountController.setAuthenticationManager(authenticationManager);
        initializeMockUserService();

        BindingResult result = EasyMock.createMock(BindingResult.class);
        EasyMock.expect(result.hasErrors()).andReturn(true);
        EasyMock.expect(result.hasErrors()).andReturn(false);
        EasyMock.replay(result);

        IdUtil idUtil = EasyMock.createNiceMock(IdUtil.class);

        AccountService as = EasyMock.createMock("AccountService",
                                                AccountService.class);
        AccountInfo acctInfo = createAccountInfo();
        EasyMock.expect(as.retrieveAccountInfo())
            .andReturn(acctInfo)
            .anyTimes();

        AccountManagerService ams = EasyMock.createMock("AccountManagerService",
                                                        AccountManagerService.class);
        EasyMock.expect(ams.createAccount(EasyMock.isA(AccountCreationInfo.class),
                                          EasyMock.isA(DuracloudUser.class)))
            .andReturn(as);

        EasyMock.replay(ams, as, idUtil);

        accountController.setAccountManagerService(ams);

        NewAccountForm newAccountForm = new NewAccountForm();
        newAccountForm.setSubdomain("testdomain");
        Model model = new ExtendedModelMap();

        // first time around has errors
        String view = accountController.add(newAccountForm, result, model);
        Assert.assertEquals(AccountController.NEW_ACCOUNT_VIEW, view);

        // second time okay
        view = accountController.add(newAccountForm, result, model);
        Assert.assertTrue(view.startsWith("redirect"));

    }

    private void initializeMockUserService() throws DBNotFoundException {
        DuracloudUserService userService =
            EasyMock.createMock(DuracloudUserService.class);
        EasyMock.expect(userService.loadDuracloudUserByUsername(TEST_USERNAME))
            .andReturn(createUser())
            .anyTimes();
        EasyMock.replay(userService);
        accountController.setUserService(userService);
    }

    private void initializeMockInstanceManagerService() throws Exception {
        instanceManagerService = EasyMock.createMock(
            "DuracloudInstanceManagerService",
            DuracloudInstanceManagerService.class);

        instanceService = EasyMock.createMock("DuracloudInstanceService",
                                              DuracloudInstanceService.class);
        Set<DuracloudInstanceService> instanceServices =
            new HashSet<DuracloudInstanceService>();
        instanceServices.add(instanceService);

        EasyMock.expect(
            instanceManagerService.getInstanceServices(EasyMock.anyInt()))
            .andReturn(instanceServices)
            .times(1);

        EasyMock.expect(instanceService.getStatus())
            .andReturn("status")
            .times(1);

        DuracloudInstance instance =
            new DuracloudInstance(0, 0, 0, "host", "providerInstanceId");
        EasyMock.expect(instanceService.getInstanceInfo())
            .andReturn(instance)
            .times(1);
    }

    private void initRestart(int accountId) throws Exception {
        EasyMock.expect(instanceManagerService.
            getInstanceService(accountId))
            .andReturn(instanceService)
            .anyTimes();

        instanceService.restart();
        EasyMock.expectLastCall()
            .times(1);
    }

    private void initStart(int accountId, String version) throws Exception {
        EasyMock.expect(instanceManagerService.
            createInstance(accountId, version))
            .andReturn(instanceService)
            .anyTimes();
    }

    private void initStop(int accountId) throws Exception {
        EasyMock.expect(instanceManagerService.
            getInstanceService(accountId))
            .andReturn(instanceService)
            .anyTimes();

        instanceService.stop();
        EasyMock.expectLastCall()
            .times(1);
    }

    private void replayMocks() {
       EasyMock.replay(instanceManagerService, instanceService);
    }

}
