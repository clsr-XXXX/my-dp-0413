package com.att.service;

import com.att.dto.Result;
import com.att.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 */
public interface IShopService extends IService<Shop> {

    /**
     * 根据id查询商铺信息
     * @param id
     */
    Result queryById(Long id);

    /**
     * 更新商铺信息
     * @param shop
     * @return
     */
    Result update(Shop shop);
}
