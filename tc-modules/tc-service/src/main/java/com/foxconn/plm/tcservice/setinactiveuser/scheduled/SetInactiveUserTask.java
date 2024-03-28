package com.foxconn.plm.tcservice.setinactiveuser.scheduled;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.tcservice.setinactiveuser.domain.UserBean;
import com.foxconn.plm.tcservice.setinactiveuser.service.SetInactiveUserService;
import com.foxconn.plm.utils.collect.CollectUtil;
import com.foxconn.plm.utils.string.StringUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用于定期设置非活动用户状态
 */
@Component
public class SetInactiveUserTask {
    private static Log log = LogFactory.get();
    private static final SimpleDateFormat _dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Resource
    SetInactiveUserService userService;

    @Value("${setinactiveuserinfo.days}")
    private String days;

    @Value("${setinactiveuserinfo.excludeusers}")
    private String excludeusers;

    //    @PostConstruct
//    @Scheduled(cron="0 0 2 * * ?") // 表示每天的凌晨两点执行
    public void setUserState() {

        writeLog("【SetInactiveUserTask】设置非活动用户定时任务开始：");

        if (StringUtil.isNotEmpty(days) && StringUtil.isNotEmpty(excludeusers)) {
            List<UserBean> userInfoLst = userService.getUserInfo(Integer.parseInt(days), Arrays.asList(excludeusers.split(",")).stream().map(e -> e.trim()).collect(Collectors.toList()));

            // Test : START
//            List<UserBean> userInfoLst = userService.getUserInfoByIds(Arrays.asList("12969", "11251", "11409", "10455", "14460").stream().map(e -> e.trim()).collect(Collectors.toList()));
            // Test : END

            writeLog("【SetInactiveUserTask】查询非激活用户数 : " + userInfoLst.size());

            if (CollectUtil.isNotEmpty(userInfoLst)) {
//                userService.updateUserState(userInfoLst);
                userService.setUserState(userInfoLst);
                writeLog("【SetInactiveUserTask】已更新非激活用户数 : " + userInfoLst.size());
            }
        }

        writeLog("【SetInactiveUserTask】设置非活动用户定时任务结束：");
    }

    private void writeLog(String msg) {
        log.info("==================" + msg + "  " + _dateFormat.format(new Date()) + "==================");
        System.err.println("==================" + msg + "  " + _dateFormat.format(new Date()) + "==================");
    }

}
