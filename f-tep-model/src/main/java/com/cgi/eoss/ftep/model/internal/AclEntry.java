package com.cgi.eoss.ftep.model.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

;

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
@Table(name = "acl_entry")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AclEntry {

    @Id
    @Column(name = "id")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "acl_object_identity", referencedColumnName = "id", nullable = false)
    private AclObjectIdentity aclObjectIdentity;

    @Column(name = "ace_order", nullable = false)
    private int aceOrder;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sid", referencedColumnName = "id", nullable = false)
    private AclSid aclSid;

    @Column(name = "mask", nullable = false)
    private int mask;

    @Column(name = "granting", nullable = false)
    private boolean granting;

    @Column(name = "audit_success", nullable = false)
    private boolean auditSuccess;

    @Column(name = "audit_failure", nullable = false)
    private boolean auditFailure;

}