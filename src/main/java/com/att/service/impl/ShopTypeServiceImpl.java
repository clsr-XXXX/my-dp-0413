package com.att.service.impl;

import cn.hutool.json.JSONUtil;
import com.att.entity.ShopType;
import com.att.mapper.ShopTypeMapper;
import com.att.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    /**
     * 查询商铺类型列表
     * @return
     */
    @Override
    public List<ShopType> queryTypeList() {
        String key = "cache:shop:type:list";
        // 查询redis中是否有
        String json = stringRedisTemplate.opsForValue().get(key);
        
        if (json != null) {
            return JSONUtil.toList(json, ShopType.class);
        }
        
        // 查询数据库
        List<ShopType> typeList = query().orderByAsc("sort").list();
        if (typeList == null) {
            return List.of();
        }
        // 写入redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(typeList), 30, TimeUnit.MINUTES);
        return typeList;
    }
}
