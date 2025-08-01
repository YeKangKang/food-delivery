package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.enumeration.OperationType;
import com.sky.vo.SetmealVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealMapper {

    /**
     * 根据分类id查询套餐的数量
     * @param id
     * @return
     */
    @Select("select count(id) from setmeal where category_id = #{categoryId}")
    Integer countByCategoryId(Long id);

    /**
     * 新增套餐
     * @param setmeal
     */
    @AutoFill(OperationType.INSERT) // aop 标记
    void save(Setmeal setmeal);

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    Page<SetmealVO> pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    /**
     * 根据 id 查询套餐
     * @param id
     * @return
     */
    @Select("select * from setmeal where id = #{id}")
    Setmeal getSetmealById(@Param("id") Long id);

    /**
     * 根据id批量删除
     * @param ids
     */
    void deleteByIds(@Param("ids") List<Long> ids);

    /**
     * 编辑套餐信息（根据id）
     * @param setmeal
     */
    @AutoFill(OperationType.UPDATE) // aop 添加更新时间
    void update(Setmeal setmeal);

    /**
     * 根据ids获得所有Setmeal
     * @param setmealIdList
     * @return
     */
    List<Setmeal> getSetMealByIds(@Param("setmealIdList") List<Long> setmealIdList);
}
