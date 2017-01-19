package com.cgi.eoss.ftep.model;

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

/**
 * F-TEP user in Drupal.
 */
@Data
@EqualsAndHashCode(exclude = {"id"})
@Table(name = "user", indexes = {@Index(name = "idxName", columnList = "name")})
@NoArgsConstructor
@Entity
public class FtepUser implements FtepEntity<FtepUser>, Searchable {

    /**
     * Unique identifier of the user.
     */
    @Id
    @Column(name = "uid")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique user name as returned from the SP.
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * User contact e-mail as returned from the SP.
     */
    @Column(name = "mail")
    private String email;

    /**
     * Create a new FtepUser instance with the minimum required parameters
     *
     * @param name Account name of the user, as returned from the SSO SP.
     */
    public FtepUser(String name) {
        this.name = name;
    }

    @Override
    public int compareTo(FtepUser o) {
        return ComparisonChain.start().compare(name, o.name).result();
    }

}
