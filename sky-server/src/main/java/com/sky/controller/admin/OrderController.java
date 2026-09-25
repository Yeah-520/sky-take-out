package com.sky.controller.admin;

import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.entity.Orders;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/order")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 条件查询订单
     *
     * @param ordersPageQueryDTO 订单查询条件
     * @return 订单分页查询结果
     */
    @GetMapping("/conditionSearch")
    public Result<PageResult> conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        log.info("条件查询订单");
        PageResult pageResult = orderService.conditionSearch(ordersPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 取消订单
     *
     * @param ordersCancelDTO 订单取消参数
     * @return 操作结果
     */
    @PutMapping("/cancel")
    public Result<String> cancelOrder(@RequestBody OrdersCancelDTO ordersCancelDTO) {
        log.info("取消订单");
        orderService.cancelOrder(ordersCancelDTO);
        log.info("取消订单成功");
        return Result.success();
    }

    /**
     * 查询订单详情
     *
     * @param id 订单id
     * @return 订单详情
     */
    @GetMapping("/details/{id}")
    public Result<OrderVO> getOrderDetails(@PathVariable Long id) {
        log.info("查询订单详情");
        OrderVO orderVO = orderService.details(id);
        return Result.success(orderVO);

    }

    /**
     * 查询订单统计
     *
     * @return 订单统计结果
     */
    @GetMapping("/statistics")
    public Result<OrderStatisticsVO> getStatistics() {
        log.info("查询订单统计");
        OrderStatisticsVO orderStatisticsVO = orderService.getStatistics();
        return Result.success(orderStatisticsVO);
    }

    /**
     * 确认订单
     *
     * @param ordersConfirmDTO 订单确认参数
     * @return 操作结果
     */
    @PutMapping("/confirm")
    public Result<String> confirm(@RequestBody OrdersConfirmDTO ordersConfirmDTO) {
        log.info("确认订单");
        orderService.confirmOrder(ordersConfirmDTO.getId());
        log.info("确认订单成功");
        return Result.success();
    }

    /**
     * 派送订单
     *
     * @param id 订单id
     * @return 操作结果
     */
    @PutMapping("/delivery/{id}")
    public Result<String> delivery(@PathVariable Long id) {
        log.info("准备派送，订单id：{}", id);
        orderService.delivery(id);
        log.info("派送中，订单id：{}", id);
        return Result.success();
    }

    /**
     * 拒绝订单
     *
     * @param ordersRejectionDTO 拒绝订单参数
     * @return 操作结果
     */
    @PutMapping("/rejection")
    public Result<String> rejection(@RequestBody OrdersRejectionDTO ordersRejectionDTO) {
        log.info("拒绝订单");
        orderService.rejection(ordersRejectionDTO);
        log.info("拒绝订单成功");
        return Result.success();
    }
}
