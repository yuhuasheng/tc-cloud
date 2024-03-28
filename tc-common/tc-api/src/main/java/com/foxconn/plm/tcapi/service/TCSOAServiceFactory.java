package com.foxconn.plm.tcapi.service;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.config.TCConnectUserConfig;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.tcapi.config.TCConnectConfig;
import com.foxconn.plm.tcapi.soa.client.AppXSession;
import com.prg0.services.loose.programinfra.ProgramManagementService;
import com.teamcenter.services.internal.strong.core.ICTService;
import com.teamcenter.services.loose.core.SessionService;
import com.teamcenter.services.strong.administration.PreferenceManagementService;
import com.teamcenter.services.strong.administration.UserManagementService;
import com.teamcenter.services.strong.cad.StructureManagementService;


import com.teamcenter.services.strong.classification.ClassificationService;
import com.teamcenter.services.strong.core.*;


import com.teamcenter.services.strong.projectmanagement.ScheduleManagementService;
import com.teamcenter.services.strong.query.SavedQueryService;
import com.teamcenter.services.strong.structuremanagement.StructureService;
import com.teamcenter.services.strong.workflow.WorkflowService;
import com.teamcenter.soa.client.Connection;
import com.teamcenter.soa.client.FileManagementUtility;
import com.teamcenter.soa.client.model.strong.User;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.FileNotFoundException;


