package com.att.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.att.dto.Result;
import com.att.entity.Shop;
import com.att.mapper.ShopMapper;
import com.att.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.att.utils.RedisConstants;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 根据id查询商铺信息
     * @param id
     * @return
     */
    @Override
    public Result queryById(Long id) {
        // 在redis查询商铺信息
        // 是JSON格式
        String string = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id);
        // 判断是否存在
        if (StrUtil.isNotBlank(string)) {
            // 存在直接返回
            Shop shop = JSONUtil.toBean(string, Shop.class);
            return Result.ok(shop);
        }
        // 存在但值为空，说明之前查询过数据库不存在
        if (string != null) {
            // 返回错误信息
            return Result.fail("商铺不存在");
        }

        // 不存在，尝试获取互斥锁
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        boolean isLock = false;
        try {
            // 尝试获取锁
            isLock = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", RedisConstants.LOCK_SHOP_TTL, TimeUnit.SECONDS);
            if (!isLock) {
                // 获取锁失败，等待一段时间后重试
                Thread.sleep(50);
                return queryById(id);
            }
            // 获取锁成功，查询数据库
            Shop shop = getById(id);
            if (shop == null) {
                // 不存在返回错误信息
                // 为了防止缓存穿透，应该将空值写入redis
                stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
                return Result.fail("商铺不存在");
            }
            // 存在写入redis
            stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
            return Result.ok(shop);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 释放锁
            if (isLock) {
                stringRedisTemplate.delete(lockKey);
            }
        }
    }

    /**
     * 更新商铺信息
     * @param shop
     * @return
     */
    @Override
    @Transactional // 保证方法里面的多条数据库操作，要么全部成功，要么全部失败（回滚），从而保证数据的一致性。
    public Result update(Shop shop) {
        // 更新数据库
        updateById(shop);
        // 删除redis中的商铺信息
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("商铺id不能为空");
        }
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + shop.getId());
        return Result.ok();
    }
}
