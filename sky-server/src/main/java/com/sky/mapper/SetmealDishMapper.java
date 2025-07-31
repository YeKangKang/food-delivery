package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    /**
     * 根据菜品id查询对应的套餐id
     * @param dishIds
     * @return
     */
    List<Long> getSetmealIdsByDishId(@Param("dishIds") List<Long> dishIds);   // 需要根据数组的个数动态创建SQL语句

    /**
     * 批量插入 菜单菜品 数据
     * @param setmealDishList
     */
    void insertBatch(@Param("setmealDishList") List<SetmealDish> setmealDishList);
}
