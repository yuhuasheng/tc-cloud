package com.foxconn.plm.spas.scheduled;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSONObject;
import com.foxconn.plm.entity.constants.TCFolderConstant;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.feign.service.TcMailClient;
import com.foxconn.plm.spas.service.impl.ManpowerServiceImpl;
import com.foxconn.plm.spas.service.impl.SynSpasDBServiceImpl;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.query.SavedQueryService;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.Folder;
import com.teamcenter.soa.client.model.strong.WorkspaceObject;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * @Author {chen.zhang@foxconn.com}
 * @Date: 2022/11/21/ 11:38
 * @description 同步SPAS数据库表数据
 */
@Component
public class SynSpasDBTask {
    private static Log log = LogFactory.get();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Value("${spring.cloud.nacos.discovery.namespace}")
    private String env;

    @Value("${mail.to}")
    private String mailTo;

    @Value("${mail.cc}")
    private String mailCC;

    @Resource
    TcMailClient tcMail;

    @Resource
    private SynSpasDBServiceImpl synSpasDBServiceImpl;

    //@PostConstruct
    //@Scheduled(cron = "0 0 0/1 * * ?")//每个整点执行一次
    @XxlJob("synSpasDB")
    public void timedTask() {

            log.info("同步SPAS数据库表数据开始：" + dateFormat.format(new Date()));
            try {
                SynSpasDBServiceImpl.updateDate();
                synSpasDBServiceImpl.addProjectPersonData();
                synSpasDBServiceImpl.addPlatformFoundData();
                synSpasDBServiceImpl.addProjectScheduleData();
                synSpasDBServiceImpl.addUserRoleData();
                synSpasDBServiceImpl.addUserData();
                synSpasDBServiceImpl.addRoleData();
                synSpasDBServiceImpl.addOrganizationData();
                synSpasDBServiceImpl.addDeptGroupData();
                synSpasDBServiceImpl.addProjectSeriesData();
                synSpasDBServiceImpl.addProjectAttributeData();
                synSpasDBServiceImpl.addProductLinePhaseData();
                synSpasDBServiceImpl.addProductLineData();
                synSpasDBServiceImpl.addCustomerData();
                synSpasDBServiceImpl.addCusAttributeCategoryData();
                synSpasDBServiceImpl.addCusAttributeData();
                synSpasDBServiceImpl.addStiTeamRosterData();
                synSpasDBServiceImpl.addFunctionData();
                synSpasDBServiceImpl.addRoutingData();
                synSpasDBServiceImpl.updateManpowerStandardData();//放到最後*/
            } catch (Exception e) {
                try {
                    JSONObject httpmap = new JSONObject();
                    httpmap.put("sendTo", mailTo);
                    httpmap.put("sendCc", mailCC);
                    httpmap.put("subject", "【专案人力同步异常】请及时处理！");
                    log.error(e.getMessage(),e);
                    String message = e.getMessage();
                    message = env+"環境 ,  【专案人力同步异常】"+message ;
                    httpmap.put("htmlmsg", message);
                    tcMail.sendMail3Method(httpmap.toJSONString());
                }catch (Exception e0) {
                    log.error(e0);
                }
                log.info("同步SPAS数据库表数据错误：" + e.getMessage());
                log.error(e);
                XxlJobHelper.handleFail(e.getLocalizedMessage());

            }
            log.info("同步SPAS数据库表数据结束：" + dateFormat.format(new Date()));

    }



}
