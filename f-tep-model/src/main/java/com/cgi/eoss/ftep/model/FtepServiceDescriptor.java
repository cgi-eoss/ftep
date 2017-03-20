package com.cgi.eoss.ftep.model;

import com.cgi.eoss.ftep.model.converters.FtepServiceDescriptorYamlConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.List;
import java.util.Map;

/**
 * <p>The detailed service configuration required to complete a WPS service definition file.</p>
 * <p>All fields are broadly aligned with the official WPS spec as configured via ZOO-Project zcfg files.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FtepServiceDescriptor {

    private String id;

    private String title;

    private String description;

    private String version;

    private boolean storeSupported;

    private boolean statusSupported;

    private String serviceType;

    private String serviceProvider;

    private List<Parameter> dataInputs;

    private List<Parameter> dataOutputs;

    public String toYaml() {
        return FtepServiceDescriptorYamlConverter.toYaml(this);
    }

    public static FtepServiceDescriptor fromYaml(String yaml) {
        return FtepServiceDescriptorYamlConverter.fromYaml(yaml);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Parameter {

        public enum DataNodeType {
            LITERAL, COMPLEX, BOUNDING_BOX
        }

        private String id;

        private String title;

        private String description;

        private int minOccurs;

        private int maxOccurs;

        private DataNodeType data;

        private Map<String, String> defaultAttrs;

        @Singular
        private List<Map<String, String>> supportedAttrs;

    }

}
