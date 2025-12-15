package com.blog.service;


import com.blog.entity.Article;
import com.blog.entity.Bookmark;
import com.blog.entity.User;
import com.blog.repository.ArticleRepository;
import com.blog.repository.BookmarkRepository;
import com.blog.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookmarkService {

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private NotificationService notificationService;

    //收藏文章
    public Bookmark addBookmark(Long articleId, Long userId){
        //检查文章是否存在
        Article article = articleRepository.findById(articleId).orElseThrow(() -> new RuntimeException("文章不存在"));
        //检查用户是否存在
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));

        //检查是否已收藏
        boolean alreadyBookmark = bookmarkRepository.existsByUserAndArticle(user, article);
        if(alreadyBookmark){
            throw new RuntimeException("你已经收藏过该文章");
        }

        //创建新的收藏记录
        Bookmark bookmark = new Bookmark();
        bookmark.setUser(user);
        bookmark.setArticle(article);
        bookmark.setCreatetime(LocalDateTime.now());

        //发送通知给作者
        User articleauthor = article.getAuthor();
        notificationService.sendNotificationToAuthor(articleauthor, "你的文章被收藏啦(*^▽^*)");


        return bookmarkRepository.save(bookmark);

    }

    //查看用户收藏的文章
    public List<Article> getBookmarks(Long userId){
        return bookmarkRepository.findByUserId(userId);
    }
}
