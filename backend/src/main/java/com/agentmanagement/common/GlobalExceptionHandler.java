package com.agentmanagement.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：把各类异常统一转换为统一响应 Result。
 * 业务异常返回 HTTP 200 + 业务 code（前端靠 code 判定）；
 * 未登录/token 失效由 security/RestAuthenticationEntryPoint 单独处理为 HTTP 401。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /** @RequestBody + @Valid 校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String msg = fieldError != null ? fieldError.getDefaultMessage() : ResultCode.PARAM_ERROR.getMessage();
        log.warn("参数校验失败: {}", msg);
        return Result.error(ResultCode.PARAM_ERROR.getCode(), msg);
    }

    /** 表单绑定校验失败 */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBind(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String msg = fieldError != null ? fieldError.getDefaultMessage() : ResultCode.PARAM_ERROR.getMessage();
        return Result.error(ResultCode.PARAM_ERROR.getCode(), msg);
    }

    /**
     * 唯一约束冲突（如并发注册撞 user 表 uk_username / uk_email）。
     * service 层 selectCount 查重与 insert 之间非原子，DB 唯一约束是最终兜底；
     * 这里把约束名映射回语义化业务码，避免落入下方兜底 500。
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicateKey(DuplicateKeyException e) {
        String msg = e.getMostSpecificCause() != null ? String.valueOf(e.getMostSpecificCause().getMessage()) : "";
        if (msg.contains("uk_username")) {
            return Result.error(ResultCode.USERNAME_EXISTS);
        }
        if (msg.contains("uk_email")) {
            return Result.error(ResultCode.EMAIL_EXISTS);
        }
        if (msg.contains("uk_kb_name")) {
            return Result.error(ResultCode.KB_NAME_EXISTS);
        }
        log.warn("唯一约束冲突: {}", msg);
        return Result.error(ResultCode.PARAM_ERROR.getCode(), "数据已存在，请刷新后重试");
    }

    /** 兜底：未知异常 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error(500, "系统繁忙，请稍后重试");
    }
}
