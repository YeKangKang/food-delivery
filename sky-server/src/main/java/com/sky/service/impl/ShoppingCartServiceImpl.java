package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 添加购物车
     * @param shoppingCartDTO
     */
    @Override
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        // 判断当前商品是否已经存在在购物车中
        ShoppingCart shoppingCart = ShoppingCart.builder()  // 构建一个 ShoppingCart 实体类用于查询，将请求用户id和dto的数据拷贝
                        .userId(BaseContext.getCurrentId())
                        .dishId(shoppingCartDTO.getDishId())
                        .dishFlavor(shoppingCartDTO.getDishFlavor())
                        .setmealId(shoppingCartDTO.getSetmealId())
                        .build();
        // 为什么返回一个list????       当前商品一定只会返回一个对象，我猜测是后面要复用这个list
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);    // 查询符合过滤要求的购物车数据

        // 如果这个商品存在，则数量加一
        if (list != null && !list.isEmpty()) {
            ShoppingCart cart = list.get(0);    // 查询当前商品一定只会返回一个，所以是第0位
            cart.setNumber(cart.getNumber() + 1);
            shoppingCartMapper.updataNumberById(cart);
        } else {
        // 如果商品不存在，将商品数据插入到购物车中

            // 判断本次添加到购物车的是菜品还是套餐，构建对应的购物车实体对象
            Long dishId = shoppingCart.getDishId();
            Long setmealId = shoppingCart.getSetmealId();
            if (dishId != null) {   // 是菜品
                Dish dish = dishMapper.getById(dishId); // 获得这个 dish 的价格，图片，名称，初试数量，创建时间，存到 shoppingCart 对象
                shoppingCart.setAmount(dish.getPrice()); // 价格
                shoppingCart.setName(dish.getName()); // 名称
                shoppingCart.setImage(dish.getImage()); // 图片
            } else {    // 是套餐
                Setmeal setmeal = setmealMapper.getSetmealById(setmealId);   // 同上，如果不是dish一定是setmeal
                shoppingCart.setAmount(setmeal.getPrice());
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
            }
            // 统一设置数量和时间
            shoppingCart.setNumber(1); // 数量
            shoppingCart.setCreateTime(LocalDateTime.now()); // 时间
            // 插入新的购物车数据
            shoppingCartMapper.insert(shoppingCart);
        }
    }
}
