package com.foxconn.plm.gateway;

import com.foxconn.plm.entity.constants.HttpResultEnum;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.DefaultErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

/**
 * @calssName JsonExceptionHandler
 * @Description 网关全局异常处理类
 * @Author jiangshaoneng
 * @DATE 2020/9/28 19:35
 */
public class ExceptionHandler extends DefaultErrorWebExceptionHandler {

    public ExceptionHandler(ErrorAttributes errorAttributes, ResourceProperties resourceProperties,
                            ErrorProperties errorProperties, ApplicationContext applicationContext) {
        super(errorAttributes, resourceProperties, errorProperties, applicationContext);
    }

    /**
     * 获取异常属性
     */
    @Override
    protected Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
        String code = HttpResultEnum.NET_ERROR.getCode();
        Throwable error = super.getError(request);
        if (error instanceof ResponseStatusException) {
            code = ""+((ResponseStatusException) error).getStatus().value();
        }
        return response(code, error.getMessage());
    }

    /**
     * 指定响应处理方法为JSON处理的方法
     * @param errorAttributes
     */
    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    /**
     * 根据code获取对应的HttpStatus
     * @param errorAttributes
     * @return
     */
    @Override
    protected int getHttpStatus(Map<String, Object> errorAttributes) {
        return 200;
    }


    /**
     * 构建返回的JSON数据格式
     * @param status		状态码
     * @param errorMessage  异常信息
     * @return
     */
    public static Map<String, Object> response(String status, String errorMessage) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", status);
        map.put("msg", errorMessage);
        return map;
    }
}
