package com.foxconn.plm.integrate.lbs.scheduling;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.foxconn.plm.entity.constants.TCDatasetEnum;
import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.entity.exception.BizException;
import com.foxconn.plm.integrate.config.MinIoClientConfig;
import com.foxconn.plm.integrate.lbs.domain.SyncRp;
import com.foxconn.plm.integrate.lbs.entity.LbsSyncEntity;
import com.foxconn.plm.integrate.lbs.mapper.LbsSyncMapper;
import com.foxconn.plm.integrate.spas.domain.D9Constants;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.minio.MinIoUtils;
import com.foxconn.plm.utils.tc.DatasetUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.core._2008_06.DataManagement;
import com.teamcenter.services.strong.core._2015_07.DataManagement.CreateIn2;
import com.teamcenter.services.strong.query.SavedQueryService;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.Dataset;
import com.teamcenter.soa.client.model.strong.Folder;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import com.teamcenter.soa.client.model.strong.WorkspaceObject;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import feign.Util;
import io.minio.MinioClient;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class LbsSyncScheduling {
    @Resource
    private LbsSyncMapper mapper;
    @Resource(name = "LbsMinioClient")
    private MinioClient minioClient;
    @Resource
    private MinIoClientConfig config;

    @XxlJob("lbsSyncScheduling")
    public void dealData() {
        XxlJobHelper.log("lbs文件同步定时任务开始");
        DateTime yesterday = DateUtil.yesterday();
        DateTime startTime = DateUtil.beginOfDay(yesterday);
        DateTime endTime = DateUtil.endOfDay(yesterday);
        List<LbsSyncEntity> list = mapper.getByTime(startTime, endTime);
        if (CollUtil.isEmpty(list)) {
            XxlJobHelper.log("未查询到需要同步的数据");
            return;
        }
        for (LbsSyncEntity entity : list) {
            String fileName = entity.getFileName();
            InputStream in = null;
            ByteArrayOutputStream out = null;
            try {
                XxlJobHelper.log("开始同步文件：" + fileName);
                in = MinIoUtils.getObject(minioClient, config.getBucketName(), fileName);
                out = new ByteArrayOutputStream();
                IoUtil.copy(in, out);
                MockMultipartFile file = new MockMultipartFile(fileName, fileName, null, out.toByteArray());
                SyncRp rp = new SyncRp();
                BeanUtil.copyProperties(entity, rp);
                rp.setExcel(file);
                // 執行同步文件操作
                sync(rp);
                // 更新數據庫記錄
                mapper.updateById(entity.getId());
                XxlJobHelper.log("文件：" + fileName + "同步完成");
            } catch (Exception e) {
                XxlJobHelper.handleFail("執行文件傳輸出錯");
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        XxlJobHelper.handleFail("關閉輸入流出錯");
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        XxlJobHelper.handleFail("關閉輸出流出錯");
                    }
                }
            }
        }
        XxlJobHelper.log("lbs文件同步定时任务执行完成");
    }


    private void sync(SyncRp rp) {
        int requestId = RandomUtil.randomInt(10000000, 20000000);
        MultipartFile excel = rp.getExcel();
        LogFactory.get().info(String.format("LBS 请求Id(%d) 开始同步参数：rev(%s),projName(%s),spasId(%s),phase(%s),excel(%s,size=%d byte),changList(%s)", requestId, rp.getRev(), rp.getProjName(), rp.getSpasId(), rp.getSpasPhase(), excel != null ? excel.getOriginalFilename() : "null", excel != null ? excel.getSize() : 0, rp.getChangList()));
        File tempExcel = null;
        File tempZip = null;
        TCSOAServiceFactory tCSOAServiceFactory = null;
        try {

            excel = rp.getExcel();
            if (excel == null) {
                throw new BizException("缺少Excel");
            }

            String excelName = excel.getOriginalFilename();
            assert excelName != null;
            if (!excelName.endsWith(".xls") && !excelName.endsWith(".xlsx")) {
                throw new BizException("Excel类型不对");
            }
            String[] split = excelName.split("\\.");
            String excelType = split[split.length - 1];
            byte[] bytes = excel.getBytes();
            tempExcel = new File(excelName);
            FileUtil.writeBytes(bytes, tempExcel);

            MultipartFile zip = rp.getZip();
            String zipName = null;
            if (zip != null) {
                zipName = zip.getOriginalFilename();
                assert zipName != null;
                if (!zipName.endsWith(".zip")) {
                    throw new BizException("zip类型不对");
                }
                bytes = excel.getBytes();
                tempZip = new File(zipName);
                FileUtil.writeBytes(bytes, tempZip);
            }

            // login ended
            tCSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS3);
            DataManagementService dataManagementService = tCSOAServiceFactory.getDataManagementService();
            Folder projectFolder = findProjectFolder(rp.getSpasId(), rp.getProjName(), tCSOAServiceFactory.getSavedQueryService());
            if (projectFolder == null) {
                throw new Exception(String.format("Fatal Error：Project (%s) not found.", rp.getSpasId()));
            }

            String phase = rp.getSpasPhase();
            if (phase.contains("(")) {
                int index = phase.indexOf("(");
                phase = phase.substring(0, index);
            }
            Folder referencesFolder = findOrCreateReferencesFolder(projectFolder, phase, dataManagementService);
            if (referencesFolder == null) {
                throw new Exception("Fatal Error: not found references Folder");
            }

            TCUtils.getProperty(dataManagementService, projectFolder, "object_name");
            String folderName = projectFolder.get_object_name();

            int changListRev = 0;
            TCUtils.getProperty(dataManagementService, referencesFolder, "contents");
            WorkspaceObject[] contents = referencesFolder.get_contents();
            for (WorkspaceObject refFolderChild : contents) {
                TCUtils.getProperty(dataManagementService, refFolderChild, "object_name");
                String object_name = refFolderChild.get_object_name();
                if (object_name.startsWith("LBS Chang List")) {
                    changListRev++;
                }
            }
            System.out.println(changListRev);

            changListRev++;
            HashMap<String, String> propMap = new HashMap<>();
            propMap.put("object_name", String.format("LBS Chang List %02d (%s)", changListRev, DateUtil.format(new Date(), "yyyy-MM-dd HH:mm")));
            propMap.put("object_desc", String.format("%s-%s-%s", folderName, phase, rp.getRev()));
            DataManagement.CreateResponse response = TCUtils.createObjects(dataManagementService, "D9_ChangeList", propMap);

            ServiceData serviceData = response.serviceData;
            if (serviceData.sizeOfPartialErrors() > 0) {
                throw new Exception("Fatal Error: create DCN failed!");
            }
            ModelObject[] items = response.output[0].objects;
            ModelObject item = items[0];
            TCUtils.addContents(dataManagementService, referencesFolder, item);
            ItemRevision itemRevision = (ItemRevision) items[2];
            TCUtils.getProperty(dataManagementService, itemRevision, "IMAN_master_form_rev");
            ModelObject[] iman_master_form_rev = itemRevision.get_IMAN_master_form_rev();
            ModelObject revForm = iman_master_form_rev[0];

            JSONArray jsonArray = JSONObject.parseArray(rp.getChangList());
            CreateIn2[] createIn2s = new CreateIn2[jsonArray.size()];
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                CreateIn2 createIn2 = new CreateIn2();
                createIn2.createData.boName = "D9_ChangeListTableRow";
                createIn2.createData.propertyNameValues.put("d9_Function", new String[]{jsonObject.getString("d9_Function")});
                createIn2.createData.propertyNameValues.put("d9_Introducer", new String[]{jsonObject.getString("d9_Introducer")});
                createIn2.createData.propertyNameValues.put("d9_WorkID", new String[]{jsonObject.getString("d9_WorkID")});
                createIn2.createData.propertyNameValues.put("d9_Telephone", new String[]{jsonObject.getString("d9_Telephone")});
                createIn2.createData.propertyNameValues.put("d9_ProposedDate", new String[]{jsonObject.getString("d9_ProposedDate")});
                createIn2.createData.propertyNameValues.put("d9_ChangeReason", new String[]{jsonObject.getString("d9_ChangeReason")});
                createIn2.createData.propertyNameValues.put("d9_ChangeList", new String[]{jsonObject.getString("d9_ChangeList")});
                createIn2.createData.propertyNameValues.put("d9_Replier", new String[]{jsonObject.getString("d9_Replier")});
                createIn2.createData.propertyNameValues.put("d9_ReplyDate", new String[]{jsonObject.getString("d9_ReplyDate")});
                createIn2.createData.propertyNameValues.put("d9_EEReply", new String[]{jsonObject.getString("d9_EEReply")});
                createIn2.createData.propertyNameValues.put("d9_Reply", new String[]{jsonObject.getString("d9_Reply")});
                createIn2.createData.propertyNameValues.put("d9_Remark", new String[]{jsonObject.getString("d9_Remark")});
                createIn2.pasteProp = "d9_ChangeListTable";
                createIn2.targetObject = revForm;
                createIn2s[i] = createIn2;
            }

            response = dataManagementService.createRelateAndSubmitObjects2(createIn2s);
            serviceData = response.serviceData;
            if (serviceData.sizeOfPartialErrors() > 0) {
                String[] messages = serviceData.getPartialError(0).getMessages();
                throw new Exception("Fatal Error: write changList failed.cause by: " + messages[0]);
            }

            String type = "xlsx".equalsIgnoreCase(excelType) ? TCDatasetEnum.MSExcelX.type() : TCDatasetEnum.MSExcel.type();
            Dataset dataset = DatasetUtil.createDataset(dataManagementService, itemRevision, excelName, type, "IMAN_specification");
            TCUtils.getProperty(dataManagementService, dataset, "ref_list");
            Boolean check = DatasetUtil.addDatasetFile(tCSOAServiceFactory.getFileManagementUtility(), // 数据集添加物理文件
                    dataManagementService, dataset, tempExcel.getAbsolutePath(), "excel", false);
            if (!check) {
                throw new Exception("数据集添加附件失败1...");
            }
            if (zip != null) {
                dataset = DatasetUtil.createDataset(dataManagementService, itemRevision, zipName, TCDatasetEnum.ZIP.type(), "IMAN_specification");
                TCUtils.getProperty(dataManagementService, dataset, "ref_list");
                check = DatasetUtil.addDatasetFile(tCSOAServiceFactory.getFileManagementUtility(), // 数据集添加物理文件
                        dataManagementService, dataset, tempZip.getAbsolutePath(), TCDatasetEnum.ZIP.refName(), false);
                if (!check) {
                    throw new Exception("数据集添加附件失败2...");
                }
            }
            TCUtils.createNewProcess(tCSOAServiceFactory.getWorkflowService(), "LBS Change List Release", "TCM Release Process", new ModelObject[]{itemRevision, revForm, dataset});
            LogFactory.get().info(String.format("LBS 请求Id(%d) 同步成功.", requestId));

        } catch (Exception e) {
            String message = e.getMessage();
            LogFactory.get().info(String.format("LBS 请求Id(%d) 同步出错：原因(%s)", requestId, message));
            if (Util.isBlank(message)) {
                LogFactory.get().info(e);
            }
            throw new BizException(HttpResultEnum.SERVER_ERROR.getCode(), message);
        } finally {
            try {
                if (tCSOAServiceFactory != null) {
                    tCSOAServiceFactory.logout();
                }
            } catch (Exception e) {
            }
            if (tempExcel != null) {
                tempExcel.delete();

            }
            if (tempZip != null) {
                tempZip.delete();
            }
        }
    }

    private Folder findProjectFolder(String spasId, String projName, SavedQueryService queryService) throws Exception {
        Map<String, Object> queryResults;
        if (StrUtil.isNotEmpty(spasId)) {
            spasId = "p" + spasId;
            queryResults = TCUtils.executeQuery(queryService, "__D9_Find_Project_Folder", new String[]{"d9_SPAS_ID"}, new String[]{spasId});
        } else if (StrUtil.isNotEmpty(projName)) {
            queryResults = TCUtils.executeQuery(queryService, "__D9_Find_Project_Folder", new String[]{"object_name"}, new String[]{projName.trim()});
        } else {
            throw new Exception("projName and spasId Cannot be null at the same time");
        }

        if (queryResults.get("succeeded") != null) {
            ModelObject[] md = (ModelObject[]) queryResults.get("succeeded");
            if (md != null && md.length > 0) {
                return (Folder) md[0];
            }
        }


        return null;
    }

    private Folder findOrCreateReferencesFolder(Folder projectFolder, String phase, DataManagementService dmService) throws Exception {
        TCUtils.getProperty(dmService, projectFolder, "contents");
        WorkspaceObject[] children = projectFolder.get_contents();
        for (WorkspaceObject child : children) {
            TCUtils.getProperty(dmService, child, "object_name");
            String object_name = child.get_object_name();
            if (!"Layout".equals(object_name)) {
                continue;
            }
            Folder layoutFolder = (Folder) child;
            TCUtils.getProperty(dmService, layoutFolder, "contents");
            WorkspaceObject[] layoutChildren = layoutFolder.get_contents();
            Folder phaseFolder = null;
            for (WorkspaceObject layoutChild : layoutChildren) {
                TCUtils.getProperty(dmService, layoutChild, "object_name");
                object_name = layoutChild.get_object_name();
                if (phase.equals(object_name.substring(0, 2))) {
                    phaseFolder = (Folder) layoutChild;
                    break;
                }
            }
            if (phaseFolder == null) {
                phaseFolder = createReferenceFolder(layoutFolder, phase, D9Constants.D9_PHASE, dmService);
            }
            if (phaseFolder == null) {
                return null;
            }
            TCUtils.getProperty(dmService, phaseFolder, "contents");
            WorkspaceObject[] phaseChildren = phaseFolder.get_contents();
            for (WorkspaceObject phaseChild : phaseChildren) {
                TCUtils.getProperty(dmService, phaseChild, "object_name");
                object_name = phaseChild.get_object_name();
                if (!"參考資料".equals(object_name) && !"参考资料".equals(object_name) && (!"References".equalsIgnoreCase(object_name))) {
                    continue;
                }
                return (Folder) phaseChild;
            }
            return createReferenceFolder(phaseFolder, "參考資料", D9Constants.D9_ARCHIVE, dmService);
        }
        return null;
    }

    //创建文件夹
    private Folder createReferenceFolder(Folder parentFolder, String folderName, String folderType, DataManagementService dmService) {
        Folder folder = null;
        Map<String, String> propMap = new HashMap<>();
        propMap.put(D9Constants.OBJECT_NAME, folderName);
        DataManagement.CreateResponse response = TCUtils.createObjects(dmService, folderType, propMap);
        ServiceData serviceData = response.serviceData;
        if (serviceData.sizeOfPartialErrors() <= 0) {
            ModelObject[] folders = response.output[0].objects;
            folder = (Folder) folders[0];
            TCUtils.addContents(dmService, parentFolder, folder);
        }
        return folder;
    }

}
