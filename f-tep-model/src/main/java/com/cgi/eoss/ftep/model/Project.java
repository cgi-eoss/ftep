package com.cgi.eoss.ftep.model;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Sets;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.Set;

/**
 * <p>A collection of F-TEP resources, e.g. databaskets and job configurations, for convenient sharing and access.</p>
 */
@Data
@EqualsAndHashCode(exclude = {"id"})
@Table(name = "ftep_projects",
        indexes = {@Index(name = "ftep_projects_name_idx", columnList = "name"), @Index(name = "ftep_projects_owner_idx", columnList = "owner")},
        uniqueConstraints = {@UniqueConstraint(name = "ftep_projects_name_owner_idx", columnNames = {"name", "owner"})})
@NoArgsConstructor
@Entity
public class Project implements FtepEntityWithOwner<Project>, Searchable {

    /**
     * <p>Fallback project.</p>
     */
    public static final Project DEFAULT = new Project("Default Project", User.DEFAULT);

    /**
     * <p>Unique identifier of the project.</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * <p>Name of the project.</p>
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * <p>Human-readable descriptive summary of the project.</p>
     */
    @Column(name = "description")
    private String description;

    /**
     * <p>The user owning the project, typically the project creator.</p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner", nullable = false)
    private User owner;

    /**
     * <p>Databaskets belonging to this project.</p>
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "ftep_project_databaskets",
            joinColumns = @JoinColumn(name = "project_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "databasket_id", referencedColumnName = "id"),
            uniqueConstraints = @UniqueConstraint(name = "ftep_project_databaskets_ids_idx", columnNames = {"project_id", "databasket_id"}))
    private Set<Databasket> databaskets = Sets.newHashSet();

    /**
     * <p>Services belonging to this project.</p>
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "ftep_project_services",
            joinColumns = @JoinColumn(name = "project_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "service_id", referencedColumnName = "id"),
            uniqueConstraints = @UniqueConstraint(name = "ftep_project_services_ids_idx", columnNames = {"project_id", "service_id"}))
    private Set<FtepService> services = Sets.newHashSet();

    /**
     * <p>Job configurations belonging to this project.</p>
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "ftep_project_job_configs",
            joinColumns = @JoinColumn(name = "project_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "job_config_id", referencedColumnName = "id"),
            uniqueConstraints = @UniqueConstraint(name = "ftep_project_job_configs_ids_idx", columnNames = {"project_id", "job_config_id"}))
    private Set<JobConfig> jobConfigs = Sets.newHashSet();

    /**
     * <p>Create a new project with the minimum required parameters.</p>
     *
     * @param name Name of the project.
     * @param owner User owning the project.
     */
    public Project(String name, User owner) {
        this.name = name;
        this.owner = owner;
    }

    @Override
    public int compareTo(Project o) {
        return ComparisonChain.start().compare(name, o.name).compare(owner.getId(), o.owner.getId()).result();
    }

}
