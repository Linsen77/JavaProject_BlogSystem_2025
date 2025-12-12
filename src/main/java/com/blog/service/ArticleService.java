package com.blog.service;

import com.blog.entity.Article;
import com.blog.entity.User;
import com.blog.repository.ArticleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ArticleService {

    @Autowired
    private ArticleRepository articleRepository;

    //创建或更新文章
    public Article saveArticle(Article article){
        //设置创建时间和更新时间
        article.setCreateDate((article.getCreateDate() == null? LocalDateTime.now():article.getCreateDate()));
        article.setCreateDate(LocalDateTime.now());
        return articleRepository.save(article);
    }

    //获取所有文章
    public List<Article> getAllArticles(){
        return articleRepository.findAll();
    }

    //根据ID获取文章
    public Optional<Article> getArticleById(long id){
        return articleRepository.findById(id);
    }

    //根据作者获取文章
    public List<Article>getArticleByAuthor(User author){
        return articleRepository.findByAuthor(author); //根据作者查找文章
    }

    //删除文章
    public void deleteArticle(Long id){
        articleRepository.deleteById(id);
    }

    //查看粉丝可见的文章
    public List<Article> findVisibleToFollowers(Long authorId) {
        return articleRepository.findVisibleToFollowers(authorId);
    }

    public List<Article> findByAuthorIdAndVisibility(Long authorId, Article.ArticleVisibility visibility) {
        return articleRepository.findByAuthorIdAndVisibility(authorId, visibility);
    }
}
