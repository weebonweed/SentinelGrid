package com.sentinelgrid.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sentinelgrid.entity.Comment;
import com.sentinelgrid.enums.AuthorType;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.post.id = :postId AND c.authorType = :authorType")
    long countByPostIdAndAuthorType(@Param("postId") UUID postId, @Param("authorType") AuthorType authorType);
}
