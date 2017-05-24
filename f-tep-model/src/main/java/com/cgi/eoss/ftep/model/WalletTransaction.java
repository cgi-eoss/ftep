package com.cgi.eoss.ftep.model;

import com.google.common.collect.ComparisonChain;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * <p>A transaction adding or removing balance to or from a user's wallet.</p>
 */
@Data
@EqualsAndHashCode(exclude = {"id"})
@Table(name = "ftep_wallet_transactions",
        indexes = {@Index(name = "ftep_wallet_transactions_wallet_idx", columnList = "wallet")})
@NoArgsConstructor
@Entity
public class WalletTransaction implements FtepEntityWithOwner<WalletTransaction> {
    /**
     * <p>Unique internal identifier of the transaction.</p>
     */
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * <p>The wallet affected by this transaction.</p>
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "wallet", nullable = false)
    private Wallet wallet;

    /**
     * <p>The change to the wallet balance.</p>
     */
    @Column(name = "balance_change", nullable = false)
    private Integer balanceChange;

    /**
     * <p>The timestamp of the transaction.</p>
     */
    @Column(name = "transaction_time", nullable = false)
    private LocalDateTime transactionTime;

    /**
     * <p>Transaction type.</p>
     */
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private Type type;

    /**
     * <p>The ID of the associated action, e.g. the {@link Job#id} for {@link Type#JOB} or the {@link FtepFile#id} for
     * {@link Type#DOWNLOAD}.</p>
     */
    @Column(name = "associated_id")
    private Long associatedId;

    @Builder
    public WalletTransaction(Wallet wallet, Integer balanceChange, LocalDateTime transactionTime, Type type, Long associatedId) {
        this.wallet = wallet;
        this.balanceChange = balanceChange;
        this.transactionTime = transactionTime;
        this.type = type;
        this.associatedId = associatedId;
    }

    @Override
    public User getOwner() {
        return wallet.getOwner();
    }

    @Override
    public void setOwner(User owner) {
        // no-op; wallet transactions cannot change their owner
    }

    @Override
    public int compareTo(WalletTransaction o) {
        return ComparisonChain.start().compare(transactionTime, o.transactionTime).result();
    }

    public enum Type {
        CREDIT(null), JOB(Job.class), DOWNLOAD(FtepFile.class);

        private final Class<?> typeClass;

        Type(Class<?> cls) {
            this.typeClass = cls;
        }

        public Class<?> getTypeClass() {
            return typeClass;
        }
    }

}
