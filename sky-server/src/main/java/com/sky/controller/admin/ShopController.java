package com.sky.controller.admin;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController(value = "adminShopController")
@RequestMapping(value = "/admin/shop")
@Api(tags = "店铺相关接口")
@Slf4j
public class ShopController {

    public static final String KEY = "SHOP_STATUS";

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 管理端设置店铺的营业状态
     * @param status
     * @return
     */
    @PutMapping("/{status}")
    @ApiOperation(value = "设置营业状态")
    public Result setStatus(@PathVariable("status") Integer status) {
        log.info("设置营业状态为：{}", status == 1 ? "营业中" : "打样中");
        redisTemplate.opsForValue().set(KEY, status);
        return Result.success();
    }

    /**
     * 管理端查询店铺的营业状态
     * @return
     */
    @GetMapping("/status")
    @ApiOperation(value = "管理端查询营业状态")
    public Result<Integer> getStatus() {
        Integer status = (Integer) redisTemplate.opsForValue().get(KEY);
        log.info("查询营业状态为：{}", status == 1 ? "营业中" : "打样中");
        return Result.success(status);
    }
}
