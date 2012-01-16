/*
 * Copyright (c) 2009-2010 DuraSpace. All rights reserved.
 */
package org.duracloud.account.common.domain;

/**
 * @author: Bill Branan
 * Date: Feb 2, 2011
 */
public class ServiceRepository extends BaseDomainData {

    public enum ServiceRepositoryType {
        VERIFIED,
        PUBLIC,
        PRIVATE;
    }

    /**
     * The type of the service repository. There are currently three options
     * 1. Verified - a service repository which includes services provided
     *               directly by DuraCloud staff or verified by DuraCloud staff
     * 2. Public   - a service repository which includes publicly available
     *               services which have not been verified by DuraCloud
     * 3. Private  - a service repository which hosts services which are not
     *               available for public use
     */
    private ServiceRepositoryType serviceRepositoryType;

    /**
     * The servicePlan of the repository.
     * This defines which services are available.
     */
    private ServicePlan servicePlan;

    /**
     * The name of the host from which a service repository is served
     */
    private String hostName;

    /**
     * The ID of the space in which the contents of a service repository reside
     */
    private String spaceId;

    /**
     * The ID of the service-xml defining services available in this plan
     */
    private String serviceXmlId;

    /**
     * The version of the DuraCloud services available is the repository
     */
    private String version;

    /**
     * The username necessary to log in to the service repository
     */
    private String username;

    /**
     * The password necesary to log in to the service repository
     */
    private String password;

    public ServiceRepository (int id,
                              ServiceRepositoryType serviceRepositoryType,
                              ServicePlan servicePlan,
                              String hostName,
                              String spaceId,
                              String serviceXmlId,
                              String version,
                              String username,
                              String password) {
        this(id,
             serviceRepositoryType,
             servicePlan,
             hostName,
             spaceId,
             serviceXmlId,
             version,
             username,
             password,
             0);
    }

    public ServiceRepository (int id,
                              ServiceRepositoryType serviceRepositoryType,
                              ServicePlan servicePlan,
                              String hostName,
                              String spaceId,
                              String serviceXmlId,
                              String version,
                              String username,
                              String password,
                              int counter) {
        this.id = id;
        this.serviceRepositoryType = serviceRepositoryType;
        this.servicePlan = servicePlan;
        this.hostName = hostName;
        this.spaceId = spaceId;
        this.serviceXmlId = serviceXmlId;
        this.version = version;
        this.username = username;
        this.password = password;
        this.counter = counter;
    }

    public ServiceRepositoryType getServiceRepositoryType() {
        return serviceRepositoryType;
    }

    public ServicePlan getServicePlan() {
        return servicePlan;
    }

    public String getHostName() {
        return hostName;
    }

    public String getSpaceId() {
        return spaceId;
    }

    public String getServiceXmlId() {
        return serviceXmlId;
    }

    public String getVersion() {
        return version;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setServiceRepositoryType(ServiceRepositoryType serviceRepositoryType) {
        this.serviceRepositoryType = serviceRepositoryType;
    }

    public void setServicePlan(ServicePlan servicePlan) {
        this.servicePlan = servicePlan;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public void setSpaceId(String spaceId) {
        this.spaceId = spaceId;
    }

    public void setServiceXmlId(String serviceXmlId) {
        this.serviceXmlId = serviceXmlId;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServiceRepository)) {
            return false;
        }

        ServiceRepository that = (ServiceRepository) o;

        if (hostName != null ? !hostName.equals(that.hostName) :
            that.hostName != null) {
            return false;
        }
        if (password != null ? !password.equals(that.password) :
            that.password != null) {
            return false;
        }
        if (servicePlan != that.servicePlan) {
            return false;
        }
        if (serviceRepositoryType != that.serviceRepositoryType) {
            return false;
        }
        if (serviceXmlId != null ? !serviceXmlId.equals(that.serviceXmlId) :
            that.serviceXmlId != null) {
            return false;
        }
        if (spaceId != null ? !spaceId.equals(that.spaceId) :
            that.spaceId != null) {
            return false;
        }
        if (username != null ? !username.equals(that.username) :
            that.username != null) {
            return false;
        }
        if (version != null ? !version.equals(that.version) :
            that.version != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = serviceRepositoryType !=
            null ? serviceRepositoryType.hashCode() : 0;
        result =
            31 * result + (servicePlan != null ? servicePlan.hashCode() : 0);
        result = 31 * result + (hostName != null ? hostName.hashCode() : 0);
        result = 31 * result + (spaceId != null ? spaceId.hashCode() : 0);
        result =
            31 * result + (serviceXmlId != null ? serviceXmlId.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        return result;
    }
}
