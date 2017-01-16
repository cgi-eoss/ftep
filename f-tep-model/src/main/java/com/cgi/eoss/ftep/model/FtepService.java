package com.cgi.eoss.ftep.model;

import com.cgi.eoss.ftep.model.enums.AccessLevel;
import com.cgi.eoss.ftep.model.enums.ModeType;
import com.cgi.eoss.ftep.model.enums.ServiceLicence;
import com.cgi.eoss.ftep.model.enums.ServiceStatus;
import com.cgi.eoss.ftep.model.enums.ServiceType;
import com.google.common.collect.ComparisonChain;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

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
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * <p>F-TEP service definition for a defined processing workflow or GUI application. This should correspond to a service
 * exposed via WPS, which is described fully by a corresponding {@link FtepServiceDescriptor}.</p>
 */
@Data
@EqualsAndHashCode(exclude = {"id"})
@Table(name = "ftep_services", indexes = {
        @Index(name = "idxName", columnList = "name"),
        @Index(name = "idxOwner", columnList = "user_id"),
}, uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "user_id"})})
@NoArgsConstructor
@Entity
public class FtepService implements FtepEntity<FtepService>, Searchable {
    /**
     * <p>Unique identifier of the service.</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sid")
    private Long id;

    /**
     * <p>Name of the service, assigned by the owner.</p>
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * <p>Human-readable descriptive summary of the service.</p>
     */
    @Column(name = "description", nullable = false)
    private String description = "";

    /**
     * <p>The type of the service, e.g. 'processor' or 'application'.</p>
     */
    @Column(name = "kind", nullable = false)
    @Enumerated(EnumType.STRING)
    private ServiceType kind = ServiceType.PROCESSOR;

    /**
     * <p>The mode of the service, e.g. 'sscale' or 'bulk'.</p>
     */
    @Column(name = "mode", nullable = false)
    @Enumerated(EnumType.STRING)
    private ModeType mode = ModeType.SSCALE;

    /**
     * <p>Overall rating of the service given by users.</p>
     */
    @Column(name = "rating", nullable = false)
    private short rating = 0;

    /**
     * <p>The user owning the service, typically the service creator.</p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private FtepUser owner;

    /**
     * <p> Access level of the service </p>
     */
    @Column(name = "access_level", nullable = false)
    @Enumerated(EnumType.STRING)
    private AccessLevel accessLevel = AccessLevel.PUBLIC;

    /**
     * <p>CPU requirements of the service.</p>
     */
    @Column(name = "cpu", nullable = false)
    private String cpu = "";

    /**
     * <p>RAM requirements of the service.</p>
     */
    @Column(name = "ram", nullable = false)
    private String ram = "";

    /**
     * <p>Cost multiplier of the service in F-TEP coins.</p>
     */
    @Column(name = "cost", nullable = false)
    private short cost = 1;

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
    private ServiceStatus status = ServiceStatus.SUBMITTED;

    /**
     * <p>Create a new FtepService instance with the minimum required parameters.</p>
     *
     * @param name Name of the service.
     * @param owner The user owning the service.
     */
    public FtepService(String name, FtepUser owner) {
        this.name = name;
        this.owner = owner;
    }

    @Override
    public int compareTo(FtepService o) {
        return ComparisonChain.start().compare(name, o.name).compare(owner.getId(), o.owner.getId()).result();
    }

}
