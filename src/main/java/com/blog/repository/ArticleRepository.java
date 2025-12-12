package com.blog.repository;

import com.blog.entity.Article;
import com.blog.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.beans.Visibility;
import java.util.List;

public interface ArticleRepository extends JpaRepository<Article,Long>{

    List<Article> findByAuthor(User author);


    List<Article>findByVisibility(Article.ArticleVisibility visibility);


    List<Article> findByAuthorIdInOrderByCreateTimeDesc(List<Long> authorIds);

    List<Article> findByAuthor_Id(Long authorId);

    List<Article> findByAuthorIdAndVisibility(Long authorId, Article.ArticleVisibility visibility);

    @Query("SELECT a FROM Article a " +
            "WHERE a.author.id = :authorId " +
            "AND (a.visibility = 'PUBLIC' OR a.visibility = 'FOLLOWERS')")
    List<Article> findVisibleToFollowers(@Param("authorId") Long authorId);
}
