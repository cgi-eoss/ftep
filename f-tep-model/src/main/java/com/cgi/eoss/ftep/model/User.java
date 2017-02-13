package com.cgi.eoss.ftep.model;

import com.google.common.collect.ComparisonChain;
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

/**
 * <p>F-TEP user account. Parameters are expected to be provided by an external SSO IdP.</p>
 */
@Data
@EqualsAndHashCode(exclude = {"id"})
@Table(name = "ftep_users", indexes = {@Index(name = "ftep_users_name_idx", columnList = "name")})
@NoArgsConstructor
@Entity
public class User implements FtepEntity<User>, Searchable {

    /**
     * <p>Unique internal identifier of the user.</p>
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
     * <p>The F-TEP application role of the user.</p>
     */
    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role = Role.GUEST;

    /**
     * Create a new User instance with the minimum required parameters
     *
     * @param name Account name of the user, as returned from the SSO SP.
     */
    public User(String name) {
        this.name = name;
    }

    @Override
    public int compareTo(User o) {
        return ComparisonChain.start().compare(name, o.name).result();
    }

}
