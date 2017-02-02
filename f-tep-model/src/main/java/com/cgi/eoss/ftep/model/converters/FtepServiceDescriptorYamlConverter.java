package com.cgi.eoss.ftep.model.converters;

import com.cgi.eoss.ftep.model.FtepServiceDescriptor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.IOException;

@Converter
@Slf4j
public class FtepServiceDescriptorYamlConverter implements AttributeConverter<FtepServiceDescriptor, String> {

    private static final TypeReference FTEP_SERVICE_DESCRIPTOR = new TypeReference<FtepServiceDescriptor>() { };

    private final ObjectMapper mapper;

    public FtepServiceDescriptorYamlConverter() {
        mapper = new ObjectMapper(new YAMLFactory());
    }

    @Override
    public String convertToDatabaseColumn(FtepServiceDescriptor attribute) {
        try {
            return mapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to convert FtepServiceDescriptor to YAML string: {}", attribute);
            throw new IllegalArgumentException("Could not convert FtepServiceDescriptor to YAML string", e);
        }
    }

    @Override
    public FtepServiceDescriptor convertToEntityAttribute(String dbData) {
        try {
            return mapper.readValue(dbData, FTEP_SERVICE_DESCRIPTOR);
        } catch (IOException e) {
            LOG.error("Failed to convert YAML string to FtepServiceDescriptor: {}", dbData);
            throw new IllegalArgumentException("Could not convert YAML string to FtepServiceDescriptor", e);
        }
    }
}

