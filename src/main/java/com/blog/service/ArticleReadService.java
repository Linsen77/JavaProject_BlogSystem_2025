package com.blog.service;

import com.blog.entity.*;
import com.blog.repository.*;
import jakarta.persistence.criteria.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

/** All HTTP article reads use this SQL policy, including nested entities and counts. */
@Service
public class ArticleReadService {
    public enum Scope { ALL, AUTHOR, TITLE, TAG, AUTHOR_NAME, FEED, BOOKMARKS }
    private final ArticleRepository articles;
    private final UserRepository users;
    private final LikeRepository likes;
    private final UserViewHistoryRepository history;
    public ArticleReadService(ArticleRepository articles,UserRepository users,LikeRepository likes,UserViewHistoryRepository history){
        this.articles=articles;this.users=users;this.likes=likes;this.history=history;
    }
    public Long viewerId(HttpSession session){
        Object value=session==null?null:session.getAttribute("loginUser");
        return value instanceof User u ? existingViewer(u.getId()) : null;
    }
    private Long existingViewer(Long id){return id!=null && users.existsById(id)?id:null;}
    private Predicate visibility(Path<Article> a,CommonAbstractCriteria q,CriteriaBuilder cb,Long viewer){
        Predicate pub=cb.equal(a.get("visibility"),Article.ArticleVisibility.PUBLIC);
        if(viewer==null)return pub;
        Subquery<Long> fan=q.subquery(Long.class);Root<Follows> f=fan.from(Follows.class);
        fan.select(f.get("id")).where(cb.equal(f.get("followerId"),viewer),cb.equal(f.get("followeeId"),a.get("author").get("id")));
        return cb.or(pub,cb.equal(a.get("author").get("id"),viewer),
                cb.and(cb.equal(a.get("visibility"),Article.ArticleVisibility.FOLLOWERS),cb.exists(fan)));
    }
    private Specification<Article> visible(Long viewer){return (a,q,cb)->visibility(a,q,cb,viewer);}
    private Specification<Article> scope(Scope scope,Long id,String term){
        return (a,q,cb)->switch(scope){
            case ALL->cb.conjunction();
            case AUTHOR->cb.equal(a.get("author").get("id"),id);
            case TITLE->cb.like(cb.lower(a.get("title")),"%"+term.toLowerCase(Locale.ROOT)+"%");
            case AUTHOR_NAME->cb.like(cb.lower(a.get("author").get("name")),"%"+term.toLowerCase(Locale.ROOT)+"%");
            case TAG->{
                Subquery<Long> sq=q.subquery(Long.class);Root<Article> aa=sq.from(Article.class);
                sq.select(aa.get("id")).where(cb.equal(aa.get("id"),a.get("id")),cb.equal(aa.join("tags").get("name"),term));
                yield cb.exists(sq);
            }
            case FEED->{
                if(id==null)yield cb.disjunction();
                Subquery<Long> sq=q.subquery(Long.class);Root<Follows> f=sq.from(Follows.class);
                sq.select(f.get("id")).where(cb.equal(f.get("followerId"),id),cb.equal(f.get("followeeId"),a.get("author").get("id")));
                yield cb.exists(sq);
            }
            case BOOKMARKS->{
                Subquery<Long> sq=q.subquery(Long.class);Root<Bookmark> b=sq.from(Bookmark.class);
                sq.select(b.get("id")).where(cb.equal(b.get("user").get("id"),id),cb.equal(b.get("article").get("id"),a.get("id")));
                yield cb.exists(sq);
            }
        };
    }
    private Specification<Article> query(Scope scope,Long id,String term,Long viewer){return visible(existingViewer(viewer)).and(scope(scope,id,term));}
    public List<Article> list(Scope scope,Long id,String term,Long viewer){
        return articles.findAll(query(scope,id,term,viewer),Sort.by(Sort.Direction.DESC,"createTime","id"));
    }
    public Page<Article> page(Scope scope,Long id,String term,Long viewer,Pageable pageable){
        return articles.findAll(query(scope,id,term,viewer),pageable);
    }
    public long count(Scope scope,Long id,Long viewer){return articles.count(query(scope,id,null,viewer));}
    public Article requireVisible(Long id,Long viewer){
        return articles.findOne(visible(existingViewer(viewer)).and((a,q,cb)->cb.equal(a.get("id"),id)))
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"文章不存在"));
    }
    public List<Article> hot(int limit,Long viewer){
        if(limit<1)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"limit必须为正数");
        Specification<Article> spec=visible(existingViewer(viewer)).and((a,q,cb)->cb.equal(a.get("visibility"),Article.ArticleVisibility.PUBLIC));
        return articles.findAll(spec,PageRequest.of(0,limit,Sort.by(Sort.Direction.DESC,"viewCount","id"))).getContent();
    }
    public long visibleAuthorLikes(Long authorId,Long viewer){
        Long valid=existingViewer(viewer);
        return likes.count((l,q,cb)->cb.and(visibility(l.get("article"),q,cb,valid),cb.equal(l.get("article").get("author").get("id"),authorId)));
    }
    public List<Article> recommend(Long viewer){
        Long valid=existingViewer(viewer);
        if(valid==null)return hot(10,null);
        List<Long> viewed=history.findViewedArticleIds(valid);
        if(viewed.isEmpty())return hot(10,valid);
        List<Long> readable=articles.findAll(visible(valid).and((a,q,cb)->a.get("id").in(viewed)))
                .stream().map(Article::getId).toList();
        if(readable.isEmpty())return hot(10,valid);
        List<String> top=articles.findTopTags(readable).stream().limit(3).map(row->(String)row[0]).toList();
        if(top.isEmpty())return hot(10,valid);
        Specification<Article> candidates=visible(valid).and((a,q,cb)->{
            Subquery<Long> sq=q.subquery(Long.class);Root<Article> aa=sq.from(Article.class);
            sq.select(aa.get("id")).where(cb.equal(aa.get("id"),a.get("id")),aa.join("tags").get("name").in(top));
            return cb.and(cb.exists(sq),cb.equal(a.get("visibility"),Article.ArticleVisibility.PUBLIC),
                    cb.not(a.get("id").in(viewed)),cb.notEqual(a.get("author").get("id"),valid));
        });
        List<Article> result=articles.findAll(candidates,PageRequest.of(0,10,Sort.by(Sort.Direction.DESC,"viewCount","id"))).getContent();
        return result.isEmpty()?hot(10,valid):result;
    }
}
