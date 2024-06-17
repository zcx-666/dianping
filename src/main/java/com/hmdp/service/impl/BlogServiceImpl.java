package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.ScrollResult;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IFollowService followService;

    @Override
    public Result queryBlogById(Long id) {
        // 查询blog
        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail("笔记不存在");
        }
        // 查询blog相关的用户
        queryBlogUser(blog);
        // 查询blog是否被点赞
        isBlogLiked(blog);
        return Result.ok(blog);
    }

    /**
     * 设置blog是否被当前用户点赞
     * @param blog blog
     */
    private void isBlogLiked(Blog blog) {
        String key = RedisConstants.BLOG_LIKED_KEY + blog.getId();
        // 获取当前登录用户
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            // 用户未登录，无需查询用户是否点赞
            return;
        }
        Long userId = user.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score != null);
    }

    /**
     * 设置blog的作者信息
     * @param blog blog
     */
    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }

    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        records.forEach(blog -> {
            this.queryBlogUser(blog);
            this.isBlogLiked(blog);

        });
        return Result.ok(records);
    }

    @Override
    public Result likeBlog(Long id) {
        // 获取登录用户
        Long userId = UserHolder.getUser().getId();
        // 判断用户是否点赞过该Blog
        String key = RedisConstants.BLOG_LIKED_KEY + id;
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if (score == null) {
            // 未点赞
            // 点赞数+1
            boolean success = update().setSql("liked = liked + 1").eq("id", id).update();
            // 添加该用户到点赞ZSet
            if (success) {
                stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
            }
        } else {
            // 点赞过，点赞数-1
            boolean success = update().setSql("liked = liked - 1").eq("id", id).update();
            // 从点赞Set中删除该用户
            if (success) {
                stringRedisTemplate.opsForZSet().remove(key, userId.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result queryBlogLikes(Long id) {
        // 查询blog前5名点赞的用户
        String key = RedisConstants.BLOG_LIKED_KEY + id;
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        // 根据用户id列表获取用户信息
        if (top5 == null || top5.isEmpty()) { // ObjectUtil.isEmpty不能去掉警告
            return Result.ok(Collections.emptyList());
        }
        List<Long> ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());
        // 用户信息脱敏
        // List<UserDTO> userDTOS = userService.listByIds(ids) // 这样查询使用的SQL不会按照参数顺序排序，而是id大小排序
        // WHERE id IN (5, 1) ORDER BY FIELD (id, 5, 1)
        String idsStr = StrUtil.join(",", ids);
        List<UserDTO> userDTOS = userService.query().in("id", ids)
                .last("ORDER BY FIELD (id," + idsStr + ")")
                .list()
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        // 返回信息
        return Result.ok(userDTOS);
    }

    @Override
    public Result saveBlog(Blog blog) {
        // Todo: 存在bug，新关注的用户无法获得推送
        // 使用推模式实现Feed流和推送
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 保存探店笔记
        boolean success = save(blog);
        if (!success) {
            return Result.fail("笔记保存失败");
        }
        // 获取作者的粉丝id名单，SELECT * FROM tb_follow WHERE follow_user_id = ?
        List<Follow> follows = followService.query().eq("follow_user_id", user.getId()).list();
        // 向所有粉丝推送笔记
        for (Follow follow : follows) {
            // 粉丝id
            Long userId = follow.getUserId();
            // 推送
            String key = RedisConstants.FEED_KEY + userId;
            stringRedisTemplate.opsForZSet().add(key, blog.getId().toString(), System.currentTimeMillis());
        }
        // 返回id
        return Result.ok(blog.getId());
    }

    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        // 获取用户id
        Long userId = UserHolder.getUser().getId();
        // 查询收件箱 ZREVRANGBYSCORE key max 0 WITHSCORE LIMIT offset count
        String key = RedisConstants.FEED_KEY + userId;
        long pageNum = 2L;
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate
                .opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, max, offset, pageNum);
        if (typedTuples == null || typedTuples.isEmpty()) {
            return Result.ok(new ScrollResult());
        }
        // 解析对象
        List<Long> ids = new ArrayList<>(typedTuples.size()); // 保存blog id
        long minTime = 0;
        int nextOffset = 1;
        for (ZSetOperations.TypedTuple<String> tuple : typedTuples) {
            // 获取blog id
            ids.add(Long.valueOf(tuple.getValue()));
            // 获取score（时间戳）
            long time = tuple.getScore().longValue();
            if (time == minTime) {
                nextOffset++;
            } else {
                minTime = time;
                nextOffset = 1;
            }
        }
        // 根据ids获取所有的blog
        String orderStr = StrUtil.join(",", ids);
        List<Blog> blogs = query().in("id", ids).last("ORDER BY FIELD (id, " + orderStr + ")").list();
        for (Blog blog : blogs) {
            this.queryBlogUser(blog);
            this.isBlogLiked(blog);
        }
        // 创建ScrollResult对象，并返回
        ScrollResult scrollResult = new ScrollResult();
        scrollResult.setList(blogs);
        scrollResult.setOffset(nextOffset);
        scrollResult.setMinTime(minTime);
        return Result.ok(scrollResult);
    }
}
