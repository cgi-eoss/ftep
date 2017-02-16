package com.cgi.eoss.ftep.model;

import com.cgi.eoss.ftep.model.converters.FtepServiceDescriptorYamlConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ComparisonChain;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Convert;
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
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Data
@EqualsAndHashCode(exclude = {"id"})
@Table(name = "ftep_services",
        indexes = {@Index(name = "ftep_services_name_idx", columnList = "name"), @Index(name = "ftep_services_owner_idx", columnList = "owner")},
        uniqueConstraints = {@UniqueConstraint(columnNames = "name")})
@NoArgsConstructor
@Entity
public class FtepService implements FtepEntity<FtepService>, Searchable {
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
    private ServiceType type = ServiceType.PROCESSOR;

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
    private ServiceLicence licence = ServiceLicence.OPEN;

    /**
     * <p>Service availability status.</p>
     */
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ServiceStatus status = ServiceStatus.IN_DEVELOPMENT;

    /**
     * <p>The full definition of the WPS service, used to build ZOO-Kernel configuration.</p>
     */
    @Lob
    @Convert(converter = FtepServiceDescriptorYamlConverter.class)
    @Column(name = "wps_descriptor")
    @JsonIgnore
    private FtepServiceDescriptor serviceDescriptor;

    /**
     * <p>Create a new Service with the minimum required parameters.</p>
     *
     * @param name Name of the service.
     * @param owner The user owning the service.
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

}
