package com.foxconn.plm.rma;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSONObject;
import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.response.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.lang.reflect.Method;


@Api(tags = "RMA 集成統一入口")
@RestController
public class RemoteMethodAccessorContoller {

    private static Log log = LogFactory.get();

    @Resource
    RemoteObjectServiceFactory remoteObjectServiceFactory;

    @ApiOperation("调用API ID 对应的接口获取数据")
    @PostMapping("/methodAccessor")
    public R invokeMethod(@RequestBody JSONObject paramJSONObject) {
        try {
            String object = paramJSONObject.getString("object");
            if (object == null || "".equalsIgnoreCase(object)) {
                return R.error(HttpResultEnum.PARAM_MISS.getCode(), "參數object不能为空");
            }

            RemoteBaseObjectService baseObjectService = remoteObjectServiceFactory.getObjectService(object);
            if (baseObjectService == null) {
                return R.error(HttpResultEnum.API_NOT_FOUND.getCode(), object + " 不存在");
            }
            String method = paramJSONObject.getString("method");
            Method[] mehods = baseObjectService.getClass().getMethods();
            Method mothod = null;
            for (Method m : mehods) {
                if (m.getName().equalsIgnoreCase(method)) {
                    mothod = m;
                    break;
                }
            }
            if (mothod == null) {
                return R.error(HttpResultEnum.API_NOT_FOUND.getCode(), "接口" + method + "方法不存在");
            }

            return (R) mothod.invoke(baseObjectService, paramJSONObject);

        } catch (Exception e) {
            e.printStackTrace();
             return R.error(HttpResultEnum.SERVER_ERROR.getCode(),e.getMessage());
        }

    }
}
