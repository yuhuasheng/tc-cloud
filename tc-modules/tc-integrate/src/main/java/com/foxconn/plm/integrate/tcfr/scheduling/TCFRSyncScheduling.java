package com.foxconn.plm.integrate.tcfr.scheduling;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSONObject;
import com.foxconn.dp.plm.privately.Access;
import com.foxconn.plm.entity.constants.*;
import com.foxconn.plm.feign.service.TcMailClient;
import com.foxconn.plm.integrate.config.properties.TCFRMinioPropertiesConfig;
import com.foxconn.plm.integrate.config.properties.TCFRProxyConfig;
import com.foxconn.plm.integrate.tcfr.domain.*;
import com.foxconn.plm.integrate.tcfr.mapper.TCFRMapper;
import com.foxconn.plm.redis.service.RedisService;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.file.FileUtil;
import com.foxconn.plm.utils.ip.IpUtil;
import com.foxconn.plm.utils.net.HttpUtil;
import com.foxconn.plm.utils.string.StringUtil;
import com.foxconn.plm.utils.tc.*;
import com.google.gson.Gson;
import com.teamcenter.schemas.soa._2006_03.exceptions.ServiceException;
import com.teamcenter.services.loose.core.SessionService;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.core.LOVService;
import com.teamcenter.services.strong.query.SavedQueryService;
import com.teamcenter.services.strong.workflow.WorkflowService;
import com.teamcenter.soa.client.FileManagementUtility;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.*;
import com.teamcenter.soa.exceptions.NotLoadedException;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import io.minio.MinioClient;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.foxconn.plm.integrate.tcfr.domain.TCFRConstant.*;

/**
 * @Author MW00333
 * @Date 2023/3/28 16:08
 * @Version 1.0
 */
@Component
public class TCFRSyncScheduling {

    private static Log log = LogFactory.get();

    private static final String format = "yyyy/MM/dd";
    @Resource
    private TCFRMapper tcfrMapper;

    @Resource
    private TcMailClient tcMailClient;

    @Resource(name = "tcfrMinioClient")
    private MinioClient minioClient;

    @Resource
    private TCFRMinioPropertiesConfig config;

    @Resource
    private TCFRProxyConfig tcfrProxyConfig;

    @Resource
    private RedisService redisService;

   @XxlJob("tcfrSyncScheduling")
   //@PostConstruct
    public void handlerData() {
        log.info("==>> tcfr获取会议附件任务开始执行");
        XxlJobHelper.log("==>>: tcfr获取会议附件任务开始执行");
        String userName=Access.getPasswordAuthentication();
        List<MeetBean> tcfrDataList = tcfrMapper.getTCFRData(Access.check(userName));
        if (CollUtil.isEmpty(tcfrDataList)) {
            XxlJobHelper.log("未查询到需要同步的数据");
            return;
        }

        TCSOAServiceFactory tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS2);
        DataManagementService dmService = tcsoaServiceFactory.getDataManagementService();
        SavedQueryService savedQueryService = tcsoaServiceFactory.getSavedQueryService();
        FileManagementUtility fmUtility = tcsoaServiceFactory.getFileManagementUtility();
        SessionService sessionService = tcsoaServiceFactory.getSessionService();
        WorkflowService wfService = tcsoaServiceFactory.getWorkflowService();
        TCUtils.byPass(sessionService, true);
        try {
            for (MeetBean rootBean : tcfrDataList) {
                ItemRevision documentItemRev = null;
                try {
                    MeetInfo meetInfo = MeetInfo.propMapping(rootBean);
                    log.info("==>> meetInfo: " + meetInfo.toString());
                    documentItemRev = getTCFRMeetingDocument(savedQueryService, dmService, meetInfo); // 获取TCFR会议附件的会议文档对象版本
                    if (null == documentItemRev) {
                        return;
                    }

                    boolean flag = addMeetingFile(fmUtility, dmService, documentItemRev, meetInfo); // 会议文档添加会议附件
                    if (!flag) {
                        continue;
                    }
                    if (!TCUtils.isReleased(dmService, documentItemRev)) { // 判断文档对象是否已经发行
                        log.info("==>> 开始启动发布文档对象流程");
                        startWorkFlow(wfService, dmService, documentItemRev); // 启动快速发行流程
                        log.info("==>> 发布文档对象流程结束");
                    }

                    log.info("==>> 开始执行updateStatus操作");
                    rootBean.setMsg("Y");
                    rootBean.setUploadFLag("success"); // 设置标识为成功
                    rootBean.setDocumentStatus("Released");
                    tcfrMapper.updateStatus(Access.check(rootBean)); // 更新状态值到DB表
                    log.info("==>> 执行updateStatus操作完成");

                } catch (Exception e) {
                    e.printStackTrace();
                    log.error(e.getLocalizedMessage());
                    if (!"failure".equals(rootBean.getUploadFLag())) {
                        MeetBean meetBean = tcfrMapper.getTCFRDataByDocumentUid(Access.check(rootBean.getDocumentUid()));
                        try {
                            meetBean.setMsg("N");
                            meetBean.setUploadFLag("failure");
                            sendFileUploadFailureMail(meetBean);
                        } catch (Exception e1) {
                            e1.printStackTrace();
                            log.error(e1.getLocalizedMessage());
                            meetBean.setUploadFLag(""); // 设置邮件标识为空
                        }

                        tcfrMapper.updateFlag(Access.check(meetBean));
                    }
                }
            }
            log.info("==>> tcfr获取会议附件任务执行结束");
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getLocalizedMessage());
            XxlJobHelper.log(e.getLocalizedMessage());
        } finally {
            tcsoaServiceFactory.logout();
            TCUtils.byPass(sessionService, false); // 关闭旁路
        }
    }

    @XxlJob("tcfrProjectInfo")
