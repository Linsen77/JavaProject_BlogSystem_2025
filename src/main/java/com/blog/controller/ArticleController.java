package com.blog.controller;

import com.blog.dto.ArticleDTO;
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

    //给文章添加标签
    @PostMapping("/{id}/tags")
    public Article addTagsToArticle(@PathVariable Long id,@RequestBody List<Long>tagIds){
        return articleService.addTagsToArticle(id,tagIds);
    }

    //获取文章的标签
    @GetMapping("/{id}/tags")
    public List<Tag>getTagOfArticle(@PathVariable Long id){
        return articleService.getTagsOfArticle(id);
    }

    //按标题搜索文章
    @GetMapping("/search/byTitle")
    public List<Article> searchByTitle(@RequestParam String keyword){
        //调用服务层方法，按标题关键词搜索文章
        return articleService.searchByTitle(keyword);
    }

    //按标签搜索文章
    @GetMapping("/search/byTag")// HTTP GET 请求
    public List<Article> searchByTag(@RequestParam String tagName){
        //调用服务层方法，按标签名称搜索文章
        return articleService.searchByTag(tagName);
    }

    //获取热门文章（按阅读量）
    @GetMapping("/hot") //HTTP GET 请求
    public List<Article>getHotArticle(@RequestParam(defaultValue = "10")int limit){
        //调用服务层方法，获取前10篇热门文章
        return articleService.getHotArticles(limit);
    }

    //推荐文章（根据用户浏览历史）
    @GetMapping("recommend/{userId}") //HTTP GET 请求
    public List<Article> recommendArticles(@PathVariable Long userId){
        //调用服务层方法，推荐用户感兴趣的文章
        return articleService.recommendArticles(userId);
    }

    //创建或更新文章
    @PostMapping
    public Article createOrUpdateArticle(@RequestBody ArticleDTO dto){
        return articleService.saveArticle(dto);
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
    @GetMapping("/byAuthor")
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
    @PostMapping("{id}/comments")
    public ResponseEntity<Comment> addComment(@PathVariable Long articleId, @RequestParam Long userId, @RequestParam String content){

        //调佣CommentService添加评论
        Comment comment = commentService.addComment(articleId, userId, content);

        //调用notificationService推送通知
        Article article = comment.getArticle();
        notificationService.sendNotificationToAuthor(article.getAuthor(), "你的文章收到了新评论！");

        return ResponseEntity.ok(comment);
    }

    //点赞功能
    @PostMapping("{id}/like")
    public ResponseEntity<Likes> addLike(@PathVariable Long articleId, @RequestParam Long userId){

        //调用LikeService来添加点赞
        Likes like = likeService.addLike(articleId, userId);

        //调用notificationService来推送通知
        Article article = like.getArticle();
        notificationService.sendNotificationToAuthor(article.getAuthor(), "你收获了一条点赞~");

        return ResponseEntity.ok(like);
    }

    @GetMapping("/{id}/likes")
    public ResponseEntity<Boolean> getLikeStatus(@PathVariable Long id, @RequestParam Long userId) {
        boolean liked = likeService.alreadyLiked(userId, id);
        return ResponseEntity.ok(liked);
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
    @GetMapping("/{id}/bookmarks")
    public ResponseEntity<List<Article>> getAllBookmarks(@RequestParam Long userId){

        //调用BoookmarkService来获取收藏的文章
        List<Article> bookmarkArticles = bookmarkService.getBookmarks(userId);

        return ResponseEntity.ok(bookmarkArticles);
    }
}
