package com.blog.entity;

import jakarta.persistence.*;
import java.util.List;

//核心表，标签表
@Entity
public class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;//标签名称

    //反向多对多关系，标签与文章关联
    @ManyToMany(mappedBy = "tags")
    private List<Article> articles;//与文章的多对关系

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Article> getArticles() {
        return articles;
    }

    public void setArticles(List<Article> articles) {
        this.articles = articles;
    }
}
