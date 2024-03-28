package com.foxconn.dp.plm.health;

import com.foxconn.plm.entity.response.HealthStatusRv;
import com.foxconn.plm.entity.response.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.*;


@Api(tags = "其他")
@RestController
@Validated
public class CheckHealthController {

    @Resource
    CheckHealthService service;


    @ApiOperation("查看服务状态")
    @GetMapping(value = "/queryServiceStatus")
    public R<List<HealthStatusRv>> queryServiceStatus() {
        return R.success(service.queryServiceStatus());
    }

    @ApiOperation("测试邮件服务")
    @GetMapping(value = "/testEmail")
    public R<String> testEmail()   {
        return R.success(service.testEmail());
    }



}
