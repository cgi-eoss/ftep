package com.cgi.eoss.ftep.model;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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

import com.cgi.eoss.ftep.model.enums.AccessLevel;
import com.google.common.collect.ComparisonChain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * F-TEP databasket object to represent a convenient collection of result items and/or products for re-use.
 */
@Data
@EqualsAndHashCode(exclude = { "id" })
@Table(name = "ftep_databasket", indexes = { @Index(name = "idxName", columnList = "name"),
        @Index(name = "idxOwner", columnList = "uid"), }, uniqueConstraints = {
                @UniqueConstraint(columnNames = { "name", "uid" }) })
@NoArgsConstructor
@Entity
public class FtepDatabasket implements FtepEntity<FtepDatabasket>, Searchable {

    /**
     * Unique identifier of the databasket
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idb")
    private Long id;

    /**
     * The name of this databasket
     */
    @Column(name = "name", nullable = false)
    private String name = "";

    /**
     * Human-readable descriptive summary of the databasket
     */
    @Column(name = "description", nullable = false)
    private String description = "";

    /**
     * The accesslevel of the databasket. e.g. private, public, group
     */
    @Column(name = "access_level", nullable = false)
    @Enumerated(EnumType.STRING)
    private AccessLevel accessLevel = AccessLevel.PRIVATE;

    /**
     * The the user owning the databasket, typically the databasket creator
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "uid")
    private FtepUser owner;

    /**
     * The group this databasket belongs to
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name="ftep_databasket_x_group",
            joinColumns=@JoinColumn(name="db_id", referencedColumnName="idb"),
            inverseJoinColumns=@JoinColumn(name="group_id", referencedColumnName="gid"))
    private Set<FtepGroup> groups;

    /**
     * Create a new FtepDatabasket instance with the minimum required parameters
     * 
     * @param name
     *            Name of the databasket
     * @param owner
     *            The user owning the databasket
     */
    public FtepDatabasket(String name, FtepUser owner) {
        this.name = name;
        this.owner = owner;
    }

    @Override
    public int compareTo(FtepDatabasket o) {
        return ComparisonChain.start().compare(name, o.name).compare(owner, o.owner).result();
    }
}
