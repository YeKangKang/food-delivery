package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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

    /**
     * 批量删除符合 setmeal_ids 的数据
     * @param ids
     */
    void deleteBySetmealIds(@Param("ids") List<Long> ids);

    /**
     * 删除符合 setmeal_id 的数据
     * @param id
     */
    @Delete("delete from setmeal_dish where setmeal_id = #{id}")
    void deleteBySetmealId(@Param("id") Long id);

    /**
     * 根据套餐关系id查询关系表数据
     * @param id
     * @return
     */
    @Select("select * from setmeal_dish where setmeal_id = #{id}")
    List<SetmealDish> getSetmealIdsById(@Param("id") Long id);

    /**
     * 根据 setmeal_id 获得所有有关的 dish_id
     * @param setmealId
     * @return
     */
    @Select("select dish_id from setmeal_dish where setmeal_id = #{setmealId}")
    List<Long> getDishIdBySetmealId(@Param("setmealId") Long setmealId);
}
