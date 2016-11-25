package com.cgi.eoss.ftep.orchestrator;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * <p>Configuration for F-TEP WPS services. Defines default (built-in) services.</p>
 */
public class FtepWpsServices {

    private final Map<String, String> serviceImages = Maps.newHashMap();

    public FtepWpsServices() {
        initDefaultServices();
    }

    private void initDefaultServices() {
        registerImageForService("QGIS", "ftep-qgis");
        registerImageForService("Sentinel2ToolboxV2", "ftep-stb_guac");
        registerImageForService("Sentinel2ToolboxV1", "ftep-stb_vnc");
        registerImageForService("TextFileJoiner", "ftep-file_joiner");
        registerImageForService("MonteverdiAppV2", "ftep-otb_guac");
        registerImageForService("MonteverdiAppV1", "ftep-otb_vnc");
        registerImageForService("Sentinel2LandCover", "ftep-s2_land_cover");
        registerImageForService("Sentinel2Ndvi", "ftep-s2_ndvi");
    }

    public String getImageFor(String serviceId) {
        return serviceImages.get(serviceId);
    }

    public void registerImageForService(String serviceId, String image) {
        serviceImages.put(serviceId, image);
    }

}
