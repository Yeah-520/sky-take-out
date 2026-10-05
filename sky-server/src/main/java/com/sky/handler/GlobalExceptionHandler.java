package com.sky.handler;

import com.sky.constant.MessageConstant;
import com.sky.exception.BaseException;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常
     * @param ex 异常对象
     * @return 统一结果
     */
    @ExceptionHandler
    public Result<String> exceptionHandler(BaseException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    @ExceptionHandler
    public Result<String> exceptionHandler(SQLIntegrityConstraintViolationException sqlEx){
        // Duplicate entry 'admin' for key 'employee.idx_username'
        String message = sqlEx.getMessage();
        if(message.contains("Duplicate entry")){
            String[] split = message.split(" ");
            String username = split[2];
            return Result.error(username + "已存在");
        }

        log.error("异常信息：{}", sqlEx.getMessage());
        return Result.error("未知异常");
    }

    /**
     * 兜底:处理未被上面两个 handler 捕获的其它异常(NPE、IllegalArgumentException 等)
     * <p>避免直接把 Spring Boot 默认的 500 错误页暴露给前端
     */
    @ExceptionHandler
    public Result<String> exceptionHandler(Exception ex){
        log.error("系统异常：", ex);
        return Result.error(MessageConstant.UNKNOWN_ERROR);
    }
}
