package com.hmdp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;

@SpringBootTest
public class RedisTest {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void init() {
    }

    @Test
    void getTest() {
        System.out.println(stringRedisTemplate.opsForValue().get("name"));
    }
}
