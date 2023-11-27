package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.LOCK_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.LOCK_SHOP_TTL;

@Slf4j
@Component
public class CacheClient {
    private final StringRedisTemplate stringRedisTemplate;
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);


    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 将Object转成JSON字符串，按照key存入Redis中，并附加过期时间
     *
     * @param key   Redis key
     * @param value Object
     * @param time  过期时间
     * @param unit  时间单位
     */
    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    /**
     * 将Object转成JSON字符串，按照key存入Redis中，并附加逻辑过期时间
     *
     * @param key   Redis key
     * @param value Object
     * @param time  过期时间
     * @param unit  时间单位
     */
    public void setWithLogicExpire(String key, Object value, Long time, TimeUnit unit) {
        // 将object封装到RedisData中，并添加过期时间
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }


    /**
     * 以缓存穿透的方式获取key对应的数据
     *
     * @param keyPrefix  Redis的Key前缀
     * @param id         目标value的查询id
     * @param type       value类型
     * @param dbFallback 从MySQL中查询value的方法
     * @param time       缓存过期时间
     * @param unit       缓存过期时间的单位
     * @param <R>        value的类别泛型
     * @param <ID>       id的泛型
     * @return value
     */
    public <R, ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        // 从Redis查询商铺id（缓存穿透方式）
        String key = keyPrefix + id;
        String jsonStr = stringRedisTemplate.opsForValue().get(key);
        // 判断Redis中是否存在
        if (StrUtil.isNotBlank(jsonStr)) {
            // 存在，直接返回
            return JSONUtil.toBean(jsonStr, type);
        }
        // 如果命中的是否是空值
        if (jsonStr != null) {
            // 经过isNotBlank，现在shopJSON只能是null或者空串，如果不是null则是""，即命中缓存的空对象
            return null;
        }
        // 不存在，根据id查询MySQL
        R r = dbFallback.apply(id);
        if (r == null) {
            // 不存在，将空值写入Redis（缓存穿透）
            stringRedisTemplate.opsForValue().set(key, "", time, unit);
            return null;
        }
        // 存在，写入Redis
        set(key, r, time, unit);
        // 返回
        return r;
    }

    public <R, ID> R queryWithLogicExpire(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        // 从Redis查询对象id（逻辑过期方式）
        String key = keyPrefix + id;
        String redisDataJSON = stringRedisTemplate.opsForValue().get(key);
        // 1.判断Redis中是否存在
        if (StrUtil.isBlank(redisDataJSON)) {
            // 1.1 不存在，直接返回，但是正常情况这种状况不应该出现
            return null;
        }
        // 1.2 存在，把JSON反序列化为对象
        RedisData redisShop = JSONUtil.toBean(redisDataJSON, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisShop.getData(), type);
        // 2.判断是否逻辑过期
        LocalDateTime expireTime = redisShop.getExpireTime();
        if (expireTime.isAfter(LocalDateTime.now())) {
            // 2.1 未过期，直接返回
            return r;
        }
        // 2.2 过期，需要缓存重建
        // 3.缓存重建
        // 3.1 获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        // 3.2 判断是否获取互斥锁
        if (isLock) {
            // 3.3 获取成功，开启独立线程，实现缓存重建（使用线程池）
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    // 重建缓存
                    // 查询数据库
                    R r1 = dbFallback.apply(id);
                    // 缓存写入Redis
                    this.setWithLogicExpire(key, r1, time, unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    // 释放锁
                    unlock(lockKey);
                }
            });

        }
        // 3.4 返回过期的信息
        return r;
    }


    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }
}
