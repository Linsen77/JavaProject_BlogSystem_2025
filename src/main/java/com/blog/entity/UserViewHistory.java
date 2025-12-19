package com.blog.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_view_history")//定义与数据库中的表名映射

public class UserViewHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) //自动生成主键ID
    private Long id;

    //用户ID，表示浏览文章的用户
    @Column(name = "user_id",nullable = false)
    private Long userId;

    //文章ID,表示用户浏览过的文章
    @Column(name = "article_id",nullable = false)
    private Long articleId;

    //浏览时间，记录用户浏览这篇文章的具体时间
    @Column(name = "view_time",nullable = false)
    private LocalDateTime viewTime;

    public UserViewHistory(){}

    //构造方法，方便创建对象时直接赋值
    public UserViewHistory(Long userId,Long articleId,LocalDateTime viewTime){
        this.userId = userId;
        this.articleId = articleId;
        this.viewTime = viewTime;
    }

    // Getter 和 Setter 方法

    public Long getId(){
        return id;//获取记录的ID
    }
    public void setId(Long id)
    {
        this.id = id;//设置记录的ID
    }

    public Long getUserId(){
        return userId;
    }
    public void setUserId(Long userId){
        this.userId = userId;
    }

    public Long getArticleId(){
        return articleId;
    }
    public void setArticleId(Long articleId){
        this.articleId = articleId;
    }

    public LocalDateTime getViewTime(){
        return viewTime;
    }
    public void setViewTime(LocalDateTime viewTime){
        this.viewTime = viewTime;
    }

    //覆写toString方法，方便调试时查看对象信息
    @Override
    public String toString(){
        return "UserViewHistory{" +
                "id="+ id +
                ",userId=" + userId +
                ",articleId=" + articleId+
                ",viewTime=" + viewTime +
                '}';
    }
}
