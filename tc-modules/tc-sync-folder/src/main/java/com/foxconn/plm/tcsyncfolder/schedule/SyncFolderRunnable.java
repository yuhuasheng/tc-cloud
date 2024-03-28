package com.foxconn.plm.tcsyncfolder.schedule;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.foxconn.plm.feign.service.TcMailClient;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.tcsyncfolder.entity.*;
import com.foxconn.plm.tcsyncfolder.mapper.TcProjectMapper;
import com.foxconn.plm.tcsyncfolder.service.*;
import com.foxconn.plm.tcsyncfolder.vo.*;
import com.foxconn.plm.utils.tc.ProjectUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.Item;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import com.teamcenter.soa.client.model.strong.TC_Project;
import com.teamcenter.soa.exceptions.NotLoadedException;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * @ClassName: SyncFolderRunnable
 * @Description:
 * @Author DY
 * @Create 2023/3/27
 */
public class SyncFolderRunnable implements Runnable {
    private static Log log = LogFactory.get();

    private ProjectVo projectVo;
    private CountDownLatch countDownLatch;
    private Map<String, Set<String>> noAccountDeptMap;
    private String bu;
    private TCSOAServiceFactory tcsoaServiceFactory;
    private TcProjectMapper tcProjectMapper;
    private FolderService folderService;
    private ProjectService projectService;
    private FolderRefService folderRefService;
    private DocumentRevService documentRevService;
    private DocumentService documentService;
    private TC_Project projectObj;
    private TcMailClient tcMailClient;
    private String env;


    public SyncFolderRunnable(ProjectVo projectVo, CountDownLatch countDownLatch, Map<String, Set<String>> noAccountDeptMap,
                              TcProjectMapper tcProjectMapper, String bu, TCSOAServiceFactory tcsoaServiceFactory, TC_Project projectObj,
                              String env) {
        this.projectVo = projectVo;
        this.countDownLatch = countDownLatch;
        this.noAccountDeptMap = noAccountDeptMap;
        this.tcProjectMapper = tcProjectMapper;
        this.bu = bu;
        this.tcsoaServiceFactory = tcsoaServiceFactory;
        this.folderService = SpringUtil.getBean(FolderService.class);
        this.projectService = SpringUtil.getBean(ProjectService.class);
        this.folderRefService = SpringUtil.getBean(FolderRefService.class);
        this.documentRevService = SpringUtil.getBean(DocumentRevService.class);
        this.documentService = SpringUtil.getBean(DocumentService.class);
        this.projectObj = projectObj;
        this.tcMailClient = SpringUtil.getBean(TcMailClient.class);
        this.env = env;
    }

    @Override
    public void run() {
        log.info(" -----> 开始同步" + projectVo.getProjectName() + "【" + projectVo.getSpasId() + "】的专案信息");
        try {
            // 查询专案信息
            ProjectEntity projectEntity = projectService.getOne(new QueryWrapper<ProjectEntity>().lambda()
                    .eq(ProjectEntity::getProjSpasId, projectVo.getSpasId())
                    .eq(ProjectEntity::getRefId, projectVo.getPuid())
            );
            if (ObjectUtil.isNull(projectEntity)) {
                // 第一次同步，创建所有信息
                initProject(projectObj);
                return;
            }
            if (!projectVo.getProjectName().equals(projectEntity.getProjName())) {
                projectEntity.setProjName(projectVo.getProjectName());
                projectService.updateById(projectEntity);
            }
            // 查询顶层文件夹
            FolderEntity folderEntity = folderService.getById(projectEntity.getFolderId());
            if (!projectVo.getProjectName().equals(folderEntity.getFldName())) {
                folderEntity.setFldName(projectVo.getProjectName());
                folderEntity.setModified("spas");
                folderService.updateById(folderEntity);
            }

            // 查询TC中的所有下级文件夹
            List<FolderVo> deptFolder = tcProjectMapper.getChildFolder(projectVo.getPuid());
            Set<String> deptIds = deptFolder.parallelStream().map(FolderVo::getPuid).collect(Collectors.toSet());
            // 查询外挂系统中的下级文件夹
            List<FolderEntity> childFolder = folderService.getChildFolder(folderEntity.getFldSn());
            // 过滤出不在部门里面的数据
            List<Integer> ids = childFolder.parallelStream().filter(item -> !deptIds.contains(item.getRefId())).map(FolderEntity::getFldSn).collect(Collectors.toList());
            // 删除所有文件夹及父子文件夹的关联关系
            delAllFolder(ids);
            // 新增或者修改文件夹
            for (FolderVo folderVo : deptFolder) {
                boolean flag = true;
                Set<String> list = noAccountDeptMap.get(bu);
                if (CollUtil.isNotEmpty(list) && list.contains(folderVo.getFolderName())) {
                    // 表示从外挂系统同步到TC系统
                    flag = false;
                }
                // 过滤外挂系统中的部门文件夹
                List<FolderEntity> collect = childFolder.parallelStream().filter(item -> folderVo.getPuid().equals(item.getRefId())).collect(Collectors.toList());
                FolderEntity entity = null;
                // 部门文件夹只能从TC同步到外挂系统
                if (CollUtil.isEmpty(collect)) {
                    // 新增
                    entity = addFolder(folderVo, flag, folderEntity.getFldSn());
                } else {
                    // 修改
                    entity = collect.get(0);
                    if (!entity.getFldName().equals(folderVo.getFolderName())) {
                        entity.setFldName(folderVo.getFolderName());
                        entity.setModified("spas");
                    }
                    if (flag) {
                        entity.setRefType(1);
                    } else {
                        entity.setRefType(0);
                    }
                    folderService.updateById(entity);
                }
                // 同步子文件夹和子文件夹下的数据
                syncChildFolder(folderVo, entity, flag);
            }
            log.info(" -----> " + projectVo.getProjectName() + "【" + projectVo.getSpasId() + "】的专案信息同步完成");
        } catch (Exception e) {
            log.error("同步" + projectVo.getProjectName() + "【" + projectVo.getSpasId() + "】的专案信息出错", e);
            // 发送邮件通知
            sendMail(e);
        } finally {
            countDownLatch.countDown();
        }
    }

