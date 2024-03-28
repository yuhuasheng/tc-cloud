package com.foxconn.plm.tcservice.tclicensereport.scheduled;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSONObject;
import com.foxconn.dp.plm.privately.Access;
import com.foxconn.plm.feign.service.TcMailClient;
import com.foxconn.plm.tcservice.tclicensereport.domain.FunctionInfo;
import com.foxconn.plm.tcservice.tclicensereport.domain.UserInfo;
import com.foxconn.plm.tcservice.tclicensereport.service.impl.FunctionServiceImpl;
import com.foxconn.plm.tcservice.tclicensereport.service.impl.UserServiceImpl;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class SynFunctionUser {
    private static Log log = LogFactory.get();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private String ip;
    @Resource
    FunctionServiceImpl functionServiceImpl;

    @Resource
    UserServiceImpl userServiceImpl;

    @Resource
    TcMailClient tcMail;

    //@PostConstruct
    //@Scheduled(cron = "0 30 23 * * ? ")//每天23:30执行
    @XxlJob("TcSynFunctionUserSchedule")
    public void timedTask() {
       try {
           //防止同一日期重复添加
           try {
               ip = getLocalIP();
               Date maxRecordDate = userServiceImpl.getMaxRecordDate();
               if (maxRecordDate != null) {
                   String format1 = dateFormat.format(maxRecordDate);
                   String format2 = dateFormat.format(new Date());
                   long time1 = dateFormat.parse(format1).getTime();
                   long time2 = dateFormat.parse(format2).getTime();
                   if (time1 == time2) {
                       return;
                   }
               }
           } catch (Exception e) {
               e.printStackTrace();
           }

           //获取昨天的时间
           Date yesterday = DateUtils.addDays(new Date(), -1);
           //获取昨天的数据
           List<UserInfo> yesterdayUserInfos = userServiceImpl.getYesterdayUserInfo(dateFormat.format(yesterday));
           //获取当天TC的数据
           List<UserInfo> tcUserInfos = userServiceImpl.getTCUserInfo();

           for (int i = 0; i < tcUserInfos.size(); i++) {
               UserInfo tcUserInfo = tcUserInfos.get(i);
               String userId = tcUserInfo.getUserId();
               Integer usedHoursInMonth = tcUserInfo.getUsedHoursInMonth();
               if (yesterdayUserInfos != null && yesterdayUserInfos.size() > 0) {
                   UserInfo yesterdayUserInfo = null;
                   Optional<UserInfo> optional = yesterdayUserInfos.stream().filter(item -> item.getUserId().equals(userId)).findFirst();
                   if (optional != null && optional.isPresent()) {
                       yesterdayUserInfo = optional.get();
                       Integer yesterdayUsedHoursInMonth = yesterdayUserInfo.getUsedHoursInMonth();
                       if (usedHoursInMonth >= yesterdayUsedHoursInMonth) {
                           Integer usedHoursInDay = usedHoursInMonth - yesterdayUsedHoursInMonth;
                           tcUserInfo.setUsedHoursInDay(usedHoursInDay);
                       } else {
                           tcUserInfo.setUsedHoursInDay(usedHoursInMonth);
                       }
                   }
               }
           }
           for (int i = 0; i < tcUserInfos.size(); i++) {
               UserInfo userInfo = tcUserInfos.get(i);
               String bu = userInfo.getBu();
               String department = userInfo.getDepartment();
               String function = userInfo.getFunction();
               String userId = userInfo.getUserId();
               String userName = userInfo.getUserName();
               Date lastLoginDate = userInfo.getLastLoginDate();
               Integer usedHoursInMonth = userInfo.getUsedHoursInMonth();
               Integer usedHoursInDay = userInfo.getUsedHoursInDay();
               Date recordDate = userInfo.getRecordDate();
               log.info(ip + "_uat_lur_user ===> bu:" + bu
                       + ",department:" + department
                       + ",function:" + function
                       + ",userId:" + userId
                       + ",userName:" + userName
                       + ",lastLoginDate:" + lastLoginDate
                       + ",usedHoursInMonth:" + usedHoursInMonth
                       + ",usedHoursInDay:" + usedHoursInDay
                       + ",recordDate:" + recordDate
               );
           }
           userServiceImpl.setUserInfo(tcUserInfos);
           List<FunctionInfo> functionInfoList = functionServiceImpl.getFunctionInfo();
           for (int i = 0; i < functionInfoList.size(); i++) {
               FunctionInfo functionInfo = functionInfoList.get(i);
               String bu = functionInfo.getBu();
               String department = functionInfo.getDepartment();
               String function = functionInfo.getFunction();
               Integer usedHoursInDay = functionInfo.getUsedHoursInDay();
               float lurInDay = functionInfo.getLurInDay();
               Date recordDate = functionInfo.getRecordDate();
               log.info(ip + "_uat_lur_function ===> bu:" + bu
                       + ",department:" + department
                       + ",function:" + function
                       + ",usedHoursInDay:" + usedHoursInDay
                       + ",lurInDay:" + lurInDay
                       + ",recordDate:" + recordDate
               );
           }
           functionServiceImpl.setFunctionInfo(functionInfoList);
       }catch(Exception e){
           try {
               JSONObject httpmap = new JSONObject();
               httpmap.put("sendTo", "hui.h.liu@foxconn.com");
               httpmap.put("sendCc", "leky.p.li@foxconn.com");
               httpmap.put("subject", "【TcSynFunctionUserSchedule】执行失败, 请及时处理！");
               log.error(e.getMessage(),e);
               String message = e.getMessage();
               message = "TcSynFunctionUserSchedule 执行失败"+message ;
               httpmap.put("htmlmsg", message);
               tcMail.sendMail3Method(httpmap.toJSONString());
           }catch (Exception e0) {
               log.error(e0);
           }
           XxlJobHelper.handleFail(e.getLocalizedMessage());
       }
    }

    public static String getLocalIP() {
        List<String> ipList = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            NetworkInterface networkInterface;
            Enumeration<InetAddress> inetAddresses;
            InetAddress inetAddress;
            String ip;
            while (networkInterfaces.hasMoreElements()) {
                networkInterface = networkInterfaces.nextElement();
                inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    inetAddress = inetAddresses.nextElement();
                    if (inetAddress != null && inetAddress instanceof Inet4Address) { // IPV4
                        ip = inetAddress.getHostAddress();
                        ipList.add(ip);
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        for (String ip : ipList) {
            if (!"127.0.0.1".equals(ip)) {
                return ip;
            }
        }
        return "未知IP";
    }
}
