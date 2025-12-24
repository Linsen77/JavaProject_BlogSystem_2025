package com.blog.dto;

import lombok.Data;

import java.util.List;

@Data
public class ArticleDTO {
    private Long id;
    private String title;
    private String content;
    private List<String> tags;
    private String visibility;
    private Long authorId;
    private String cover;
}
