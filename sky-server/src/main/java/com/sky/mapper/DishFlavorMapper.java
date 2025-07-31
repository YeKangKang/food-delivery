package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishFlavorMapper {

    /**
     * 批量插入口味数据
     * @param dishFlavorList
     */
    void insertBatch(List<DishFlavor> dishFlavorList);

    /**
     * 删除所有给定 dishIds 的口味
     * @param dishIds
     */
    void deleteByDishIds(@Param("dishIds") List<Long> dishIds);

    /**
     * 删除所有给定 dishId（一个） 的口味
     * @param dishId
     */
    @Delete("delete from dish_flavor where dish_id = #{dishId}")
    void deleteByDishId(@Param("dishId") Long dishId);

    /**
     * 根据 dishId 查询对应的口味数据
     * @param dishId
     * @return
     */
    @Select("select * from dish_flavor where dish_id = #{dishId}")
    List<DishFlavor> getByDishId(@Param("dishId") Long dishId);
}
