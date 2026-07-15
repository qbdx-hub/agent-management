package com.agentmanagement.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结构：{ code, message, data }。
 * 约定 code = 0 表示成功，前端 (api/index.ts) 靠 code !== 0 判定失败。
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务状态码：0 = 成功，其余为各类业务错误码（见 ResultCode） */
    private Integer code;

    private String message;

    private T data;

    public Result() {
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success() {
        return new Result<T>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<T>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return new Result<T>(code, message, null);
    }

    public static <T> Result<T> error(ResultCode resultCode) {
        return new Result<T>(resultCode.getCode(), resultCode.getMessage(), null);
    }
}
