package com.cgi.eoss.ftep.model.converters;

import com.cgi.eoss.ftep.model.FtepServiceDockerBuildInfo;

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
public class FtepServiceDockerBuildInfoYamlConverter implements AttributeConverter<FtepServiceDockerBuildInfo, String>, UserType {

    private static final TypeReference FTEP_SERVICE_DOCKER_BUILD_INFO = new TypeReference<FtepServiceDockerBuildInfo>() {
    };

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

    public FtepServiceDockerBuildInfoYamlConverter() {
    }

    @Override
    public String convertToDatabaseColumn(FtepServiceDockerBuildInfo attribute) {
        return toYaml(attribute);
    }

    @Override
    public FtepServiceDockerBuildInfo convertToEntityAttribute(String dbData) {
        return fromYaml(dbData);
    }

    public static String toYaml(FtepServiceDockerBuildInfo ftepServiceDockerBuildInfo) {
        try {
            return MAPPER.writeValueAsString(ftepServiceDockerBuildInfo);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to convert FtepServiceDockerBuildInfo to YAML string: {}", ftepServiceDockerBuildInfo);
            throw new IllegalArgumentException("Could not convert FtepServiceDockerBuildInfo to YAML string", e);
        }
    }

    public static FtepServiceDockerBuildInfo fromYaml(String yaml) {
        try {
            return MAPPER.readValue(yaml, FTEP_SERVICE_DOCKER_BUILD_INFO);
        } catch (IOException e) {
            LOG.error("Failed to convert YAML string to FtepServiceDockerBuildInfo: {}", yaml);
            throw new IllegalArgumentException("Could not convert YAML string to FtepServiceDockerBuildInfo", e);
        }
    }

    @Override
    public int[] sqlTypes() {
        return new int[]{Types.VARCHAR};
    }

    @Override
    public Class returnedClass() {
        return FtepServiceDockerBuildInfo.class;
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
    public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner) throws HibernateException, SQLException, SQLException {
        String value = rs.getString(names[0]);
        return value == null ? new FtepServiceDockerBuildInfo() : convertToEntityAttribute(value);
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
        FtepServiceDockerBuildInfo nullSafeValue = getNullSafeValue(value);
        st.setString(index, convertToDatabaseColumn(nullSafeValue));
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        FtepServiceDockerBuildInfo original = (FtepServiceDockerBuildInfo) value;
        return value == null ? null : FtepServiceDockerBuildInfo.builder()
                .dockerBuildStatus(original.getDockerBuildStatus())
                .lastBuiltFingerprint(original.getLastBuiltFingerprint())
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
    private FtepServiceDockerBuildInfo getNullSafeValue(Object value) {
        return value == null ? new FtepServiceDockerBuildInfo() : (FtepServiceDockerBuildInfo) value;
    }
}