@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class TCSOAServiceFactory {
    private Log log = LogFactory.get();

    private StructureService structureService;

    private StructureManagementService structureManagementService; // bom 结构

    private DataManagementService dataManagementService; //tc object

    private FileManagementUtility fileManagementUtility; // fms

    private PreferenceManagementService preferenceManagementService; // 首选项

    private ProgramManagementService programManagementService;  // 项目

    private ClassificationService classificationService;

    private UserManagementService userManagementService;

    private SessionService sessionservice;// 用户session

    private SavedQueryService savedQueryService; // 搜索

    private ICTService ictService;

    private ReservationService reservation; // 出库入库服务

    private LOVService lovService;

    private WorkflowService wfService;

    private DispatcherManagementService dispatcherService;


    private ProjectLevelSecurityService projectLevelSecurityService;

    private EnvelopeService envelopeService;


    private ITCSOAClientConfig tcSOAClientConfig;

    private ScheduleManagementService scheduleManagementService;
    @Resource
    private TCConnectConfig tcConnectConfig;
    private User user;


    public User getUser() {
        return user;
    }


    public void logout() {
        tcSOAClientConfig.destroy();
    }

    public TCSOAServiceFactory() {
    }

    public TCSOAServiceFactory(TCUserEnum tcConnect) {
        tcConnectConfig = BeanFactoryService.getBean(TCConnectConfig.class);
        initTCConnect(tcConnect.getValue());
    }

    public void initTCConnect(String tcConnType) {
        // String tcConnType = TCConnectContextHolder.getTCConnectType();
        if (StringUtils.hasLength(tcConnType)) {
            AppXSession appXSession = new AppXSession(tcConnectConfig.getConnectUrl());
            TCConnectUserConfig tcUser = tcConnectConfig.getUserConfig().get(tcConnType);
            if (tcUser == null) {
                throw new RuntimeException("【ERRORR】登录TC系统失败: " + tcConnType + " 不存在 配置文件里面!");
            }
            user = appXSession.login(tcUser.getUserName(), tcUser.getPassword(), "", "");
            if (user == null) {
                throw new RuntimeException("【ERRORR】登录TC系统失败，请联系管理员！");
            }
            log.info("login in tc success !!!");
            tcSOAClientConfig = new ITCSOAClientConfig() {
                @Override
                public Connection getConnection() {
                    return appXSession.getConnection();
                }

                public void destroy() {
                    appXSession.logout();
                }
            };
        }
    }

    public Connection getConnection() {
        return tcSOAClientConfig.getConnection();
    }

    public WorkflowService getWorkflowService() {
        if (wfService == null) {
            wfService = WorkflowService.getService(tcSOAClientConfig.getConnection());
        }
        return wfService;
    }

    public ClassificationService getClassificationService() {
        if (classificationService == null) {
            classificationService = ClassificationService.getService(tcSOAClientConfig.getConnection());
        }
        return classificationService;
    }


    public UserManagementService getUserManagementService() {
        if (userManagementService == null) {
            userManagementService = UserManagementService.getService(tcSOAClientConfig.getConnection());
        }
        return userManagementService;
    }


    public ReservationService getReservationService() {
        if (reservation == null) {
            reservation = ReservationService.getService(tcSOAClientConfig.getConnection());
        }
        return reservation;
    }

    public SavedQueryService getSavedQueryService() {
        if (savedQueryService == null) {
            savedQueryService = SavedQueryService.getService(tcSOAClientConfig.getConnection());
        }
        return savedQueryService;
    }


    public SessionService getSessionService() {
        if (sessionservice == null) {
            sessionservice = SessionService.getService(tcSOAClientConfig.getConnection());
        }
        return sessionservice;
    }

    public StructureManagementService getStructureManagementService() {

        if (structureManagementService == null) {
            structureManagementService = StructureManagementService.getService(tcSOAClientConfig.getConnection());
        }
        return structureManagementService;
    }

    public DataManagementService getDataManagementService() {
        if (dataManagementService == null) {
            dataManagementService = DataManagementService.getService(tcSOAClientConfig.getConnection());
        }
        return dataManagementService;
    }


    public StructureService getStructureService() {
        if (structureService == null) {
            structureService = StructureService.getService(tcSOAClientConfig.getConnection());
        }
        return structureService;
    }

    public FileManagementUtility getFileManagementUtility() {

        String fmsUrl = tcConnectConfig.getFmsUrl();
        if (fileManagementUtility == null) {
            if (fmsUrl != null && fmsUrl.length() > 0) {
                try {
                    fileManagementUtility = new FileManagementUtility(tcSOAClientConfig.getConnection(), null, null, new String[]{fmsUrl}, null);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
            if (fileManagementUtility == null) {
                fileManagementUtility = new FileManagementUtility(tcSOAClientConfig.getConnection());
            }
        }
        return fileManagementUtility;
    }

    public FileManagementUtility getFileManagementUtility(String cachDir) {

        String fmsUrl = tcConnectConfig.getFmsUrl();
        log.info("fmsUrl =====>"+fmsUrl);
        if (fileManagementUtility == null) {
            if (fmsUrl != null && fmsUrl.length() > 0) {
                try {
                    fileManagementUtility = new FileManagementUtility(tcSOAClientConfig.getConnection(), null, null, new String[]{fmsUrl}, cachDir);
                } catch (FileNotFoundException e) {
                    log.error(e.getMessage(),e);
                }
            }
            if (fileManagementUtility == null) {
                fileManagementUtility = new FileManagementUtility(tcSOAClientConfig.getConnection());
            }
        }
        return fileManagementUtility;
    }

    public PreferenceManagementService getPreferenceManagementService() {

        if (preferenceManagementService == null) {
            preferenceManagementService = PreferenceManagementService.getService(tcSOAClientConfig.getConnection());
        }
        return preferenceManagementService;
    }

    public ProgramManagementService getProgramManagementService() {
        if (programManagementService == null) {
            programManagementService = ProgramManagementService.getService(tcSOAClientConfig.getConnection());
        }
        return programManagementService;
    }

    public DispatcherManagementService getDispatcherService() {
        if (dispatcherService == null) {
            dispatcherService = DispatcherManagementService.getService(tcSOAClientConfig.getConnection());
        }
        return dispatcherService;
    }

    public ProjectLevelSecurityService getProjectLevelSecurityService() {
        if (projectLevelSecurityService == null) {
            projectLevelSecurityService = ProjectLevelSecurityService.getService(tcSOAClientConfig.getConnection());
        }
        return projectLevelSecurityService;
    }

    public EnvelopeService getEnvelopeService() {
        if (envelopeService == null) {
            envelopeService = EnvelopeService.getService(tcSOAClientConfig.getConnection());
        }
        return envelopeService;
    }

    public LOVService getLovService() {
        if (lovService == null) {
            lovService = lovService.getService(tcSOAClientConfig.getConnection());
        }
        return lovService;
    }

    public ICTService getICTService() {
        if (ictService == null) {
            ictService = ICTService.getService(tcSOAClientConfig.getConnection());
        }
        return ictService;
    }

    public ScheduleManagementService getScheduleManagementService() {
        if (scheduleManagementService == null) {
            scheduleManagementService = ScheduleManagementService.getService(tcSOAClientConfig.getConnection());
        }
        return scheduleManagementService;
    }

    public interface ITCSOAClientConfig {
        public Connection getConnection();

        public void destroy();
    }
}