    private void sendMail(Exception e) {
        JSONObject obj = JSONUtil.createObj();
        obj.set("sendTo", "leky.p.li@foxconn.com,mindy.m.wu@foxconn.com,dane.d.wu@foxconn.com,cheryl.l.wang@foxconn.com,ye.dong@foxconn.com");
        obj.set("sendCc", "thomas.l.yang@foxconn.com");
        obj.set("subject", "专案【" + projectVo.getProjectName() + "】同步出错");
        StringBuilder sb = new StringBuilder();
        if ("prod".equalsIgnoreCase(env)) {
            sb.append("生产环境");
        } else if ("local".equalsIgnoreCase(env)) {
            sb.append("UAT环境");
        } else {
            sb.append("DEV环境");
        }
        sb.append("同步专案").append(projectVo.getProjectName()).append("【")
                .append(projectVo.getSpasId()).append("】异常，错误信息如下：").append(e.getMessage());
        obj.set("htmlmsg", sb.toString());
        tcMailClient.sendMail3Method(JSONUtil.toJsonStr(obj));
    }

    /**
     * 初始化项目及文件夹、文档、文档版本、版本状态信息
     */
    private void initProject(TC_Project projectObj) {
        // 创建顶层文件夹
        FolderEntity root = new FolderEntity();
        root.setFldSn(folderService.getId());
        root.setRefId(projectVo.getPuid());
        root.setFldName(projectVo.getProjectName());
        root.setCreator("spas");
        root.setRefType(1);
        folderService.save(root);
        // 新建项目信息
        ProjectEntity projectEntity = new ProjectEntity();
        projectEntity.setProjSn(projectService.getId());
        projectEntity.setProjSpasId(projectVo.getSpasId());
        projectEntity.setProjName(projectVo.getProjectName());
        projectEntity.setRefId(projectVo.getPuid());
        projectEntity.setActiveFlag(0);
        projectEntity.setFolderId(root.getFldSn());
        projectService.save(projectEntity);

        // 查询TC中的所有下级文件夹
        List<FolderVo> deptFolder = tcProjectMapper.getChildFolder(projectVo.getPuid());
        for (FolderVo folderVo : deptFolder) {
            boolean flag = true;
            Set<String> list = noAccountDeptMap.get(bu);
            if (CollUtil.isNotEmpty(list) && list.contains(folderVo.getFolderName())) {
                // 表示从外挂系统同步到TC系统
                flag = false;
            }
            // 新增部门文件夹
            FolderEntity entity = addFolder(folderVo, flag, root.getFldSn());
            // 同步子文件夹和子文件夹下的数据，只从TC同步到外挂系统
            syncChildFolder(folderVo, entity, true);
        }


    }

    /**
     * 同步子文件夹
     *
     * @param folderVo     父文件夹在TC中的puid和名称
     * @param parentFolder 外挂系统父文件夹的对象信息
     * @param flag         是否从TC同步到外挂系统,true表示从TC同步到外挂系统，false表示从外挂系统同步到TC
     */
    private void syncChildFolder(FolderVo folderVo, FolderEntity parentFolder, boolean flag) {
        // 判断当前文件夹是否有文件需要同步
        if ("D9_ArchiveFolder".equals(folderVo.getFolderType())) {
            // 从外挂系统往TC同步文档及文档版本
            syncDocumentAndVersion(folderVo, parentFolder, flag);
        }
        // 查询当前文件夹的子文件夹
        List<FolderVo> tcChildrenFolders = tcProjectMapper.getChildFolder(folderVo.getPuid());
        List<FolderEntity> childrenFolders = folderService.getChildFolder(parentFolder.getFldSn());
        if (CollUtil.isEmpty(tcChildrenFolders) && CollUtil.isEmpty(childrenFolders)) {
            return;
        }
        if (flag) {
            syncFolderFromTC(tcChildrenFolders, childrenFolders, parentFolder.getFldSn(), flag);
        } else {
            // 从外挂系统往TC同步，以外挂系统为准
            syncFolderToTC(tcChildrenFolders, childrenFolders, folderVo.getPuid(), flag);
        }

    }