//    @PostConstruct
    public void syncProjectInfo() {
        try {
            log.info("============>> 开始获取TCFR getProjectInfos ============>>");
            XxlJobHelper.log("============>> 开始获取TCFR getProjectInfos ============>>");
            List<TCFRProjectInfoPojo> pojList = tcfrMapper.getTCFRProjectInfoPojos();
            if (CollUtil.isEmpty(pojList)) {
                XxlJobHelper.log("==>> 不存在待同步到TCFR的专案信息");
                log.info("==>> 不存在待同步到TCFR的专案信息");
                return;
            }

            pojList.removeIf(ObjUtil::isEmpty);
            pojList.removeIf(bean -> {
                String spasProjId = bean.getSpasProjId();
                List<String> projectFolders = tcfrMapper.getTCProjectFolder("p" + spasProjId);// 判断专案文件夹是否存在
                if (CollUtil.isEmpty(projectFolders)) {
                    return true;
                } else {
                    projectFolders.removeIf(StringUtil::isEmpty);
                    if (CollUtil.isEmpty(projectFolders)) {
                        return true;
                    }
                }
                return false;
            });
          /*  pojList.removeIf(info -> !info.getSpasProjId().equalsIgnoreCase("2148")
                    && !info.getSpasProjId().equalsIgnoreCase("2146")
                    && !info.getSpasProjId().equalsIgnoreCase("2144"));*/

            log.info("============>> 获取TCFR getProjectInfos 结束 ============>>");
            XxlJobHelper.log("============>> 获取TCFR getProjectInfos 结束 ============>>");

            if (CollUtil.isEmpty(pojList)) {
                log.error("============>> 不存在专案信息待同步到TCFR ============>>");
                return;
            }


            log.info("============>> 专案信息开始传递TCFR ============>>");
            XxlJobHelper.log("============>> 专案信息开始传递TCFR ============>>");
            JSONObject paramsObject = new JSONObject();
//            paramsObject.put("method", "getProjectInfos");
            paramsObject.put("data", pojList);

            String result = sendParamsToTCFR(paramsObject, SYNCPROJECTINFO);
            log.info("==>> result: " + result);

            log.info("============>> 专案信息传递TCFR结束 ============>>");
            XxlJobHelper.log("============>> 专案信息传递TCFR结束 ============>>");
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getLocalizedMessage());
            XxlJobHelper.log(e.getLocalizedMessage());
        }
    }


    @XxlJob("tcfrCustomerLov")
