package com.foxconn.plm.mail.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.mail.service.TCMailService;
import com.foxconn.plm.rabbitmq.constant.RabbitMQEnum;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.collect.CollectUtil;
import com.foxconn.plm.utils.tc.DatasetUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.google.gson.Gson;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.soa.client.FileManagementUtility;
import com.teamcenter.soa.client.GetFileResponse;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.Dataset;
import com.teamcenter.soa.client.model.strong.ImanFile;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import com.teamcenter.soa.exceptions.NotLoadedException;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import javax.annotation.Resource;
import java.io.*;
import java.util.*;

/**
 * @Classname TCMailController
 * @Description
 * @Date 2022/1/4 19:44
 * @Created by HuashengYu
 */
@Controller
@RequestMapping(value = "/teamcenter")
public class TCMailController {

    private static Log log = LogFactory.get();

    @Resource
    private TCMailService tcMailService;

    @Resource
    private AmqpTemplate amqpTemplate;

    @RequestMapping(value = "/sendMail")
    @ResponseBody
    private String sendMailMethod(@RequestParam String data, @RequestParam("file") MultipartFile... files) {
        HashMap map = new HashMap();
        log.info("==>> data:" + data);
        Gson gson = new Gson();
        map = gson.fromJson(data, map.getClass());
        return tcMailService.sendTCMailService(map, files);

    }

    @RequestMapping(value = "/sendMail2")
    @ResponseBody
    private String sendMail2Method(@RequestParam String data) {
        log.info("sendMail2  data =====" + data);
        Map<String, String> dataMap = new HashMap<String, String>();
        Gson gson = new Gson();
        dataMap = gson.fromJson(data, dataMap.getClass());

        String host = dataMap.get("host");
        String from = dataMap.get("from");
        String to = dataMap.get("to");
        String title = dataMap.get("title");
        String content = dataMap.get("content");

        int firstIndex1 = title.indexOf("『") + 1;
        int lastIndex1 = title.lastIndexOf("『") + 1;
        int firstIndex2 = title.indexOf("』");
        int lastIndex2 = title.lastIndexOf("』");

        String personLiable = title.substring(firstIndex1, firstIndex2);
        String projectId = title.substring(lastIndex1, lastIndex2);

        return tcMailService.sendTCMail2Service(host, from, to, title, content, personLiable, projectId);
    }

    /**
     * 发送邮件(包含附件)
     *
     * @param data
     * @param files 附件列表, 前端file参数不传递，则默认为null
     * @return
     */
    @RequestMapping("/sendMail3")
    @ResponseBody
    private String sendMail3Method(@RequestParam String data, @RequestParam(value = "file", required = false) MultipartFile... files) throws Exception {
        log.info("==>> data:" + data);
        log.info("******** Begin Save Message to RabbitMQ ********");
        Map map = new HashMap<>();
        Gson gson = new Gson();
        map = gson.fromJson(data, map.getClass());
        map = tcMailService.generateAttachments(map, files);
        amqpTemplate.convertAndSend(RabbitMQEnum.TCMail_RabbitMQ.exchangeName(), RabbitMQEnum.TCMail_RabbitMQ.routingkey(), map);
        return "success";
    }


    @RequestMapping("/sendMail4")
    @ResponseBody
    private String sendMail4Method(@RequestParam String data, @RequestParam(value = "file", required = false) MultipartFile... files) throws Exception {
        Map map = new HashMap();
        log.info("==>> data:" + data);
        Gson gson = new Gson();
        map = gson.fromJson(data, map.getClass());
        map = tcMailService.generateAttachments(map, files);
        Boolean aBoolean = tcMailService.sendTCMail4Service(map);
        return aBoolean?"success":"failed";

    }


    /**
     * 发送邮件(包含附件)
     *
     * @param data
     * @param files 附件列表, 前端file参数不传递，则默认为null
     * @return
     */
    @RequestMapping("/sendMail5")
    @ResponseBody
    private String sendMail5Method(@RequestParam String data, @RequestParam(value = "file", required = false) List<MultipartFile> files) throws Exception {
        log.info("==>> data:" + data);
        log.info("******** Begin Save Message to RabbitMQ ********");
        Map map = new HashMap<>();
        Gson gson = new Gson();
        map = gson.fromJson(data, map.getClass());
        if (CollectUtil.isNotEmpty(files)) {
            map = tcMailService.generateAttachments(map, files.toArray(new MultipartFile[files.size()]));
        }
        amqpTemplate.convertAndSend(RabbitMQEnum.TCMail_RabbitMQ.exchangeName(), RabbitMQEnum.TCMail_RabbitMQ.routingkey(), map);
        return "success";
    }

}
