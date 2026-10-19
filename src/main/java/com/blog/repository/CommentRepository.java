package com.blog.repository;

import com.blog.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    //获取评论列表
    List<Comment> findByArticleId(Long id);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM Comment c WHERE c.article.id = :articleId")
    int deleteForArticle(@org.springframework.data.repository.query.Param("articleId") Long articleId);

}
