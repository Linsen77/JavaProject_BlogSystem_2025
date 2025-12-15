package com.blog.controller;

import com.blog.entity.*;
import com.blog.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private BookmarkService bookmarkService;

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
    public List<Article> getArticleByAuthor(@PathVariable User author)
    {
        return articleService.getArticleByAuthor(author);
    }

    //删除文章
    @DeleteMapping("/{id}")
    public void deleteArticle(@PathVariable Long id){
        articleService.deleteArticle(id);
    }


    //评论功能
    @PostMapping("/{articleId}/comments")
    public ResponseEntity<Comment> addComment(@PathVariable Long articleId, @RequestParam Long userId, @RequestParam String content){

        //调佣CommentService添加评论
        Comment comment = commentService.addComment(articleId, userId, content);

        //调用notificationService推送通知
        Article article = comment.getArticle();
        notificationService.sendNotificationToAuthor(article.getAuthor(), "你的文章收到了新评论！");

        return ResponseEntity.ok(comment);
    }

    //点赞功能
    @PostMapping("/{articleId}/likes")
    public ResponseEntity<Likes> addLike(@PathVariable Long articleId, @RequestParam Long userId){

        //调用LikeService来添加点赞
        Likes like = likeService.addLike(articleId, userId);

        //调用notificationService来推送通知
        Article article = like.getArticle();
        notificationService.sendNotificationToAuthor(article.getAuthor(), "你收获了一条点赞~");

        return ResponseEntity.ok(like);
    }

    //收藏功能
    @PostMapping("/{articleId}/bookmark")
    public ResponseEntity<Bookmark> addBookmark(@PathVariable Long articleId, @RequestParam Long userId){

        //调用BookmarkService来添加收藏
        Bookmark bookmark = bookmarkService.addBookmark(articleId, userId);

        //调用notificationService来推送通知
        Article article = bookmark.getArticle();
        notificationService.sendNotificationToAuthor(article.getAuthor(), "你的文章被收藏啦(*^▽^*)");

        return ResponseEntity.ok(bookmark);

    }

    //获取用户收藏的文章
    @GetMapping("/bookmarks")
    public ResponseEntity<List<Article>> getAllBookmarks(@RequestParam Long userId){

        //调用BoookmarkService来获取收藏的文章
        List<Article> bookmarkArticles = bookmarkService.getBookmarks(userId);

        return ResponseEntity.ok(bookmarkArticles);
    }
}
