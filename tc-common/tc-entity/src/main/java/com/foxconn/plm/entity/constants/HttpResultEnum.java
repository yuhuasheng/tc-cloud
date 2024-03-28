package com.foxconn.plm.entity.constants;

public enum HttpResultEnum {

    SUCCESS("0000", "成功!"),
    NO_RESULT("2000", "未查詢到數據！"),
    NET_ERROR("4001", "網絡連接失敗！"),
    SERVER_ERROR("4002", "系统繁忙，请稍后再试！！"),
    PARAM_ERROR("4003", "輸入參數錯誤"),
    PARAM_MISS("4004", "輸入參數缺失"),
    AUTHOR_INVALID("4005", "身份驗證無效"),
    API_STOP("4009", "API 停用"),
    API_NOT_FOUND("4010", "API不存在");


    HttpResultEnum(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    /** 错误码 */
    private final String code;

    /** 错误描述 */
    private final String msg;


    public String getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
