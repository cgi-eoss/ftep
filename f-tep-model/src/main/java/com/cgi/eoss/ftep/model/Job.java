package com.cgi.eoss.ftep.model;

import com.cgi.eoss.ftep.model.converters.StringMultimapYamlConverter;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Multimap;
import lombok.Data;
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
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/**
 * <p>Representation of a single {@link JobConfig} execution.</p>
 */
@Data
@Table(name = "ftep_jobs",
        indexes = {@Index(name = "ftep_jobs_job_config_idx", columnList = "job_config"), @Index(name = "ftep_jobs_owner_idx", columnList = "owner")},
        uniqueConstraints = {@UniqueConstraint(columnNames = "ext_id")})
@NoArgsConstructor
@Entity
public class Job implements FtepEntity<Job> {

    /**
     * <p>Internal unique identifier of the job.</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * <p>The external unique job identifier. Must be provided (e.g. UUID from the WPS server).</p>
     */
    @Column(name = "ext_id", nullable = false, updatable = false)
    private String extId;

    /**
     * <p>The job configuration used to launch this specific execution.</p>
     */
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "job_config", nullable = false)
    private JobConfig config;

    /**
     * <p>The user executing this job.</p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner", nullable = false)
    private User owner;

    /**
     * <p>The date and time this job was launched.</p>
     */
    @Column(name = "start_time")
    private LocalDateTime startTime;

    /**
     * <p>The date and time this job execution ended.</p>
     */
    @Column(name = "end_time")
    private LocalDateTime endTime;

    /**
     * <p>The current execution status of the job.</p>
     */
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private JobStatus status = JobStatus.CREATED;

    /**
     * <p>Current stage of execution. Maybe arbitrarily set by service implementations to inform the user of
     * progress.</p>
     */
    @Column(name = "stage")
    private String stage;

    /**
     * <p>URL to the graphical interface if this is a {@link ServiceType#APPLICATION}.</p>
     */
    @Column(name = "gui_url")
    private String guiUrl;

    /**
     * <p>The job execution outputs.</p>
     */
    @Lob
    @Convert(converter = StringMultimapYamlConverter.class)
    @Column(name = "outputs")
    private Multimap<String, String> outputs;

    public Job(JobConfig config, String extId, User owner) {
        this.config = config;
        this.extId = extId;
        this.owner = owner;
    }

    @Override
    public int compareTo(Job o) {
        return ComparisonChain.start().compare(startTime, o.startTime).result();
    }

}
