package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper {

    /**
     * 插入订单数据
     * @param orders
     */
    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(@Param("orderNumber") String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 分页条件查询并按下单时间排序
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    // TODO 注释后提交
    /**
     * 这是一个替换方法: 历史订单分页查询
     * @param userId
     * @return
     */
    @Select("select id from orders where user_id = #{userId}")
    List<Long> getIdListByUserId(@Param("userId") Long userId);

    /**
     * 查询订单详情
     * @param id
     * @return
     */
    @Select("select * from orders where id = #{id}")
    Orders getById(@Param("id") Long id);

    /**
     * 查询 待接单，待派送，派送中的订单数量
     * @param status
     * @return
     */
    @Select("select count(*) from orders where status = #{status}")
    Integer countStatus(@Param("status") Integer status);

    /**
     * 根据订单状态和下单时间，查询所有超时订单
     * @param status
     * @param nowTime
     * @return
     */
    @Select("select * from orders where status = #{status} and order_time < #{nowTime}")
    List<Orders> getByStatusAndOrderTimeLT(@Param("status") Integer status, @Param("nowTime") LocalDateTime nowTime);
}
