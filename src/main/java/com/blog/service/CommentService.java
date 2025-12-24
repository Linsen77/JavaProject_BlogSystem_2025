package com.blog.service;


import com.blog.entity.Article;
import com.blog.entity.Comment;
import com.blog.entity.User;
import com.blog.repository.ArticleRepository;
import com.blog.repository.CommentRepository;
import com.blog.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private NotificationService notificationService;
    @Autowired
    private ArticleRepository articleRepository;
    @Autowired
    private UserRepository userRepository;


    //评论文章功能
    public Comment addComment(Long articleId, Long userId, String content){
        Article article = articleRepository.findById(articleId).orElseThrow(() -> new RuntimeException("文章不存在"));
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));

        Comment comment = new Comment();
        comment.setArticle(article);
        comment.setUser(user);
        comment.setContent(content);
        comment.setCreatetime(LocalDateTime.now());

        //保存评论
        Comment savedComment = commentRepository.save(comment);

        //发送通知给作者
        notificationService.sendNotificationToAuthor(article.getAuthor(), "收到一条评论~");

        return savedComment;
    }

    public boolean canDeleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));

        // 评论作者 或 文章作者
        return comment.getUser().getId().equals(userId)
                || comment.getArticle().getAuthor().getId().equals(userId);
    }

    public void deleteComment(Long commentId) {
        commentRepository.deleteById(commentId);
    }

    public List<Comment> getCommentsByArticleId(Long id) {
        return commentRepository.findByArticleId(id);
    }
}
