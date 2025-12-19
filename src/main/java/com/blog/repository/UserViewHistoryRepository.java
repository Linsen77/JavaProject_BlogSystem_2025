package com.blog.repository;

import com.blog.entity.UserViewHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserViewHistoryRepository
        extends JpaRepository<UserViewHistory,Long>{
    /**
     * 查询某个用户浏览过的所有文章ID
     * 这里定义了一个JPQL（Java Persistence Query Language）查询，获取某个用户浏览过的文章的ID列表。
     * JPQL是一种面向对象的查询语言，它通过实体类属性来进行查询，而非数据库表字段。
     *
     * @param userId 需要查询的用户ID
     * @return 返回该用户浏览过的所有文章ID列表
     */
    @Query("""
        select h.articleId
        from UserViewHistory h
        where h.userId = :userId
    """)
    List<Long> findViewedArticleIds(Long userId); // 返回值是该用户浏览过的所有文章ID列表
}

