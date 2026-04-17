package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.IVoucherOrderService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 */
@RestController
@Slf4j
@RequestMapping("/voucher-order")
public class VoucherOrderController {
    @Resource
    IVoucherOrderService voucherOrderService;

    @PostMapping("seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {

        log.info("秒杀券id：{}", voucherId);

        return voucherOrderService.seckillVoucher(voucherId);
    }
}
