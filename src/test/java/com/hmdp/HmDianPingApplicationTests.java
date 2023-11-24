package com.hmdp;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;

@Slf4j
@SpringBootTest
class HmDianPingApplicationTests {
    @Autowired
    private Environment env;


    @Test
    void envTest() {
        System.out.println("Hello World!");
        log.info(env.getProperty("spring.profiles"));
    }

}
