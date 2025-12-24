package com.blog.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

//使用 JPA注释创建数据库表
@Entity
@Data
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) //自动生成ID
    private Long id;//文章ID

    private String title; //文章标题
    private String content; //文章内容，使用markdown 格式
    private String cover;//封面URL
    private int viewCount; //阅读量

    @ManyToOne
    @JoinColumn(name = "author_id", referencedColumnName = "id")  // author_id 外键
    private User author;//作者

    @Enumerated(EnumType.STRING)
    private ArticleVisibility visibility; //文章公开性：公开、仅粉丝、私人

    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateDate; // 更新时间
    //文章与标签的多对多关系
    @ManyToMany
    @JoinTable(
            name = "article_tag", //中间表名称
            joinColumns = @JoinColumn(name = "article_id"), //文章的外键列
            inverseJoinColumns = @JoinColumn(name = "tag_id") //标签的外键列
    )
    private List<Tag> tags;
    //文章的可见性枚举类型
    public enum ArticleVisibility{
        PUBLIC,//公开
        FOLLOWERS,//仅粉丝
        PRIVATE //私人
    }
    // ===== Getter 和 Setter =====

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    public ArticleVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(ArticleVisibility visibility) {
        this.visibility = visibility;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }
}
