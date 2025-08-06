package com.sky.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "服务器处理完订单后返回的数据格式")
public class OrderSubmitVO implements Serializable {

    //订单id
    @ApiModelProperty(value = "订单id")
    private Long id;
    //订单号
    @ApiModelProperty(value = "订单号")
    private String orderNumber;
    //订单金额
    @ApiModelProperty(value = "订单金额")
    private BigDecimal orderAmount;
    //下单时间
    @ApiModelProperty(value = "下单时间")
    private LocalDateTime orderTime;
}
