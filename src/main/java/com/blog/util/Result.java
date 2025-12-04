package com.blog.util;

import lombok.Data;

//接口统一返回结果
@Data
public class Result<T> {
    private int code; // 200=成功，500=失败
    private String msg; // 提示信息
    private T data; // 返回数据

    // 成功响应
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg("操作成功");
        result.setData(data);
        return result;
    }

    // 失败响应
    public static <T> Result<T> fail(String msg) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMsg(msg);
        result.setData(null);
        return result;
    }
}