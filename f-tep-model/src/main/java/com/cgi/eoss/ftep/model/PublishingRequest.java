package com.cgi.eoss.ftep.model;

import com.google.common.collect.ComparisonChain;
import lombok.Builder;
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
import java.time.LocalDateTime;

/**
 * <p>A transaction adding or removing balance to or from a user's wallet.</p>
 */
@Data
@EqualsAndHashCode(exclude = {"id"})
@Table(name = "ftep_publishing_requests",
        indexes = {@Index(name = "ftep_publishing_requests_owner_idx", columnList = "owner")},
        uniqueConstraints = {@UniqueConstraint(name = "ftep_publishing_requests_owner_object_idx", columnNames = {"owner", "type", "associated_id"})})
@NoArgsConstructor
@Entity
public class PublishingRequest implements FtepEntityWithOwner<PublishingRequest> {
    /**
     * <p>Unique internal identifier of the transaction.</p>
     */
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * <p>The user making the request.</p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner", nullable = false)
    private User owner;

    /**
     * <p>The UTC timestamp when the request was created. Set by JPA event listener.</p>
     */
    @Column(name = "request_time", insertable = false, updatable = false)
    private LocalDateTime requestTime;

    /**
     * <p>The UTC timestamp when the request was last updated. Set by JPA event listener.</p>
     */
    @Column(name = "updated_time", insertable = false, updatable = false)
    private LocalDateTime updatedTime;

    /**
     * <p>The current status of the request.</p>
     */
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    /**
     * <p>Associated object type.</p>
     */
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private Type type;

    /**
     * <p>The ID of the associated object to be published, e.g. the {@link JobConfig#id} for {@link Type#JOB_CONFIG} or
     * the {@link FtepService#id} for {@link Type#SERVICE}.</p>
     */
    @Column(name = "associated_id")
    private Long associatedId;

    @Builder
    public PublishingRequest(User owner, Status status, Type type, Long associatedId) {
        this.owner = owner;
        this.status = status;
        this.type = type;
        this.associatedId = associatedId;
    }

    @Override
    public int compareTo(PublishingRequest o) {
        return ComparisonChain.start().compare(requestTime, o.requestTime).result();
    }

    public enum Status {
        REQUESTED, GRANTED, NEEDS_INFO, REJECTED
    }

    public enum Type {
        DATABASKET(Databasket.class),
        DATASOURCE(DataSource.class),
        FILE(FtepFile.class),
        SERVICE(FtepService.class),
        GROUP(Group.class),
        JOB_CONFIG(JobConfig.class),
        PROJECT(Project.class);

        private final Class<?> typeClass;

        Type(Class<?> cls) {
            this.typeClass = cls;
        }

        public Class<?> getTypeClass() {
            return typeClass;
        }
    }

}
