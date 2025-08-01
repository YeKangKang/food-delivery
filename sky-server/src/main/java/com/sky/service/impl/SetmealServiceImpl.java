package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishMapper;

    /**
     * 新增套餐 + 套餐包括的菜品
     * @param setmealDTO
     */
    @Override
    @Transactional // 开启事务
    public void saveWithSetmealDish(SetmealDTO setmealDTO) {

        // 创建一个 Setmeal 实体类，用来向 Mapper 传递数据
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);  // 拷贝数据
        setmealMapper.save(setmeal);    // 开启主键回写，获取自增id

        // 获取回写的自增 id
        Long setmealId = setmeal.getId();

        // 获取前端传来的套餐菜品组，使用动态数组向 setmeal_dish 表中存入
        List<SetmealDish> setmealDishList = setmealDTO.getSetmealDishes();  // 获取前端传来的套餐菜品组
        if ( setmealDishList != null && !setmealDishList.isEmpty()) {
            // 给List中的每一个 SetmealDish 对象的 setmeal_id 写入对应的 Setmeal 主键
            setmealDishList.forEach(setmealDish -> {setmealDish.setSetmealId(setmealId);});
            setmealDishMapper.insertBatch(setmealDishList); // 如果数组里有东西，且完成了主键的填写，持久化
        }
    }

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    @Override
    public PageResult page(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 批量删除套餐
     * @param ids
     */
    @Override
    @Transactional  // 开启事务
    public void deleteBatch(List<Long> ids) {
        // 判断套餐是否起售，起售无法删除
        for (Long id : ids) {
            Setmeal s = setmealMapper.getSetmealById(id);
            if (StatusConstant.ENABLE == s.getStatus()) {
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }

        // 如果菜单没有起售，可以删除：删除 setmeal 表、setmeal_dish 表
        setmealMapper.deleteByIds(ids); // 根据 ids 批量删除 setmeal 表中的数据
        setmealDishMapper.deleteBySetmealIds(ids); // 根据 ids 批量删除 setmeal_dish 表中 setmeal_id=id的数据
    }

    /**
     * 根据 id 查询套餐和套餐菜品信息
     * @param id
     * @return
     */
    @Override
    public SetmealVO getSetMealAndSetmealDishById(Long id) {
        // 通过 id 获取 setmeal 和 setmealDish
        Setmeal setmeal = setmealMapper.getSetmealById(id);
        List<SetmealDish> setmealDish = setmealDishMapper.getSetmealIdsById(id);

        // 组装成 SetmealVO 返回
        SetmealVO s = new SetmealVO();
        BeanUtils.copyProperties(setmeal, s);
        s.setSetmealDishes(setmealDish);
        return s;
    }

    /**
     * 编辑菜单信息
     * @param setmealDTO
     */
    @Override
    @Transactional  // 开启事务
    public void update(SetmealDTO setmealDTO) {
        // 修改 setmeal 表中的信息
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.update(setmeal);

        // 获取本次 setmeal 的 id
        Long setmealId = setmeal.getId();

        // 修改 setmeal_dish 表中的信息(先全部删除，再重新添加)
        // 1. 删除原先所有相关的 setmeal_dish 数据
        setmealDishMapper.deleteBySetmealId(setmealId);

        // 2. 重新添加信息到 setmeal_dish 表
        List<SetmealDish> setmealDishList = setmealDTO.getSetmealDishes();  // 获取新的数据
        if (setmealDishList != null && !setmealDishList.isEmpty()) {
            setmealDishList.forEach(setmealDish -> {
                setmealDish.setSetmealId(setmealId);
            });
            setmealDishMapper.insertBatch(setmealDishList);
        }
    }

    /**
     * 起售、停售套餐
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        // 1. 如果要起售套餐，先检查套餐中是否有停售的套餐
        if (status == StatusConstant.ENABLE) {
            // 我要找到所有和这个套餐有关的菜品：通过 关系表 可以知道所有和这个 id 有关的 dish_id
            List<Long> dishIds = setmealDishMapper.getDishIdBySetmealId(id);    // 这个list不可能为空，因为套餐一定有菜品
            // 然后我需要找到 dish id 之后通过 select * from dish where id in (?????) 找到所有的 dish 来判断
            List<Dish> dishList = dishMapper.getByIds(dishIds);

            // 只要有一个的 status 是停售，就直接抛出异常
            dishList.forEach(dish -> {
                if (dish.getStatus() == StatusConstant.DISABLE) {
                    throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                }
            });
        }

        // 2. 如果套餐里的菜品都是起售状态，则启售套餐
        Setmeal setmeal = Setmeal.builder()
                .id(id)
                .status(status)
                .build();
        setmealMapper.update(setmeal);
    }


}
