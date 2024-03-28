package com.foxconn.plm.integrate.mail.controller;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.integrate.mail.domain.ItemInfo;
import com.foxconn.plm.integrate.mail.domain.MailUser;
import com.foxconn.plm.integrate.mail.service.MailGroupMnageService;
import com.foxconn.plm.integrate.mail.service.MailGroupSettingService;
import com.foxconn.plm.integrate.mail.utils.MailSupport;
import com.foxconn.plm.integrate.spas.domain.SPASUser;
import com.foxconn.plm.integrate.spas.service.impl.SpasServiceImpl;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.teamcenter.services.strong.administration.PreferenceManagementService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/mailgroup")
public class MailGroupContoller {

    private static Log log = LogFactory.get();

    @Autowired(required = false)
    private MailGroupMnageService mailGroupManageServiceImpl;

    @Autowired(required = false)
    MailGroupSettingService mailGroupSettingImpl;

    @Autowired(required = false)
    private SpasServiceImpl reportServiceImpl;


    @ApiOperation("零件作废邮件")
    @PostMapping("/sendMail2")
    public R<Long> sendMail2(@RequestBody String datas) {
        log.info("begin sendMail2");
        log.info(datas);
        TCSOAServiceFactory tcSOAServiceFactory = null;
        try {
            tcSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
            PreferenceManagementService preferenceManagementService = tcSOAServiceFactory.getPreferenceManagementService();
            JSONArray arr = JSONArray.parseArray(datas);
            MailSupport mailSupport = new MailSupport();
            for (int i = 0; i < arr.size(); i++) {
                String s = arr.getString(i);
                String[] m = s.split(",");
                String userId = "";
                String om = "";
                List<String> ls = new ArrayList<String>();
                for (int k = 0; k < m.length; k++) {
                    String str = m[k];
                    if (str == null || "".equalsIgnoreCase(str)) {
                        continue;
                    }
                    if (k == 0) {
                        userId = str;
                    } else if (k == 1) {
                        om = str;
                    } else {
                        ls.add(str);
                    }
                }
                List<SPASUser> spasUsers = reportServiceImpl.queryTeamRosterByEmpId(userId);

                if (spasUsers == null || spasUsers.size() <= 0) {
                    continue;
                }
                String notes = spasUsers.get(0).getNotes();
                if (notes == null | "".equalsIgnoreCase(notes)) {
                    continue;
                }
                String html = mailSupport.genMailBody(om, ls);
                HashMap<String, String> httpmap = new HashMap<>();
                httpmap.put("ruleName", "/tc-mail/teamcenter/sendMail3");
                httpmap.put("sendTo", notes);
                httpmap.put("subject", "【零件废弃通知】" + om + "已废弃,请确认!");
                httpmap.put("htmlmsg", html);
                mailSupport.sendMai(preferenceManagementService, httpmap);
            }

            log.info("end sendMail2");
        } finally {
            try {
                if (tcSOAServiceFactory != null) {
                    tcSOAServiceFactory.logout();
                }
            } catch (Exception e) {
            }
        }
        return R.success(1l);
    }


    @ApiOperation("技术文档归档邮件通知")
    @PostMapping("/sendMail")
    public R<Long> sendMail(@RequestBody String datas) {
        log.info("begin sendMail");
        log.info(datas);
        TCSOAServiceFactory tcSOAServiceFactory = null;
        try {
            tcSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
            PreferenceManagementService preferenceManagementService = tcSOAServiceFactory.getPreferenceManagementService();
            MailSupport mailSupport = new MailSupport();
            JSONObject o = JSONObject.parseObject(datas);
            Set<String> keys = o.keySet();
            String descr = o.getString("descr");
            if (descr == null) {
                descr = "";
            }

            String fname = o.getString("fname");
            if (fname == null) {
                fname = "";
            }
            for (String key : keys) {
                if (key == null || "".equalsIgnoreCase(key.trim())) {
                    continue;
                }
                if ("descr".equalsIgnoreCase(key)) {
                    continue;
                }
                if ("fname".equalsIgnoreCase(key)) {
                    continue;
                }
                String uids = o.getString(key);

                List<MailUser> mailUsers = mailGroupSettingImpl.getGroupUsers(Long.parseLong(key));
                if (mailUsers == null || mailUsers.size() <= 0) {
                    continue;
                }
                String sendTo = "";
                int k = 0;
                for (MailUser u : mailUsers) {
                    String mail = u.getMail();
                    if (mail == null || "".equalsIgnoreCase(mail.trim())) {
                        continue;
                    }
                    mail = mail.trim();
                    mail = replaceBlank(mail);
                    mail = mail.trim();
                    mail = mail.replaceAll("[\\s\\u00A0]+", "");
                    if (mail == null || "".equalsIgnoreCase(mail.trim())) {
                        continue;
                    }
                    sendTo += mail + ",";
                    k++;
                    if (k > 40) {
                        if (sendTo.endsWith(",")) {
                            sendTo = sendTo.substring(0, sendTo.length() - 1);
                        }
                        log.info("sendTo:" + sendTo);
                        List<ItemInfo> itemInfos = mailGroupManageServiceImpl.getItemInfos(uids);
                        String html = mailSupport.genMailBody(preferenceManagementService, itemInfos, descr);
                        System.out.print(html);
                        log.info("html:" + html);
                        HashMap<String, String> httpmap = new HashMap<String, String>();
                        httpmap.put("ruleName", "/tc-mail/teamcenter/sendMail3");
                        httpmap.put("sendTo", sendTo);
                        httpmap.put("subject", "File[" + fname + "] has been released !");
                        httpmap.put("htmlmsg", html);
                        mailSupport.sendMai(preferenceManagementService, httpmap);
                        sendTo = "";
                        k = 0;
                        try {
                            Thread.sleep(10000);
                        } catch (Exception e) {
                        }
                    }
                }
                if (sendTo != null && !"".equalsIgnoreCase(sendTo.trim())) {
                    if (sendTo.endsWith(",")) {
                        sendTo = sendTo.substring(0, sendTo.length() - 1);
                    }
                    log.info("sendTo:" + sendTo);
                    List<ItemInfo> itemInfos = mailGroupManageServiceImpl.getItemInfos(uids);
                    String html = mailSupport.genMailBody(preferenceManagementService, itemInfos, descr);
                    System.out.print(html);
                    log.info("html:" + html);
                    HashMap<String, String> httpmap = new HashMap<String, String>();
                    httpmap.put("ruleName", "/tc-mail/teamcenter/sendMail3");
                    httpmap.put("sendTo", sendTo);
                    httpmap.put("subject", "File[" + fname + "] has been released !");
                    httpmap.put("htmlmsg", html);
                    mailSupport.sendMai(preferenceManagementService, httpmap);
                }
            }
            log.info("end sendMail");
        } finally {
            try {
                if (tcSOAServiceFactory != null) {
                    tcSOAServiceFactory.logout();
                }
            } catch (Exception e) {
            }
        }
        return R.success(1l);
    }


    private String replaceBlank(String str) {
        String dest = "";
        if (dest != null) {
            Pattern p = Pattern.compile("\t|\r|\n|\\s+");
            Matcher m = p.matcher(str);
            dest = m.replaceAll(" ");
        }
        return dest;
    }


}