    /**
     * 讲外挂系统中的文件夹同步到TC中
     *
     * @param tcChildrenFolders
     * @param childrenFolders
     * @param pUid
     * @param flag
     */
    private void syncFolderToTC(List<FolderVo> tcChildrenFolders, List<FolderEntity> childrenFolders, String pUid, boolean flag) {
        Set<String> tcUid = childrenFolders.parallelStream().map(FolderEntity::getRefId).collect(Collectors.toSet());
        List<String> uids = tcChildrenFolders.parallelStream().filter(item -> !tcUid.contains(item.getPuid()))
                .map(FolderVo::getPuid).collect(Collectors.toList());
        for (String uid : uids) {
            // 删除文件夹
            ModelObject object = TCUtils.findObjectByUid(tcsoaServiceFactory.getDataManagementService(), uid);
            if (ObjectUtil.isNotNull(object)) {
                TCUtils.deleteFolder(tcsoaServiceFactory.getDataManagementService(), new ModelObject[]{object});
            }
        }
        for (FolderEntity childrenFolder : childrenFolders) {
            List<FolderVo> collect = tcChildrenFolders.parallelStream().filter(item -> item.getPuid().equals(childrenFolder.getRefId())).collect(Collectors.toList());
            if (CollUtil.isEmpty(collect)) {
                // 新增文件夹
                String tcFolderId = TCUtils.createTCFolder(tcsoaServiceFactory.getDataManagementService(), pUid, childrenFolder.getFldName(),childrenFolder.getFldDesc());
                childrenFolder.setRefId(tcFolderId);
                folderService.updateById(childrenFolder);
                FolderVo vo = tcProjectMapper.getByUid(tcFolderId);
                if (ObjectUtil.isNotNull(vo)) {
                    syncChildFolder(vo, childrenFolder, flag);
                }
            } else {
                // 修改文件夹名称
                if (!collect.get(0).getFolderName().equals(childrenFolder.getFldName())) {
                    ModelObject obj = TCUtils.findObjectByUid(tcsoaServiceFactory.getDataManagementService(), collect.get(0).getPuid());
                    if (obj != null) {
                        TCUtils.setProperties(tcsoaServiceFactory.getDataManagementService(), obj, "object_name", childrenFolder.getFldName());
                    }
                }
                boolean descFlag = (StrUtil.isNotBlank(childrenFolder.getFldDesc()) && !childrenFolder.getFldDesc().equals(collect.get(0).getFolderDesc()))
                        || (StrUtil.isNotBlank(collect.get(0).getFolderDesc()) && !collect.get(0).getFolderDesc().equals(childrenFolder.getFldDesc()));
                if (descFlag) {
                    ModelObject obj = TCUtils.findObjectByUid(tcsoaServiceFactory.getDataManagementService(), collect.get(0).getPuid());
                    if (obj != null) {
                        TCUtils.setProperties(tcsoaServiceFactory.getDataManagementService(), obj, "object_desc", StrUtil.isNotBlank(childrenFolder.getFldDesc()) ? childrenFolder.getFldDesc() : "");
                    }
                }
                syncChildFolder(collect.get(0), childrenFolder, flag);
            }
        }
    }

    /**
     * 将TC中的文件夹同步到外挂系统中
     *
     * @param tcChildrenFolders
     * @param childrenFolders
     * @param fldSn
     * @param flag
     */
    private void syncFolderFromTC(List<FolderVo> tcChildrenFolders, List<FolderEntity> childrenFolders, Integer fldSn, boolean flag) {
        Set<String> tcUid = tcChildrenFolders.parallelStream().map(FolderVo::getPuid).collect(Collectors.toSet());
        List<Integer> ids = childrenFolders.parallelStream().filter(item -> !tcUid.contains(item.getRefId())).map(FolderEntity::getFldSn).collect(Collectors.toList());
        delAllFolder(ids);
        for (FolderVo itemFolder : tcChildrenFolders) {
            List<FolderEntity> collect = childrenFolders.parallelStream()
                    .filter(item -> itemFolder.getPuid().equals(item.getRefId())).collect(Collectors.toList());
            FolderEntity entity = null;
            if (CollUtil.isEmpty(collect)) {
                // 新增
                entity = addFolder(itemFolder, flag, fldSn);
            } else {
                entity = collect.get(0);
                boolean updateFlag = false;
                if (!entity.getFldName().equals(itemFolder.getFolderName())) {
                    entity.setFldName(itemFolder.getFolderName());
                    entity.setModified("spas");
                    updateFlag = true;
                }
                boolean changeDesc = (StrUtil.isNotBlank(itemFolder.getFolderDesc()) && !itemFolder.getFolderDesc().equals(entity.getFldDesc())) ||
                        (StrUtil.isNotBlank(entity.getFldDesc()) && !entity.getFldDesc().equals(itemFolder.getFolderDesc()));
                if (changeDesc) {
                    entity.setFldDesc(StrUtil.isNotBlank(itemFolder.getFolderDesc()) ? itemFolder.getFolderDesc() : "");
                    entity.setModified("spas");
                    updateFlag = true;
                }
                if (updateFlag) {
                    folderService.updateById(entity);
                }
            }
            syncChildFolder(itemFolder, entity, flag);
        }
    }

