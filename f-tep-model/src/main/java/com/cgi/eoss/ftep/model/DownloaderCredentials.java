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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * <p>Credentials (username/password or x509 certificate) for downloading from a given host/domain.</p>
 */
@Data
@EqualsAndHashCode(exclude = {"id"})
@Table(name = "ftep_credentials",
        indexes = {@Index(name = "ftep_credentials_host_idx", columnList = "host")},
        uniqueConstraints = {@UniqueConstraint(columnNames = {"host"})})
@NoArgsConstructor
@Entity
public class DownloaderCredentials implements FtepEntity<DownloaderCredentials> {

    public enum Type {
        BASIC,
        X509
    }

    /**
     * Unique database identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * <p>The host accessed by these credentials.</p>
     */
    @Column(name = "host", nullable = false)
    private String host;

    /**
     * <p>The type of these credentials: basic (username/password) or x509 (client certificate).</p>
     */
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private Type type;

    /**
     * <p>The username required to access the host.</p>
     */
    @Column(name = "username")
    private String username;

    /**
     * <p>The password (in plaintext) required to access the host.</p>
     */
    @Column(name = "password")
    private String password;

    /**
     * <p>The on-disk path to an x509 client certificate. The file must be managed externally.</p>
     */
    @Column(name = "certificate_path")
    private String certificatePath;

    @Builder
    public DownloaderCredentials(String host, String username, String password) {
        this.host = host;
        this.type = Type.BASIC;
        this.username = username;
        this.password = password;
    }

    public DownloaderCredentials(String host, String certificatePath) {
        this.host = host;
        this.type = Type.X509;
        this.certificatePath = certificatePath;
    }

    @Override
    public int compareTo(DownloaderCredentials o) {
        return ComparisonChain.start().compare(host, o.host).result();
    }

}
