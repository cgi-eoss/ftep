package com.cgi.eoss.ftep.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cgi.eoss.ftep.model.enums.AccessLevel;
import com.google.common.collect.ComparisonChain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * F-TEP file representing either the output product or result item.
 */
@Data
@EqualsAndHashCode(exclude = { "id" })
@Table(name = "ftep_file")
@NoArgsConstructor
@Entity
public class FtepFile implements FtepEntity<FtepFile> {

    /**
     * Unique identifier of the file
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fid")
    private Long id;

    /**
     * The name of this file
     */
    @Column(name = "name", nullable = false)
    private String name = "";

    /**
     * The location of the file
     */
    @Column(name = "url", nullable = false)
    private String url = "";

    /**
     * The access level of this file. e.g. private, public, or group
     */
    @Column(name = "datasource", nullable = false)
    private AccessLevel accessLevel = AccessLevel.PRIVATE;

    @Override
    public int compareTo(FtepFile o) {
        return ComparisonChain.start().compare(name, o.name).compare(url, o.url).result();
    }
}