    /**
     * 同步文档、文档版本、发布状态
     *
     * @param itemFolder Archive Folder文件夹信息
     * @param entity     外挂系统对应的文件夹信息
     * @param flag       同步方向，true表示从TC同步到外挂系统，false表示从外挂系统同步到TC
     */
    private void syncDocumentAndVersion(FolderVo itemFolder, FolderEntity entity, boolean flag) {
        // 查询TC中文件夹下的文档对象或者版本对象
        List<DocumentVo> documentList = getTcDocumentList(itemFolder.getPuid());
        // 查询外挂系统中的版本对象
        List<DocumentRevEntity> list = documentRevService.list(new QueryWrapper<DocumentRevEntity>().lambda()
                .eq(DocumentRevEntity::getFldId, entity.getFldSn())
        );
        if (CollUtil.isEmpty(documentList) && CollUtil.isEmpty(list)) {
            return;
        }
        if (flag) {
            // 将TC中的文件同步到外挂系统中
            // 先删除多余的文件
            Set<String> idSet = new HashSet<>();
            for (DocumentVo documentVo : documentList) {
                for (DocumentRevisionVo revisionVo : documentVo.getDocumentRevisionVoList()) {
                    idSet.add(revisionVo.getDocumentRevisionUid());
                }
            }
            List<Integer> ids = list.parallelStream().filter(item -> !idSet.contains(item.getRefId()))
                    .map(DocumentRevEntity::getRevSn).collect(Collectors.toList());
            documentRevService.removeByIds(ids);
            List<DocumentRevEntity> collect = list.parallelStream().filter(item -> idSet.contains(item.getRefId())).collect(Collectors.toList());
            for (DocumentVo documentVo : documentList) {
                for (DocumentRevisionVo revisionVo : documentVo.getDocumentRevisionVoList()) {
                    try {
                        syncDocumentFromTC(revisionVo, documentVo, collect, entity.getFldSn());
                    } catch (Exception e) {
                        log.error("TC同步文档到外挂系统出错，错误数据为 ：" + JSONUtil.toJsonStr(documentVo) +
                                "-------->父文件夹信息:" + JSONUtil.toJsonStr(entity));
                    }
                }
            }
        } else {
            // 讲外挂系统中的文件同步到TC中
            // 删除TC中的文件关联关系
            Set<String> idSet = list.parallelStream().map(DocumentRevEntity::getRefId).collect(Collectors.toSet());
            List<DocumentVo> collect = new ArrayList<>();
            for (DocumentVo documentVo : documentList) {
                for (DocumentRevisionVo revisionVo : documentVo.getDocumentRevisionVoList()) {
                    if (idSet.contains(revisionVo.getDocumentRevisionUid())) {
                        collect.add(documentVo);
                    } else {
                        // 断开版本和文件夹的关联关系
                        ModelObject object = TCUtils.findObjectByUid(tcsoaServiceFactory.getDataManagementService(), itemFolder.getPuid());
                        ModelObject object1 = TCUtils.findObjectByUid(tcsoaServiceFactory.getDataManagementService(), revisionVo.getDocumentRevisionUid());
                        if (ObjectUtil.isNotNull(object) && ObjectUtil.isNotNull(object1)) {
                            TCUtils.deleteRelation(tcsoaServiceFactory.getDataManagementService(), object, object1, "contents");
                        }
                    }
                }
            }
            for (DocumentRevEntity documentRevEntity : list) {
                try {
                    syncDocumentVersionToTC(documentRevEntity, collect, itemFolder.getPuid());
                } catch (Exception e) {
                    log.error("外挂系统同步文档到TC出错，错误数据为 ：" + JSONUtil.toJsonStr(documentRevEntity) +
                            "-------->父文件夹信息:" + JSONUtil.toJsonStr(entity));
                }
            }
        }
    }

