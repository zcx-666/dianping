package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisIdWorker;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootTest
class HmDianPingApplicationTests {

    @Resource
    private Environment env;

    @Resource
    private ShopServiceImpl shopService;

    @Resource
    private CacheClient cacheClient;

    @Resource
    private RedisIdWorker redisIdWorker;

    private final ExecutorService es = Executors.newFixedThreadPool(500);

    @Test
    void redisIdWorkerTest() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(300);
        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                long id = redisIdWorker.nextId("order");
                System.out.println("id = " + id);
            }
            latch.countDown();
        };
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            es.submit(task);
        }
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println();
        System.out.println("time = " + (end - begin));
    }

    @Test
    void envTest() {
        System.out.println("Hello World!");
        log.info(env.getProperty("spring.profiles"));
        log.info(env.getProperty("spring.redis.host"));
    }

    @Resource
    private Student resource;

    @Test
    void autoBeanTest() {
        if (resource == null) {
            System.out.println("GG");
        } else {
            System.out.println("ok");
            System.out.println(resource);
        }
//        BeanTest beanTest = new BeanTest();
//        assert beanTest.stringRedisTemplate != null : "注入失败";
    }

    @Test
    @Transactional
    void shopSaveTest() {
        // 测试使用逻辑过期解决缓存击穿问题
        Long id = 1L;
        Shop shop = shopService.getById(id);
        cacheClient.setWithLogicExpire(RedisConstants.CACHE_SHOP_KEY + id, shop, 10L, TimeUnit.SECONDS);
    }


    @Resource
    RedissonClient redissonClient;

    @Resource
    StringRedisTemplate stringRedisTemplate;
    @Test
    void redisLock() {
        RLock lock = redissonClient.getLock("sdfafsda");
        lock.tryLock();
        stringRedisTemplate.opsForValue().set("tt", "asd");
        System.out.println(123);
    }


}
