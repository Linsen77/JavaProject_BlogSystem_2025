package com.blog.repository;

import com.blog.entity.Article;
import com.blog.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;

public interface ArticleRepository extends JpaRepository<Article,Long>{

    List<Article> findByAuthor(User author);

    // 根据作者 ID 统计文章数量
    long countByAuthorId(Long authorId);


    List<Article>findByVisibility(Article.ArticleVisibility visibility);


    List<Article> findByAuthorIdInOrderByCreateTimeDesc(List<Long> authorIds);

    List<Article> findByAuthor_Id(Long authorId);

    List<Article> findByAuthorIdAndVisibility(Long authorId, Article.ArticleVisibility visibility);

    @Query("SELECT a FROM Article a " +
            "WHERE a.author.id = :authorId " +
            "AND (a.visibility = 'PUBLIC' OR a.visibility = 'FOLLOWERS')")
    List<Article> findVisibleToFollowers(@Param("authorId") Long authorId);

    //1.按标题模糊搜索文章
    List<Article> findByTitleContainingAndVisibility(String title,Article.ArticleVisibility visibility);

    //2.根据标签搜索文章
    @Query("SELECT a from Article a JOIN a.tags t WHERE t.name = :tagName AND a.visibility = PUBLIC")
    List<Article> findByTag(String tagName);

    //3.获取热门文章（按阅读量排序）

    /**
     * 按照阅读量降序排序，获取热门文章
     *
     * @param pageable 分页参数，控制获取的文章数量
     * @return 按阅读量排序的文章列表
     */
    @Query("SELECT a FROM Article a WHERE a.visibility = PUBLIC ORDER BY a.viewCount DESC")
    List<Article>findHotArticles(Pageable pageable);

    //4.根据标签推荐文章
    @Query("SELECT a FROM Article a JOIN a.tags t WHERE t.name IN :tags ORDER BY a.viewCount DESC")
    List<Article>findArticlesByTags(List<String> tags);

    //5.根据浏览历史统计标签（Top标签）
    @Query("SELECT t.name,COUNT(t) FROM Article a JOIN a.tags t WHERE a.id IN :articleIds GROUP BY t.name ORDER BY COUNT(t) DESC")
    List<Object[]>findTopTags(List<Long> articleIds);

    //根据作者名搜索文章
    List<Article> findByAuthor_NameContainingIgnoreCase(String authorName);
}
