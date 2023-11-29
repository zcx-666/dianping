package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("lua/seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024);
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    @PostConstruct
    private void init() {
        SECKILL_ORDER_EXECUTOR.execute(new VoucherOrderHandler());
    }

    private class VoucherOrderHandler implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    // 获取阻塞队列中的订单信息
                    VoucherOrder voucherOrder = orderTasks.take();
                    // 创建订单
                    handleVoucherOrder(voucherOrder);
                } catch (Exception e) {
                    log.error("出现订单异常", e);
                }
            }
        }
    }

    private void handleVoucherOrder(VoucherOrder voucherOrder) {
        // 创建分布式锁对象（其实Redis中已经进行过超卖和一人一单的判断了，这里不需要锁，但是为了兜底还是加了）
        Long userId = voucherOrder.getUserId();
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        // 获取锁
        boolean isLock = lock.tryLock();
        if (!isLock) {
            // 获取锁失败，报错
            log.error("不允许重复下单");
            return;
        }
        try {
            // 获取代理对象（解决事务失效问题）
            proxy.createVoucherOrder(voucherOrder);
        } finally {
            // 释放锁
            lock.unlock();
        }
    }

    private IVoucherOrderService proxy;

    public Result seckillVoucher(Long voucherId) {
        // 1.查询秒杀优惠券（乐观锁）
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        // 2.判断秒杀时间是否合法
        LocalDateTime beginTime = seckillVoucher.getBeginTime();
        LocalDateTime endTime = seckillVoucher.getEndTime();
        if (beginTime.isAfter(LocalDateTime.now()) || endTime.isBefore(LocalDateTime.now())) {
            // 秒杀时间非法
            return Result.fail("秒杀未开始或已结束");
        }
        // 执行seckill.lua完成库存和一人一单的校验，0：成功，1：库存不足，2：重复下单
        UserDTO user = UserHolder.getUser();
        Long result = stringRedisTemplate.execute(SECKILL_SCRIPT, new ArrayList<>(), voucherId.toString(), user.getId().toString());
        // 结果判断
        int r = result.intValue();
        if (r == 1) {
            return Result.fail("库存不足");
        } else if (r == 2) {
            return Result.fail("请勿重复下单");
        }

        // 下单信息保存到阻塞队列
        // 创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        // 用户id
        voucherOrder.setUserId(user.getId());
        // 代金券id
        voucherOrder.setVoucherId(voucherId);
        orderTasks.add(voucherOrder);
        // 在主线程中获取代理对象，子线程中无法获取代理对象（通过代理对象保证事务）
        proxy = (IVoucherOrderService) AopContext.currentProxy();
        return Result.ok(orderId);
    }

    /* public Result seckillVoucher(Long voucherId) {
        // 1.查询秒杀优惠券（乐观锁）
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        // 2.判断秒杀时间是否合法
        LocalDateTime beginTime = seckillVoucher.getBeginTime();
        LocalDateTime endTime = seckillVoucher.getEndTime();
        if (beginTime.isAfter(LocalDateTime.now()) || endTime.isBefore(LocalDateTime.now())) {
            // 秒杀时间非法
            return Result.fail("秒杀未开始或已结束");
        }
        // 3.判断库存是否充足
        if (seckillVoucher.getStock() < 1) {
            return Result.fail("库存不足");
        }
        Long userId = UserHolder.getUser().getId();
        // 创建分布式锁对象
        // SimpleRedisLock lock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        // 获取锁
        boolean isLock = lock.tryLock();
        if (!isLock) {
            // 获取锁失败，报错
            return Result.fail("请勿重复下单");
        }
        try {
            // 获取代理对象（解决事务失效问题）
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        } finally {
            // 释放锁
            lock.unlock();
        }
    }
 */
    @Transactional
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        // 4.一人一单
        Long userId = voucherOrder.getUserId();
        // 4.1 查询订单
        Long voucherId = voucherOrder.getVoucherId();
        Long count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        // 4.2 判断是否存在
        if (count > 0) {
            // 用户已经购买过秒杀券
            log.error("请勿重复下单");
            return;
        }
        // 5.扣减库存
        boolean success = seckillVoucherService.update().setSql("stock = stock - 1").eq("voucher_id", voucherId).gt("stock", 0).update();
        if (!success) {
            log.error("库存不足");
            return;
        }
        save(voucherOrder);
    }
}
