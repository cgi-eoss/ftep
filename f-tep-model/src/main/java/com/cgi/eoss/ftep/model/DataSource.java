package com.cgi.eoss.ftep.model;

import com.google.common.collect.ComparisonChain;
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
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Data
@EqualsAndHashCode(exclude = {"id"})
@Table(name = "ftep_data_sources",
        indexes = {@Index(name = "ftep_data_sources_name_idx", columnList = "name"), @Index(name = "ftep_data_sources_owner_idx", columnList = "owner")},
        uniqueConstraints = {@UniqueConstraint(columnNames = "name")})
@NoArgsConstructor
@Entity
public class DataSource implements FtepEntityWithOwner<DataSource>, Searchable {

    /**
     * <p>Unique internal identifier of the data source.</p>
     */
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * <p>Unique name of the data source, assigned by the owner.</p>
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * <p>The user owning the data source.</p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner", nullable = false)
    private User owner;

    public DataSource(String name, User owner) {
        this.name = name;
        this.owner = owner;
    }

    @Override
    public int compareTo(DataSource o) {
        return ComparisonChain.start().compare(name, o.name).result();
    }

}
