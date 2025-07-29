package com.sky.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(description = "添加员工时传递的数据对象")
public class EmployeeDTO implements Serializable {

    @ApiModelProperty("自增id，无需传输")
    private Long id;

    @ApiModelProperty("用户名")
    private String username;

    @ApiModelProperty("员工名")
    private String name;

    @ApiModelProperty("手机号")
    private String phone;

    @ApiModelProperty("性别（1/0）")
    private String sex;

    @ApiModelProperty("身份证号（18位）")
    private String idNumber;

}
