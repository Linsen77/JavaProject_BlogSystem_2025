package com.blog.entity;


//核心表，点赞

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class Likes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;//点赞者

    @ManyToOne
    private Article article;//被点赞的文章

    private LocalDateTime createTime;//点赞时间

}
