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

/**
 * <p>A comment linked to a user.</p>
 */
@Data
@EqualsAndHashCode(exclude = {"id"})
@Table(name = "ftep_comments",
        indexes = {
            @Index(name = "ftep_comments_owner_idx", columnList = "owner"),
            @Index(name = "ftep_comments_creator_idx", columnList = "creator")
})
@NoArgsConstructor
@Entity
public class Comment implements FtepEntityWithOwner<Comment> {

    /**
     * <p>Unique internal identifier of the comment.</p>
     */
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * <p>The user the comment is linked to.</p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner", nullable = false)
    private User owner;

    /**
     * <p>The creation time of the comment.</p>
     */
    @Column(name = "creation_time")
    private LocalDateTime creationTime = LocalDateTime.now(ZoneOffset.UTC);

    /**
     * <p>The user who created this comment.</p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "creator")
    private User creator;

    /**
     * <p>The comment text.</p>
     */
    @Column(name = "comment_text")
    private String commentText;

    @Override
    public int compareTo(Comment o) {
        return ComparisonChain.start().compare(owner.getName(), o.owner.getName()).result();
    }
}
