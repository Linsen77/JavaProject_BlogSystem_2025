package com.blog.repository;

import com.blog.entity.Follows;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FollowRepository extends JpaRepository<Follows, Long> {

    //判断是否已关注
    boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    //取消关注
    void deleteByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    //我的关注的列表（我作为follower）
    List<Follows> findByFollowerId(Long followerId);

    //粉丝列表（别人作为follower）
    List<Follows> findByFolloweeId(Long followeeId);

    //统计粉丝数量（别人关注我）
    long countByFolloweeId(Long userId);

    //统计关注数量（我关注别人）
    long countByFollowerId(Long userId);
}
