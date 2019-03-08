package com.cgi.eoss.ftep.model.converters;

import com.cgi.eoss.ftep.model.FtepServiceDescriptor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
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
public class FtepServiceDescriptorYamlConverter implements AttributeConverter<FtepServiceDescriptor, String>, UserType {

    private static final TypeReference FTEP_SERVICE_DESCRIPTOR = new TypeReference<FtepServiceDescriptor>() {
    };

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

    @Override
    public int[] sqlTypes() {
        return new int[]{Types.VARCHAR};
    }

    @Override
    public Class returnedClass() {
        return FtepServiceDescriptor.class;
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
        return value == null ? new FtepServiceDescriptor() : convertToEntityAttribute(value);
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
        FtepServiceDescriptor nullSafeValue = getNullSafeValue(value);
        st.setString(index, convertToDatabaseColumn(nullSafeValue));
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        FtepServiceDescriptor original = (FtepServiceDescriptor) value;
        return value == null ? null : FtepServiceDescriptor.builder()
                .id(original.getId())
                .title(original.getTitle())
                .description(original.getDescription())
                .version(original.getVersion())
                .storeSupported(original.isStoreSupported())
                .statusSupported(original.isStatusSupported())
                .serviceType(original.getServiceType())
                .serviceProvider(original.getServiceProvider())
                .dataInputs(original.getDataInputs())
                .dataOutputs(original.getDataOutputs())
                .build();
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
    private FtepServiceDescriptor getNullSafeValue(Object value) {
        return value == null ? new FtepServiceDescriptor() : (FtepServiceDescriptor) value;
    }
}
