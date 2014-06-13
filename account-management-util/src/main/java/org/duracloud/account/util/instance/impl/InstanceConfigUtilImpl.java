/*
 * Copyright (c) 2009-2011 DuraSpace. All rights reserved.
 */
package org.duracloud.account.util.instance.impl;

import org.duracloud.account.common.domain.AccountInfo;
import org.duracloud.account.common.domain.AmaEndpoint;
import org.duracloud.account.common.domain.DuracloudInstance;
import org.duracloud.account.common.domain.ServerDetails;
import org.duracloud.account.common.domain.StorageProviderAccount;
import org.duracloud.account.db.DuracloudAccountRepo;
import org.duracloud.account.db.DuracloudRepoMgr;
import org.duracloud.account.db.DuracloudStorageProviderAccountRepo;
import org.duracloud.account.db.error.DBNotFoundException;
import org.duracloud.account.init.domain.AmaConfig;
import org.duracloud.account.util.error.DuracloudProviderAccountNotAvailableException;
import org.duracloud.account.util.error.InstanceAccountNotFoundException;
import org.duracloud.account.util.instance.InstanceConfigUtil;
import org.duracloud.account.util.instance.InstanceUtil;
import org.duracloud.account.util.notification.NotificationMgrConfig;
import org.duracloud.account.util.util.AccountUtil;
import org.duracloud.appconfig.domain.DurabossConfig;
import org.duracloud.appconfig.domain.DuradminConfig;
import org.duracloud.appconfig.domain.DurastoreConfig;
import org.duracloud.appconfig.domain.NotificationConfig;
import org.duracloud.storage.domain.AuditConfig;
import org.duracloud.storage.domain.StorageAccount;
import org.duracloud.storage.domain.impl.StorageAccountImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Bill Branan
 * Date: 2/22/11
 */
public class InstanceConfigUtilImpl implements InstanceConfigUtil {

    protected static final String DEFAULT_SSL_PORT = "443";
    protected static final String DEFAULT_DURASTORE_CONTEXT = "durastore";
    protected static final String DEFAULT_SERVICE_COMPUTE_TYPE = "AMAZON_EC2";
    protected static final String DEFAULT_SERVICE_COMPUTE_IMAGE_ID = "unknown";
    protected static final String NOTIFICATION_TYPE = "EMAIL";

    private DuracloudInstance instance;
    private DuracloudRepoMgr repoMgr;
    private AccountUtil accountUtil;
    private NotificationMgrConfig notMgrConfig;
    private AmaConfig amaConfig;
    
    public InstanceConfigUtilImpl(DuracloudInstance instance,
                                  DuracloudRepoMgr repoMgr,
                                  AccountUtil accountUtil,
                                  NotificationMgrConfig notMgrConfig,
                                  AmaConfig amaConfig) {
        this.instance = instance;
        this.repoMgr = repoMgr;
        this.accountUtil = accountUtil;
        this.notMgrConfig = notMgrConfig;
        this.amaConfig = amaConfig;
    }

    public DuradminConfig getDuradminConfig() {
        DuradminConfig config = new DuradminConfig();
        config.setDurastoreHost(instance.getHostName());
        config.setDurastorePort(DEFAULT_SSL_PORT);
        config.setDurastoreContext(DurastoreConfig.QUALIFIER);
        config.setAmaUrl(AmaEndpoint.getUrl());
        return config;
    }

    public DurastoreConfig getDurastoreConfig() {
        DurastoreConfig config = new DurastoreConfig();
        DuracloudStorageProviderAccountRepo storageProviderAcctRepo =
            repoMgr.getStorageProviderAccountRepo();
        Set<StorageAccount> storageAccts = new HashSet<StorageAccount>();
        ServerDetails serverDetails =
            accountUtil.getServerDetails(getAccount());

        // Primary Storage Provider
        int primaryProviderAccountId =
            serverDetails.getPrimaryStorageProviderAccountId();
        storageAccts.add(getStorageAccount(storageProviderAcctRepo,
                                           primaryProviderAccountId,
                                           true));
        // Secondary Storage Providers
        Set<Integer> providerAccountIds =
            serverDetails.getSecondaryStorageProviderAccountIds();
        for(int providerAccountId : providerAccountIds) {
            storageAccts.add(getStorageAccount(storageProviderAcctRepo,
                                               providerAccountId,
                                               false));
        }

        config.setStorageAccounts(storageAccts);
        AuditConfig audit = config.getAuditConfig();
        audit.setAuditQueueName(this.amaConfig.getAuditQueue());
        audit.setAuditUsername(this.amaConfig.getUsername());
        audit.setAuditPassword(this.amaConfig.getPassword());
        return config;
    }

    private AccountInfo getAccount() {
        DuracloudAccountRepo accountRepo = repoMgr.getAccountRepo();
        try {
            return accountRepo.findById(instance.getAccountId());
        } catch(DBNotFoundException e) {
            throw new InstanceAccountNotFoundException(instance.getId(),
                                                       instance.getAccountId());
        }
    }

    private StorageAccount getStorageAccount(
        DuracloudStorageProviderAccountRepo storageProviderAcctRepo,
        int providerAccountId,
        boolean primary) {
        try {
            StorageProviderAccount provider =
                storageProviderAcctRepo.findById(providerAccountId);
            StorageAccount storageAccount =
                new StorageAccountImpl(String.valueOf(providerAccountId),
                                   provider.getUsername(),
                                   provider.getPassword(),
                                   provider.getProviderType());
            storageAccount.setPrimary(primary);

            String storageClass = "rrs";
            if(!provider.isRrs())
                storageClass = "standard";
            
            storageAccount.setOption(StorageAccount.OPTS.STORAGE_CLASS.name(),
                                     storageClass);

            return storageAccount;
        } catch(DBNotFoundException e) {
            String error = "Storage Provider Account with ID: " +
                providerAccountId + " does not exist in the database.";
            throw new DuracloudProviderAccountNotAvailableException(error, e);
        }
    }

    public DurabossConfig getDurabossConfig() {
        DurabossConfig config = new DurabossConfig();
        config.setDurastoreHost(instance.getHostName());
        config.setDurastorePort(DEFAULT_SSL_PORT);
        config.setDurastoreContext(DurastoreConfig.QUALIFIER);
        NotificationConfig notificationConfig = new NotificationConfig();
        notificationConfig.setType(NOTIFICATION_TYPE);
        notificationConfig.setUsername(notMgrConfig.getUsername());
        notificationConfig.setPassword(notMgrConfig.getPassword());
        notificationConfig.setOriginator(notMgrConfig.getFromAddress());

        List<String> admins = new ArrayList<String>();
        for(String admin : notMgrConfig.getAdminAddresses()) {
            admins.add(admin);
        }
        notificationConfig.setAdmins(admins);

        Map<String, NotificationConfig> notificationConfigMap =
            new HashMap<String, NotificationConfig>();
        notificationConfigMap.put("0", notificationConfig);
        config.setNotificationConfigs(notificationConfigMap);

        config.setDurabossContext(InstanceUtil.DURABOSS_CONTEXT);
        return config;
    }
    
}
