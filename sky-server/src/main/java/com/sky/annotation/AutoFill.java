package com.sky.annotation;

import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解：标记方法是否需要进行公共字段填充
 */
@Target(ElementType.METHOD)             // 注解只能用于方法上
@Retention(RetentionPolicy.RUNTIME)    // 注解在运行时保留，可通过反射读取
public @interface AutoFill {
    OperationType value();             // 申明操作类型（插入/更新），用于 AOP 判断
}
