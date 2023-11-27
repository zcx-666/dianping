package com.hmdp.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.ShopMessageConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryTypeList() {

        // 从Redis中查询商户类型
        String shopTypeKey = RedisConstants.CACHE_SHOP_TYPE_KEY;
        List<String> typeListStr = stringRedisTemplate.opsForList().range(shopTypeKey, 0, -1);
        // 判断是否存在
        if (typeListStr != null && !typeListStr.isEmpty()) {
            // Redis中存在，返回
            List<ShopType> typeList = typeJSONListToList(typeListStr);
            return Result.ok(typeList);
        }
        // 不存在，在MySQL中查询
        List<ShopType> typeList = query().orderByAsc("sort").list();
        // 判断MySQL中是否存在
        if (typeList.isEmpty()) {
            // 不存在，报错
            return Result.fail(ShopMessageConstants.NO_SHOP_TYPE_ERROR_MESSAGE);
        }
        // 存在，存入Redis
        typeListStr = typeListToJSON(typeList);
        stringRedisTemplate.opsForList().rightPushAll(shopTypeKey, typeListStr);
        // 返回
        return Result.ok(typeList);
    }

    private List<ShopType> typeJSONListToList(List<String> typeListStr) {
        List<ShopType> res = new ArrayList<>(typeListStr.size());
        for (String s : typeListStr) {
            res.add(JSONUtil.toBean(s, ShopType.class));
        }
        return res;
    }

    private List<String> typeListToJSON(List<ShopType> typeList) {
        JSONArray temp = JSONUtil.parseArray(typeList);
        return temp.toList(String.class);
    }
}
