package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
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
}
