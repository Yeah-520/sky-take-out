package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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

    @Update("update orders set `status` = #{status}, pay_status=#{payStatus},checkout_time=#{checkoutTime} where id = #{id}")
    void fakeUpdate(Orders orders);

    /**
     * 根据条件查询订单
     *
     * @param ordersPageQueryDTO 订单查询条件
     * @return 订单列表
     */
    Page<Orders> conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);
}
