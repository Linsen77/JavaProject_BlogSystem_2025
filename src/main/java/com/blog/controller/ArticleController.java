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

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    @Autowired
    private ArticleService articleService;

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

    //创建文章
    @PostMapping
    public Article createOrUpdateArticle(@RequestBody ArticleDTO dto){
        return articleService.saveArticle(dto);
    }

    //更新编辑文章
    @PutMapping("/{id}")
    public Article updateArticle(@PathVariable Long id, @RequestBody ArticleDTO dto) {
        dto.setId(id);
        return articleService.saveArticle(dto);
    }

    //获取所有文章
    @GetMapping
    public ResponseEntity<?> getAllArticles(HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        Long viewerId = (loginUser != null) ? loginUser.getId() : null;
        List<Article> all = articleService.getAllArticles();
        List<Article> visibleArticles = all.stream()
                .filter(article -> {
                    Long authorId = article.getAuthor().getId();
                    Article.ArticleVisibility visibility = article.getVisibility();
                    // 公开文章：所有人可见
                    if (visibility == Article.ArticleVisibility.PUBLIC) {
                        return true;
                    }
                    // 未登录用户不能看非公开文章
                    if (viewerId == null) {
                        return false;
                    }
                    // 作者本人永远可见
                    if (viewerId.equals(authorId)) {
                        return true;
                    }
                    // 粉丝可见
                    if (visibility == Article.ArticleVisibility.FOLLOWERS) {
                        return followService.isFollowing(viewerId, authorId);
                    }
                    // 私密文章（仅作者本人可见）
                    return false;
                })
                .toList();
        return ResponseEntity.ok(visibleArticles);
    }


    //根据 ID 获取文章
    @GetMapping("/{id}")
    public ResponseEntity<?> getArticleById(
            @PathVariable Long id,
            HttpSession session
    ) {
        Article article = articleService.getArticleById(id)
                .orElseThrow(() -> new RuntimeException("文章不存在"));

        articleService.incrementViewCount(id);

        User loginUser = (User) session.getAttribute("loginUser");
        Long authorId = article.getAuthor().getId();
        Article.ArticleVisibility visibility = article.getVisibility();

        //写入浏览记录
        if (loginUser != null) {
            userViewHistoryRepository.save(
                    new UserViewHistory(loginUser.getId(), id, LocalDateTime.now())
            );
        }

        // 公开文章：任何人都能看
        if (visibility == Article.ArticleVisibility.PUBLIC) {
            return ResponseEntity.ok(article);
        }

        // 未登录用户不能看非公开文章
        if (loginUser == null) {
            return ResponseEntity.status(403).body("无权限查看");
        }

        // 作者本人永远可以看
        if (loginUser.getId().equals(authorId)) {
            return ResponseEntity.ok(article);
        }

        // 粉丝可见
        if (visibility == Article.ArticleVisibility.FOLLOWERS) {
            boolean isFan = followService.isFollowing(loginUser.getId(), authorId);
            if (isFan) {
                return ResponseEntity.ok(article);
            } else {
                return ResponseEntity.status(403).body("仅粉丝可见");
            }
        }

        // 私密文章（仅自己可见）
        if (visibility == Article.ArticleVisibility.PRIVATE) {
            return ResponseEntity.status(403).body("仅作者本人可见");
        }

        return ResponseEntity.status(403).body("无权限查看");
    }



    //根据作者获取文章
    @GetMapping("/byAuthor/{authorId}")
    public ResponseEntity<?> getArticleByAuthor(
            @PathVariable Long authorId,
            HttpSession session
    ) {
        User loginUser = (User) session.getAttribute("loginUser");
        Long viewerId = (loginUser != null) ? loginUser.getId() : null;
        List<Article> all = articleService.getArticleByAuthorId(authorId);
        List<Article> visibleArticles = all.stream()
                .filter(article -> {
                    Article.ArticleVisibility visibility = article.getVisibility();
                    Long articleAuthorId = article.getAuthor().getId();
                    // 公开文章
                    if (visibility == Article.ArticleVisibility.PUBLIC) {
                        return true;
                    }
                    // 未登录用户不能看非公开文章
                    if (viewerId == null) {
                        return false;
                    }
                    // 作者本人永远可见
                    if (viewerId.equals(articleAuthorId)) {
                        return true;
                    }
                    // 粉丝可见
                    if (visibility == Article.ArticleVisibility.FOLLOWERS) {
                        return followService.isFollowing(viewerId, articleAuthorId);
                    }
                    // 私密文章（仅作者本人）
                    return false;
                })
                .toList();
        return ResponseEntity.ok(visibleArticles);
    }


    //根据作者名搜索文章
    @GetMapping("/search/byAuthor")
    public List<Article> searchByAuthor(@RequestParam String authorName) {
        return articleService.searchByAuthor(authorName);
    }


    //删除文章
    @DeleteMapping("/{id}")
    public void deleteArticle(@PathVariable Long id){
        articleService.deleteArticle(id);
    }

    //评论功能
    @PostMapping("/{articleId}/comments")
    public ResponseEntity<?> addComment(
            @PathVariable Long articleId,
            @RequestBody CommentDTO dto,
            HttpSession session
    ) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(403).body("请先登录");
        }

        Long userId = loginUser.getId();
        String content = dto.getContent();

        Comment comment = commentService.addComment(articleId, userId, content);

        Article article = comment.getArticle();

        return ResponseEntity.ok(comment);
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
    public ResponseEntity<List<Comment>> getComments(@PathVariable Long id) {
        List<Comment> comments = commentService.getCommentsByArticleId(id);
        return ResponseEntity.ok(comments);
    }

    //点赞功能
    @PostMapping("/{articleId}/like")
    public ResponseEntity<Likes> addLike(@PathVariable Long articleId, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(403).body(null);
        }

        Long userId = loginUser.getId();

        Likes like = likeService.addLike(articleId, userId);

        Article article = like.getArticle();

        return ResponseEntity.ok(like);
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
    public ResponseEntity<Boolean> getLikeStatus(@PathVariable Long id, @RequestParam Long userId) {
        boolean liked = likeService.hasUserLikedArticle(userId, id);
        return ResponseEntity.ok(liked);
    }

    //获取点赞数
    @GetMapping("/{id}/likes/count")
    public ResponseEntity<Integer> getLikeCount(@PathVariable Long id) {
        int count = likeService.countLikes(id);
        return ResponseEntity.ok(count);
    }

    //收藏功能
    @PostMapping("/{articleId}/bookmark")
    public ResponseEntity<?> addBookmark(
            @PathVariable Long articleId,
            HttpSession session
    ) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(403).body("请先登录");
        }

        Long userId = loginUser.getId();

        Bookmark bookmark = bookmarkService.addBookmark(articleId, userId);

        Article article = bookmark.getArticle();

        return ResponseEntity.ok(bookmark);
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
    public ResponseEntity<?> getBookmarkStatus(
            @PathVariable Long articleId,
            HttpSession session
    ) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.ok(false);
        }
        Long userId = loginUser.getId();
        boolean bookmarked = bookmarkService.isBookmarked(userId, articleId);

        return ResponseEntity.ok(bookmarked);
    }


}
