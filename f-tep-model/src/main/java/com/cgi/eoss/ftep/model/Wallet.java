package com.cgi.eoss.ftep.model;

import com.google.common.collect.ComparisonChain;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(exclude = {"id"})
@ToString(exclude = {"transactions"})
@Table(name = "ftep_wallets",
        indexes = {@Index(name = "ftep_wallets_owner_idx", columnList = "owner")},
        uniqueConstraints = {@UniqueConstraint(columnNames = "owner")})
@NoArgsConstructor
@Entity
public class Wallet implements FtepEntityWithOwner<Wallet> {

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

    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WalletTransaction> transactions = new ArrayList<>();

    /**
     * <p>Instantiate a new Wallet for the given User with the default starting balance.</p>
     *
     * @param owner The user who owns this wallet.
     */
    public Wallet(User owner) {
        this.owner = owner;
    }

    @Override
    public int compareTo(Wallet o) {
        return ComparisonChain.start().compare(owner.getName(), o.owner.getName()).result();
    }

}
