package com.sky.service;

import com.sky.dto.SetmealDTO;

public interface SetmealService {
    /**
     * 添加套餐 + 套餐菜品
     * @param setmealDTO
     */
    void saveWithSetmealDish(SetmealDTO setmealDTO);
}