//    @PostConstruct
    public void syncCustomerLov() {
        try {
            log.info("============>> 开始获取TCFR customer ============>>");
            XxlJobHelper.log("============>> 开始获取TCFR customer ============>>");
            List<CustomerPojo> customerLovNames = tcfrMapper.getCustomerLov();
            if (CollUtil.isEmpty(customerLovNames)) {
                log.error("==>> 获取客户LOV下拉值失败");
                XxlJobHelper.log("==>> 获取客户LOV下拉值失败");
                return;
            }

            customerLovNames.removeIf(info -> StringUtil.isEmpty(info.getCustomerName()));
           // customerLovNames.removeIf(info -> !info.getCustomerName().equalsIgnoreCase("dell") && !info.getCustomerName().equalsIgnoreCase("hp") && !info.getCustomerName().equalsIgnoreCase("lenovo"));

            log.info("============>> 获取TCFR customer 结束 ============>>");
            XxlJobHelper.log("============>> 获取TCFR customer 结束 ============>>");

            if (CollUtil.isEmpty(customerLovNames)) {
                log.error("============>> 不存在客户信息Lov待同步到TCFR ============>>");
                return;
            }

            log.info("============>> 客户信息开始传递TCFR ============>>");
            XxlJobHelper.log("============>> 客户信息开始传递TCFR ============>>");

            JSONObject paramsObject = new JSONObject();
//            paramsObject.put("method", "getCustomer");
            paramsObject.put("data", customerLovNames);

            String result = sendParamsToTCFR(paramsObject, SYNCCUSTOMERLOV);
            log.info("==>> result: " + result);

            log.info("============>> 客户信息传递TCFR结束 ============>>");
            XxlJobHelper.log("============>> 客户信息传递TCFR结束 ============>>");
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getLocalizedMessage());
        }
    }

    @XxlJob("tcfrMeetingTypeLov")
