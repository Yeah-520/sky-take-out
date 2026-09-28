package com.sky.service;

import com.sky.vo.TurnoverReportVO;

import java.time.LocalDate;

public interface ReportService {

    /**
     * 营业额统计
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 营业额统计结果
     */
    TurnoverReportVO getTurnoverStatistics(LocalDate startTime, LocalDate endTime);
}
