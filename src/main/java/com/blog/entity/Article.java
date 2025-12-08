package com.blog.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

//使用 JPA注释创建数据库表
@Entity
@Data
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) //自动生成ID
    private Long id;//文章ID

    private String title; //文章标题
    private String content; //文章内容，使用markdown 格式
    private String author; //作者

    @Enumerated(EnumType.STRING)
    private ArticleVisibility visibility; //文章公开性：公开、仅粉丝、私人

    private LocalDateTime createDate; // 创建时间
    private LocalDateTime updateDate; // 更新时间


    //文章的可见性枚举类型
    public enum ArticleVisibility{
        PUBLIC,//公开
        FOLLOWERS,//仅粉丝
        PRIVATE //私人
    }
}