    /**
     * 将外挂系统中的文档版本同步到TC中
     *
     * @param documentRevEntity
     * @param documentList
     * @param pUid
     */
    private void syncDocumentVersionToTC(DocumentRevEntity documentRevEntity, List<DocumentVo> documentList, String pUid) {
        DocumentRevisionVo vo = null;
        for (DocumentVo documentVo : documentList) {
            List<DocumentRevisionVo> collect = documentVo.getDocumentRevisionVoList().parallelStream()
                    .filter(item -> item.getDocumentRevisionUid().equals(documentRevEntity.getRefId()) &&
                            item.getVersion().equals(documentRevEntity.getRevNum()))
                    .collect(Collectors.toList());
            if (CollUtil.isNotEmpty(collect)) {
                vo = collect.get(0);
                break;
            }
        }
        if (ObjectUtil.isNull(vo)) {
            DocumentEntity documentEntity = documentService.getById(documentRevEntity.getDocId());
            // 没有找到，需要同步到TC
            try {
                Item item = null;
                ItemRevision itemRevision = null;
                if (StrUtil.isNotBlank(documentEntity.getRefId())) {
                    // 已经存在文档对象
                    item = (Item) TCUtils.findObjectByUid(tcsoaServiceFactory.getDataManagementService(), documentEntity.getRefId());
                    TCUtils.getProperty(tcsoaServiceFactory.getDataManagementService(), item, "revision_list");
                    TCUtils.refreshObject(tcsoaServiceFactory.getDataManagementService(), item);
                    ModelObject[] revisionList = item.get_revision_list();
                    for (ModelObject object : revisionList) {
                        TCUtils.getProperty(tcsoaServiceFactory.getDataManagementService(), object, "item_revision_id");
                        TCUtils.refreshObject(tcsoaServiceFactory.getDataManagementService(), object);
                        String revisionId = object.getPropertyObject("item_revision_id").getStringValue();
                        if (revisionId.equals(documentRevEntity.getRevNum())) {
                            TCUtils.setProperties(tcsoaServiceFactory.getDataManagementService(), object, "object_name", documentRevEntity.getRevName());
                            itemRevision = (ItemRevision) object;
                        }
                    }
                    if (ObjectUtil.isNull(itemRevision)) {
                        itemRevision = (ItemRevision) TCUtils.reviseItemRev(tcsoaServiceFactory.getDataManagementService(), revisionList[revisionList.length - 1], documentRevEntity.getRevName(), documentRevEntity.getRevNum());
                    }
                } else {
                    // 查询文档对象是否存在，如果存在对比下文档版本信息
                    Map<String, Object> queryResults = TCUtils.executeQuery(tcsoaServiceFactory.getSavedQueryService(), "Item...",
                            new String[]{"item_id"}, new String[]{documentEntity.getDocNum()});
                    if (queryResults.get("succeeded") == null) {
                        throw new Exception("未查询到项目信息");
                    }
                    ModelObject[] objs = (ModelObject[]) queryResults.get("succeeded");
                    if (objs.length > 0) {
                        for (int i = 0; i < objs.length; i++) {
                            TCUtils.getProperty(tcsoaServiceFactory.getDataManagementService(), objs[i], "object_name");
                            TCUtils.refreshObject(tcsoaServiceFactory.getDataManagementService(), objs[i]);
                            String objectName = objs[i].getPropertyObject("object_name").getStringValue();
                            if (documentEntity.getDocName().equals(objectName)) {
                                item = (Item) objs[i];
                                documentEntity.setRefId(item.getUid());
                                documentService.updateById(documentEntity);
                            }
                        }
                    } else {
                        // 不存在文档对象
                        Map<String, String> propMap = new HashMap<>();
                        propMap.put("d9_DocumentType", documentEntity.getDocumentCategory());
                        item = TCUtils.createDocument(tcsoaServiceFactory.getDataManagementService(), documentEntity.getDocNum(),
                                "Document", documentEntity.getDocName(), propMap);
                        documentEntity.setRefId(item.getUid());
                        documentService.updateById(documentEntity);
                    }
                    TCUtils.getProperty(tcsoaServiceFactory.getDataManagementService(), item, "revision_list");
                    TCUtils.refreshObject(tcsoaServiceFactory.getDataManagementService(), item);
                    ModelObject[] revisionList = item.get_revision_list();
                    if ("01".equals(documentRevEntity.getRevNum())) {
                        itemRevision = (ItemRevision) revisionList[revisionList.length - 1];
                        TCUtils.setProperties(tcsoaServiceFactory.getDataManagementService(), itemRevision, "object_name", documentRevEntity.getRevName());
                    } else {
                        itemRevision = (ItemRevision) TCUtils.reviseItemRev(tcsoaServiceFactory.getDataManagementService(), revisionList[revisionList.length - 1], documentRevEntity.getRevName(), documentRevEntity.getRevNum());
                    }
                }
                FileVo fileInfo = documentRevService.getFileInfo(documentRevEntity.getRevSn());
                // TC中itemRevision对象没有d9_UploadDate属性
                //TCUtils.setProperties(tcsoaServiceFactory.getDataManagementService(), itemRevision, "d9_UploadDate", DateUtil.formatLocalDateTime(documentRevEntity.getCreated()));
                if (ObjectUtil.isNotNull(fileInfo)) {
                    // 查询是否发布
                    TCUtils.getProperty(tcsoaServiceFactory.getDataManagementService(), new ModelObject[]{itemRevision}, new String[]{"release_status_list", "IMAN_specification"});
                    TCUtils.refreshObject(tcsoaServiceFactory.getDataManagementService(), itemRevision);
                    List<ModelObject> fileList = itemRevision.getPropertyObject("IMAN_specification").getModelObjectListValue();
                    boolean flag = true;
                    if (CollUtil.isNotEmpty(fileList)) {
                        for (ModelObject object : fileList) {
                            TCUtils.getProperty(tcsoaServiceFactory.getDataManagementService(), object, "object_name");
                            TCUtils.refreshObject(tcsoaServiceFactory.getDataManagementService(), object);
                            String name = object.getPropertyObject("object_name").getStringValue();
                            if (name.equals(fileInfo.getFileName() + "." + fileInfo.getFileType())) {
                                flag = false;
                            }
                        }
                    }
                    if (flag) {
                        TCUtils.createDataset(tcsoaServiceFactory.getDataManagementService(), itemRevision,
                                fileInfo.getFileName() + "." + fileInfo.getFileType(), fileInfo.getFileVersionSn());
                    }
                    List<ModelObject> statusList = itemRevision.getPropertyObject("release_status_list").getModelObjectListValue();
                    if (ObjectUtil.isNotNull(documentRevEntity.getLifecyclePhase()) && documentRevEntity.getLifecyclePhase() == 0 &&
                            CollUtil.isEmpty(statusList)) {
                        String workflowName = "TCM Release Process：" + documentEntity.getDocNum() + "/" + documentRevEntity.getRevNum();
                        TCUtils.createNewProcess(tcsoaServiceFactory.getWorkflowService(), workflowName, "TCM Release Process", new ModelObject[]{itemRevision});
                    }
                }
                ModelObject obj = TCUtils.findObjectByUid(tcsoaServiceFactory.getDataManagementService(), pUid);
                if (ObjectUtil.isNotNull(obj)) {
                    TCUtils.addContents(tcsoaServiceFactory.getDataManagementService(), obj, itemRevision, "contents");
                }
                if (ObjectUtil.isNotNull(projectObj)) {
                    ProjectUtil.assignedProject(tcsoaServiceFactory.getProjectLevelSecurityService(), itemRevision, projectObj);
                }
                documentRevEntity.setRefId(itemRevision.getUid());
                documentRevService.updateById(documentRevEntity);
            } catch (Exception e) {
                log.error("操作TC错误 ---> " + JSONUtil.toJsonStr(documentRevEntity), e);
            }
        } else {
            // 找到了，看是否修改名称或者发布流程
            ModelObject obj = TCUtils.findObjectByUid(tcsoaServiceFactory.getDataManagementService(), vo.getDocumentRevisionUid());
            if (!vo.getDocumentRevisionName().equals(documentRevEntity.getRevName()) && ObjectUtil.isNotNull(obj)) {
                TCUtils.setProperties(tcsoaServiceFactory.getDataManagementService(), obj, "object_name", documentRevEntity.getRevName());
            }
            if (!vo.getRelease().equals(documentRevEntity.getLifecyclePhase()) && ObjectUtil.isNotNull(obj)) {
                try {
                    String workflowName = "TCM Release Process：" + vo.getDocumentId() + "/" + vo.getVersion();
                    TCUtils.createNewProcess(tcsoaServiceFactory.getWorkflowService(), workflowName, "TCM Release Process", new ModelObject[]{obj});
                } catch (Exception e) {
                    log.error("发布流程失败");
                }
            }
        }
    }


