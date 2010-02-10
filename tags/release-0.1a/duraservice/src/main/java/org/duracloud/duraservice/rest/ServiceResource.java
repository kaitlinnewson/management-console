package org.duracloud.duraservice.rest;

import java.io.InputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.duracloud.duraservice.domain.ServiceException;
import org.duracloud.duraservice.domain.ServiceManager;

/**
 * Provides interaction with content
 *
 * @author Bill Branan
 */
public class ServiceResource {

    private static ServiceManager serviceManager;

    public static void configureManager(InputStream configXml) {
        serviceManager.configure(configXml);
    }

    public static List<String> getAllServices() {
        return serviceManager.getAllServices();
    }

    public static List<String> getDeployedServices() {
        return serviceManager.getDeployedServices();
    }

    public static List<String> getAvailableServices() {
        List<String> allServices = serviceManager.getAllServices();
        List<String> deployedServices = serviceManager.getDeployedServices();

        List<String> availableServices = new ArrayList<String>();
        for(String service : allServices) {
            if(!deployedServices.contains(service)) {
                availableServices.add(service);
            }
        }

        return availableServices;
    }

    public static Map<String, String> getService(String serviceId)
    throws ServiceException {
        return serviceManager.getService(serviceId);
    }

    public static void deployService(String serviceId, String serviceHost)
    throws ServiceException {
        serviceManager.deployService(serviceId, serviceHost);
    }

    public static void configureService(String serviceId, InputStream configXml)
    throws ServiceException {
        serviceManager.configureService(serviceId, configXml);
    }

    public static void undeployService(String serviceId)
    throws ServiceException {
        serviceManager.undeployService(serviceId);
    }

    public static List<String> getServiceHosts()
    throws ServiceException {
        return serviceManager.getServiceHosts();
    }

    public ServiceManager getServiceManager() {
        return serviceManager;
    }

    public void setServiceManager(ServiceManager manager) {
        serviceManager = manager;
    }

}