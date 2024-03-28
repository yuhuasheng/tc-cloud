package com.foxconn.plm.tcservice.electronicdispatcher.service.impl;


import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;

import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.entity.response.SPASUser;
import com.foxconn.plm.feign.service.TcIntegrateClient;
import com.foxconn.plm.feign.service.TcMailClient;
import com.foxconn.plm.redis.service.RedisService;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.tcservice.electronicdispatcher.constant.Constant;
import com.foxconn.plm.tcservice.electronicdispatcher.constant.ServiceName;
import com.foxconn.plm.tcservice.electronicdispatcher.constant.States;
import com.foxconn.plm.tcservice.electronicdispatcher.service.ElectDispatcherService;
import com.foxconn.plm.tcservice.mapper.master.ElectronicDispatcherMapper;
import com.foxconn.plm.utils.collect.CollectUtil;
import com.foxconn.plm.utils.string.StringUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.google.gson.Gson;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import com.xxl.job.core.context.XxlJobHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author HuashengYu
 * @Date 2022/8/2 10:26
 * @Version 1.0
 */
@Service
public class ElectDispatcherServiceImpl implements ElectDispatcherService {
    private static Log log = LogFactory.get();
    @Resource
    ElectronicDispatcherMapper electronicDispatcherMapper;


    @Resource
    TcMailClient tcMail;

    @Value("${electdispatcher.admin-email}")
    private String adminEmail;

    @Resource
    private RedisService redisService;

    @Resource
    private TcIntegrateClient tcIntegrate;

    @Override
    public void monitorElectDispatcher() {
        TCSOAServiceFactory tcsoaServiceFactory = null;
        try {

            List<Map> list = electronicDispatcherMapper.getElectDispatcherList(Arrays.asList(ServiceName.cadence.name(), ServiceName.edif200.name()),
                    Arrays.asList(States.COMPLETE.name(), States.TERMINAL.name()));
            list.removeIf(e -> e.get("email") == null || "".equals(e.get("email").toString().trim())); // 移除邮箱为空的记录
            if (CollectUtil.isEmpty(list)) {
                log.info("不存在符合条件，需要处理的Dispatcher记录！");
                return;
            }

            tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);

            for (Map e : list) {
                String secondryUid = e.get("secondryUid") == null ? "" : e.get("secondryUid").toString().trim();
                if (StringUtil.isEmpty(secondryUid)) {
                    continue;
                }
                String states = e.get("states") == null ? "" : e.get("states").toString().trim();
                log.info("==>> states: " + states);
                String userId = e.get("userId") == null ? "" : e.get("userId").toString().trim();
                log.info("==>> userId: " + userId);
                String userName = e.get("userName") == null ? "" : e.get("userName").toString().trim();
                log.info("==>> userName: " + userName);
                String serviceName = e.get("serviceName") == null ? "" : e.get("serviceName").toString().trim();
                log.info("==>> serviceName: " + serviceName);
                String email = e.get("email") == null ? "" : e.get("email").toString().trim();
                log.info("==>> email: " + email);
                ItemRevision itemRev = (ItemRevision) TCUtils.findObjectByUid(tcsoaServiceFactory.getDataManagementService(), secondryUid);
                TCUtils.getProperties(tcsoaServiceFactory.getDataManagementService(), itemRev, new String[]{"item_id", "item_revision_id", "object_name", "d9_ActualUserID"});
                String itemId = itemRev.get_item_id();
                log.info("==>> itemId: " + itemId);
                String version = itemRev.get_item_revision_id();
                log.info("==>> version: " + version);
                String objectName = itemRev.get_object_name();
                log.info("==>> objectName: " + objectName);
                String actualUserID = itemRev.getPropertyObject("d9_ActualUserID").getStringValue();
                log.info("==>> actualUserID: " + actualUserID);
                States statesEnum = States.valueOf(states);
                String user = "";
                if (StringUtil.isNotEmpty(actualUserID)) {
                    user = actualUserID;
                    Map<String, String> dataMap = new HashMap<>();
                    dataMap.put("empId", user.substring(user.indexOf("(") + 1, user.indexOf(")")));
                    Gson gson = new Gson();
                    List<SPASUser> spasUsers = tcIntegrate.getTeamRosterByEmpId(gson.toJson(dataMap));
                    if (CollectUtil.isNotEmpty(spasUsers)) {
                        email = spasUsers.get(0).getNotes();
                    }

                } else {
                    user = userName + "(" + userId + ")";
                }
                String msg = "Dear " + user + " 用戶， 您的物件ID為：" + itemId + "/" + version + ", 物件名稱為: " + objectName + ", ";
                String subject = "";
                HashMap httpmap = new HashMap();
                switch (statesEnum) {
                    case COMPLETE:
                        if (serviceName.equals(ServiceName.edif200.name())) {
                            msg += "Schematic原理圖轉換完成，請登陸TC進行查看，謝謝！";
                            subject = "Schematic原理圖可視化轉換完成";
                        } else if (serviceName.equals(ServiceName.cadence.name())) {
                            msg += "Layout Boardfile可視化轉換完成，請登陸TC進行查看，謝謝！";
                            subject = "Layout Boardfile可視化轉換完成";
                        }
                        httpmap.put("sendTo", email);
                        httpmap.put("sendCc", adminEmail);
                        break;
                    case TERMINAL:
                        if (serviceName.equals(ServiceName.edif200.name())) {
                            msg += "Schematic原理圖轉換失敗，請聯繫TC管理員進行處理，謝謝！";
                            subject = "Schematic原理圖可視化轉換失败";
                        } else if (serviceName.equals(ServiceName.cadence.name())) {
                            msg += "Layout Boardfile可視化轉換失敗，請聯繫TC管理員進行處理，謝謝！";
                            subject = "Layout Boardfile可視化轉換失败";
                        }
                        httpmap.put("sendTo", email);
                        httpmap.put("sendCc", adminEmail);
                        break;
                }

                String key = itemId + "/" + version + "_" + states;
                if (checkMailRecord(key)) { // 判断此次记录是否已经发送邮件
                    continue;
                }
                httpmap.put("htmlmsg", "<html><head></head><body><h3 style=\"font-family: 宋体;  font-size:15px;\">" + msg + "</h3></body></html>");
                httpmap.put("subject", subject);
                Gson gson = new Gson();
                String data = gson.toJson(httpmap);
                String result = tcMail.sendMail3Method(data);
                log.info("==>> result: " + result);
                if ("success".equals(result)) {
                    XxlJobHelper.log("==>> 零组件ID为: " + itemId + ", 版本号为: " + version + ", 已经处理");
                    log.info("==>> 零组件ID为: " + itemId + ", 版本号为: " + version + ", 任务状态为: " + states + ", 发送邮件成功!");
                    redisService.setCacheMapValue(Constant.ELECDISPATCHERREDISKEY, key, msg); // 将记录缓存到redis缓存数据库
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getLocalizedMessage());
            XxlJobHelper.handleFail(e.getLocalizedMessage());

        } finally {
            if (tcsoaServiceFactory != null) {
                tcsoaServiceFactory.logout();
            }
        }

    }

    /**
     * 判断邮件是否已经发送
     *
     * @param key
     * @return
     */
    private boolean checkMailRecord(String key) {
        Map<String, Object> map = redisService.getCacheMap(Constant.ELECDISPATCHERREDISKEY);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (key.equals(entry.getKey())) {
                return true;
            }
        }
        return false;
    }
}
