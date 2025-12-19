package com.blog.repository;

import com.blog.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 标签repository
 * 该接口用于与数据库交互，操作与标签相关的数据。
 * 继承自JpaRepository，自动提供常见的CRUD方法。
 * 也可以在此定义一些自定义查询方法，如根据标签名称查找标签。
 */
public interface TagRepository extends JpaRepository<Tag,Long> {
    /**
     * 根据标签名称查找标签
     * 使用Spring Data JPA提供的查询方法，自动根据方法生成SQL查询。
     *
     * @param name 标签名称
     * @return 返回与名称匹配的标签对象
     */
    Tag findByName(String name);//根据标签名称查找标签
}
