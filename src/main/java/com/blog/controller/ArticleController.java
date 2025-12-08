package com.blog.controller;

import com.blog.entity.Article;
import com.blog.service.ArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    //创建或更新文章
    @PostMapping
    public Article createOrUpdateArticle(@RequestBody Article article){
        return articleService.saveArticle(article);
    }

    //获取所有文章
    @GetMapping
    public List<Article> getAllArticles(){
        return articleService.getAllArticles();
    }

    //根据 ID 获取文章
    @GetMapping("/{id}")
    public Optional<Article> getArticleById(@PathVariable Long id){
        return articleService.getArticleById(id);
    }

    //根据作者获取文章
    @GetMapping("/author/{author}")
    public List<Article> getArticleByAuthor(@PathVariable String author)
    {
        return articleService.getArticleByAuthor(author);
    }

    //删除文章
    @DeleteMapping("/{id}")
    public void deleteArticle(@PathVariable Long id){
        articleService.deleteArticle(id);
    }
}
