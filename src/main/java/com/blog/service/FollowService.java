package com.blog.service;


import com.blog.entity.Follows;
import com.blog.repository.FollowRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class FollowService {
    @Autowired
    private FollowRepository followRepository;

    //判断是否已经关注
    public boolean isFollowing(Long viewerId, Long authorId) {
        return followRepository.existsByFollowerIdAndFolloweeId(viewerId, authorId);
    }

    //关注用户
    public boolean follow(Long followerId, Long followeeId) {
        if(isFollowing(followerId, followeeId)) {
            return false;//已关注
        }

        Follows follow = new Follows();
        follow.setFollowerId(followerId);
        follow.setFolloweeId(followeeId);
        followRepository.save(follow);

        return true;
    }

    //取消关注用户
    public boolean unfollow(Long followerId, Long followeeId) {
        if(!isFollowing(followerId, followeeId)) {
            return false;//未关注
        }

        followRepository.deleteByFollowerIdAndFolloweeId(followerId, followeeId);
        return true;
    }
}
