package com.cgi.eoss.ftep.model;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>Configuration representing the inputs of a specific service execution.</p>
 * <p>This object is suitable for sharing for re-execution, independent of the actual run status and outputs.</p>
 */
@Data
@EqualsAndHashCode(exclude = {"id", "parent"})
@ToString(exclude = {"inputs", "parent"})
@Table(name = "ftep_job_configs",
        indexes = {
                @Index(name = "ftep_job_configs_service_idx", columnList = "service"),
                @Index(name = "ftep_job_configs_owner_idx", columnList = "owner"),
                @Index(name = "ftep_job_configs_label_idx", columnList = "label")
        },
        uniqueConstraints = @UniqueConstraint(columnNames = {"owner", "service", "inputs", "parent"}))
@NoArgsConstructor
@Entity
public class JobConfig implements FtepEntityWithOwner<JobConfig> {

    /**
     * <p>Unique identifier of the job.</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * <p>The user owning the job configuration.</p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner", nullable = false)
    private User owner;

    /**
     * <p>Parent job to attach to, if any.</p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent")
    private Job parent;

    /**
     * <p>The service this job is configuring.</p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "service")
    private FtepService service;

    /**
     * <p>The job input parameters.</p>
     */
    @Lob
    @Type(type = "com.cgi.eoss.ftep.model.converters.StringMultimapYamlConverter")
    @Column(name = "inputs")
    private Multimap<String, String> inputs = HashMultimap.create();

    /**
     * <p>Human-readable label to tag and identify jobs launched with this configuration.</p>
     */
    @Column(name = "label")
    private String label;

    /**
     * <p>Tag and identify parameter that will be dynamically getting values.</p>
     */
    @Column(name = "systematic_parameter")
    private String systematicParameter;

    /**
     * <p>The FtepFiles required as job inputs.</p>
     */
    @OneToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "ftep_job_config_input_files",
            joinColumns = @JoinColumn(name = "job_config_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "file_id", nullable = false),
            indexes = @Index(name = "ftep_job_config_input_files_job_config_file_idx", columnList = "job_config_id, file_id", unique = true)
    )
    private Set<FtepFile> inputFiles = new HashSet<>();

    /**
     * <p>Create a new JobConfig instance with the minimum required parameters.</p>
     *
     * @param owner   The user who owns the job
     * @param service The service this job is running on
     */
    public JobConfig(User owner, FtepService service) {
        this.owner = owner;
        this.service = service;
    }

    public void addInputFile(FtepFile inputFile) {
        inputFiles.add(inputFile);
    }

    @Override
    public int compareTo(JobConfig o) {
        return ComparisonChain.start().compare(service, o.service).result();
    }
}
