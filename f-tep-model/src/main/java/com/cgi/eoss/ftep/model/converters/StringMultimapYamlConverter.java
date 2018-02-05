package com.cgi.eoss.ftep.model.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import lombok.extern.log4j.Log4j2;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.IOException;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

@Converter
@Log4j2
public class StringMultimapYamlConverter implements AttributeConverter<Multimap<String, String>, String>, UserType {

    private static final TypeReference STRING_MULTIMAP = new TypeReference<Multimap<String, String>>() {};

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

    @Override
    public int[] sqlTypes() {
        return new int[]{Types.VARCHAR};
    }

    @Override
    public Class returnedClass() {
        return Multimap.class;
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        return Objects.equals(x, y);
    }

    @Override
    public int hashCode(Object x) throws HibernateException {
        return Objects.hashCode(x);
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner) throws HibernateException, SQLException {
        String value = rs.getString(names[0]);
        return value == null ? HashMultimap.create() : convertToEntityAttribute(value);
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
        Multimap<String, String> nullSafeValue = getNullSafeValue(value);
        st.setString(index, convertToDatabaseColumn(nullSafeValue));
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        return HashMultimap.create(getNullSafeValue(value));
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        return (Serializable) deepCopy(value);
    }

    @Override
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return deepCopy(cached);
    }

    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return deepCopy(original);
    }

    @SuppressWarnings("unchecked")
    private Multimap<String, String> getNullSafeValue(Object value) {
        return value == null ? HashMultimap.create() : (Multimap<String, String>) value;
    }

}

