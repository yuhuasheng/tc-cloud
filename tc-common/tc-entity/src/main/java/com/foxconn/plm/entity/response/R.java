package com.foxconn.plm.entity.response;

import com.foxconn.plm.entity.constants.HttpResultEnum;
import lombok.Data;

import java.util.ArrayList;

@Data
public class R<T> {

    String code;
    String msg;
    T data;

    public static <T> R<T> success() {
        return success(null);
    }

    public static <T> R<T> success(String msg) {
        R<T> r = new R<>();
        r.setCode(HttpResultEnum.SUCCESS.getCode());
        r.setMsg(msg);
        r.setData(null);
        return r;
    }

    public static <T> R<T> success(String msg, T data) {
        R<T> r = new R<>();
        r.setCode(HttpResultEnum.SUCCESS.getCode());
        r.setMsg(msg);
        r.setData(data);
        return r;
    }

    public static <T> R<T> success(T data) {
        R<T> r = new R<>();
        r.setCode(HttpResultEnum.SUCCESS.getCode());
        r.setMsg(HttpResultEnum.SUCCESS.getMsg());
        r.setData(data);
        return r;
    }


    public static <T> R<T> error(String code, String msg) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMsg(msg);

        return r;
    }

    public static <T> R<T> error(String code,  String msg,T data) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMsg(msg);
        r.setData(data);
        return r;
    }
}
