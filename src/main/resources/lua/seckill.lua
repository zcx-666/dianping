---
--- Created by zcx.
--- DateTime: 2023/11/29 14:24
---

-- 优惠券id
local voucherId = ARGV[1];
-- 用户id
local userId = ARGV[2];
--
local orderId = ARGV[3];
-- 库存key
local stockKey = 'seckill:stock:' .. voucherId;
-- 订单集合key
local orderKey = 'seckill:order:' .. voucherId;

-- 判断库存是否充足
if (tonumber(redis.call('GET', stockKey)) <= 0) then
    -- 库存不足，返回1
    return 1;
end

-- 判断用户是否下单
if (redis.call('SISMEMBER', orderKey, userId) == 1) then
    -- 用户已经下过单了
    return 2;
end
-- 扣库存
redis.call('INCRBY', stockKey, -1);
-- 下单，把用户保存到订单集合中
redis.call('SADD', orderKey, userId);
-- 发送消息到消息队列中，因为在VoucherOrder类中，orderId的实际对应名称为id，所以方便起见，在存的时候key设置为id
redis.call('XADD', 'stream.orders', '*', 'userId', userId, 'voucherId', voucherId, 'id', orderId);
return 0;