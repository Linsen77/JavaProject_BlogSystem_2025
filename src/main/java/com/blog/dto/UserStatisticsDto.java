package com.blog.dto;

import lombok.Data;

@Data
public class UserStatisticsDto {
    private long articleCount;
    private long followerCount;
    private long followeeCount;
    private long likeCount;

    public UserStatisticsDto(long articleCount, long followerCount, long followeeCount, long likeCount) {
        this.articleCount = articleCount;
        this.followerCount = followerCount;
        this.followeeCount = followeeCount;
        this.likeCount = likeCount;
    }
}
