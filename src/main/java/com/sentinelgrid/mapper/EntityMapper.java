package com.sentinelgrid.mapper;

import org.springframework.stereotype.Component;

import com.sentinelgrid.dto.response.CommentResponse;
import com.sentinelgrid.dto.response.LikeResponse;
import com.sentinelgrid.dto.response.PostResponse;
import com.sentinelgrid.entity.Comment;
import com.sentinelgrid.entity.Post;
import com.sentinelgrid.entity.PostLike;

@Component
public class EntityMapper {

    public PostResponse toPostResponse(Post post) {
        return PostResponse.builder()
            .id(post.getId())
            .authorId(post.getAuthorId())
            .authorType(post.getAuthorType())
            .content(post.getContent())
            .createdAt(post.getCreatedAt())
            .build();
    }

    public CommentResponse toCommentResponse(Comment comment) {
        return CommentResponse.builder()
            .id(comment.getId())
            .postId(comment.getPost().getId())
            .parentCommentId(comment.getParentComment() != null
                ? comment.getParentComment().getId() : null)
            .authorId(comment.getAuthorId())
            .authorType(comment.getAuthorType())
            .content(comment.getContent())
            .depthLevel(comment.getDepthLevel())
            .createdAt(comment.getCreatedAt())
            .build();
    }

    public LikeResponse toLikeResponse(PostLike like) {
        return LikeResponse.builder()
            .likeId(like.getId())
            .postId(like.getPost().getId())
            .authorId(like.getAuthorId())
            .authorType(like.getAuthorType())
            .createdAt(like.getCreatedAt())
            .build();
    }
}
