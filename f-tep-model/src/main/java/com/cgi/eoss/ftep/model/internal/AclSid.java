package com.cgi.eoss.ftep.model.internal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * This entity represents one of the 4 Spring ACL tables. This is necessary to fully support all the
 * rules, pagination and filtering queries. This Entity is marked as immutable as it should
 * be never be changed. The Spring Security framework will manage making changes to these tables in
 * a secure fashion.
 *
 * @see Immutable
 */
@Entity
@Immutable
@Table(name = "acl_sid")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AclSid {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "principal", nullable = false)
    private boolean principal;

    @Column(name = "sid", nullable = false)
    private String sid;

}