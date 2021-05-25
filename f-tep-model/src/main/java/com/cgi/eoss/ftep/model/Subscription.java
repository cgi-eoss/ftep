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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * <p>Subscriptions enabling users to access the platform.</p>
 */
@Data
@EqualsAndHashCode(exclude = {"id"})
@Table(name = "ftep_subscriptions",
        indexes = {
            @Index(name = "ftep_subscriptions_owner_idx", columnList = "owner"),
            @Index(name = "ftep_subscriptions_creator_idx", columnList = "creator"),
            @Index(name = "ftep_subscriptions_canceller_idx", columnList = "canceller"),
        })
@NoArgsConstructor
@Entity
public class Subscription implements FtepEntityWithOwner<Subscription> {

    /**
     * <p>Unique internal identifier of the subscription.</p>
     */
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * <p>The user who owns this subscription.</p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner", nullable = false)
    private User owner;

    /**
     * Subscription package name.
     */
    @Column(name = "package_name")
    private String packageName;

    /**
     * Storage quota of the subscription package.
     */
    @Column(name = "storage_quota")
    private Long storageQuota;

    /**
     * Processing quota of the subscription package.
     */
    @Column(name = "processing_quota")
    private Long processingQuota;

    /**
     * <p>An admin's comment.</p>
     */
    @Column(name = "comment_text")
    private String commentText;

    /**
     * <p>The start date and time of the subscription.</p>
     */
    @Column(name = "subscription_start")
    private LocalDateTime subscriptionStart;

    /**
     * <p>The end date and time of the subscription.</p>
     */
    @Column(name = "subscription_end")
    private LocalDateTime subscriptionEnd;

    /**
     * Storage quota usage of the subscription package.
     */
    @Column(name = "storage_quota_usage")
    private Long storageQuotaUsage = 0L;

    /**
     * Processing quota usage of the subscription package.
     */
    @Column(name = "processing_quota_usage")
    private Long processingQuotaUsage = 0L;

    /**
     * <p>The date and time the subscription was created.</p>
     */
    @Column(name = "creation_time")
    private LocalDateTime creationTime = LocalDateTime.now(ZoneOffset.UTC);

    /**
     * <p>The user who created this subscription.</p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "creator")
    private User creator;

    /**
     * <p>The date and time the subscription was cancelled.</p>
     */
    @Column(name = "cancellation_time")
    private LocalDateTime cancellationTime;

    /**
     * <p>The user who cancelled this subscription.</p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "canceller")
    private User canceller;


    @Override
    public int compareTo(Subscription o) {
        return ComparisonChain.start().compare(owner.getName(), o.owner.getName()).result();
    }

    public Subscription(User currentUser, LocalDateTime currentTime) {
        this.owner = currentUser;
        this.subscriptionStart = currentTime;
        this.subscriptionEnd = currentTime.plusDays(30);
        this.creator = currentUser;
    }

    public boolean isActive(LocalDateTime currentTime) {
        return currentTime.isAfter(Optional.ofNullable(subscriptionStart).orElse(LocalDateTime.MIN))
                && currentTime.isBefore(Optional.ofNullable(subscriptionEnd).orElse(LocalDateTime.MAX))
                && cancellationTime == null;
    }

    public boolean exceedsProcessingQuota() {
        return processingQuotaUsage > Optional.ofNullable(processingQuota).orElse(Long.MAX_VALUE);
    }

    public boolean exceedsStorageQuota() {
        return storageQuotaUsage > Optional.ofNullable(storageQuota).orElse(Long.MAX_VALUE);
    }
}