    /**
     * 讲文档版本从TC中同步到外挂系统
     *
     * @param revisionVo
     * @param documentVo
     * @param list
     * @param fldSn
     */
    private void syncDocumentFromTC(DocumentRevisionVo revisionVo, DocumentVo documentVo, List<DocumentRevEntity> list, Integer fldSn) {
        List<DocumentRevEntity> collect = list.parallelStream().filter(item -> revisionVo.getDocumentRevisionUid().equals(item.getRefId())).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(collect)) {
            // 可以找到文档版本
            DocumentRevEntity documentRevEntity = collect.get(0);
            DocumentEntity documentEntity = documentService.getById(documentRevEntity.getDocId());
            if (StrUtil.isNotBlank(documentVo.getDocumentUid())) {
                documentEntity.setRefId(documentVo.getDocumentUid());
                if (!documentVo.getDocumentNum().equals(documentEntity.getDocNum())) {
                    documentEntity.setDocNum(documentVo.getDocumentNum());
                }
                if (!documentVo.getDocumentType().equals(documentEntity.getDocumentCategory())) {
                    documentEntity.setDocumentCategory(documentVo.getDocumentType());
                }
                if (!documentVo.getDocumentName().equals(documentEntity.getDocName())) {
                    documentEntity.setDocName(documentVo.getDocumentName());
                }
                if (!documentVo.getOwnUser().getUserId().equals(documentEntity.getCreator())) {
                    documentEntity.setCreator(documentVo.getOwnUser().getUserId());
                }
                if (!documentVo.getOwnUser().getUserName().equals(documentEntity.getCreatorName())) {
                    documentEntity.setCreatorName(documentVo.getOwnUser().getUserName());
                }
            } else {
                if (!revisionVo.getDocumentId().equals(documentEntity.getDocNum())) {
                    documentEntity.setDocNum(revisionVo.getDocumentId());
                }
                if (!revisionVo.getDocumentRevisionName().equals(documentEntity.getDocName())) {
                    documentEntity.setDocName(revisionVo.getDocumentRevisionName());
                }
                if (!revisionVo.getOwnUser().getUserId().equals(documentEntity.getCreator())) {
                    documentEntity.setCreator(revisionVo.getOwnUser().getUserId());
                }
                if (!revisionVo.getOwnUser().getUserName().equals(documentEntity.getCreatorName())) {
                    documentEntity.setCreatorName(revisionVo.getOwnUser().getUserName());
                }
            }
            documentService.updateById(documentEntity);
            if (!revisionVo.getDocumentRevisionName().equals(documentRevEntity.getRevName())) {
                documentRevEntity.setRevName(revisionVo.getDocumentRevisionName());
            }
            if (!revisionVo.getVersion().equals(documentRevEntity.getRevNum())) {
                documentRevEntity.setRevNum(revisionVo.getVersion());
            }
            if (!revisionVo.getOwnUser().getUserId().equals(documentRevEntity.getCreator())) {
                documentRevEntity.setCreator(revisionVo.getOwnUser().getUserId());
            }
            if (!revisionVo.getOwnUser().getUserName().equals(documentRevEntity.getCreatorName())) {
                documentRevEntity.setCreatorName(revisionVo.getOwnUser().getUserName());
            }
            if (!revisionVo.getRelease().equals(documentRevEntity.getLifecyclePhase())) {
                documentRevEntity.setLifecyclePhase(revisionVo.getRelease());
            }
            documentRevService.updateById(documentRevEntity);
        } else {
            // 未找到文档版本,新增
            // 先查询文档是否存在，存在判断是否要改名称，不存在就新建
            String docNum = StrUtil.isNotBlank(documentVo.getDocumentNum()) ? documentVo.getDocumentNum() : revisionVo.getDocumentRevisionName();
            DocumentEntity documentEntity = documentService.getOne(new QueryWrapper<DocumentEntity>().lambda()
                    .eq(StrUtil.isNotBlank(documentVo.getDocumentUid()), DocumentEntity::getRefId, documentVo.getDocumentUid())
                    .eq(DocumentEntity::getDocNum, docNum)
            );
            if (ObjectUtil.isNull(documentEntity)) {
                documentEntity = new DocumentEntity();
                documentEntity.setDocSn(documentService.getId());
                documentEntity.setDocOrigin(1L);
                if (ObjectUtil.isNotNull(documentVo.getDocumentUid())) {
                    documentEntity.setDocNum(documentVo.getDocumentNum());
                    documentEntity.setDocName(documentVo.getDocumentName());
                    documentEntity.setDocumentCategory(documentVo.getDocumentType());
                    documentEntity.setCreator(documentVo.getOwnUser().getUserId());
                    documentEntity.setCreatorName(documentVo.getOwnUser().getUserName());
                    documentEntity.setRefId(documentVo.getDocumentUid());
                } else {
                    documentEntity.setDocNum(revisionVo.getDocumentId());
                    documentEntity.setDocName(revisionVo.getDocumentRevisionName());
                    documentEntity.setCreator(revisionVo.getOwnUser().getUserId());
                    documentEntity.setCreatorName(revisionVo.getOwnUser().getUserName());
                }
                documentService.save(documentEntity);
            } else {
                String documentName = StrUtil.isNotBlank(documentVo.getDocumentName()) ? documentVo.getDocumentName() : revisionVo.getDocumentRevisionName();
                if (!documentEntity.getDocName().equals(documentName)) {
                    documentEntity.setDocName(documentName);
                    documentService.updateById(documentEntity);
                }
            }
            DocumentRevEntity documentRevEntity = new DocumentRevEntity();
            documentRevEntity.setRevSn(documentRevService.getId());
            documentRevEntity.setDocId(documentEntity.getDocSn());
            documentRevEntity.setFldId(fldSn);
            documentRevEntity.setRevName(revisionVo.getDocumentRevisionName());
            documentRevEntity.setRevNum(revisionVo.getVersion());
            documentRevEntity.setLifecyclePhase(revisionVo.getRelease());
            documentRevEntity.setCreator(revisionVo.getOwnUser().getUserId());
            documentRevEntity.setCreatorName(revisionVo.getOwnUser().getUserName());
            documentRevEntity.setRefId(revisionVo.getDocumentRevisionUid());
            documentRevService.save(documentRevEntity);
        }
    }

    /**
     * 查询TC中的所有文档对象和文档版本对象
     *
     * @param pUid
     * @return
     */
    private List<DocumentVo> getTcDocumentList(String pUid) {
        List<DocumentVo> list = new ArrayList<>();
        try {
            ModelObject obj = TCUtils.findObjectByUid(tcsoaServiceFactory.getDataManagementService(), pUid);
            if (ObjectUtil.isNull(obj)) {
                return list;
            }
            // 查询外挂系统中文件夹下的版本对象
            TCUtils.getProperty(tcsoaServiceFactory.getDataManagementService(), obj, "contents");
            TCUtils.refreshObject(tcsoaServiceFactory.getDataManagementService(), obj);
            List<ModelObject> contents = obj.getPropertyObject("contents").getModelObjectListValue();
            for (ModelObject object : contents) {
                TCUtils.getProperty(tcsoaServiceFactory.getDataManagementService(), object, "object_type");
                TCUtils.refreshObject(tcsoaServiceFactory.getDataManagementService(), object);
                String objectType = object.getPropertyObject("object_type").getStringValue();
                DocumentVo vo = new DocumentVo();
                vo.setDocumentRevisionVoList(Collections.emptyList());
                if ("Document".equals(objectType)) {
                    // 查询属性
                    String[] kes = new String[]{"current_id", "d9_DocumentType", "object_name", "owning_user", "revision_list"};
                    TCUtils.getProperty(tcsoaServiceFactory.getDataManagementService(), new ModelObject[]{object}, kes);
                    TCUtils.refreshObject(tcsoaServiceFactory.getDataManagementService(), object);
                    vo.setDocumentUid(object.getUid());
                    vo.setDocumentNum(object.getPropertyObject("current_id").getStringValue());
                    vo.setDocumentName(object.getPropertyObject("object_name").getStringValue());
                    vo.setDocumentType(object.getPropertyObject("d9_DocumentType").getStringValue());
                    ModelObject user = object.getPropertyObject("owning_user").getModelObjectValue();
                    UserVo ownUser = getOwnUser(user);
                    if (ObjectUtil.isNotNull(ownUser)) {
                        vo.setOwnUser(ownUser);
                    }
                    List<ModelObject> revisionList = object.getPropertyObject("revision_list").getModelObjectListValue();
                    if (CollUtil.isNotEmpty(revisionList)) {
                        List<DocumentRevisionVo> revisionVoList = new ArrayList<>();
                        for (ModelObject modelObject : revisionList) {
                            DocumentRevisionVo revisionVo = getRevisionVo(modelObject);
                            if (ObjectUtil.isNotNull(revisionVo)) {
                                revisionVoList.add(revisionVo);
                            }
                        }
                        vo.setDocumentRevisionVoList(revisionVoList);
                    }
                }
                if ("DocumentRevision".equals(objectType)) {
                    DocumentRevisionVo revisionVo = getRevisionVo(object);
                    if (ObjectUtil.isNotNull(revisionVo)) {
                        vo.setDocumentRevisionVoList(CollUtil.newArrayList(revisionVo));
                    }
                }
                if (CollUtil.isNotEmpty(vo.getDocumentRevisionVoList())) {
                    list.add(vo);
                }
            }
            return list;
        } catch (Exception e) {
            log.error("查询TC错误,错误的我对象id为：" + pUid, e);
        }
        return Collections.emptyList();
    }

    /**
     * 查询TC中的文档版本信息，并转换成vo对象
     *
     * @param object
     * @return
     * @throws NotLoadedException
     */
    private DocumentRevisionVo getRevisionVo(ModelObject object) throws NotLoadedException {
        if (ObjectUtil.isNull(object)) {
            return null;
        }
        DocumentRevisionVo revisionVo = new DocumentRevisionVo();
        TCUtils.getProperty(tcsoaServiceFactory.getDataManagementService(), new ModelObject[]{object},
                new String[]{"object_name", "item_id", "item_revision_id", "owning_user", "release_status_list"});
        TCUtils.refreshObject(tcsoaServiceFactory.getDataManagementService(), object);
        revisionVo.setDocumentId(object.getPropertyObject("item_id").getStringValue());
        revisionVo.setDocumentRevisionUid(object.getUid());
        revisionVo.setDocumentRevisionName(object.getPropertyObject("object_name").getStringValue());
        revisionVo.setVersion(object.getPropertyObject("item_revision_id").getStringValue());
        ModelObject revisionUser = object.getPropertyObject("owning_user").getModelObjectValue();
        UserVo revisionOwnUser = getOwnUser(revisionUser);
        if (ObjectUtil.isNotNull(revisionOwnUser)) {
            revisionVo.setOwnUser(revisionOwnUser);
        }
        List<ModelObject> statusList = object.getPropertyObject("release_status_list").getModelObjectListValue();
        if (CollUtil.isNotEmpty(statusList)) {
            revisionVo.setRelease(0);
        }
        return revisionVo;
    }

    /**
     * 查询TC中的用户信息
     *
     * @param user
     * @return
     * @throws NotLoadedException
     */
    private UserVo getOwnUser(ModelObject user) throws NotLoadedException {
        if (ObjectUtil.isNull(user)) {
            return null;
        }
        TCUtils.getProperty(tcsoaServiceFactory.getDataManagementService(), new ModelObject[]{user}, new String[]{"user_id", "user_name"});
        TCUtils.refreshObject(tcsoaServiceFactory.getDataManagementService(), user);
        UserVo vo = new UserVo();
        vo.setUserId(user.getPropertyObject("user_id").getStringValue());
        vo.setUserName(user.getPropertyObject("user_name").getStringValue());
        return vo;
    }

    /**
     * 删除外挂系统下所有的文件夹和文件夹之间的关联关系
     *
     * @param parentIds
     */
    private void delAllFolder(List<Integer> parentIds) {
        if (CollUtil.isEmpty(parentIds)) {
            return;
        }
        Set<Integer> relIds = new HashSet<>();
        Set<Integer> folderIds = new HashSet<>(parentIds);
        for (Integer parentId : parentIds) {
            // 查询父级部门的关联关系
            List<FolderRefEntity> list = folderRefService.list(new QueryWrapper<FolderRefEntity>().lambda()
                    .eq(FolderRefEntity::getFldChildId, parentId)
            );
            if (CollUtil.isNotEmpty(list)) {
                for (FolderRefEntity refEntity : list) {
                    relIds.add(refEntity.getFldStruSn());
                }
            }
            // 查询所有子集部门的数据
            getAllChildrenId(relIds, folderIds, parentId);
        }
        if (CollUtil.isNotEmpty(relIds)) {
            folderRefService.removeByIds(relIds);
        }
        if (CollUtil.isNotEmpty(folderIds)) {
            folderService.removeByIds(folderIds);
        }
    }

    /**
     * 获取外挂系统文件夹下的所有关联关系id和文件夹id
     *
     * @param relIds
     * @param folderIds
     * @param parentId
     */
    private void getAllChildrenId(Set<Integer> relIds, Set<Integer> folderIds, Integer parentId) {
        List<FolderRefEntity> list = folderRefService.list(new QueryWrapper<FolderRefEntity>().lambda()
                .eq(FolderRefEntity::getFldId, parentId)
        );
        if (CollUtil.isEmpty(list)) {
            folderIds.add(parentId);
            return;
        }
        for (FolderRefEntity folderRefEntity : list) {
            relIds.add(folderRefEntity.getFldStruSn());
            folderIds.add(folderRefEntity.getFldChildId());
            getAllChildrenId(relIds, folderIds, folderRefEntity.getFldChildId());
        }
    }


    /**
     * 新增子文件夹及父子文件夹的关联关系
     *
     * @param folderVo
     * @param flag
     * @param parentFolderId
     * @return
     */
    private FolderEntity addFolder(FolderVo folderVo, boolean flag, Integer parentFolderId) {
        FolderEntity entity = new FolderEntity();
        entity.setFldSn(folderService.getId());
        entity.setRefId(folderVo.getPuid());
        entity.setFldName(folderVo.getFolderName());
        if (StrUtil.isNotBlank(folderVo.getFolderDesc())) {
            entity.setFldDesc(folderVo.getFolderDesc());
        }
        entity.setCreator("spas");
        if (flag) {
            entity.setRefType(1);
        } else {
            entity.setRefType(0);
        }
        folderService.save(entity);
        // 新增顶层文件夹和部门文件夹的关联关系
        FolderRefEntity refEntity = new FolderRefEntity();
        refEntity.setFldStruSn(folderRefService.getId());
        refEntity.setFldId(parentFolderId);
        refEntity.setFldChildId(entity.getFldSn());
        refEntity.setCreator("spas");
        folderRefService.save(refEntity);
        return entity;
    }
}
