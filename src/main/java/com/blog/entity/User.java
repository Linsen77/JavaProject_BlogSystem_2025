package com.blog.entity;



import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Table(name = "user")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder

//核心表,用户核心实体
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String email;

    @JsonIgnore
    private String password;//密码

    private String bio;

    private String avatarUrl;//头像url

    private boolean emailVerified;//邮箱是否注册验证通过并绑定

    private Integer followingCount = 0;//关注数
    private Integer followerCount = 0;//粉丝数
    private Integer postCount = 0;//文章数
    private Integer likesCount = 0;//被点赞数

    private LocalDateTime createdAt;//用户创建时间
    private LocalDateTime updateAt;//用户更新时间

}
