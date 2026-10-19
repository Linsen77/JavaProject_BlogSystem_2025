# JavaProject_BlogSystem_2025 Saki_orz Blog
后端项目  
配合[Saki_orz Blog 前端项目](https://github.com/Linsen77/Java_Project_BlogSystem_fronted_2025)食用  
后端技术路线：Java 21 + Maven + Spring Boot 3.2.5 + Spring MVC + Spring Data JPA + Hibernate + MySQL + WebSocket 在线通知与持久化通知历史。
  
实现了  
1.用户功能：注册登录（邮箱验证注册）、个人主页（头像、简介、发布的文章列表）、编辑个人资料。  
2.文章管理：发布文章（支持 Markdown 格式，插入图片）、编辑 / 删除自己的文章，设置文章可见范围（公开 / 仅粉丝 / 私密）。  
3.社交互动：关注 / 取消关注其他用户，查看关注列表动态。文章评论与点赞，收到评论 / 点赞时推送通知。  
4.搜索与推荐：按标题、标签、作者搜索文章，热门文章按阅读量排序。基于浏览历史的高频标签匹配推荐，并提供热门回退；不是协同过滤算法。
5.新增功能：①新增收藏功能，可以收藏、取消收藏文章，在个人主页可以看到收藏列表，自己的文章被收藏也能得到通知 ②新增统计功能，在个人主页显示统计用户发表文章数、关注数、粉丝数和收藏数。③新增统计获赞功能：显示在个人主页，用户获赞总数④新增评论删除功能：文章作者可以删除自己文章下的评论⑤新增标记消息已读功能。  
  
已补充作者授权、跨入口文章可见性、通知与 WebSocket 身份校验，以及文章关联删除和通知事务一致性测试。仍有上传安全、并发互动去重等限制，不代表全站安全或可靠消息投递。

当前配置采用环境变量；此前历史中出现的凭据需要轮换，当前文件脱敏不代表清除了 Git 历史。
运行环境中的数据独立保存，仓库不包含真实用户数据。

# 本地配置与验证

需要 Java 21、Maven、MySQL。数据库与 SMTP 配置使用环境变量，参见 `.env.example`；Spring Boot 不会自动加载 `.env`。默认 `ddl-auto=validate`，请先准备匹配的数据库结构，不要对现有数据盲目启用自动建表。

头像目录通过 `BLOG_AVATAR_UPLOAD_DIR`（`blog.avatar.upload-dir`）配置，默认 `./data/avatars`，相对应用工作目录解析，上传时自动创建。接口返回同源 `/avatar/<随机文件名>.png`，资源映射与保存共用目录；跨域前端应按后端 API Origin 解析此相对地址，反向代理需转发 `/avatar/`。本地头像文件不要提交，定制目录应放在仓库外。

隔离回归：`python3 scripts/run-ownership-tests.py public-release-final`。需要 Docker 和 Homebrew OpenJDK 21；脚本只创建独立临时 MySQL，使用合成测试数据和邮件替身，不读取 `.env`，不连接开发库。
