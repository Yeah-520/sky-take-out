package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
     * @param toBeConfirmed 待确认订单
     * @return 订单统计
     */
    @Select("select count(status) from orders where status = #{toBeConfirmed}")
    Integer getStatistics(Integer toBeConfirmed);

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
     * 根据状态和送达时间查询订单
     *
     * @param status       订单状态
     * @param deliveryTime 送达时间阈值(早于该时间即视为超时)
     * @return 订单列表
     */
    @Select("select * from orders where status = #{status} and delivery_time < #{deliveryTime}")
    List<Orders> getByStatusAndDeliveryTimeLT(@Param("status") Integer status,
                                              @Param("deliveryTime") LocalDateTime deliveryTime);

    /**
     * 修改订单状态
     *
     * @param orderStatus     订单状态
     * @param orderPaidStatus 订单支付状态
     * @param check_out_time  订单checkout时间
     * @param id              订单id
     */
    @Update("update orders set status = #{orderStatus},pay_status = #{orderPaidStatus} ,checkout_time = #{check_out_time} where id = #{id}")
    void updateStatus(Integer orderStatus, Integer orderPaidStatus, LocalDateTime check_out_time, Long id);

    /**
     * 查询营业额
     *
     * @param map 查询条件
     * @return 订单列表
     */
    Double sumByMap(Map<String, Object> map);

    /**
     * 根据条件查询订单数量
     *
     * @param map 查询条件
     * @return 订单数量
     */
    Integer countByMap(Map<String, Object> map);

    /**
     * 【性能优化】按订单状态分组统计:一次查询拿到全部状态的订单数与金额合计
     * <p>替代"同一张表按不同 status 反复 count/sum"的写法(原来 4~5 次查询 → 1 次)
     * <p>用 {@code @Param} 显式声明参数名,避免 map key 写错导致条件静默失效
     *
     * @param begin 下单时间起(可空)
     * @param end   下单时间止(可空)
     * @return 每行三个字段:{@code status}、{@code cnt}(订单数)、{@code total}(金额合计)
     */
    List<Map<String, Object>> countGroupByStatus(@Param("begin") LocalDateTime begin,
                                                 @Param("end") LocalDateTime end);

    /**
     * 按"天"分组统计订单:一次查询拿到区间内每天的订单数、有效订单数、营业额
     *
     * @param begin 下单时间起(含)
     * @param end   下单时间止(含)
     * @return 按天聚合的结果
     */
    List<Map<String, Object>> countGroupByDate(@Param("begin") LocalDateTime begin,
                                               @Param("end") LocalDateTime end);

    /**
     * 查询Top10菜品
     *
     * @param begin 开始时间
     * @param end   结束时间
     * @return 菜品列表
     */
    List<GoodsSalesDTO> getSalesTop10(LocalDateTime begin, LocalDateTime end);
}
