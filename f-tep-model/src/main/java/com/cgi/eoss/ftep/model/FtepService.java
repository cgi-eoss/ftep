package com.cgi.eoss.ftep.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Strings;
import com.google.common.collect.ComparisonChain;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Data
@EqualsAndHashCode(exclude = {"id", "contextFiles"})
@ToString(exclude = {"serviceDescriptor", "contextFiles"})
@Table(name = "ftep_services",
        indexes = {
                @Index(name = "ftep_services_name_idx", columnList = "name"),
                @Index(name = "ftep_services_owner_idx", columnList = "owner")
        },
        uniqueConstraints = {@UniqueConstraint(columnNames = "name")})
@NoArgsConstructor
@Entity
public class FtepService implements FtepEntityWithOwner<FtepService>, Searchable {

    private static final String DATA_SOURCE_NAME_PREFIX = "FTEP_";

    private static final String DEFAULT_APPLICATION_PORT = "8080/tcp";

    /**
     * <p>Internal unique identifier of the service.</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * <p>Unique name of the service, assigned by the owner.</p>
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * <p>Human-readable descriptive summary of the service.</p>
     */
    @Column(name = "description")
    private String description;

    /**
     * <p>The type of the service, e.g. 'processor' or 'application'.</p>
     */
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private Type type = Type.PROCESSOR;

    /**
     * <p>The user owning the service, typically the service creator.</p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner", nullable = false)
    private User owner;

    /**
     * <p>The docker container identifier to be used for running the service. It is expected that this is already
     * available on a worker.</p>
     */
    @Column(name = "docker_tag", nullable = false)
    private String dockerTag;

    /**
     * <p>Usage restriction of the service, e.g. 'open' or 'restricted'.</p>
     */
    @Column(name = "licence", nullable = false)
    @Enumerated(EnumType.STRING)
    private Licence licence = Licence.OPEN;

    /**
     * <p>Service availability status.</p>
     */
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status = Status.IN_DEVELOPMENT;

    /**
     * <p>The full definition of the WPS service, used to build ZOO-Kernel configuration.</p>
     */
    @Lob
    @org.hibernate.annotations.Type(type = "com.cgi.eoss.ftep.model.converters.FtepServiceDescriptorYamlConverter")
    @Column(name = "wps_descriptor")
    private FtepServiceDescriptor serviceDescriptor;

    /**
     * <p>The files required to build this service's docker image.</p>
     */
    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<FtepServiceContextFile> contextFiles = new HashSet<>();

    /**
     * <p>Docker container build information.</p>
     */
    @Lob
    @org.hibernate.annotations.Type(type = "com.cgi.eoss.ftep.model.converters.FtepServiceDockerBuildInfoYamlConverter")
    @Column(name = "docker_build_info")
    private FtepServiceDockerBuildInfo dockerBuildInfo;

    /**
     * <p>Application port to access GUI services.</p>
     */
    @Column(name = "application_port")
    private String applicationPort;

    /**
     * <p>The 'easy mode' definition of the WPS service. Only {@link FtepServiceDescriptor#dataInputs} is used.</p>
     */
    @Basic(fetch = FetchType.LAZY)
    @org.hibernate.annotations.Type(type = "com.cgi.eoss.ftep.model.converters.FtepServiceDescriptorYamlConverter")
    @Column(name = "easy_mode_descriptor")
    private FtepServiceDescriptor easyModeServiceDescriptor;

    /**
     * <p>The template to translate 'easy mode' parameters object into the full service parameters object (JSON to JSON).</p>
     */
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "easy_mode_parameter_template")
    private String easyModeParameterTemplate;

    /**
     * <p>A flag to determine whether the service should have READ access to /eodata.</p>
     */
    @Column(name = "mount_eodata")
    private boolean mountEodata = false;

    /**
     * <p>Create a new Service with the minimum required parameters.</p>
     *
     * @param name      Name of the service.
     * @param owner     The user owning the service.
     * @param dockerTag The tag used at build.
     */
    public FtepService(String name, User owner, String dockerTag) {
        this.name = name;
        this.owner = owner;
        this.dockerTag = dockerTag;
    }

    @Override
    public int compareTo(FtepService o) {
        return ComparisonChain.start().compare(name, o.name).result();
    }

    public void setContextFiles(Set<FtepServiceContextFile> contextFiles) {
        contextFiles.forEach(f -> f.setService(this));
        this.contextFiles = contextFiles;
    }

    public String getDataSourceName() {
        return DATA_SOURCE_NAME_PREFIX + this.name;
    }

    /**
     * <p>Application port to access GUI services.</p>
     */
    public String getApplicationPort() {
        return Optional.ofNullable(Strings.emptyToNull(applicationPort)).orElse(DEFAULT_APPLICATION_PORT);
    }

    public enum Type {
        PROCESSOR, APPLICATION
    }

    public enum Status {
        IN_DEVELOPMENT, AVAILABLE
    }

    public enum Licence {
        OPEN, RESTRICTED
    }
}
