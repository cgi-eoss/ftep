package com.cgi.eoss.ftep.model.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.Multimap;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.IOException;

@Converter
@Slf4j
public class StringMultimapYamlConverter implements AttributeConverter<Multimap<String, String>, String> {

    private static final TypeReference STRING_MULTIMAP = new TypeReference<Multimap<String,String>>() { };

    private final ObjectMapper mapper;

    public StringMultimapYamlConverter() {
        mapper = new ObjectMapper(new YAMLFactory()).registerModule(new GuavaModule());
    }

    @Override
    public String convertToDatabaseColumn(Multimap<String, String> attribute) {
        try {
            return mapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to convert Multimap to YAML string: {}", attribute);
            throw new IllegalArgumentException("Could not convert Multimap to YAML string", e);
        }
    }

    @Override
    public Multimap<String, String> convertToEntityAttribute(String dbData) {
        try {
            return mapper.readValue(dbData, STRING_MULTIMAP);
        } catch (IOException e) {
            LOG.error("Failed to convert YAML string to Multimap: {}", dbData);
            throw new IllegalArgumentException("Could not convert YAML string to Multimap", e);
        }
    }
}

