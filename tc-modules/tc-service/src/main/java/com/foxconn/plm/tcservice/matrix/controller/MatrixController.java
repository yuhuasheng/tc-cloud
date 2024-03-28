package com.foxconn.plm.tcservice.matrix.controller;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.tcservice.matrix.service.MatrixService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

@Controller
@RequestMapping(value = "/teamcenter")
public class MatrixController {

    private static Log log = LogFactory.get();

    @Resource
    private MatrixService matrixService;

    @GetMapping("/sendMatrixEmail")
    @ResponseBody
    private String sendMatrixEmail(@RequestParam String taskName, @RequestParam String subject, @RequestParam String currentName, @RequestParam(required = false) String changeDesc, @RequestParam String firstTargetUid, @RequestParam String attachments) {
        log.info("==>> taskName:" + taskName);
        log.info("==>> subject:" + subject);
        log.info("==>> currentName:" + currentName);
        log.info("==>> changeDesc:" + changeDesc);
        log.info("==>> firstTargetUid:" + firstTargetUid);
        log.info("==>> attachments:" + attachments);
        return matrixService.sendMatrixEmail(taskName, subject, currentName, changeDesc, firstTargetUid, attachments);
    }
}
