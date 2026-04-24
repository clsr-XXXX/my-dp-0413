package com.att.service.impl;

import com.att.dto.Result;
import com.att.entity.VoucherOrder;
import com.att.mapper.VoucherOrderMapper;
import com.att.service.ISeckillVoucherService;
import com.att.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.att.utils.RedisIdWorker;
import com.att.utils.UserHolder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.concurrent.*;

/**
 * <p>
 *  服务实现类
 * </p>
 */
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private RedissonClient redissonClient;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT = new DefaultRedisScript<>();
    static {
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    private BlockingDeque<VoucherOrder> orderTasks = new LinkedBlockingDeque<>();
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    @PostConstruct
    private void init() {
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }
    private class VoucherOrderHandler implements Runnable {

        @Override
        public void run() {
            while (true){

                try {
                    // 获取队列中的订单信息
                    VoucherOrder order = orderTasks.take();

                    // 创建订单
                    hanleVoucherOrder(order);
                } catch (InterruptedException e) {
                    log.error("处理订单异常",e);
                }

            }
        }
    }

    private void hanleVoucherOrder(VoucherOrder order) {
        // 获取用户
        Long userId = order.getUserId();
        // 获取锁
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        boolean isLock = lock.tryLock();
        // 5.3.判断是否获取锁成功
        if (!isLock) {
            // 获取锁失败，返回错误或重试
            log.error("不允许重复下单");
            return;
        }

        try {
            // 获取代理对象
            proxy.createVoucherOrder(order);
        } finally {
            // 释放锁
            lock.unlock();
        }
    }


    private IVoucherOrderService proxy;
    /**
     * 秒杀优惠券
     * @param voucherId
     * @return
     */
    @Override
    public Result seckillVoucher(Long voucherId){
        Long userId = UserHolder.getUser().getId();
        //  执行lua脚本
        Long result = stringRedisTemplate.execute(SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString()
        );
        // 判断结果是否为0
        int r = result.intValue();
        if(r != 0){
            if( r == 1){
                return Result.fail("库存不足");

            }

            return Result.fail("不能重复下单");
        }

        long orderId = redisIdWorker.nextId("order");
        // TODO 保存阻塞队列
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        orderTasks.add(voucherOrder);
        // 获取代理对象
        proxy = (IVoucherOrderService) AopContext.currentProxy();
        return Result.ok(orderId);

    }
    /*
    public Result seckillVoucher(Long voucherId) {
        // 1. 查询优惠券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        if (voucher == null) {
            return Result.fail("优惠券不存在");
        }
        // 2. 判断秒杀是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀尚未开始");
        }
        // 3. 判断秒杀是否已经结束
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀已经结束");
        }
        // 4. 判断库存是否充足
        if (voucher.getStock() < 1) {
            return Result.fail("库存不足");
        }

        Long userId = UserHolder.getUser().getId();
        // 5. 一人一单逻辑
        // 5.1.创建锁对象
       //SimpleRedisLock redisLock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
        // 5.2.获取锁
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        boolean isLock = lock.tryLock();
        // 5.3.判断是否获取锁成功
        if (!isLock) {
            // 获取锁失败，返回错误或重试
            return Result.fail("不允许重复下单");
        }

        try {
            // 获取代理对象
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        } finally {
            // 释放锁
            lock.unlock();
        }
    }
    */
    @Transactional
    public void createVoucherOrder(VoucherOrder order) {
        // 5. 一人一单
        Long userId = UserHolder.getUser().getId();

        // 5.1. 查询订单
        Long count = query().eq("user_id", userId).eq("voucher_id", order).count();
        // 5.2. 判断是否存在
        if (count > 0) {
            log.error("用户已经购买过一次");
            return;
        }

        // 6. 扣减库存
        boolean success = seckillVoucherService.updateStock(order.getVoucherId());
        if (!success) {
            log.error("库存不足");
            return;
        }

        // 7. 创建订单

        save(order);

        // 7. 返回订单id


    }
}
