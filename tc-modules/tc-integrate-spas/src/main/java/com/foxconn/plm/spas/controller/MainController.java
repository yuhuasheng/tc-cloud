package com.foxconn.plm.spas.controller;

import com.foxconn.plm.entity.response.HealthStatusRv;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.spas.scheduled.SynSpasWorkItemTask;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

@RestController
public class MainController {

    @Value("${spring.application.name}")
    private String serverName;

    @Resource
    SynSpasWorkItemTask synSpasWorkItemTask;

    @ApiOperation("检查状态")
    @GetMapping(value = "/status")
    public R<HealthStatusRv> status() {
        HealthStatusRv statusRv = new HealthStatusRv(serverName);
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        statusRv.setJvmMaxMemory(runtime.maxMemory() / 1024 / 1024 + "M");
        statusRv.setJvmTotalMemory(runtime.totalMemory() / 1024 / 1024 + "M");
        statusRv.setJvmUsedMemory((totalMemory - freeMemory) / 1024 / 1024 + "M");
        statusRv.setJvmThreadNum(threadMXBean.getThreadCount() + "");
        return R.success(statusRv);
    }

    @ApiOperation("同步Spas工时")
    @GetMapping(value = "/synSpasWorkItem")
    public void synSpasWorkItem() {
        synSpasWorkItemTask.sync();
    }

}
