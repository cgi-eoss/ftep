package com.cgi.eoss.ftep.model;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Sets;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

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
 * F-TEP group to share products and services between users.
 */
@Data
@EqualsAndHashCode(exclude = {"id"})
@Table(name = "ftep_group", indexes = {@Index(name = "idxName", columnList = "name"),
        @Index(name = "idxOwner", columnList = "uid"),}, uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name", "uid"})})
@NoArgsConstructor
@Entity
public class FtepGroup implements FtepEntity<FtepGroup>, Searchable {

    /**
     * Unique identifier of the group
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gid")
    private Long id;

    /**
     * Name of the group
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Human-readable descriptive summary of the group
     */
    @Column(name = "description", nullable = false)
    private String description = "";

    /**
     * The user owning the group, typically the group creator
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "uid")
    private FtepUser owner;

    /**
     * Databaskets that belong to this group
     */
    @ManyToMany(mappedBy = "groups", fetch = FetchType.EAGER)
    private Set<FtepDatabasket> databaskets = Sets.newHashSet();

    /**
     * List of members of this group
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "ftep_group_member", joinColumns = @JoinColumn(name = "group_id", referencedColumnName = "gid"),
            inverseJoinColumns = @JoinColumn(name = "userid", referencedColumnName = "uid"))
    private Set<FtepUser> members = Sets.newHashSet();

    /**
     * Create a new FtepGroup instance with the minimum required parameters
     *
     * @param name Name of the group
     * @param owner User who created the group
     */
    public FtepGroup(String name, FtepUser owner) {
        this.name = name;
        this.owner = owner;
    }

    @Override
    public int compareTo(FtepGroup o) {
        return ComparisonChain.start().compare(name, o.name).compare(owner.getId(), o.owner.getId()).result();
    }

}
