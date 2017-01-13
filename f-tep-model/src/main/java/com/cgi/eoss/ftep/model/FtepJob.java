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
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * F-TEP job representing the inputs and outputs of a service user has run.
 */
@Data
@EqualsAndHashCode(exclude = {"id"})
@Table(name = "ftep_job", indexes = {@Index(name = "idxJobId", columnList = "jid"),}, uniqueConstraints = {
        @UniqueConstraint(columnNames = {"jid"})})
@NoArgsConstructor
@Entity
public class FtepJob implements FtepEntity<FtepJob> {

    /**
     * Unique identifier of the job
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * This is the job ID as assigned by the WPS server
     */
    @Column(name = "jid", nullable = false)
    private String jobId;

    /**
     * Input data
     */
    @Column(name = "inputs")
    private String inputs = "";

    /**
     * Output data
     */
    @Column(name = "outputs")
    private String outputs = "";

    /**
     * This contains the host:port of the vnc server (if any)
     */
    @Column(name = "guiendpoint", nullable = true)
    private String guiEndPoint;

    /**
     * The user owning the job, typically the job creator
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "uid")
    private FtepUser owner;

    /**
     * The service this job is running on
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sid")
    private FtepService service;

    /**
     * Short description of current activity of the job
     */
    @Column(name = "step", nullable = false)
    private String step;

    /**
     * Create a new FtepJob instance with the minimum required parameters
     *
     * @param jobId The job ID as assigned by the WPS server
     * @param owner The user who owns the job
     * @param service The service this job is running on
     * @param step Short description of current activity of the job
     */
    public FtepJob(String jobId, FtepUser owner, FtepService service, String step) {
        this.jobId = jobId;
        this.owner = owner;
        this.service = service;
        this.step = step;
    }

    @Override
    public int compareTo(FtepJob o) {
        return ComparisonChain.start().compare(jobId, o.jobId).result();
    }
}
