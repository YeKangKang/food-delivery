package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
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
    @Autowired
    private SetmealDishMapper setmealDishMapper;

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

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        // 使用 PageHelper 自动在 SQL 添加分页字段
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 批量删除菜品
     * @param ids
     */
    @Override
    @Transactional  // 开启事务
    public void deleteBatch(List<Long> ids) {

        // 判断是否能够删除：这个/这组 菜品是否含有起售中菜品
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            // 启售中的菜品不能删除
            if (dish.getStatus() == StatusConstant.ENABLE) {
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        // 判断是否能够删除：这个/这组 菜品是否和套餐关联
        List<Long> setmealIdsByDishId = setmealDishMapper.getSetmealIdsByDishId(ids);
        // 如果查到且值不为空，则说明至少有一个关联的，不能删除，抛出异常
        if (setmealIdsByDishId != null && setmealIdsByDishId.size() > 0) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        // 删除 菜品表 中对应id的菜品数据
        dishMapper.deleteByIds(ids);

        // 删除 口味表 中对应id的口味数据（删除操作是幂等的，即使有些菜品没有选择口味也不会出错）
        dishFlavorMapper.deleteByDishIds(ids);
    }

    /**
     * 根据ID查询菜品 + 口味数据
     * @param id
     * @return
     */
    @Override
    public DishVO getByIdWithFlavor(Long id) {
        // 根据 id 查询菜品
        Dish dish = dishMapper.getById(id);

        // 根据 id 查询口味数据
        List<DishFlavor> dishFlavor = dishFlavorMapper.getByDishId(id);

        // 封装 dish 和 口味为 DishVO 返回
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavor);
        return dishVO;
    }

    /**
     * 根据id修改菜品的基本信息和口味信息
     * @param dishDTO
     */
    @Override
    @Transactional // 事务
    public void updateWithFlavor(DishDTO dishDTO) {
        // 修改菜品表基本信息
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.update(dish);

        // 修改口味数据（先删除所有口味信息，再添加新的信息覆盖）
        dishFlavorMapper.deleteByDishId(dishDTO.getId());
        // 获取新的口味数据再添加进口味表中
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {

            // 给每一个口味对象实体类设置对应的菜品id
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishDTO.getId());
            });

            dishFlavorMapper.insertBatch(flavors);   // 动态插入 dish_flavor 表中
        }
    }

    /**
     * 菜品启售、停售
     * @param status
     * @param id
     * @return
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        // 通过实体类和 MyBatis 交互
        Dish dish = Dish.builder()
                        .id(id)
                        .status(status)
                        .build();

        dishMapper.update(dish);
    }
}
