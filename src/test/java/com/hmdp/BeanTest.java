package com.hmdp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@SpringBootTest
public class BeanTest {

    @Resource
    public Student student;


    @Test
    public void fun() {
        System.out.println(student);
    }
}
