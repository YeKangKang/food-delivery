package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.SetmealVO;

import java.util.List;

public interface SetmealService {
    /**
     * 添加套餐 + 套餐菜品
     * @param setmealDTO
     */
    void saveWithSetmealDish(SetmealDTO setmealDTO);

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    PageResult page(SetmealPageQueryDTO setmealPageQueryDTO);

    /**
     * 批量删除套餐
     * @param ids
     */
    void deleteBatch(List<Long> ids);

    /**
     * 根据 id 查询套餐和套餐菜品信息
     * @param id
     * @return
     */
    SetmealVO getSetMealAndSetmealDishById(Long id);

    /**
     * 编辑菜单信息
     * @param setmealDTO
     */
    void update(SetmealDTO setmealDTO);

    void startOrStop(Integer status, Long id);
}
