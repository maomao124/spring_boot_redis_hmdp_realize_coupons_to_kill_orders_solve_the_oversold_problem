package mao.spring_boot_redis_hmdp.service.impl;


import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import mao.spring_boot_redis_hmdp.dto.Result;
import mao.spring_boot_redis_hmdp.entity.SeckillVoucher;
import mao.spring_boot_redis_hmdp.entity.VoucherOrder;
import mao.spring_boot_redis_hmdp.mapper.VoucherOrderMapper;
import mao.spring_boot_redis_hmdp.service.ISeckillVoucherService;
import mao.spring_boot_redis_hmdp.service.IVoucherOrderService;
import mao.spring_boot_redis_hmdp.utils.RedisIDGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;


@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService
{
    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIDGenerator redisIDGenerator;

    @Override
    @Transactional
    public Result seckillVoucher(Long voucherId)
    {
        //查询优惠券
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        //判断是否存在
        if (seckillVoucher == null)
        {
            return Result.fail("活动不存在");
        }
        //判断是否开始
        if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now()))
        {
            //未开始
            return Result.fail("秒杀活动未开始");
        }
        //判断是否结束
        if (seckillVoucher.getEndTime().isBefore(LocalDateTime.now()))
        {
            //结束
            return Result.fail("秒杀活动已经结束");
        }
        //判断库存是否充足
        if (seckillVoucher.getStock() <= 0)
        {
            //库存不足
            return Result.fail("库存不足");
        }
        //扣减库存
        UpdateWrapper<SeckillVoucher> updateWrapper = new UpdateWrapper<>();
        updateWrapper.setSql("stock = stock - 1").eq("voucher_id", voucherId).gt("stock",0);
        boolean update = seckillVoucherService.update(updateWrapper);
        if (!update)
        {
            //失败
            return Result.fail("库存扣减失败");
        }
        //扣减成功
        //生成订单
        VoucherOrder voucherOrder = new VoucherOrder();
        //生成id
        Long orderID = redisIDGenerator.nextID("order");
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setId(orderID);
        //设置用户
        //Long userID = UserHolder.getUser().getId();
        //voucherOrder.setUserId(userID);
        voucherOrder.setUserId(5L);
        //todo:记得更改回来
        //保存订单
        this.save(voucherOrder);
        //返回
        return Result.ok(orderID);
    }
}
