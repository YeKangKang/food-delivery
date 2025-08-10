package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import lombok.extern.slf4j.Slf4j;
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

/**
 * 数据统计相关服务
 */
@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 统计指定时间区间内的营业额
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {

        // 构建 DateList 日期列表字符串
        List<LocalDate> dateList = new ArrayList<>();   // 存放从 begin 到 end 的所有日期
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);  // 不可变对象，每次赋值/修改都会返回一个新的副本（不需要new）
            dateList.add(begin);
        }
        String dateListString = StringUtils.join(dateList, ",");    // 使用,分隔list并转换为字符串

        // 构建 turnoverList 营业额列表字符串
        List<Double> turnoverList = new ArrayList<>();  // 存放对应某一天的营业额
        for (LocalDate date : dateList) {
            // 营业额 = date这一天，状态为“已完成”的订单合计
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);    // 一天的开始：将时间对象转换为包含 时:分:秒
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            // select sum(amount) form orders where order_time > beginTime and order_time < endTime and status = 5
            Map map = new HashMap<>();
            map.put("beginTime", beginTime);
            map.put("endTime", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            turnover = (turnover == null) ? 0.0 : turnover;
            turnoverList.add(turnover);
        }
        String turnoverListString = StringUtils.join(turnoverList, ",");    // 转换为String，用逗号分隔

        return new TurnoverReportVO(dateListString, turnoverListString);
    }
}
