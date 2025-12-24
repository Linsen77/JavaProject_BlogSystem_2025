package com.blog.repository;

import com.blog.entity.Article;
import com.blog.entity.Bookmark;
import com.blog.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    //检查是否已收藏文章
    boolean existsByUserAndArticle(User user, Article article);

    //通过userId来查找收藏的文章
    List<Bookmark> findByUserId(Long userId);

    //查看某用户是否收藏某文章
    boolean existsByUserIdAndArticleId(Long userId, Long articleId);

    //获取收藏数
    int countByUserId(Long userId);

    //取消收藏
    @Transactional
    void deleteByUserIdAndArticleId(Long userId, Long articleId);
}
