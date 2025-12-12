package com.blog.entity;


import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

//核心表，关注
@Data
@Entity
@Table(name = "follow")
public class Follows {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //关注者(粉丝)
    private Long followerId;

    //被关注者(关注的人)
    private Long followeeId;

    private LocalDateTime createTime = LocalDateTime.now();

}
