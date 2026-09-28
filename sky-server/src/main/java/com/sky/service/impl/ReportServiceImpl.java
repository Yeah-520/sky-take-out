package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
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


    /**
     * 获取营业额统计
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
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
}
