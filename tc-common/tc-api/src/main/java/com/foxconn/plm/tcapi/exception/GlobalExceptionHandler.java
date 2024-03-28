package com.foxconn.plm.tcapi.exception;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSONObject;
import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.exception.BizException;
import com.foxconn.plm.entity.response.R;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.ConstraintViolationException;
import java.util.Objects;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static Log logger = LogFactory.get();

    /**
     * 处理自定义的业务异常
     */
    @ExceptionHandler(value = BizException.class)
    @ResponseBody
    public JSONObject bizExceptionHandler(BizException e){
        logger.error("发生业务异常！原因是：{}",e.getMsg());
        JSONObject result = new JSONObject();
        result.put("code",e.getCode());
        result.put("msg",e.getMsg());
        return result;
    }

    /**
     * 处理自定义的业务异常
     */
    @ExceptionHandler(value = {MethodArgumentNotValidException.class,ConstraintViolationException.class})
    @ResponseBody
    public JSONObject ValidExceptionHandler(Exception e){
        String msg;
        if(e instanceof MethodArgumentNotValidException){
             msg = Objects.requireNonNull(((MethodArgumentNotValidException)e).getBindingResult().getFieldError()).getDefaultMessage();
        }else {
             msg = Objects.requireNonNull(((ConstraintViolationException) e).getConstraintViolations().stream().findFirst().get().getMessage());
        }
        logger.error("请求参数解析失败！原因是：{}",msg);
        JSONObject result = new JSONObject();
        result.put("code", HttpResultEnum.PARAM_ERROR.getCode());
        result.put("msg",msg);
        return result;
    }


    /**
     * 处理其他异常
     */
    @ExceptionHandler(value =Exception.class)
    @ResponseBody
    public R exceptionHandler(Exception e){
        logger.error("未知异常！原因是:",e);

        return R.error(HttpResultEnum.PARAM_ERROR.getCode(),e.toString());
    }
}