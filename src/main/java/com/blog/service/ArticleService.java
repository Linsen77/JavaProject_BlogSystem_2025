package com.blog.service;

import com.blog.entity.Article;
import com.blog.entity.User;
import com.blog.entity.Tag;
import com.blog.repository.ArticleRepository;
import com.blog.repository.TagRepository;
import com.blog.repository.UserViewHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ArticleService {

    @Autowired
    private ArticleRepository articleRepository;
    @Autowired
    private UserViewHistoryRepository userViewHistoryRepository;
    @Autowired
    private TagRepository tagRepository;

    //创建或更新文章
    public Article saveArticle(Article article){
        //设置创建时间和更新时间
        article.setCreateTime((article.getCreateTime() == null? LocalDateTime.now():article.getCreateTime()));
        article.setCreateTime(LocalDateTime.now());
        return articleRepository.save(article);
    }

    //添加标签到文章
    public Article addTagsToArticle(Long articleId,List<Long>tagIds){
        Optional<Article>articleOptional = articleRepository.findById(articleId);
        if(articleOptional.isPresent()){
            Article article = articleOptional.get();

            //遍历标签ID并将标签添加到文章
            for(Long tagId:tagIds)
            {
                //查找标签
                Tag tag = tagRepository.findById(tagId).orElseThrow(()->new RuntimeException("Tag not found"));
                article.getTags().add(tag);//添加标签
            }
            return articleRepository.save(article);//保存更新后的文章

        }
        throw new RuntimeException("Article not found");
    }



    //获取所有文章
    public List<Article> getAllArticles(){
        return articleRepository.findAll();
    }

    //根据ID获取文章
    public Optional<Article> getArticleById(long id){
        return articleRepository.findById(id);
    }

    //根据作者获取文章
    public List<Article>getArticleByAuthor(User author){
        return articleRepository.findByAuthor(author); //根据作者查找文章
    }

    //删除文章
    public void deleteArticle(Long id){
        articleRepository.deleteById(id);
    }

    //查看粉丝可见的文章
    public List<Article> findVisibleToFollowers(Long authorId) {
        return articleRepository.findVisibleToFollowers(authorId);
    }

    public List<Article> findByAuthorIdAndVisibility(Long authorId, Article.ArticleVisibility visibility) {
        return articleRepository.findByAuthorIdAndVisibility(authorId, visibility);
    }

    //1.按标题搜索文章
    public List<Article>searchByTitle(String keyword){
        //使用JPA查询按标题关键词搜索文章
        return articleRepository.findByTitleContainingAndVisibility(keyword, Article.ArticleVisibility.PUBLIC);
    }

    //2.按标签搜索文章
    public List<Article>searchByTag(String tagName){
        //使用JPA查询标签搜索文章
        return articleRepository.findByTag(tagName);
    }
    /**
     * 获取文章的所有标签
     *
     * @param articleId 文章ID
     * @return 文章的标签列表
     */
    public List<Tag>getTagsOfArticle(Long articleId)
    {
        Optional<Article> articleOptional = articleRepository.findById(articleId);
        if(articleOptional.isPresent()){
            return articleOptional.get().getTags();//返回文章的标签列表
        }
        throw new RuntimeException("Article not found");
    }

    //3.获取热门文章（按阅读量排序)

    /**
     * 获取热门文章（按阅读量排序）
     *
     * param limit 返回的文章数量
     * @return 按阅读量排序的热门文章列表
     */
    public List<Article>getHotArticles(int limit){
        //获取前10篇阅读量最高的公开文章
        Pageable pageable = PageRequest.of(0,limit);
        return articleRepository.findHotArticles(pageable);
    }


    //4.推荐文章（根据用户的浏览历史）

    /**
     * 基于用户浏览历史推荐文章
     *
     * @param userId 用户ID
     * @return 推荐的文章列表
     */
    public List<Article>recommendArticles(Long userId){
        //获取用户浏览过的文章ID列表
        List<Long> viewedArticleIds = userViewHistoryRepository.findViewedArticleIds(userId);

        if(viewedArticleIds.isEmpty()){
            //如果没有浏览历史，返回热门文章
            return getHotArticles(10);
        }

        //根据浏览过的文章统计标签信息
        List<String> topTags = getTopTagsFromArticle(viewedArticleIds);

        //返回推荐的文章
        return articleRepository.findArticlesByTags(topTags);
    }

    /**
     * 获取文章的标签并返回出现频率最高的标签
     *
     * @param articleIds 文章ID列表
     * @return 标签列表
     */
    private List<String>getTopTagsFromArticle(List<Long>articleIds){
        List<String>topTags = new ArrayList<>();

        //获取文章的标签信息
        for(Long articleId : articleIds){
            List<Tag>tags = articleRepository.findById(articleId).get().getTags();
            for(Tag tag : tags){
                topTags.add(tag.getName());//将标签名称加入到列表
            }
        }

        //获取出现频率最多的前3个标签
        return topTags.stream()
                .distinct()
                .limit(3)
                .collect(Collectors.toList());
    }
}
