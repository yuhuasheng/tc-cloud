package com.foxconn.plm.entity.exception;

import com.foxconn.plm.entity.constants.HttpResultEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    protected String code;
    protected String msg;

    public BizException(String msg) {
        super(msg);
        this.code = HttpResultEnum.SERVER_ERROR.getCode();
        this.msg = msg;
    }

    public BizException(String code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

}

