package com.foxconn.plm.tcserviceawc.exception;

/**
 * @author DY
 * @CLassName: CommonException
 * @Description:
 * @create 2022/9/1
 */
public class CommonException extends Exception {
    private int status;
    private String errorMsg;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public CommonException() {
    }

    public CommonException(int status, String errorMsg) {
        this.status = status;
        this.errorMsg = errorMsg;
    }

    public static CommonException exception_201(String errorMsg){
        return new CommonException(201,errorMsg);
    }

    public static CommonException exception_400(String errorMsg){
        return new CommonException(400,errorMsg);
    }

    public static  CommonException exception(int status,String errorMsg){
        return new CommonException(status,errorMsg);
    }
}
