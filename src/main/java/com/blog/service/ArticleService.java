package com.blog.service;

import com.blog.dto.ArticleDTO;
import com.blog.entity.Article;
import com.blog.entity.User;
import com.blog.entity.Tag;
import com.blog.repository.ArticleRepository;
import com.blog.repository.TagRepository;
import com.blog.repository.UserRepository;
import com.blog.repository.UserViewHistoryRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ArticleService {

    @Autowired
    private ArticleRepository articleRepository;
    @Autowired
    private UserViewHistoryRepository userViewHistoryRepository;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired private com.blog.repository.CommentRepository commentRepository;
    @Autowired private com.blog.repository.LikeRepository likeRepository;
    @Autowired private com.blog.repository.BookmarkRepository bookmarkRepository;


    @Transactional
    public Article createArticle(ArticleDTO dto, Long actorId) {
        User author=requireUser(actorId);
        if (dto.getId()!=null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"创建文章不能指定ID，请使用更新接口");
        Article article=new Article();
        article.setAuthor(author);
        article.setCreateTime(LocalDateTime.now());
        return applyArticle(article,dto);
    }

    @Transactional
    public Article updateArticle(Long id, ArticleDTO dto, Long actorId) {
        return applyArticle(requireOwnedArticle(id,actorId),dto);
    }

    private User requireUser(Long actorId) {
        if(actorId==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"请先登录");
        return userRepository.findById(actorId).orElseThrow(()->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED,"登录用户已失效"));
    }

    private Article requireOwnedArticle(Long id, Long actorId) {
        requireUser(actorId);
        Article article=articleRepository.findById(id).orElseThrow(()->
                new ResponseStatusException(HttpStatus.NOT_FOUND,"文章不存在"));
        if(article.getAuthor()==null || !actorId.equals(article.getAuthor().getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"仅文章作者可以操作");
        return article;
    }

    private Article applyArticle(Article article, ArticleDTO dto){
        // 设置标题和内容
        article.setTitle(dto.getTitle());
        article.setContent(dto.getContent());
        // 设置可见性（枚举）
        try {
            article.setVisibility(Article.ArticleVisibility.valueOf(dto.getVisibility().toUpperCase(java.util.Locale.ROOT)));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"文章可见范围非法");
        }
        // dto.authorId is compatibility input only; never controls author identity.

        // 设置标签（支持字符串标签名）
        List<Tag> tagEntities = new ArrayList<>();
        for (String tagName : dto.getTags()==null ? List.<String>of() : dto.getTags()) {
            Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> {
                        Tag newTag = new Tag();
                        newTag.setName(tagName);
                        return tagRepository.save(newTag);
                    });
            tagEntities.add(tag);
        }
        article.setTags(tagEntities);
        // 更新时间
        article.setUpdateDate(LocalDateTime.now());
        //设置封面
        article.setCover(dto.getCover());


        return articleRepository.save(article);
    }

    //添加标签到文章
    @Transactional
    public Article addTagsToArticle(Long articleId,List<Long>tagIds,Long actorId){
            Article article = requireOwnedArticle(articleId,actorId);

            //遍历标签ID并将标签添加到文章
            for(Long tagId:tagIds)
            {
                //查找标签
                Tag tag = tagRepository.findById(tagId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"标签不存在"));
                article.getTags().add(tag);//添加标签
            }
            return articleRepository.save(article);//保存更新后的文章

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

    //根据作者名搜索文章
    public List<Article> searchByAuthor(String authorName) {
        return articleRepository.findByAuthor_NameContainingIgnoreCase(authorName);
    }

    //删除文章
    @Transactional
    public void deleteArticle(Long id,Long actorId){
        Article article=requireOwnedArticle(id,actorId);
        article=articleRepository.lockForDeletion(id).orElseThrow(()->
                new ResponseStatusException(HttpStatus.NOT_FOUND,"文章不存在"));
        // Delete only dependent rows; users and shared Tag definitions survive.
        commentRepository.deleteForArticle(id);
        likeRepository.deleteForArticle(id);
        bookmarkRepository.deleteForArticle(id);
        userViewHistoryRepository.deleteForArticle(id);
        // JPA removes this Article's owning article_tag links before the Article row.
        articleRepository.delete(article);
        articleRepository.flush();
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
    /**
     * 基于用户浏览历史推荐文章（协同过滤 + 标签权重）
     */
    public List<Article> recommendArticles(Long userId) {

        // 1. 查询用户浏览过的文章 ID
        List<Long> viewedArticleIds = userViewHistoryRepository.findViewedArticleIds(userId);

        // 2. 如果没有浏览历史 → 返回热门文章
        if (viewedArticleIds.isEmpty()) {
            return getHotArticles(10);
        }

        // 3. 统计用户浏览文章的标签权重（使用你已经写好的 findTopTags）
        List<String> topTags = getTopTagsFromArticle(viewedArticleIds);

        if (topTags.isEmpty()) {
            return getHotArticles(10);
        }

        // 4. 根据标签查找相关文章（按阅读量排序）
        List<Article> candidates = articleRepository.findArticlesByTags(topTags);

        // 5. 过滤掉用户已经看过的文章、私密文章、粉丝可见文章（如果不是粉丝）
        List<Article> filtered = candidates.stream()
                .filter(a -> !viewedArticleIds.contains(a.getId())) // 不推荐看过的
                .filter(a -> a.getVisibility() == Article.ArticleVisibility.PUBLIC) // 只推荐公开文章
                .filter(a -> !a.getAuthor().getId().equals(userId)) // 不推荐自己的文章
                .limit(10) // 限制数量
                .toList();

        // 6. 如果过滤后为空 → fallback 到热门文章
        if (filtered.isEmpty()) {
            return getHotArticles(10);
        }

        return filtered;
    }


    /**
     * 获取文章的标签并返回出现频率最高的标签
     *
     * @param articleIds 文章ID列表
     * @return 标签列表
     */
    private List<String>getTopTagsFromArticle(List<Long>articleIds){
        List<String>topTags = new ArrayList<>();
        //获取出现频率最多的前3个标签
        List<Object[]> rows = articleRepository.findTopTags(articleIds);
        return rows.stream()
                .map(row -> (String) row[0]) // 标签名
                .limit(3)
                .toList();
    }

    //通过作者id查找文章
    public List<Article> getArticleByAuthorId(Long authorId) {
        return articleRepository.findByAuthorId(authorId);
    }

    @Transactional
    public void incrementViewCount(Long id) {
        articleRepository.incrementViewCount(id);
    }

    @Transactional
    public void recordView(Long id,Long viewerId){
        // UPDATE locks the Article row until the history insert commits; deletion
        // locks that same row before cleanup. A deleted row must not gain history.
        if(articleRepository.incrementViewCount(id)==0)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"文章不存在");
        if(viewerId!=null)userViewHistoryRepository.save(new com.blog.entity.UserViewHistory(viewerId,id,LocalDateTime.now()));
    }

}
