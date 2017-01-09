package com.cgi.eoss.ftep.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.google.common.collect.ComparisonChain;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * F-TEP user in Drupal.
 */
@Data
@Table(name = "user")
@NoArgsConstructor
@Entity
public class FtepUser implements FtepEntity<FtepUser> {

    /**
     * Unique identifier of the user as returned from SP
     */
    @Id
    @Column(name = "uid")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique user name as returned from the SP
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * User contact e-mail as returned from the SP
     */
    @Column(name = "mail")
    private String email;

    /**
     * Create a new FtepUser instance with the minimum required parameters
     * 
     * @param username
     * @param email
     */
    public FtepUser(String name) {
        this.name = name;
    }

    @Override
    public int compareTo(FtepUser o) {
        return ComparisonChain.start().compare(name, o.name).result();
    }
}
