package com.cgi.eoss.ftep.model;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Sets;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.Set;

/**
 * <p>F-TEP group of users for access control lists.</p>
 */
@Data
@EqualsAndHashCode(exclude = {"id"})
@Table(name = "ftep_groups",
        indexes = {@Index(name = "ftep_groups_name_idx", columnList = "name"), @Index(name = "ftep_groups_owner_idx", columnList = "owner")},
        uniqueConstraints = {@UniqueConstraint(name = "ftep_groups_name_owner_idx", columnNames = {"name", "owner"})})
@NoArgsConstructor
@Entity
public class Group implements FtepEntity<Group>, Searchable, GrantedAuthority {

    private static final String GROUP_AUTHORITY_PREFIX = "GROUP_";

    /**
     * <p>Unique identifier of the group.</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gid")
    private Long id;

    /**
     * <p>Name of the group.</p>
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * <p>Human-readable descriptive summary of the group.</p>
     */
    @Column(name = "description")
    private String description;

    /**
     * <p>The user owning the group, typically the group creator.</p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner", nullable = false)
    private User owner;

    /**
     * <p>Members of this group.</p>
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "ftep_group_member",
            joinColumns = @JoinColumn(name = "group_id", referencedColumnName = "gid"),
            inverseJoinColumns = @JoinColumn(name = "user_id", referencedColumnName = "uid"),
            uniqueConstraints = @UniqueConstraint(name = "ftep_group_member_user_group_idx", columnNames = {"group_id", "user_id"}))
    private Set<User> members = Sets.newHashSet();

    /**
     * <p>Create a new group of users with the minimum parameters.</p>
     *
     * @param name Name of the group.
     * @param owner User owning the group.
     */
    public Group(String name, User owner) {
        this.name = name;
        this.owner = owner;
    }

    @Override
    public int compareTo(Group o) {
        return ComparisonChain.start().compare(name, o.name).compare(owner.getId(), o.owner.getId()).result();
    }

    @Override
    public String getAuthority() {
        return GROUP_AUTHORITY_PREFIX + id;
    }

}
