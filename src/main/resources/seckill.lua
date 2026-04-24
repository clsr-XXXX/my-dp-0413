---
--- Created by gml.
--- DateTime: 2026/4/17 14:28
---

-- 判断库存是否充足
local voucherId = ARGV[1]
local userId = ARGV[2]

local stockKey = 'seckill:stock:' .. voucherId
local orderKey = 'seckill:order:' .. voucherId

-- 获取库存值（关键修复1：处理nil值）
local stock = redis.call('get', stockKey)

-- 当stock为nil或0时，表示库存不足
if stock == false or tonumber(stock) <= 0 then
    -- 库存不足
    return 1
end

-- 判断用户是否下单
if(redis.call('sismember',orderKey,userId) == 1) then
    -- 重复下单
    return 2
end

-- 下单扣库存
redis.call('incrby',stockKey,-1)
-- 下单保存用户
redis.call('sadd',orderKey,userId)
-- 成功
return 0