package com.sky.service;

import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService {

    /**
     * 提交订单
     *
     * @param ordersSubmitDTO 订单提交参数
     * @return 订单提交结果
     */
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO 订单支付参数
     * @return 订单支付结果
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo 订单号
     */
    void paySuccess(String outTradeNo);

    /**
     * 虚假的支付成功，手动修改数据库内容：order表的付款字段相关内容
     *
     * @param outTradeNo 订单号
     */
    void paySuccess(String outTradeNo, boolean isFake);

    /**
     * 条件查询订单
     *
     * @param ordersPageQueryDTO 订单查询条件
     * @return 订单分页查询结果
     */
    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 取消订单
     *
     * @param ordersCancelDTO 订单取消参数
     */
    void cancelOrder(OrdersCancelDTO ordersCancelDTO);

    /**
     * 根据id查询订单
     *
     * @param id 订单id
     * @return 订单详情
     */
    OrderVO details(Long id);

    /**
     * 获取订单统计信息
     *
     * @return 订单统计信息
     */
    OrderStatisticsVO getStatistics();

    /**
     * 确认订单
     *
     * @param id 订单id
     */
    void confirmOrder(Long id);

    /**
     * 准备派送，修改订单状态
     *
     * @param id 订单id
     */
    void delivery(Long id);
}
