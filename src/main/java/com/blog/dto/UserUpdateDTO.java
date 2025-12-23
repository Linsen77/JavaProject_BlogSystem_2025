package com.blog.dto;


import lombok.Data;

@Data
public class UserUpdateDTO {
    private String name;
    private String bio;
    private String avatarUrl;
}
