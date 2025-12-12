package com.blog.dto;

import com.blog.entity.Article;
import com.blog.entity.User;
import lombok.Data;

import java.util.List;

@Data
public class UserProfileDTO {
    private Long id;
    private String username;
    private String avatar;
    private String bio;
    private List<Article> articles;

    public UserProfileDTO(User user, List<Article> articles) {
        this.id = user.getId();
        this.username = user.getName();
        this.avatar = user.getAvatarUrl();
        this.bio = user.getBio();
        this.articles = articles;
    }
}
