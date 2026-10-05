package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 订单取消任务，处理超时订单
     */
    @Scheduled(cron = "0 * * * * ?")
    public void processTimeoutOrders() {
        log.info("订单取消任务，处理超时订单");
        List<Orders> list = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, LocalDateTime.now().plusMinutes(-15));

        if (list != null && !list.isEmpty()) {
            for (Orders orders : list) {
                if (!Orders.PENDING_PAYMENT.equals(orders.getStatus())) {
                    continue;
                }
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelTime(LocalDateTime.now());
                orders.setCancelReason("订单超时取消");
                orderMapper.update(orders);
            }
        }
    }

    /**
     * 订单完成任务，处理待完成订单
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void processDeliverOrders() {
        log.info("订单发货任务，处理待发货订单");
        List<Orders> list = orderMapper.getByStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS, LocalDateTime.now().plusMinutes(-60));

        if (list != null && !list.isEmpty()) {
            for (Orders orders : list) {
                orders.setStatus(Orders.COMPLETED);
                orderMapper.update(orders);
            }
        }
    }
}
