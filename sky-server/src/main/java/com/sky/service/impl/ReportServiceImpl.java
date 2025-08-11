package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据统计相关服务
 */
@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

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

    /**
     * 统计时间范围内的用户数量（总量，新增）
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        // 构建日期列表字符串
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        String dateListString = StringUtils.join(dateList, ",");    // 用“，”分隔日期列表

        // 构建总用户数量、新用户数量列表字符串
        List<Integer> totalUserList = new ArrayList<>();    // 存放总用户数量表
        List<Integer> newUserList = new ArrayList<>();      // 存放新用户数量表

        // 查询每一天的总用户、新用户数量，并加入集合中
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);   // 起始
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);       // 结束

            // 创建map用以查询（Map是可变对象，每一次循环都需要new）
            Map map = new HashMap();
            map.put("endTime", endTime);

            Integer dallyTotalUser = userMapper.countByMap(map);    // 日总用户数量

            map.put("beginTime", beginTime);
            Integer dallyNewUser = userMapper.countByMap(map);  // 日新增用户数量

            // 添加进集合中
            totalUserList.add(dallyTotalUser);
            newUserList.add(dallyNewUser);
        }

        return UserReportVO.builder()
                .dateList(dateListString)
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .build();
    }

    @Override
    public OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end) {
        // 构建日期列表字符串
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        String dateListString = StringUtils.join(dateList, ",");    // 用“，”分隔日期列表

        // 查询有效订单数、订单总数
        List<Integer> orderCountList = new ArrayList<>();       // 订单数集合
        List<Integer> validOrderCountList = new ArrayList<>();  // 有效订单数集合
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);   // 起始
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);       // 结束

            // 查询每天的有效订单数
            Integer orderCount = getOrderCount(beginTime, endTime, null);

            // 查询每天的订单总数
            Integer validOrderCount = getOrderCount(beginTime, endTime, Orders.COMPLETED);

            // 添加到list
            orderCountList.add(orderCount);
            validOrderCountList.add(validOrderCount);
        }

        // 计算订单总数
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();

        // 计算有效订单总数
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();

        // 计算订单完成率
        Double orderCompletionRate = 0.0;
        if (totalOrderCount != 0) {
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        }

        return OrderReportVO.builder()
                .dateList(dateListString)
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 根据条件统计订单数量
     * @return
     */
    private Integer getOrderCount(LocalDateTime begin, LocalDateTime end, Integer status) {
        Map map = new HashMap();
        map.put("beginTime", begin);
        map.put("endTime", end);
        map.put("status", status);

        return orderMapper.countByMap(map);
    }

    /**
     * 销量排名Top10
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        // 多表查询前十的商品名称和销量（只查询有效订单，同时满足时间范围的）
        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);

        // 将集合里面的 name 拿到并组成一个新的集合对象
        List<String> nameList = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());   // 拿到 GoodsSalesDTO::getName，然后封装成新的集合对象
        List<Integer> numberList = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());  // 拿到 GoodsSalesDTO::getNumber，然后封装为集合对象

        // 转换为 String，用逗号分隔
        String nameListString = StringUtils.join(nameList, ",");
        String numberListString = StringUtils.join(numberList, ",");

        return SalesTop10ReportVO.builder()
                .nameList(nameListString)
                .numberList(numberListString)
                .build();
    }
}
