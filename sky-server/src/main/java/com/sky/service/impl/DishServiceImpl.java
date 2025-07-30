package com.sky.service.impl;

import com.sky.dto.DishDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 菜品管理业务层
 */
@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    /**
     * 新增菜品和对应的口味数据
     * @param dishDTO
     */
    @Override
    @Transactional  // 事务
    public void saveWithFlavor(DishDTO dishDTO) {

        // 向菜品表插入数据
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);    // 拷贝 DishDTO 到 Dish 实体类，公共字段由 AOP 织入
        dishMapper.insert(dish);    // 接口设置了主键回显，自增的主键值回写到 id 字段中

        // 读取回写的主键 id，作为后面的 dishId
        Long dishId = dish.getId();

        // 向口味表动态插入数据
        List<DishFlavor> dishFlavorList = dishDTO.getFlavors();
        // TODO: 前端可能传入仅包含空属性的 DishFlavor
        // TODO: 此时 dishFlavorList 非空，但实际内容无效，需手动过滤避免插入空口味
        if (dishFlavorList != null && !dishFlavorList.isEmpty()) {

            // 给每一个口味对象实体类设置对应的菜品id
            dishFlavorList.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
            });

            dishFlavorMapper.insertBatch(dishFlavorList);   // 动态插入 dish_flavor 表中
        }
    }
}