//    @PostConstruct
    private void syncMeetingTypeLov() {
        try {
            log.info("============>> 开始获取TCFR MeetingType ============>>");
            XxlJobHelper.log("============>> 开始获取TCFR MeetingType ============>>");

            List<MeetingTypeBean> meetingTypeLovList = tcfrMapper.getMeetingTypeLov();
            if (CollUtil.isEmpty(meetingTypeLovList)) {
                log.error("==>> 获取会议类型LOV下拉值失败");
                XxlJobHelper.log("==>> 获取会议类型LOV下拉值失败");
                return;
            }

            meetingTypeLovList.removeIf(ObjUtil::isEmpty);

            log.info("============>> 获取TCFR MeetingType 结束 ============>>");
            XxlJobHelper.log("============>> 获取TCFR MeetingType 结束 ============>>");

            if (CollUtil.isEmpty(meetingTypeLovList)) {
                log.error("============>> 不存在会议类型Lov待同步到TCFR ============>>");
                return;
            }

            log.info("============>> 会议类型开始传递TCFR ============>>");
            XxlJobHelper.log("============>> 会议类型开始传递TCFR ============>>");

            JSONObject paramsObject = new JSONObject();
            paramsObject.put("data", meetingTypeLovList);

            String result = sendParamsToTCFR(paramsObject, SYNCMEETINGTYPELOV);
            log.info("==>> result: " + result);

            log.info("============>> 会议类型传递TCFR结束 ============>>");
            XxlJobHelper.log("============>> 会议类型传递TCFR结束 ============>>");

        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getLocalizedMessage());
        }
    }


    /**
     * 传递字段信息至TCFR
     *
     * @param paramsObject
     * @return
     * @throws Exception
     */
    private String sendParamsToTCFR(JSONObject paramsObject, String operation) throws Exception {
        log.info("==>> jsonData: " + JSONUtil.toJsonPrettyStr(paramsObject.toJSONString()));
        List<String> ipList = IpUtil.getRealIP();
        ipList.removeIf(str -> !str.startsWith("10.203") && !str.startsWith("10.205"));
        String localIP = ipList.get(0);
        System.out.println("==>> localIP: " + localIP);
        String proxyHost = null;
        if (localIP.startsWith("10.203")) {
            proxyHost = tcfrProxyConfig.getServerProxyIp();
        } else if (localIP.startsWith("10.205")) {
            proxyHost = tcfrProxyConfig.getClientProxtIp();
        }

        String url = null;
        if (SYNCPROJECTINFO.equals(operation)) {
            url = tcfrProxyConfig.getProjectInfoParamsUrl();
        } else if (SYNCCUSTOMERLOV.equals(operation)) {
            url = tcfrProxyConfig.getCustomerParamsUrl();
        } else if (SYNCMEETINGTYPELOV.equals(operation)) {
            url = tcfrProxyConfig.getMeetingTypeParamsUrl();
        }
        String result = HttpUtil.httpPost(url, null, paramsObject.toJSONString(), "Y", 15000, proxyHost, tcfrProxyConfig.getPort());
        JSONObject json = JSONObject.parseObject(result);
        if (json.get("msg").equals("failure")) {
            throw new Exception("==>> 传递信息到TCFR失败");
        }
        return result;
    }


    /**
     * 获取TCFR会议附件的会议文档对象版本
     *
     * @param savedQueryService 查询服务类
     * @param dmService         工具类
     * @param meetInfo
     * @return
     * @throws Exception
     */
    private ItemRevision getTCFRMeetingDocument(SavedQueryService savedQueryService, DataManagementService dmService, MeetInfo meetInfo) throws Exception {
        String projectId = "p" + meetInfo.getSpasProjId();
        String phaseName = meetInfo.getSpasProjPhase();
        ModelObject[] objects = TCUtils.executequery(savedQueryService, dmService, TCSearchEnum.D9_FIND_PROJECT_FOLDER.queryName(),
                TCSearchEnum.D9_FIND_PROJECT_FOLDER.queryParams(), new String[]{projectId, "*"});
        if (ArrayUtil.isEmpty(objects)) {
            throw new Exception("SPAS ID为: " + meetInfo.getSpasProjId() + ", 在TC中未查询到专案文件夹");
        }

        Folder projectFolder = (Folder) objects[0];
        String projectName = TCUtils.getPropStr(dmService, projectFolder, TCFolderConstant.PROPERTY_OBJECT_NAME);
        log.info("==>> projectName: " + projectName);

        ModelObject TCFRFolder = null;
        TCUtils.refreshObject(dmService, projectFolder);
        ModelObject[] functionFolders = TCUtils.getPropModelObjectArray(dmService, projectFolder, TCFolderConstant.REL_CONTENTS);
        if (ArrayUtil.isNotEmpty(functionFolders)) {
            TCFRFolder = TCUtils.checkModelObjExist(dmService, functionFolders, TCFRConstant.TCFRFOLDER, TCFolderConstant.TYPE_D9_FUNCTIONFOLDER, TCFolderConstant.PROPERTY_OBJECT_NAME);
        }
        if (null == TCFRFolder) {
            return null;
        }

        ModelObject TCFRPhaseFolder = null;
        TCUtils.refreshObject(dmService, TCFRFolder);
        ModelObject[] phaseFolders = TCUtils.getPropModelObjectArray(dmService, TCFRFolder, TCFolderConstant.REL_CONTENTS);
        if (ArrayUtil.isNotEmpty(phaseFolders)) {
            TCFRPhaseFolder = TCUtils.checkModelObjExist(dmService, phaseFolders, phaseName, TCFolderConstant.TYPE_D9_PHASEFOLDER, TCFolderConstant.PROPERTY_OBJECT_NAME);
        }

        if (null == TCFRPhaseFolder) {
            return null;
        }

        ItemRevision documentItemRev = null;
        TCUtils.refreshObject(dmService, new ModelObject[]{TCFRPhaseFolder});
        ModelObject[] objs = TCUtils.getPropModelObjectArray(dmService, TCFRPhaseFolder, TCFolderConstant.REL_CONTENTS);
        if (ArrayUtil.isNotEmpty(objs)) {
            ModelObject document = TCUtils.checkModelObjExist(dmService, objs, meetInfo.getMeetingTitle(), TCItemTypeConstant.DOCUMENT_ITEM_TYPE, TCItemConstant.PROPERTY_OBJECT_NAME);
            if (document != null) {
                documentItemRev = TCUtils.getItemLatestRevision(dmService, (Item) document);
            }
        }
        return documentItemRev;
    }


    /**
     * 会议文档添加会议附件
     *
     * @param fmUtility
     * @param dmService
     * @param itemRev
     * @param meetInfo
     * @throws Exception
     */
    private boolean addMeetingFile(FileManagementUtility fmUtility, DataManagementService dmService, ItemRevision itemRev, MeetInfo meetInfo) throws Exception {
        String meetingMinutesPath = meetInfo.getMeetingMinutesPath();
        File file = downloadMeetingFile(dmService, itemRev, meetingMinutesPath); // 通过teams代理的方式下载会议附件
        if (file == null) {
            return false;
        }
        TCUtils.refreshObject(dmService, new ModelObject[]{itemRev});
        ModelObject[] objs = TCUtils.getPropModelObjectArray(dmService, itemRev, TCItemConstant.REL_IMAN_SPECIFICATION);
        String dsName = file.getName();
        String fileName = file.getAbsolutePath();
        String type = null;
        String relationType = null;
        String refName = null;
        if (dsName.endsWith(TCDatasetEnum.MSExcel.fileExtensions())) {
            type = TCDatasetEnum.MSExcel.type();
            relationType = TCDatasetEnum.MSExcel.relationType();
            refName = TCDatasetEnum.MSExcel.refName();
        } else if (dsName.endsWith(TCDatasetEnum.MSExcelX.fileExtensions())) {
            type = TCDatasetEnum.MSExcelX.type();
            relationType = TCDatasetEnum.MSExcelX.relationType();
            refName = TCDatasetEnum.MSExcelX.refName();
        }

        if (StringUtil.isEmpty(type) || StringUtil.isEmpty(refName)) {
            throw new Exception("==>> 会议名称为: " + meetingMinutesPath + ", 不符合上传规范");
        }

        ModelObject meetingDataset = TCUtils.checkModelObjExist(dmService, objs, dsName, type, TCItemConstant.PROPERTY_OBJECT_NAME);
        if (null == meetingDataset) {
            meetingDataset = DatasetUtil.createDataset(dmService, itemRev, dsName, type, relationType);
        } else {
            DatasetUtil.removeFileFromDataset(dmService, (Dataset) meetingDataset, refName);
        }

        DatasetUtil.addDatasetFile(fmUtility, dmService, (Dataset) meetingDataset, fileName, refName, false);

        return true;
    }


    /**
     * 会议附件上传失败，发送邮件给会议主owner邮件提醒
     * @param meetBean
     * @throws Exception
     */
    private void sendFileUploadFailureMail(MeetBean meetBean) {
        String meetMainOwner = meetBean.getMeetMainOwner();
        if (StringUtil.isEmpty(meetMainOwner)) {
            return;
        }

        List<String> convertOwners = ScheduleTaskUtil.convertOwners(meetMainOwner);
        if (CollUtil.isEmpty(convertOwners)) {
            return;
        }

        String to = "";
        String userName = "";
        String tcUserId = "";
        for (String str : convertOwners) {
            TCUserBean tcUserInfo = tcfrMapper.getTCUserInfo(str);
            if (null == tcUserInfo) {
                continue;
            }
            userName += tcUserInfo.getWorkId() + "(" + tcUserInfo.getWorkName() + ")" + ",";
            tcUserId += tcUserInfo.getTcUserId() + ",";
            to += str + ",";
        }

        if ("".equals(userName)) {
            return;
        }

        if ("".equals(to)) {
            return;
        }

        userName = userName.substring(0, userName.length() - 1);
        tcUserId = tcUserId.substring(0, tcUserId.length() - 1);
        to = to.substring(0, to.length() - 1);

//        String documentName = TCUtils.getPropStr(dmService, itemRev, TCItemConstant.PROPERTY_ITEM_ID) + "|" + TCUtils.getPropStr(dmService, itemRev, TCItemConstant.PROPERTY_OBJECT_NAME);
        Map<String, String> httpmap = new HashMap<>();
        httpmap.put("sendTo", to);
//        httpmap.put("sendTo", "hua-sheng.yu@foxconn.com");
//        httpmap.put("sendCc", "hua-sheng.yu@foxconn.com");
        httpmap.put("subject", "TCFR會議記錄自動歸檔至TC系統失敗通知");
        String msg = "<html><head></head><body>"
                + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "Dear " + userName + "</div><br/>"
                + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "以下TCFR會議記錄自動歸檔至TC系統失敗，请登陆下方Teamcenter賬號進行查看，谢谢！" + "</div>"
                + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>Teamcenter账号：</strong>" + tcUserId + "</div>"
                + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>會議名称:</strong>" + meetBean.getMeetingTitle() + "</div>"
                + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>歸檔日期：</strong>" + new SimpleDateFormat(format).format(new Date()) + "</div>"
                + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>任务路径:</strong>" + "D事業群企業知識庫/專案知識庫" + "</div>"
                + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>专案系列:</strong>" + meetBean.getSpasSeries() + "</div>"
                + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>专案名:</strong>" + meetBean.getProjectName() + "</div>"
                + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>专案阶段:</strong>" + meetBean.getSpasProjPhase() + "</div>"
                + "</body></html>";

        httpmap.put("htmlmsg", msg);
        Gson gson = new Gson();
        String data = gson.toJson(httpmap);
        String result = tcMailClient.sendMail3Method(data);// 发送邮件
        log.info("==>> result: " + result);
    }


    /**
     * 下载会议附件到本地
     *
     * @param filePath
     * @return
     * @throws Exception
     */
    private File downloadMeetingFile(DataManagementService dmService, ItemRevision itemRev, String filePath) throws Exception {
        log.info("==>> filePath: " + filePath);
        String documentItemId = TCUtils.getPropStr(dmService, itemRev, TCItemConstant.PROPERTY_ITEM_ID);
        String documentVersion = TCUtils.getPropStr(dmService, itemRev, TCItemConstant.PROPETY_ITEM_REVISION_ID);
        String key = documentItemId + "/" + documentVersion + "_" + filePath;

        JSONObject paramsObject = new JSONObject();
        paramsObject.put("meetingMinutesPath", filePath);

        List<String> ipList = IpUtil.getRealIP();
        ipList.removeIf(str -> !str.startsWith("10.203") && !str.startsWith("10.205"));
        String localIP = ipList.get(0);
        System.out.println("==>> localIP: " + localIP);
        String proxyHost = null;
        if (localIP.startsWith("10.203")) {
            proxyHost = tcfrProxyConfig.getServerProxyIp();
        } else if (localIP.startsWith("10.205")) {
            proxyHost = tcfrProxyConfig.getClientProxtIp();
        }

        String url = tcfrProxyConfig.getMeetingFileParamsUrl();
        InputStream in = HttpUtil.httpPostFileInputStream(url, null, paramsObject.toJSONString(), "Y", 15000, proxyHost, tcfrProxyConfig.getPort()); // 通过代理的方式获取文件流
//        InputStream in = MinIoUtils.getObject(minioClient, config.getBucketName(), filePath); // 通过minio的方式获取文件流
        if (null == in) {
            throw new Exception("Teams代理获取指定文件流出錯，錯誤信息:");
        }
        String dir = FileUtil.getFilePath(TCFRConstant.TCFRFOLDER);
        log.info("==>> dir: " + dir);

        FileUtil.deletefile(dir); // 删除文件夹下面的所有文件
        String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
        File file = FileUtil.downloadFileByStream(in, dir, fileName);
        if (null == file) {
            throw new Exception("会议路径为: " + filePath + ", 下载失败");
        }

        if (redisService.getCacheMapValue(TCFRConstant.SYNCMEETINGFILEREDISKEY, key) == null) {
            redisService.setCacheMapValue(TCFRConstant.SYNCMEETINGFILEREDISKEY, key, file); // 第一次排程将文件存放到redis缓存数据库中
            return null;
        } else {
            return file;
        }
    }


    /**
     * 启动流程
     *
     * @param wfService
     * @param dmService
     * @param itemRev
     * @throws NotLoadedException
     * @throws ServiceException
     */
    private void startWorkFlow(WorkflowService wfService, DataManagementService dmService, ItemRevision itemRev) throws NotLoadedException, ServiceException {
        String documentName = TCUtils.getPropStr(dmService, itemRev, TCItemConstant.PROPERTY_ITEM_ID) + "|" + TCUtils.getPropStr(dmService, itemRev, TCItemConstant.PROPERTY_OBJECT_NAME);
        ModelObject[] objects = TCUtils.getPropModelObjectArray(dmService, itemRev, TCItemConstant.REL_IMAN_SPECIFICATION);
        List<ModelObject> list = new ArrayList<>();
        if (ArrayUtil.isNotEmpty(objects)) {
            list = Convert.convert(new TypeReference<>() {}, objects);
        }
        list.add(itemRev);

        ModelObject[] totalObjs = Convert.convert(new TypeReference<>() {}, list);
        TCUtils.createNewProcess(wfService, documentName, TCFRConstant.FXN25_TCFR_FAST_RELEASE, totalObjs); // 启动快速发行流程
    }
}
