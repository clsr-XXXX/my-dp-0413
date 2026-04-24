package com.att.service;

import com.att.dto.Result;
import com.att.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    /**
     * 秒杀功能
     * @param voucherId
     * @return
     */
    Result seckillVoucher(Long voucherId);

    /**
     * 创建订单
     *
     * @param order
     * @return
     */
    void createVoucherOrder(VoucherOrder order);
}
