package com.sky.controller.user;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController(value = "userShopController")
@RequestMapping(value = "/user/shop")
@Api(tags = "用户相关接口")
@Slf4j
public class ShopController {

    public static final String KEY = "SHOP_STATUS";

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 用户端查询店铺营业状态
     * @return
     */
    @GetMapping("/status")
    @ApiOperation(value = "用户端查询营业状态")
    public Result<Integer> getStatus() {
        Integer status = (Integer) redisTemplate.opsForValue().get(KEY);
        log.info("查询营业状态为：{}", status == 1 ? "营业中" : "打样中");
        return Result.success(status);
    }
}
