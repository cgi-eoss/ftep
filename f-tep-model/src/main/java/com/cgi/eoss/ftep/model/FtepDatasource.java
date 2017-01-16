package com.cgi.eoss.ftep.model;

import com.github.jasminb.jsonapi.annotations.Type;
import com.google.common.collect.ComparisonChain;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * F-TEP datasource to search from.
 */
@Data
@EqualsAndHashCode(exclude = {"id"})
@Table(name = "ftep_datasource", indexes = {@Index(name = "idxName", columnList = "name"),}, uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name"})})
@NoArgsConstructor
@Entity
@Type("datasource")
public class FtepDatasource implements FtepEntity<FtepDatasource>, Searchable {

    /**
     * Unique identifier of the datasource
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * The name of this datasource
     */
    @Column(name = "name", nullable = false)
    private String name = "";

    /**
     * Human-readable descriptive summary of the datasource
     */
    @Column(name = "description", nullable = false)
    private String description = "";

    /**
     * The endpoint of the service
     */
    @Column(name = "endpoint", nullable = false)
    private String endPoint = "";

    /**
     * The template of the datasource
     */
    @Column(name = "template", nullable = false)
    private String template;

    /**
     * The class used to parse the response of the datasource
     */
    @Column(name = "parser", nullable = false)
    private String parser = "";

    /**
     * A reference to the access policy of the datasource
     */
    @Column(name = "policy")
    private String policy;

    /**
     * Flag to enable/disable the datasource
     */
    @Column(name = "enabled", nullable = false)
    private boolean enabled = false;

    /**
     * This is the domain managed by this datasource
     */
    @Column(name = "download_domain", nullable = false)
    private String downloadDomain;

    /**
     * Credentials data
     */
    @Column(name = "credentials_data", nullable = false)
    private String credentialsData;

    /**
     * Create a new FtepDatasource instance with the minimum required parameters
     *
     * @param name Name of the datasource
     * @param template Template of the datasource
     * @param dowloadDomain Domain managed by this datasource
     * @param credentialsData Credentials data
     */
    public FtepDatasource(String name, String template, String dowloadDomain, String credentialsData) {
        this.name = name;
        this.template = template;
        this.downloadDomain = dowloadDomain;
        this.credentialsData = credentialsData;
    }

    @Override
    public int compareTo(FtepDatasource o) {
        return ComparisonChain.start().compare(name, o.name).result();
    }
}
