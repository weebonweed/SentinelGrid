package com.sentinelgrid.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.sentinelgrid.enums.AuthorType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "comments", indexes = {
    @Index(name = "idx_comments_post_id", columnList = "post_id"),
    @Index(name = "idx_comments_author_id", columnList = "author_id"),
    @Index(name = "idx_comments_depth_level", columnList = "depth_level"),
    @Index(name = "idx_comments_created_at", columnList = "created_at"),
    @Index(name = "idx_comments_parent_id", columnList = "parent_comment_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false, updatable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id", updatable = false)
    private Comment parentComment;

    @Column(name = "author_id", nullable = false, updatable = false)
    private UUID authorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "author_type", nullable = false, updatable = false, length = 10)
    private AuthorType authorType;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Column(name = "depth_level", nullable = false)
    private int depthLevel;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;
}
