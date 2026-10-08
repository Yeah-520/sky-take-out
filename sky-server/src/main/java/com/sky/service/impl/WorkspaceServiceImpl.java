package com.sky.service.impl;

import com.sky.constant.StatusConstant;
import com.sky.entity.Orders;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.WorkspaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class WorkspaceServiceImpl implements WorkspaceService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 根据时间段统计营业数据
     *
     * @param begin 开始时间
     * @param end   结束时间
     * @return 营业数据
     */
    @Override
    public BusinessDataVO getBusinessData(LocalDateTime begin, LocalDateTime end) {
        /*
          营业额：当日已完成订单的总金额
          有效订单：当日已完成订单的数量
          订单完成率：有效订单数 / 总订单数
          平均客单价：营业额 / 有效订单数
          新增用户：当日新增用户的数量
         */

        List<Map<String, Object>> rows = orderMapper.countGroupByStatus(begin, end);

        int totalOrderCount = 0;
        int validOrderCount = 0;
        double turnover = 0.0;
        for (Map<String, Object> row : rows) {
            int cnt = toInt(row.get("cnt"));
            // 全部状态相加即总订单数
            totalOrderCount += cnt;
            // 已完成那一行即有效订单数与营业额
            if (Orders.COMPLETED.equals(toInteger(row.get("status")))) {
                validOrderCount = cnt;
                turnover = toDouble(row.get("total"));
            }
        }

        double unitPrice = 0.0;
        double orderCompletionRate = 0.0;
        if (totalOrderCount != 0 && validOrderCount != 0) {
            //订单完成率
            orderCompletionRate = (double) validOrderCount / totalOrderCount;
            //平均客单价
            unitPrice = turnover / validOrderCount;
        }

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("begin", begin);
        userMap.put("end", end);
        Integer newUsers = userMapper.countByMap(userMap);

        return BusinessDataVO.builder()
                .turnover(turnover)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .unitPrice(unitPrice)
                .newUsers(newUsers)
                .build();
    }


    /**
     * 查询订单管理数据
     *
     * @return 订单管理数据
     */
    @Override
    public OrderOverViewVO getOrderOverView() {
        LocalDateTime begin = LocalDateTime.now().with(LocalTime.MIN);

        // 【性能优化】一次 group by 替代原来的 5 次 count（待接单/待派送/已完成/已取消/全部）
        Map<Integer, Integer> countMap = toStatusCountMap(orderMapper.countGroupByStatus(begin, null));

        // 全部订单 = 各状态之和
        int allOrders = countMap.values().stream().mapToInt(Integer::intValue).sum();

        return OrderOverViewVO.builder()
                .waitingOrders(countMap.getOrDefault(Orders.TO_BE_CONFIRMED, 0))   //待接单
                .deliveredOrders(countMap.getOrDefault(Orders.CONFIRMED, 0))      //待派送
                .completedOrders(countMap.getOrDefault(Orders.COMPLETED, 0))      //已完成
                .cancelledOrders(countMap.getOrDefault(Orders.CANCELLED, 0))      //已取消
                .allOrders(allOrders)
                .build();
    }

    /**
     * 查询菜品总览
     *
     * @return 菜品总览数据
     */
    @Override
    public DishOverViewVO getDishOverView() {
        // 【性能优化】一次 group by 替代原来的 2 次 count
        Map<Integer, Integer> countMap = toStatusCountMap(dishMapper.countGroupByStatus());

        return DishOverViewVO.builder()
                .sold(countMap.getOrDefault(StatusConstant.ENABLE, 0))
                .discontinued(countMap.getOrDefault(StatusConstant.DISABLE, 0))
                .build();
    }

    /**
     * 查询套餐总览
     *
     * @return 套餐总览数据
     */
    @Override
    public SetmealOverViewVO getSetmealOverView() {
        Map<Integer, Integer> countMap = toStatusCountMap(setmealMapper.countGroupByStatus());

        return SetmealOverViewVO.builder()
                .sold(countMap.getOrDefault(StatusConstant.ENABLE, 0))
                .discontinued(countMap.getOrDefault(StatusConstant.DISABLE, 0))
                .build();
    }

    /**
     * 把 group by 查询结果转成 status -> count 的映射
     *
     * @param rows countGroupByStatus 的查询结果
     * @return key = status，value = 数量
     */
    private Map<Integer, Integer> toStatusCountMap(List<Map<String, Object>> rows) {
        Map<Integer, Integer> countMap = new HashMap<>();
        for (Map<String, Object> row : rows) {
            countMap.put(toInteger(row.get("status")), toInt(row.get("cnt")));
        }
        return countMap;
    }

    /**
     * 安全转换说明：MyBatis 的 {@code resultType="java.util.Map"} 返回时，
     * 数值列的具体 Java 类型由 JDBC 驱动决定（count 通常是 Long，sum(decimal) 通常是 BigDecimal），
     * 因此统一按 {@link Number} 处理，避免直接强转引发 ClassCastException。
     */
    private Integer toInteger(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private int toInt(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }

    private double toDouble(Object value) {
        return value == null ? 0.0 : ((Number) value).doubleValue();
    }
}
