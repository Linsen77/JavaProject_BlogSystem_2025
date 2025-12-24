package com.blog.dto;

import lombok.Data;

@Data
public class UserStatisticsDto {
    private long articleCount;
    private long followingCount; //我关注的人
    private long followerCount; //粉丝
    private long likeCount;

    public UserStatisticsDto(long articleCount, long followingCount, long followerCount, long likeCount) {
        this.articleCount = articleCount;
        this.followerCount = followerCount;
        this.followingCount = followingCount;
        this.likeCount = likeCount;
    }
}
