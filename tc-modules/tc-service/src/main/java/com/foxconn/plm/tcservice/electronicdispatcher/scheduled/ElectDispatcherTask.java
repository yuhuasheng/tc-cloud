package com.foxconn.plm.tcservice.electronicdispatcher.scheduled;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.tcservice.electronicdispatcher.service.ElectDispatcherService;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * @Author HuashengYu
 * @Date 2022/8/2 9:53
 * @Version 1.0
 */
@Component
public class ElectDispatcherTask {
    private static Log log = LogFactory.get();

    @Value("${electdispatchertask.switch.is-open}")
    private boolean flag;

    @Resource
    private ElectDispatcherService electDispatcherService;

//    @Scheduled(cron = "${electdispatchertask.corn.task-corn}")
    @XxlJob("ElectDispatcherSchedule")
    private void scheduleSendMail() {
        electDispatcherService.monitorElectDispatcher();
    }
}
