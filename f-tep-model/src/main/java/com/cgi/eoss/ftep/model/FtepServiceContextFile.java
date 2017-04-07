package com.cgi.eoss.ftep.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ComparisonChain;
import lombok.Builder;
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
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * <p>A file to be added to the Docker build context of an {@link FtepService}.</p>
 * <p>At a minimum, most services should include a Dockerfile, and most will include processing scripts, configuration,
 * and similar files, which will be added to the docker image via ADD or COPY commands in the Dockerfile.</p>
 */
@Data
@EqualsAndHashCode(exclude = {"id"})
@Table(name = "ftep_service_files",
        indexes = {@Index(name = "ftep_service_files_filename_idx", columnList = "filename"), @Index(name = "ftep_service_files_service_idx", columnList = "service")},
        uniqueConstraints = {@UniqueConstraint(name = "ftep_service_files_filename_service_idx", columnNames = {"filename", "service"})})
@NoArgsConstructor
@Entity
public class FtepServiceContextFile implements FtepEntityWithOwner<FtepServiceContextFile> {

    /**
     * <p>Unique identifier of the service file.</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * <p>The service for which this file is used.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service", nullable = false)
    private FtepService service;

    /**
     * <p>Filename to be used when materialising the "docker build" context.</p>
     */
    @Column(name = "filename", nullable = false)
    private String filename;

    /**
     * <p>Whether the file should be treated as executable when materialised on a filesystem.</p>
     */
    @Column(name = "executable", nullable = false)
    private boolean executable = false;

    /**
     * <p>The file content.</p>
     */
    @Lob
    @Column(name = "content")
    private String content;

    /**
     * <p>Construct a new service file with the minimum mandatory parameters (without file content).</p>
     */
    public FtepServiceContextFile(FtepService service, String filename) {
        this.service = service;
        this.filename = filename;
    }
    /**
     * <p>Construct a new service file with all properties set.</p>
     */
    @Builder
    public FtepServiceContextFile(FtepService service, String filename, boolean executable, String content) {
        this.service = service;
        this.filename = filename;
        this.executable = executable;
        this.content = content;
    }

    @Override
    public int compareTo(FtepServiceContextFile o) {
        return ComparisonChain.start().compare(service, o.service).compare(filename, o.filename).result();
    }

    @Override
    @JsonIgnore
    public User getOwner() {
        return service.getOwner();
    }

    @Override
    @JsonIgnore
    public void setOwner(User owner) {
        // no-op; service files should not change their owner's service
    }
}
