package com.cgi.eoss.ftep.model.internal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Immutable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Set;

/**
 * This entity represents one of the 4 Spring ACL tables. This is necessary to fully support all the
 * rules, pagination and filtering queries. This Entity is marked as immutable as it should
 * be never be changed. The Spring Security framework will manage making changes to these tables in
 * a secure fashion.
 * <p>
 * Making it Serializable to prevent error outlined in https://hibernate.atlassian.net/browse/HHH-7668
 *
 * @see Immutable
 */
@Entity
@Immutable
@Table(name = "acl_object_identity")
@Data
@EqualsAndHashCode(exclude = "aclEntries")
@ToString(exclude = "aclEntries")
@NoArgsConstructor
@AllArgsConstructor
public class AclObjectIdentity implements Serializable {

    @Id
    @Column(name = "ID")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "object_id_class", referencedColumnName = "id", nullable = false)
    private AclClass objectIdClass;

    @Column(name = "object_id_identity")
    private Long objectIdIdentity;

    @ManyToOne
    @JoinColumn(name = "parent_object", referencedColumnName = "id")
    private AclObjectIdentity parentObject;

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_sid", referencedColumnName = "id", nullable = false)
    private AclSid ownerSid;

    @Column(name = "entries_inheriting", nullable = false)
    private boolean entriesInheriting;

    @OneToMany(mappedBy = "aclObjectIdentity")
    private Set<AclEntry> aclEntries;

}