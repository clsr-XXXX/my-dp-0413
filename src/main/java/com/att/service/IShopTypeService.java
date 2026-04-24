package com.att.service;

import com.att.entity.ShopType;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 */
public interface IShopTypeService extends IService<ShopType> {

    /**
     * 查询商铺类型列表
     * @return
     */
    List<ShopType> queryTypeList();
}
