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

@Data
@EqualsAndHashCode(exclude = {"id"})
@Table(name = "ftep_databaskets",
        indexes = {@Index(name = "ftep_databaskets_name_idx", columnList = "name"), @Index(name = "ftep_databaskets_owner_idx", columnList = "owner")},
        uniqueConstraints = {@UniqueConstraint(name = "ftep_databaskets_name_owner_idx", columnNames = {"name", "owner"})})
@NoArgsConstructor
@Entity
public class Databasket implements FtepEntityWithOwner<Databasket>, Searchable, FtepFileReferencer {
    /**
     * <p>Internal unique identifier of the databasket.</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    /**
     * <p>Name of the group.</p>
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * <p>Human-readable descriptive summary of the databasket.</p>
     */
    @Column(name = "description")
    private String description;

    /**
     * <p>The user owning the databasket, typically the databasket creator.</p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner", nullable = false)
    private User owner;

    /**
     * <p>Member files of this databasket.</p>
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "ftep_databasket_files",
            joinColumns = @JoinColumn(name = "databasket_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "file_id", referencedColumnName = "id"),
            uniqueConstraints = @UniqueConstraint(name = "ftep_databasket_files_basket_file_idx", columnNames = {"databasket_id", "file_id"}))
    private Set<FtepFile> files = Sets.newHashSet();

    /**
     * <p>Create a new databasket with the minimum mandatory parameters.</p>
     *
     * @param name Name of the databasket. Must be unique per user.
     * @param owner User owning the databasket.
     */
    public Databasket(String name, User owner) {
        this.name = name;
        this.owner = owner;
    }

    @Override
    public int compareTo(Databasket o) {
        return ComparisonChain.start().compare(name, o.name).compare(owner.getId(), o.owner.getId()).result();
    }

    @Override
    public Boolean removeReferenceToFtepFile(FtepFile file) {
        return this.files.remove(file);
    }

}
