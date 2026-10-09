package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.dto.GoodsSalesDTO;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;


    /**
     * 获取营业额统计
     *
     * @param begin 开始时间
     * @param end   结束时间
     * @return TurnoverReportVO
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {

        List<LocalDate> dateList = buildDateList(begin, end);

        // 【性能优化】一次按天分组查询取回区间内每天的营业额,替代"循环 N 天 × 每天查库"
        Map<LocalDate, Map<String, Object>> dailyOrder = queryDailyOrder(begin, end);

        List<Double> turnoverlist = new ArrayList<>();
        for (LocalDate date : dateList) {
            Map<String, Object> row = dailyOrder.get(date);
            turnoverlist.add(row == null ? 0.0 : toDouble(row.get("turnover")));
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

        List<LocalDate> dateList = buildDateList(begin, end);

        // ① 区间开始之前已存在的用户数(累计基数),1 次查询
        Map<String, Object> baseMap = new HashMap<>();
        baseMap.put("end", LocalDateTime.of(begin, LocalTime.MIN).minusSeconds(1));
        int baseTotal = toInt(userMapper.countByMap(baseMap));

        // ② 区间内每天的新增用户,1 次分组查询(替代原来逐天 2 次查库)
        Map<LocalDate, Integer> dailyNewUser = queryDailyNewUser(begin, end);

        List<Integer> newUserList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();
        int cumulative = baseTotal;
        for (LocalDate date : dateList) {
            int newUser = dailyNewUser.getOrDefault(date, 0);
            cumulative += newUser;      // 当日累计用户 = 之前累计 + 当日新增
            newUserList.add(newUser);
            totalUserList.add(cumulative);
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
        List<LocalDate> dateList = buildDateList(begin, end);

        // 【性能优化】一次按天分组查询同时拿到"订单数"与"有效订单数",替代逐天 2 次查库
        Map<LocalDate, Map<String, Object>> dailyOrder = queryDailyOrder(begin, end);

        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();
        for (LocalDate date : dateList) {
            Map<String, Object> row = dailyOrder.get(date);
            orderCountList.add(row == null ? 0 : toInt(row.get("cnt")));
            validOrderCountList.add(row == null ? 0 : toInt(row.get("validCnt")));
        }

        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).orElse(0);

        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).orElse(0);

        double orderCompletionRate = 0.0;
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
     * 获取销量排名前十的商品
     *
     * @param begin 开始时间
     * @param end   结束时间
     * @return SalesTop10ReportVO
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);
        List<String> names = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String nameList = StringUtils.join(names, ",");

        List<Integer> numbers = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String numberList = StringUtils.join(numbers, ",");

        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }

    /**
     * 一次查出 [begin, end] 区间内每天的订单聚合数据(订单数/有效订单数/营业额)
     *
     * @param begin 开始日期
     * @param end   结束日期
     * @return key = 日期,value = countGroupByDate 的一行
     */
    private Map<LocalDate, Map<String, Object>> queryDailyOrder(LocalDate begin, LocalDate end) {
        List<Map<String, Object>> rows = orderMapper.countGroupByDate(
                LocalDateTime.of(begin, LocalTime.MIN), LocalDateTime.of(end, LocalTime.MAX));
        return toDateRowMap(rows);
    }

    /**
     * 一次查出 [begin, end] 区间内每天的新增用户数
     *
     * @param begin 开始日期
     * @param end   结束日期
     * @return key = 日期,value = 当日新增用户数
     */
    private Map<LocalDate, Integer> queryDailyNewUser(LocalDate begin, LocalDate end) {
        List<Map<String, Object>> rows = userMapper.countGroupByDate(
                LocalDateTime.of(begin, LocalTime.MIN), LocalDateTime.of(end, LocalTime.MAX));

        Map<LocalDate, Integer> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            LocalDate date = toDate(row.get("d"));
            if (date != null) {
                result.put(date, toInt(row.get("cnt")));
            }
        }
        return result;
    }

    /**
     * 把"按天分组"的查询结果转成 date -> row 的映射
     */
    private Map<LocalDate, Map<String, Object>> toDateRowMap(List<Map<String, Object>> rows) {
        Map<LocalDate, Map<String, Object>> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            LocalDate date = toDate(row.get("d"));
            if (date != null) {
                result.put(date, row);
            }
        }
        return result;
    }

    /**
     * 把分组查询里的日期字段转成 LocalDate
     * <p>SQL 用 {@code date_format(...)} 返回字符串,这里同时兼容 java.sql.Date / LocalDate,
     * 避免不同驱动把 DATE 映射成不同类型而抛 ClassCastException。
     */
    private LocalDate toDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        }
        return LocalDate.parse(value.toString());
    }

    /**
     * 安全取整:MyBatis 的 resultType=Map 时,count 列可能是 Long / Integer
     */
    private int toInt(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }

    /**
     * 安全取小数:sum(decimal) 通常返回 BigDecimal
     */
    private double toDouble(Object value) {
        return value == null ? 0.0 : ((Number) value).doubleValue();
    }

    /**
     * 导出业务数据
     *
     * @param response 响应对象
     */
    @Override
    public void exportBusinessData(HttpServletResponse response) {
        //1. 查询数据库，获取营业数据---查询最近30天的运营数据(与模板的 30 行明细一一对应)
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);
        List<LocalDate> dateList = buildDateList(dateBegin, dateEnd);

        // 【性能优化】2 次分组查询拿到 30 天明细,替代原来"逐天调用 getBusinessData"(原来 120+ 次查询)
        Map<LocalDate, Map<String, Object>> dailyOrder = queryDailyOrder(dateBegin, dateEnd);
        Map<LocalDate, Integer> dailyNewUser = queryDailyNewUser(dateBegin, dateEnd);

        //2. 通过POI将数据写入到Excel文件中
        InputStream template = this.getClass().getClassLoader()
                .getResourceAsStream("template/运营数据报表模板.xlsx");
        if (template == null) {
            throw new OrderBusinessException("运营数据报表模板不存在,请检查 classpath:template/ 下的模板文件");
        }

        // 设置下载响应头,避免浏览器靠内容猜测类型/文件名
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + encodeFileName("运营数据报表.xlsx"));

        // try-with-resources 保证模板流与工作簿一定被关闭
        try (InputStream in = template;
             XSSFWorkbook excel = new XSSFWorkbook(in)) {

            //获取表格文件的Sheet页
            XSSFSheet sheet = excel.getSheet("Sheet1");

            //填充数据--时间
            sheet.getRow(1).getCell(1).setCellValue("时间：" + dateBegin + "至" + dateEnd);

            // 汇总值由逐日数据累加得到(与原来调用 getBusinessData 的结果一致)
            double totalTurnover = 0.0;
            int totalOrders = 0;
            int totalValidOrders = 0;
            int totalNewUsers = 0;

            //填充明细数据
            for (int i = 0; i < dateList.size(); i++) {
                LocalDate date = dateList.get(i);
                Map<String, Object> orderRow = dailyOrder.get(date);
                int orderCount = orderRow == null ? 0 : toInt(orderRow.get("cnt"));
                int validOrderCount = orderRow == null ? 0 : toInt(orderRow.get("validCnt"));
                double turnover = orderRow == null ? 0.0 : toDouble(orderRow.get("turnover"));
                int newUsers = dailyNewUser.getOrDefault(date, 0);

                double completionRate = orderCount == 0 ? 0.0 : (double) validOrderCount / orderCount;
                double unitPrice = validOrderCount == 0 ? 0.0 : turnover / validOrderCount;

                //获得某一行
                XSSFRow row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(turnover);
                row.getCell(3).setCellValue(validOrderCount);
                row.getCell(4).setCellValue(completionRate);
                row.getCell(5).setCellValue(unitPrice);
                row.getCell(6).setCellValue(newUsers);

                totalTurnover += turnover;
                totalOrders += orderCount;
                totalValidOrders += validOrderCount;
                totalNewUsers += newUsers;
            }

            //获得第4行:营业额 / 订单完成率 / 新增用户
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(totalTurnover);
            row.getCell(4).setCellValue(totalOrders == 0 ? 0.0 : (double) totalValidOrders / totalOrders);
            row.getCell(6).setCellValue(totalNewUsers);

            //获得第5行:有效订单数 / 平均客单价
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(totalValidOrders);
            row.getCell(4).setCellValue(totalValidOrders == 0 ? 0.0 : totalTurnover / totalValidOrders);

            //3. 通过输出流将Excel文件下载到客户端浏览器
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);
            out.flush();
        } catch (IOException e) {
            // 不再只打日志:那样用户会拿到一个空响应却以为导出成功
            log.error("导出业务数据失败", e);
            throw new OrderBusinessException("导出业务数据失败,请稍后重试");
        }
    }

    /**
     * 对中文文件名做 URL 编码(Content-Disposition 头只允许 ASCII)
     */
    private String encodeFileName(String fileName) {
        try {
            return URLEncoder.encode(fileName, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return "business-data.xlsx";
        }
    }

    /**
     * 生成 [begin, end] 闭区间的日期列表(含首尾)
     *
     * @param begin 开始日期
     * @param end   结束日期
     * @return 日期列表
     */
    private List<LocalDate> buildDateList(LocalDate begin, LocalDate end) {
        if (begin.isAfter(end)) {
            throw new OrderBusinessException(MessageConstant.START_DATE_LATER_THAN_END_DATE);
        }
        List<LocalDate> dateList = new ArrayList<>();
        LocalDate cur = begin;
        while (!cur.isAfter(end)) {
            dateList.add(cur);
            cur = cur.plusDays(1);
        }
        return dateList;
    }
}
