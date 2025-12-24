package com.blog.dto;

import lombok.Data;

@Data
public class UserStatisticsDto {
    private long articleCount;
    private long followeeCount; //我关注的人
    private long followerCount; //粉丝
    private long likeCount;
    private int bookmarks;

    public UserStatisticsDto(long articleCount, long followerCount, long followeeCount, long likeCount, int bookmarks) {
        this.articleCount = articleCount;
        this.followerCount = followerCount;
        this.followeeCount = followeeCount;
        this.likeCount = likeCount;
        this.bookmarks = bookmarks;
    }
}
