package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.CacheClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.ShopMessageConstants.NO_SHOP_ERROR_MESSAGE;
import static com.hmdp.utils.ShopMessageConstants.NULL_SHOP_ID_ERROR_MESSAGE;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    @Override
    public Result queryById(Long id) {
        // 缓存穿透
        // Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        // 互斥锁解决缓存击穿
        // Shop shop = queryWithMutex(id);

        // 逻辑过期解决缓存击穿
        Shop shop = cacheClient
                .queryWithLogicExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, 10L, TimeUnit.SECONDS);

        if (shop == null) {
            return Result.fail(NO_SHOP_ERROR_MESSAGE);
        }
        return Result.ok(shop);
    }


    public Shop queryWithMutex(Long id) {
        // 从Redis查询商铺id（缓存穿透方式）
        String shopKey = CACHE_SHOP_KEY + id;
        String shopJSON = stringRedisTemplate.opsForValue().get(shopKey);
        // 1. 判断Redis中是否存在
        if (StrUtil.isNotBlank(shopJSON)) {
            // 1.1 存在，直接返回
            return JSONUtil.toBean(shopJSON, Shop.class);
        }
        // 2. 如果命中的是否是空值（缓存穿透）
        if (shopJSON != null) {
            // 经过isNotBlank，现在shopJSON只能是null或者空串，如果不是null则是""，即命中缓存的空对象
            return null;
        }
        // 2. 实现缓存重建
        // 2.1 获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        Shop shop;
        try {
            boolean isLock = tryLock(lockKey);
            // 2.2 判断是否获取成功
            if (!isLock) {
                // 2.3 失败，休眠并重试
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            // 2.4 成功，根据id查询数据库
            shop = getById(id);
            // 模拟重建的延时
            // Thread.sleep(200);
            if (shop == null) {
                // 3. 不存在，将空值写入Redis（缓存穿透）
                stringRedisTemplate.opsForValue().set(shopKey, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                // 3.1 返回空值
                return null;
            }
            // 4.存在，写入Redis
            stringRedisTemplate.opsForValue().set(shopKey, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 5.释放互斥锁
            unlock(lockKey);
        }
        // 返回
        return shop;
    }


    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

    @Override
    @Transactional
    public Result updateShop(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail(NULL_SHOP_ID_ERROR_MESSAGE);
        }
        // 更新数据库
        updateById(shop);
        // 删除缓存
        String shopKey = CACHE_SHOP_KEY + id;
        stringRedisTemplate.delete(shopKey);
        return Result.ok();
    }
}
