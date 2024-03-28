package com.foxconn.plm.integrate.tcfr.controller;

import com.alibaba.fastjson.JSONObject;
import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.integrate.tcfr.domain.SpasToTCBean;
import com.foxconn.plm.integrate.tcfr.service.MeetService;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.*;
import java.util.List;

@RestController
@RequestMapping("/meet")
public class MeetController {

    @Resource
    MeetService meetService;

    @ApiOperation("发送会议邮件")
    @PostMapping("/sendMeetEmail")
    public R<String> sendMeetEmail(@RequestBody JSONObject param) {

        try {
            meetService.sendMeetEmail(param);
            return R.success();
        } catch (Exception e) {
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(), e.getMessage());
        }

    }

    @ApiOperation("发送任务完成邮件")
    @GetMapping("/sendMeetTaskEmail")
    public R<String> sendMeetTaskEmail(String scheduleId, String endDate, String taskId) {
        try {
            meetService.sendMeetCompleteTaskEmail(scheduleId, endDate, taskId);
            return R.success();
        } catch (Exception e) {
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(), e.getMessage());
        }
    }


    @ApiOperation("传递任务状态到TCFR")
    @GetMapping("/sendTaskStatusToTCFR")
    public R sendTaskStatusToTCFR(String taskId, String actionItemId, String state) {
        return meetService.sendTaskStatusToTCFR(taskId, actionItemId, state);
    }


    @ApiOperation("手工传递任务状态到TCFR")
    @GetMapping("/manualSendTaskStatus")
    public R manualSendTaskStatus(String startDate, String endDate) {
        return meetService.manualSendTaskStatus(startDate, endDate);
    }


    @ApiOperation("更新SPAS和TC账号")
    @PostMapping ("/updateSpasToTC")
    public R updateSpasToTC(@RequestBody List<SpasToTCBean> list, String operation) {
        return meetService.updateSpasToTC(list, operation);
    }


    @ApiOperation("下载模板")
    @GetMapping("/downloadTemplate")
    public String downloadTemplate() throws IOException {
        return meetService.downloadTemplate();
    }

    @ApiOperation("传递TC账号信息到TCFR")
    @GetMapping("/sendTCInfoToTCFR")
    public R sendTCInfoToTCFR() {
        return meetService.sendTCInfoToTCFR();
    }
}
