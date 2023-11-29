package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisIdWorker;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

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
    private Student student;

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
    }

    @Test
    void autoBeanTest() {
        if (student == null) {
            System.out.println("GG");
        } else {
            System.out.println("ok");
        }
    }

    @Test
    void shopSaveTest() {
        Long id = 1L;
        Shop shop = shopService.getById(id);
        cacheClient.setWithLogicExpire(RedisConstants.CACHE_SHOP_KEY + id, shop, 10L, TimeUnit.SECONDS);
    }
}
