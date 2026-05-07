package com.sentinelgrid.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sentinelgrid.entity.PostLike;
import com.sentinelgrid.enums.AuthorType;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, UUID> {
    boolean existsByPostIdAndAuthorIdAndAuthorType(UUID postId, UUID authorId, AuthorType authorType);
}
