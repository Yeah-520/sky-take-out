package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;


    /**
     * 获取营业额统计
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return TurnoverReportVO
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate startTime, LocalDate endTime) {

        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(startTime);

        while (!startTime.equals(endTime)) {
            startTime = startTime.plusDays(1);
            dateList.add(startTime);
        }

        List<Double> turnoverlist = new ArrayList<>();

        for (LocalDate dateTime : dateList) {
            LocalDateTime startDateTime = LocalDateTime.of(dateTime, LocalTime.MIN);
            LocalDateTime endDateTime = LocalDateTime.of(dateTime, LocalTime.MAX);

            Map<String, Object> map = new HashMap<>();
            map.put("startDateTime", startDateTime);
            map.put("endDateTime", endDateTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0 : turnover;
            turnoverlist.add(turnover);
        }

        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverlist, ","))
                .build();
    }

    /**
     * 获取用户统计
     *
     * @param begin 开始时间
     * @param end   结束时间
     * @return UserReportVO
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {

        List<LocalDate> dateList = new ArrayList<>();

        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> newUserList = new ArrayList<>();

        List<Integer> totalUserList = new ArrayList<>();

        for (LocalDate dateTime : dateList) {
            LocalDateTime startDateTime = LocalDateTime.of(dateTime, LocalTime.MIN);
            LocalDateTime endDateTime = LocalDateTime.of(dateTime, LocalTime.MAX);

            Map<String, Object> map = new HashMap<>();
            map.put("end", endDateTime);

            Integer totalUser = userMapper.countByMap(map);

            map.put("begin", startDateTime);
            Integer newUser = userMapper.countByMap(map);

            newUserList.add(newUser);
            totalUserList.add(totalUser);
        }


        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .build();
    }

    /**
     * 获取订单统计
     *
     * @param begin 开始时间
     * @param end   结束时间
     * @return OrderReportVO
     */
    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();

        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> orderCountList = new ArrayList<>();

        List<Integer> validOrderCountList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Integer orderCount = getOrderCount(beginTime, endTime, null);

            Integer validOrderCount = getOrderCount(beginTime, endTime, Orders.COMPLETED);

            orderCountList.add(orderCount);
            validOrderCountList.add(validOrderCount);
        }

        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();

        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();

        Double orderCompletionRate = 0.0;
        if (totalOrderCount != 0) {
            //计算订单完成率
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        }


        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 根据时间范围和状态获取订单数量
     *
     * @param beginTime 开始时间
     * @param endTime   结束时间
     * @param status    状态
     * @return 订单数量
     */
    private Integer getOrderCount(LocalDateTime beginTime, LocalDateTime endTime, Integer status) {
        Map<String, Object> map = new HashMap<>();
        map.put("startDateTime", beginTime);
        map.put("endDateTime", endTime);
        map.put("status", status);
        return orderMapper.countByMap(map);
    }

}
