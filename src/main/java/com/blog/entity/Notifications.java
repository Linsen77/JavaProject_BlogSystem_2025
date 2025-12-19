package com.blog.entity;

//核心表，站内系统通知（有谁点赞，关注，收藏了你）

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class Notifications {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;//接受通知的用户

    private String content;//通知内容

    /**
     * 是否已读
     */
    @Column(name = "is_read")
    private boolean readFlag;//是否已读

    private LocalDateTime createtime;//通知时间

}
