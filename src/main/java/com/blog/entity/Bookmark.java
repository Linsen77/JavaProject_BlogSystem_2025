package com.blog.entity;

//核心表，收藏

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;

@Entity
@Data
public class Bookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    private User user;//收藏者

    @ManyToOne
    private Article article;//被收藏的文章

    private LocalDateTime createtime;//收藏时间
}
