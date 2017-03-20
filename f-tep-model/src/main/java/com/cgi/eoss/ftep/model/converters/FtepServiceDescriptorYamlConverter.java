package com.cgi.eoss.ftep.model.converters;

import com.cgi.eoss.ftep.model.FtepServiceDescriptor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.log4j.Log4j2;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.IOException;

@Converter
@Log4j2
public class FtepServiceDescriptorYamlConverter implements AttributeConverter<FtepServiceDescriptor, String> {

    private static final TypeReference FTEP_SERVICE_DESCRIPTOR = new TypeReference<FtepServiceDescriptor>() { };

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

    public FtepServiceDescriptorYamlConverter() {
    }

    @Override
    public String convertToDatabaseColumn(FtepServiceDescriptor attribute) {
        return toYaml(attribute);
    }

    @Override
    public FtepServiceDescriptor convertToEntityAttribute(String dbData) {
        return fromYaml(dbData);
    }

    public static String toYaml(FtepServiceDescriptor ftepServiceDescriptor) {
        try {
            return MAPPER.writeValueAsString(ftepServiceDescriptor);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to convert FtepServiceDescriptor to YAML string: {}", ftepServiceDescriptor);
            throw new IllegalArgumentException("Could not convert FtepServiceDescriptor to YAML string", e);
        }
    }

    public static FtepServiceDescriptor fromYaml(String yaml) {
        try {
            return MAPPER.readValue(yaml, FTEP_SERVICE_DESCRIPTOR);
        } catch (IOException e) {
            LOG.error("Failed to convert YAML string to FtepServiceDescriptor: {}", yaml);
            throw new IllegalArgumentException("Could not convert YAML string to FtepServiceDescriptor", e);
        }
    }

}

