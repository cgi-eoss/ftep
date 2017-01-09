package com.cgi.eoss.ftep.model;

import com.google.common.collect.ComparisonChain;
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
 * F-TEP project to organize products and services.
 */
@Data
@EqualsAndHashCode(exclude = { "id" })
@Table(name = "ftep_project", indexes = { @Index(name = "idxName", columnList = "name"),
        @Index(name = "idxOwner", columnList = "uid"), }, uniqueConstraints = {
                @UniqueConstraint(columnNames = { "name", "uid" }) })
@NoArgsConstructor
@Entity
public class FtepProject implements FtepEntity<FtepProject> {

    /**
     * Unique identifier of the project
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pid")
    private Long id;

    /**
     * Project name
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Human-readable descriptive summary of the project
     */
    @Column(name = "description")
    private String description = "";

    /**
     * The user owning the project, typically the project creator
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "uid")
    private FtepUser owner;

    /**
     * Set of jobs related to the project
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "ftep_project_x_job", joinColumns = @JoinColumn(name = "project_id", referencedColumnName = "pid"), 
        inverseJoinColumns = @JoinColumn(name = "job_id", referencedColumnName = "jid"))
    private Set<FtepJob> jobs;

    /**
     * Create a new FtepProject instance with the minimum required parameters
     * 
     * @param name
     *            Name of the project
     * @param owner
     *            The ID of the user owning the project
     */
    public FtepProject(String name, FtepUser owner) {
        this.name = name;
        this.owner = owner;
    }

    @Override
    public int compareTo(FtepProject o) {
        return ComparisonChain.start().compare(name, o.name).compare(owner.getId(), o.owner.getId()).result();
    }
}
