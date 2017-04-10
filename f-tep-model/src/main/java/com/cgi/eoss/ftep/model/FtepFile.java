package com.cgi.eoss.ftep.model;

import com.cgi.eoss.ftep.model.converters.UriStringConverter;
import com.google.common.collect.ComparisonChain;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.net.URI;
import java.util.UUID;

/**
 * <p>A raw reference to an F-TEP file-type object. Files may be physically located on disk, or simply references to
 * external files. These objects may be included in databaskets.</p>
 */
@Data
@EqualsAndHashCode(exclude = {"id"})
@Table(name = "ftep_files",
        indexes = {
                @Index(name = "ftep_files_uri_idx", columnList = "uri"),
                @Index(name = "ftep_files_resto_id_idx", columnList = "resto_id"),
                @Index(name = "ftep_files_owner_idx", columnList = "owner")},
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "uri"),
                @UniqueConstraint(columnNames = "resto_id")})
@NoArgsConstructor
@Entity
public class FtepFile implements FtepEntityWithOwner<FtepFile> {
    /**
     * <p>Internal unique identifier of the file.</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * <p>Unique URI for the file. May be in an F-TEP internal form, e.g. "<code>"ftep://refData/${owner}/${restoId}"</code>".</p>
     */
    @Column(name = "uri", nullable = false)
    @Convert(converter = UriStringConverter.class)
    private URI uri;

    /**
     * <p>Resto catalogue identifier for the file. Used to hydrate API responses with comprehensive metadata.</p>
     */
    @Column(name = "resto_id", nullable = false)
    private UUID restoId;

    /**
     * <p>File type.</p>
     */
    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private FtepFileType type;

    /**
     * <p>The user owning the file, typically the file uploader or job creator.</p>
     * <p>May be null, particularly in the case of {@link FtepFileType#EXTERNAL_PRODUCT}.</p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner")
    private User owner;

    /**
     * <p>Real filename to be used when materialising this file reference.</p>
     */
    @Column(name = "filename")
    private String filename;

    /**
     * <p>Construct a new FtepFile instance with the minimum mandatory (and unique) parameters.</p>
     *
     * @param uri
     * @param restoId
     */
    public FtepFile(URI uri, UUID restoId) {
        this.uri = uri;
        this.restoId = restoId;
    }

    public FtepFile(String reference) {
        // No-op, for SDR https://stackoverflow.com/questions/41324078/spring-data-rest-can-not-update-patch-a-list-of-child-entities-that-have-a-r
    }

    @Override
    public int compareTo(FtepFile o) {
        return ComparisonChain.start().compare(uri, o.uri).result();
    }

}
