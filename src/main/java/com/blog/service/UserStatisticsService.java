package com.blog.service;

import com.blog.dto.UserStatisticsDto;
import com.blog.repository.ArticleRepository;
import com.blog.repository.FollowRepository;
import com.blog.repository.LikeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserStatisticsService {

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private LikeRepository likeRepository;

    // 获取用户统计信息
    public UserStatisticsDto getUserStatistics(Long userId) {

        //获取用户的文章数
        long articleCount = articleRepository.countByAuthorId(userId);

        //获取用户的粉丝数
        long followerCount = followRepository.countByFolloweeId(userId);

        //获取用户的关注数
        long  followeeCount = followRepository.countByFollowerId(userId);

        //获取用户文章的总点赞数
        long likeCount = likeRepository.countByArticleAuthorId(userId);

        return new UserStatisticsDto(articleCount, followerCount, followeeCount, likeCount);
    }
}
