package com.foxconn.dp.plm.fileservice.serviceImpl;


import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;

import com.foxconn.dp.plm.privately.FileServerPropertitesUtils;

import com.foxconn.plm.tcapi.soa.client.AppXSession;
import com.teamcenter.soa.client.Connection;
import com.teamcenter.soa.client.model.strong.User;
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
    private AppXSession appXSession;

    @PostConstruct
    public void initTcConnect() throws Exception {
        appXSession = new AppXSession(FileServerPropertitesUtils.getProperty("tc.url"));
        User user = appXSession.login(FileServerPropertitesUtils.getProperty("tc.username"), FileServerPropertitesUtils.getProperty("tc.pwd"), "", "");
        if (user == null) {
            throw new Exception("【ERRORR】登录TC系统失败，请联系管理员！");
        } else {
            log.info("login in tc success !!!");
        }

    }

    public Connection getConnection() {
        return appXSession.getConnection();
    }

    @PreDestroy
    public void destroy() {
        log.info("destroy tc connection !!!");
        appXSession.logout();
    }
}
