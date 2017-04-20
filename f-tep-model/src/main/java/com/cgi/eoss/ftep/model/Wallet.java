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
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Data
@EqualsAndHashCode(exclude = {"id"})
@Table(name = "ftep_wallets",
        indexes = {@Index(name = "ftep_wallets_owner_idx", columnList = "owner")},
        uniqueConstraints = {@UniqueConstraint(columnNames = "owner")})
@NoArgsConstructor
@Entity
public class Wallet implements FtepEntity<Wallet> {

    /**
     * <p>Unique internal identifier of the wallet.</p>
     */
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * <p>The user who owns this wallet.</p>
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner", nullable = false)
    private User owner;

    /**
     * Current wallet balance.
     */
    @Column(name = "balance", nullable = false)
    private Integer balance = 0;

    /**
     * <p>Instantiate a new Wallet for the given User with the default starting balance.</p>
     *
     * @param owner The user who owns this wallet.
     */
    public Wallet(User owner) {
        this.owner = owner;
    }

    /**
     * <p>Instantiate a new Wallet for the given User with the given balance.</p>
     *
     * @param owner The owner who owns this wallet.
     * @param balance The starting balance of the wallet.
     */
    public Wallet(User owner, int balance) {
        this.owner = owner;
        this.balance = balance;
    }

    @Override
    public int compareTo(Wallet o) {
        return ComparisonChain.start().compare(owner.getName(), o.owner.getName()).result();
    }

}
