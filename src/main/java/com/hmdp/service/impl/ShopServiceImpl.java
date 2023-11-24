package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.ShopMessageConstants;
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
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryById(Long id) {
        // 从Redis查询商铺id
        String shopKey = CACHE_SHOP_KEY + id;
        String shopJSON = stringRedisTemplate.opsForValue().get(shopKey);
        // 判断Redis中是否存在
        if (StrUtil.isNotBlank(shopJSON)) {
            // 存在，直接返回
            Shop shop = JSONUtil.toBean(shopJSON, Shop.class);
            return Result.ok(shop);
        }
        // 如果命中的是否是空值（缓存穿透）
        if (shopJSON != null) {
            // 经过isNotBlank，现在shopJSON只能是null或者空串，如果不是null则是""，即命中缓存的空对象
            return Result.fail(NO_SHOP_ERROR_MESSAGE);
        }
        // 不存在，根据id查询MySQL
        Shop shop = getById(id);
        if (shop == null) {
            // 不存在，将空值写入Redis（缓存穿透）
            stringRedisTemplate.opsForValue().set(shopKey, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return Result.fail(NO_SHOP_ERROR_MESSAGE);
        }
        // 存在，写入Redis
        stringRedisTemplate.opsForValue().set(shopKey, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        // 返回
        return Result.ok(shop);
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
