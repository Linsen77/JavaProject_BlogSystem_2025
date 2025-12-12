package com.blog.entity;

//核心表，评论，支持嵌套回复（楼中楼）

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;//评论者

    @ManyToOne
    private Article article;//被评论的文章

    private String content;//评论内容

    private LocalDateTime createtime;//评论时间


}
