package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 自定义切面类：实现公共字段织入逻辑
 */
@Aspect
@Component
@Slf4j // 记录日志
public class AutoFillAspect {

    /**
     * 切入点：mapper 包内所有使用 AutoFill 注解的方法
     */
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointCut() {}

    /**
     * 前置通知：在执行数据库操作前，通过反射为实体对象注入公共字段
     * @param joinPoint 当前连接点，包含方法签名、参数等信息
     */
    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint) {
        log.info("前置增强：织入公共字段自动填充逻辑...");

        // 1. 获取注解元信息（操作类型：INSERT / UPDATE）
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();         // 获取方法签名对象（含方法名、参数、注解等）
        AutoFill annotation = signature.getMethod().getAnnotation(AutoFill.class);      // 获取该方法上的 @AutoFill 注解实例
        OperationType operationType = annotation.value();                               // 获取注解中指定的数据库操作类型（INSERT 或 UPDATE）

        // 2. 获取方法参数实体对象（默认第一个参数为数据实体）
        Object[] joinPointArgs = joinPoint.getArgs();                       // 获取拦截的方法所有的参数
        if (joinPointArgs == null || joinPointArgs.length == 0) return;     // 一般进不去这个判断
        Object entity = joinPointArgs[0];                                   // 我们约定 Mapper 接口的第一个参数是需要考反射注入的

        // 3. 构造注入数据
        LocalDateTime currentTime = LocalDateTime.now();    // 当前时间
        Long id = BaseContext.getCurrentId();               // 当前用户ID（通过 ThreadLocal 获取）

        // 4. 根据注解元数据（不同的操作），通过反射对实体类公共字段属性赋值
        if (operationType == OperationType.INSERT) {
            try {
                // INSERT 操作
                // 通过反射获取实体对象 4 个方法
                Method setCreateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);  // 使用常量类（非硬编码）
                Method setCreateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                // 通过反射使用获取的方法给实体对象赋值
                setCreateTime.invoke(entity, currentTime);
                setCreateUser.invoke(entity, id);
                setUpdateTime.invoke(entity, currentTime);
                setUpdateUser.invoke(entity, id);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (operationType == OperationType.UPDATE) {
            try {
                // UPDATE 操作
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                setUpdateTime.invoke(entity, currentTime);
                setUpdateUser.invoke(entity, id);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
