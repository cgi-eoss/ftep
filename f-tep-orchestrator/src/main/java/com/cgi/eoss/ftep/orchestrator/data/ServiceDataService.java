package com.cgi.eoss.ftep.orchestrator.data;

import com.cgi.eoss.ftep.model.enums.ServiceType;
import com.google.common.collect.Maps;

import java.util.Map;

/**
 * <p>Configuration for F-TEP WPS services. Defines default (built-in) services.</p>
 */
// TODO Remove this when direct java API is functional
public class ServiceDataService {

    // TODO Add the service-dockerImage mapping to the FtepService class
    private final Map<String, String> serviceImages = Maps.newHashMap();
    private final Map<String, ServiceType> serviceTypes = Maps.newHashMap();

    public ServiceDataService() {
        initDefaultServices();
    }

    // Initialise the key attributes of the hardcoded pre-defined services provided by F-TEP
    private void initDefaultServices() {
        // TODO Build actual FtepService objects
        registerService("QGIS", "ftep-qgis", ServiceType.APPLICATION);
        registerService("Sentinel2ToolboxV2", "ftep-stb_guac", ServiceType.APPLICATION);
        registerService("Sentinel2ToolboxV1", "ftep-stb_vnc", ServiceType.APPLICATION);
        registerService("MonteverdiAppV2", "ftep-otb_guac", ServiceType.APPLICATION);
        registerService("MonteverdiAppV1", "ftep-otb_vnc", ServiceType.APPLICATION);
        registerService("Sentinel2Ndvi", "ftep-s2_ndvi", ServiceType.PROCESSOR);
        registerService("LandCoverS2", "ftep-landcovers2", ServiceType.PROCESSOR);
        registerService("TextFileJoiner", "ftep-file_joiner", ServiceType.PROCESSOR);
    }

    /**
     * <p>Determine the Docker image (i.e. ID or tag) to be launched for the requested service.</p>
     *
     * @param serviceId The service for which a docker container is needed.
     * @return The docker image ID (or tag) required by the service.
     */
    public String getImageFor(String serviceId) {
        return serviceImages.get(serviceId);
    }

    /**
     * <p>Determine the service type (i.e. APPLICATION or PROCESSOR) of the requested service.</p>
     *
     * @param serviceId The service for which a docker container is needed.
     * @return The docker image ID (or tag) required by the service.
     */
    public ServiceType getServiceType(String serviceId) {
        return serviceTypes.get(serviceId);
    }

    public void registerService(String serviceId, String image, ServiceType type) {
        serviceImages.put(serviceId, image);
        serviceTypes.put(serviceId, type);
    }

}
