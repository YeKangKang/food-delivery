package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务类，定时处理订单状态
 */
@Component  // 需要注册为Bean由Spring管理
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理超时订单状态
     */
    @Scheduled(cron = "0 * * * * ?")    // 秒 分 时 日 月 周 【年】（每分钟触发一次）
    public void processTimeoutOrder () {
        log.info("定时处理超时订单: {}", LocalDateTime.now());

        // 查询超时的订单: select * from orders where status = ? and order_time < (当前时间 - 15分钟)
        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);  // 15分钟之前
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, time);

        // 判断是否有超时订单
        if (ordersList != null && !ordersList.isEmpty()) {
            for (Orders o : ordersList) {
                o.setStatus(Orders.CANCELLED);          // 订单状态：已取消（6）
                o.setCancelReason("订单超时未支付");       // 取消理由
                o.setCancelTime(LocalDateTime.now());   // 取消时间

                orderMapper.update(o);  // 更新
            }
        }
    }

    /**
     * 处理上一天一直处于派送中的订单
     */
    @Scheduled(cron = "0 0 1 * * ?")    // 每天凌晨1点触发
    public void processDeliveryOrder() {
        log.info("定时处理派送中订单: {}", LocalDateTime.now());

        // 查询未完成订单：select * from orders where status = ? and order_time < (当前时间 - 15分钟)
        LocalDateTime time = LocalDateTime.now().plusHours(-1); // 一个小时之前，也就是00:00的时候
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS, time);

        // 判断是否有未处理派送中订单
        if (ordersList != null && !ordersList.isEmpty()) {
            for(Orders orders : ordersList) {
                orders.setStatus(Orders.COMPLETED);     // 订单完成
//                orders.setDeliveryTime(LocalDateTime.now());    // 设置送达时间
                orderMapper.update(orders); // 更新
            }
        }
    }
}
