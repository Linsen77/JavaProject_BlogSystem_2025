package com.blog.controller;

import com.blog.dto.ArticleDTO;
import com.blog.dto.CommentDTO;
import com.blog.entity.*;
import com.blog.repository.UserViewHistoryRepository;
import com.blog.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    @Autowired
    private ArticleService articleService;
    @Autowired private ArticleReadService reads;

    @Autowired
    private CommentService commentService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private BookmarkService bookmarkService;

    @Autowired
    private FollowService followService;

    @Autowired
    private UserViewHistoryRepository userViewHistoryRepository;

    //给文章添加标签
    @PostMapping("/{id}/tags")
    public Article addTagsToArticle(@PathVariable Long id,@RequestBody List<Long>tagIds, HttpSession session){
        return articleService.addTagsToArticle(id,tagIds,currentUserId(session));
    }

    //获取文章的标签
    @GetMapping("/{id}/tags")
    public List<Tag> getTagOfArticle(@PathVariable Long id,HttpSession session) {
        return reads.requireVisible(id,reads.viewerId(session)).getTags();
    }

    //按标题搜索文章
    @GetMapping("/search/byTitle")
    public List<Article> searchByTitle(@RequestParam String keyword,HttpSession session) {
        return reads.list(ArticleReadService.Scope.TITLE,null,keyword,reads.viewerId(session));
    }

    //按标签搜索文章
    @GetMapping("/search/byTag")// HTTP GET 请求
    public List<Article> searchByTag(@RequestParam String tagName,HttpSession session) {
        return reads.list(ArticleReadService.Scope.TAG,null,tagName,reads.viewerId(session));
    }

    //获取热门文章（按阅读量）
    @GetMapping("/hot") //HTTP GET 请求
    public List<Article> getHotArticle(@RequestParam(defaultValue="10") int limit,HttpSession session) {
        return reads.hot(limit,reads.viewerId(session));
    }

    //推荐文章（根据用户浏览历史）
    @GetMapping("recommend/{userId}") //HTTP GET 请求
    public List<Article> recommendArticles(@PathVariable Long userId,HttpSession session) {
        return reads.recommend(reads.viewerId(session));
    }

    //创建文章
    @PostMapping
    public Article createOrUpdateArticle(@RequestBody ArticleDTO dto, HttpSession session){
        return articleService.createArticle(dto,currentUserId(session));
    }

    //更新编辑文章
    @PutMapping("/{id}")
    public Article updateArticle(@PathVariable Long id, @RequestBody ArticleDTO dto, HttpSession session) {
        return articleService.updateArticle(id,dto,currentUserId(session));
    }

    //获取所有文章
    @GetMapping
    public ResponseEntity<?> getAllArticles(HttpSession session) {
        return ResponseEntity.ok(reads.list(ArticleReadService.Scope.ALL,null,null,reads.viewerId(session)));
    }


    //根据 ID 获取文章
    @GetMapping("/{id}")
    public ResponseEntity<?> getArticleById(@PathVariable Long id,HttpSession session) {
        Long viewer=reads.viewerId(session);
        Article article=reads.requireVisible(id,viewer);
        articleService.recordView(id,viewer);
        return ResponseEntity.ok(article);
    }



    //根据作者获取文章
    @GetMapping("/byAuthor/{authorId}")
    public ResponseEntity<?> getArticleByAuthor(@PathVariable Long authorId,HttpSession session) {
        return ResponseEntity.ok(reads.list(ArticleReadService.Scope.AUTHOR,authorId,null,reads.viewerId(session)));
    }


    //根据作者名搜索文章
    @GetMapping("/search/byAuthor")
    public List<Article> searchByAuthor(@RequestParam String authorName,HttpSession session) {
        return reads.list(ArticleReadService.Scope.AUTHOR_NAME,null,authorName,reads.viewerId(session));
    }


    //删除文章
    @DeleteMapping("/{id}")
    public void deleteArticle(@PathVariable Long id, HttpSession session){
        articleService.deleteArticle(id,currentUserId(session));
    }

    private Long readableInteraction(Long id,HttpSession session){
        Long viewer=reads.viewerId(session);
        if(viewer==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"请先登录");
        reads.requireVisible(id,viewer);return viewer;
    }

    private Long currentUserId(HttpSession session) {
        Object value=session.getAttribute("loginUser");
        if (!(value instanceof User user) || user.getId()==null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"请先登录");
        return user.getId();
    }

    //评论功能
    @PostMapping("/{articleId}/comments")
    public ResponseEntity<?> addComment(@PathVariable Long articleId,@RequestBody CommentDTO dto,HttpSession session) {
        return ResponseEntity.ok(commentService.addComment(articleId,readableInteraction(articleId,session),dto.getContent()));
    }

    @DeleteMapping("/{articleId}/comments/{commentId}")
    public ResponseEntity<?> deleteComment(
            @PathVariable Long articleId,
            @PathVariable Long commentId,
            HttpSession session
    ) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(403).body("请先登录");
        }

        // 只有评论作者或文章作者可以删除评论
        if (!commentService.canDeleteComment(commentId, loginUser.getId())) {
            return ResponseEntity.status(403).body("无权限删除该评论");
        }

        commentService.deleteComment(commentId);
        return ResponseEntity.ok("删除成功");
    }


    //获取评论列表
    @GetMapping("/{id}/comments")
    public ResponseEntity<List<Comment>> getComments(@PathVariable Long id,HttpSession session) {
        reads.requireVisible(id,reads.viewerId(session));
        return ResponseEntity.ok(commentService.getCommentsByArticleId(id));
    }

    //点赞功能
    @PostMapping("/{articleId}/like")
    public ResponseEntity<Likes> addLike(@PathVariable Long articleId,HttpSession session) {
        return ResponseEntity.ok(likeService.addLike(articleId,readableInteraction(articleId,session)));
    }

    @DeleteMapping("/{articleId}/like")
    public ResponseEntity<?> removeLike(
            @PathVariable Long articleId,
            HttpSession session
    ) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(403).body("请先登录");
        }

        Long userId = loginUser.getId();

        likeService.removeLike(articleId, userId);

        return ResponseEntity.ok("取消点赞成功");
    }

    //获取是否点赞
    @GetMapping("/{id}/likes")
    public ResponseEntity<Boolean> getLikeStatus(@PathVariable Long id,@RequestParam Long userId,HttpSession session) {
        Long viewer=reads.viewerId(session);reads.requireVisible(id,viewer);
        return ResponseEntity.ok(viewer!=null && likeService.hasUserLikedArticle(viewer,id));
    }

    //获取点赞数
    @GetMapping("/{id}/likes/count")
    public ResponseEntity<Integer> getLikeCount(@PathVariable Long id,HttpSession session) {
        reads.requireVisible(id,reads.viewerId(session));
        return ResponseEntity.ok(likeService.countLikes(id));
    }

    //收藏功能
    @PostMapping("/{articleId}/bookmark")
    public ResponseEntity<?> addBookmark(@PathVariable Long articleId,HttpSession session) {
        return ResponseEntity.ok(bookmarkService.addBookmark(articleId,readableInteraction(articleId,session)));
    }

    @DeleteMapping("/{articleId}/bookmark")
    public ResponseEntity<?> removeBookmark(
            @PathVariable Long articleId,
            HttpSession session
    ) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(403).body("请先登录");
        }

        Long userId = loginUser.getId();

        bookmarkService.removeBookmark(articleId, userId);

        return ResponseEntity.ok("取消收藏成功");
    }

    //查询某用户是否收藏了某文章
    @GetMapping("/{articleId}/bookmark/status")
    public ResponseEntity<?> getBookmarkStatus(@PathVariable Long articleId,HttpSession session) {
        Long viewer=reads.viewerId(session);reads.requireVisible(articleId,viewer);
        return ResponseEntity.ok(viewer!=null && bookmarkService.isBookmarked(viewer,articleId));
    }


}
