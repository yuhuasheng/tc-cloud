package com.foxconn.plm.tcserviceawc.exception;

import cn.hutool.core.util.StrUtil;
import com.foxconn.plm.entity.response.R;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

/**
 * @author DY
 * @CLassName: GlobalException
 * @Description:
 * @create 2022/9/1
 */
@ControllerAdvice
public class GlobalException {

    @ExceptionHandler(CommonException.class)
    @ResponseBody
    public R exceptionHandle(CommonException exception){
        return R.error(String.valueOf(exception.getStatus()),exception.getErrorMsg());
    }

    @ExceptionHandler(BindException.class)
    @ResponseBody
    public R ValidationExceptionHandle(BindException exception){
        BindingResult bindingResult = exception.getBindingResult();
        StringBuilder sb = new StringBuilder();
        if(bindingResult.hasErrors()){
            for (ObjectError allError : bindingResult.getAllErrors()) {
                if(StrUtil.isNotBlank(sb)){
                    sb.append(";");
                }
                sb.append(allError.getDefaultMessage());
            }
        }
        String message = StrUtil.isBlank(sb) ? exception.getMessage() : sb.toString();
        return R.error("400",message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseBody
    public R ValidationExceptionHandle(ConstraintViolationException exception){
        StringBuilder sb = new StringBuilder();
        for (ConstraintViolation<?> constraintViolation : exception.getConstraintViolations()) {
            if(StrUtil.isNotBlank(sb)){
                sb.append(";");
            }
            sb.append(constraintViolation.getMessage());
        }
        String message = StrUtil.isBlank(sb) ? exception.getMessage() : sb.toString();
        return R.error("400",message);
    }
}
