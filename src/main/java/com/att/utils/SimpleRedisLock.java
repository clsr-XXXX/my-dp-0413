package com.att.utils;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements AutoCloseable {

    private StringRedisTemplate stringRedisTemplate;
    private String lockName;
    private static final String KEY_PREFIX = "lock:";
    private static final String ID_PREFIX = UUID.randomUUID().toString() + "-";
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>();

    static {
        // 正确写法
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    public SimpleRedisLock(String lockName, StringRedisTemplate stringRedisTemplate) {
        this.lockName = lockName;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public boolean tryLock(long timeoutSec) {
        // 获取线程标识
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        // 获取锁
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockName, threadId + "", timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    /*
    public void unlock() {
        // 获取线程标识
        String threadId = ID_PREFIX+Thread.currentThread().getId();
        // 获取锁中的标识
        String id = stringRedisTemplate.opsForValue().get(lockName);
        // 判断标识是否一致
        if(threadId.equals(id)) {
            // 释放锁
            stringRedisTemplate.delete(lockName);
        }
    }
     */
    // 基于lua脚本实现原子操作
    public void unlock() {
        // 调用脚本
        stringRedisTemplate.execute(UNLOCK_SCRIPT,
                Collections.singletonList(lockName),
                ID_PREFIX + Thread.currentThread().getId());

    }


    @Override
    public void close() throws Exception {
        unlock();
    }
}
