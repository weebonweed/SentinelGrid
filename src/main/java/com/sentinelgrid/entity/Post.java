package com.sentinelgrid.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.sentinelgrid.enums.AuthorType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "posts", indexes = {
    @Index(name = "idx_posts_author_id", columnList = "author_id"),
    @Index(name = "idx_posts_author_type", columnList = "author_type"),
    @Index(name = "idx_posts_created_at", columnList = "created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "author_id", nullable = false, updatable = false)
    private UUID authorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "author_type", nullable = false, updatable = false, length = 10)
    private AuthorType authorType;

    @Column(name = "content", nullable = false, length = 2000)
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;
}
