package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import com.sky.vo.OrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper {
    /**
     * 新增订单
     *
     * @param orders 订单信息
     */
    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     *
     * @param orderNumber 订单号
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     *
     * @param orders 订单信息
     */
    void update(Orders orders);

    /**
     * 订单信息修改
     *
     * @param orders 订单信息
     */
    @Update("update orders set status = #{status}, pay_status=#{payStatus},checkout_time=#{checkoutTime} where id = #{id}")
    void fakeUpdate(Orders orders);

    /**
     * 根据条件查询订单
     *
     * @param ordersPageQueryDTO 订单查询条件
     * @return 订单列表
     */
    Page<Orders> conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 取消订单
     *
     * @param orders 订单信息
     */
    @Update("update orders set status = #{status},cancel_time = #{cancelTime},cancel_reason = #{cancelReason} where id = #{id}")
    void cancelOrder(Orders orders);

    /**
     * 根据id查询订单
     *
     * @param id 订单id
     * @return 订单信息
     */
    @Select("select * from orders where id = #{id}")
    Orders getById(Long id);

    /**
     * 订单统计
     *
     * @param status 待确认订单
     * @return 订单统计
     */
    @Select("select count(status) from orders where status = #{toBeConfirmed}")
    Integer getStatistics(Integer status);

    /**
     * 根据条件查询订单
     *
     * @param ordersPageQueryDTO 订单查询条件
     * @return 订单列表
     */
    Page<Orders> historyOrders(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据状态和订单时间查询订单
     *
     * @param status    订单状态
     * @param orderTime 订单时间
     * @return 订单列表
     */
    @Select("select * from orders where status = #{status} and order_time < #{orderTime}")
    List<Orders> getByStatusAndOrderTimeLT(Integer status, LocalDateTime orderTime);

    /**
     * 修改订单状态
     *
     * @param orderStatus
     * @param orderPaidStatus
     * @param check_out_time
     * @param id
     */
    @Update("update orders set status = #{orderStatus},pay_status = #{orderPaidStatus} ,checkout_time = #{check_out_time} where id = #{id}")
    void updateStatus(Integer orderStatus, Integer orderPaidStatus, LocalDateTime check_out_time, Long id);
}
