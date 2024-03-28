package com.foxconn.dp.plm.hdfs.service.impl;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.dp.plm.hdfs.config.TCPropertiesConfig;
import com.foxconn.plm.tcapi.soa.client.AppXSession;
import com.teamcenter.soa.client.Connection;
import com.teamcenter.soa.client.model.strong.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * @author leky
 */
@Component
@Scope("request")
public class TCSOAClientConfigImpl {
    private static Log log = LogFactory.get();
    @Autowired
    private TCPropertiesConfig tCPropertiesConfig;
    private AppXSession appXSession;

    @PostConstruct
    public void initTcConnect() throws Exception {


    }

    public Connection getConnection(String site) throws Exception {
        if (appXSession != null) {
            return appXSession.getConnection();
        }
        String userId = "";
        String jj123 = null;
        if ("wh".equalsIgnoreCase(site)) {
            userId = tCPropertiesConfig.getUserNameWh();
            jj123 = tCPropertiesConfig.getPasswordWh();
        } else if ("cq".equalsIgnoreCase(site)) {
            userId = tCPropertiesConfig.getUserNameCq();
            jj123 = tCPropertiesConfig.getPasswordCq();
        } else if ("lh".equalsIgnoreCase(site)) {
            userId = tCPropertiesConfig.getUserNameLh();
            jj123 = tCPropertiesConfig.getPasswordLh();
        } else if ("tpe".equalsIgnoreCase(site)) {
            userId = tCPropertiesConfig.getUserNameTpe();
            jj123 = tCPropertiesConfig.getPasswordTpe();
        } else if ("Hsinchu".equalsIgnoreCase(site)) {
            userId = tCPropertiesConfig.getUserNameHsinchu();
            jj123 = tCPropertiesConfig.getPasswordHsinchu();
        }
        User user;
        if (jj123 != null && !("".equalsIgnoreCase(jj123))) {
            appXSession = new AppXSession(tCPropertiesConfig.getConnectUrl());
        }
        user = appXSession.login(userId, jj123, "", "");
        if (user == null) {
            throw new Exception("【ERRORR】登录TC系统失败，请联系管理员！");
        } else {
            log.info("login in tc success !!!");
        }
        return appXSession.getConnection();
    }

    @PreDestroy
    public void destroy() {
        log.info("destroy tc connection !!!");
        appXSession.logout();
        appXSession = null;
    }
}
