package com.blog.repository;

import com.blog.entity.Follows;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FollowRepository extends JpaRepository<Follows, Long> {

    //判断是否已关注
    boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    //取消关注
    void deleteByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    //我的关注的列表
    List<Follows> findByFollowerId(Long followerId);

    //粉丝列表
    List<Follows> findByFolloweeId(Long followeeId);
}
