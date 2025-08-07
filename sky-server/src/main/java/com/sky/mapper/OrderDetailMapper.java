package com.sky.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderDetailMapper {

    /**
     * 批量插入订单明细数据
     * @param orderDetailList
     */
    void insertBatch(@Param("orderDetailList") List<OrderDetail> orderDetailList);

    /**
     * 根据订单 id 查询所有订单
     * @param id
     * @return
     */
    @Select("select * from order_detail where order_id = #{id}")
    List<OrderDetail> getByOrderId(@Param("id") Long id);

    // TODO 注释后提交
    /**
     * 这是一个替换方法: 历史订单分页查询
     * @param orderIdList
     * @return
     */
    List<OrderDetail> getByOrderIdList(@Param("orderIdList") List<Long> orderIdList);
}
