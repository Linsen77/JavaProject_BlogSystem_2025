package com.blog.repository;

import com.blog.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArticleRepository extends JpaRepository<Article,Long>{

    List<Article> findByAuthor(String author);

    List<Article>findByVisibility(Article.ArticleVisibility visibility);
}
