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

import java.time.LocalDateTime;

/**
 * 自定义切面类，用于实现自动填充功能
 */
@Aspect
@Component
@Slf4j
public class AutoFillAspect {

    /**
     * 定义一个切入点，匹配所有Mapper接口中的方法
     */
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointCut() {
    }

    /**
     * 前置通知 在切入点方法执行之前，进行自动填充
     */
    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint) {
        log.info("开始执行公共字段自动填充");

        // 获取当前被拦截的方法上的数据库操作类型
        MethodSignature signature = (MethodSignature) joinPoint.getSignature(); // 获取方法签名
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class); // 获取方法上的AutoFill注解
        OperationType operationType = autoFill.value(); // 获取注解中的数据库操作类型

        // 获取当前被拦截方法的参数 -- 实体对象
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return;
        }
        Object entity = args[0];

        // 准备赋值的对象
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();

        // 根据不同操作类型通过反射来赋值
        if (operationType == OperationType.INSERT) {
            // 赋值
            try {
                // 调用setCreateTime方法并传入当前时间
                entity.getClass()
                        .getMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class)
                        .invoke(entity, now);
                // 调用setUpdateTime方法并传入当前时间
                entity.getClass()
                        .getMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class)
                        .invoke(entity, now);
                // 调用setCreateUser方法并传入当前用户ID
                entity.getClass()
                        .getMethod(AutoFillConstant.SET_CREATE_USER, Long.class)
                        .invoke(entity, currentId);
                // 调用setUpdateUser方法并传入当前用户ID
                entity.getClass()
                        .getMethod(AutoFillConstant.SET_UPDATE_USER, Long.class)
                        .invoke(entity, currentId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (operationType == OperationType.UPDATE) {
            try {
                // 调用setUpdateTime方法并传入当前时间
                entity.getClass()
                        .getMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class)
                        .invoke(entity, now);
                // 调用setUpdateUser方法并传入当前用户ID
                entity.getClass()
                        .getMethod(AutoFillConstant.SET_UPDATE_USER, Long.class)
                        .invoke(entity, currentId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
