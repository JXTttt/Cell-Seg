package com.urine.cell_seg_sys.common;

import lombok.Data;

/**
 * 统一返回结果封装类
 * 前端收到这个 JSON: { "code": 200, "msg": "成功", "data": ... }
 */
@Data
public class Result<T> {
    private Integer code; // 200成功, 500失败
    private String msg;   // 提示信息
    private T data;       // 返回的数据

    public Integer getCode() { return code; }
    public void setCode(Integer code) { this.code = code; }

    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.msg = "操作成功";
        r.data = data;
        return r;
    }

    public static <T> Result<T> error(String msg) {
        Result<T> r = new Result<>();
        r.code = 500;
        r.msg = msg;
        return r;
    }
}