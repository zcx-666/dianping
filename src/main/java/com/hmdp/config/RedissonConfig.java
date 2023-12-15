package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.annotation.Resource;

@Configuration
public class RedissonConfig {
    @Resource
    private Environment env;

    @Bean
    public RedissonClient redissonClient() {
        // 配置
        Config config = new Config();
        config.useSingleServer().setAddress("redis://" + env.getProperty("spring.redis.host") + ":6379").setPassword("123321");
        // 创建RedissonClient对象
        return Redisson.create(config);
    }
}
