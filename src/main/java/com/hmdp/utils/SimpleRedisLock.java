package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.BooleanUtil;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock {
    private final String name;
    private final StringRedisTemplate stringRedisTemplate;
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    private static final String KEY_PREFIX = "lock:";

    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";
    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("lua/unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        // 获取线程标识
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        // 获取锁
        String key = KEY_PREFIX + name;
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, threadId, timeoutSec, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(success);
    }

    @Override
    public void unlock() {
        // 调用lua脚本实现释放锁的原子性
        // 获取线程标识
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        // 获取锁中标识
        String key = KEY_PREFIX + name;
        String id = stringRedisTemplate.opsForValue().get(key);
        stringRedisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key), threadId);
    }
}
