import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.tcsyncfolder.Application;
import com.foxconn.plm.tcsyncfolder.entity.DocumentEntity;
import com.foxconn.plm.tcsyncfolder.entity.DocumentRevEntity;
import com.foxconn.plm.tcsyncfolder.entity.FolderEntity;
import com.foxconn.plm.tcsyncfolder.entity.ProjectEntity;
import com.foxconn.plm.tcsyncfolder.mapper.TcProjectMapper;
import com.foxconn.plm.tcsyncfolder.schedule.SyncFolderScheduling;
import com.foxconn.plm.tcsyncfolder.service.*;
import com.foxconn.plm.tcsyncfolder.vo.*;
import com.foxconn.plm.utils.tc.ProjectUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.Item;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import com.teamcenter.soa.client.model.strong.TC_Project;
import com.teamcenter.soa.exceptions.NotLoadedException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName: VerifyTest
 * @Description:
 * @Author DY
 * @Create 2023/4/11
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class VerifyTest {
    private static Log log = LogFactory.get();
    @Resource
    private TcProjectMapper tcProjectMapper;
    @Resource
    private ProjectService projectService;
    @Resource
    private FolderService folderService;
    @Resource
    private FolderRefService folderRefService;
    @Resource
    private TCSOAServiceFactory tcsoaServiceFactory;
    @Resource
    private DocumentService documentService;
    @Resource
    private DocumentRevService documentRevService;
    @Resource
    private SyncFolderScheduling syncFolderScheduling;

    @Test
    public void syncFolder() throws Exception {
        syncFolderScheduling.syncFolder();
    }

    @Test
    public void getFolder() throws Exception {

        tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS2);

        DocumentRevEntity documentRevEntity = documentRevService.getById(35216);

        FolderEntity folderEntity = folderService.getById(documentRevEntity.getFldId());

        DocumentEntity documentEntity = documentService.getById(documentRevEntity.getDocId());

        ItemRevision itemRevision = null;
        // 已经存在文档对象
        Item item = (Item) TCUtils.findObjectByUid(tcsoaServiceFactory.getDataManagementService(), documentEntity.getRefId());
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
        ModelObject obj = TCUtils.findObjectByUid(tcsoaServiceFactory.getDataManagementService(), folderEntity.getRefId());
        if (ObjectUtil.isNotNull(obj)) {
            TCUtils.addContents(tcsoaServiceFactory.getDataManagementService(), obj, itemRevision, "contents");
        }
        // 查询项目对象
        TC_Project projectObj = null;
        Map<String, Object> queryResults = TCUtils.executeQuery(tcsoaServiceFactory.getSavedQueryService(), "__D9_Find_Project",
                new String[]{"project_id"}, new String[]{"p1567"});
        if (queryResults.get("succeeded") == null) {
            log.error("未查询到项目信息");
        }
        ModelObject[] objs = (ModelObject[]) queryResults.get("succeeded");
        if (objs.length > 0) {
            projectObj = (TC_Project) objs[0];
        }
        if (ObjectUtil.isNotNull(projectObj)) {
            ProjectUtil.assignedProject(tcsoaServiceFactory.getProjectLevelSecurityService(), itemRevision, projectObj);
        }
        tcsoaServiceFactory.logout();
    }


    @Test
    public void verifyDocument() {
        tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
        FolderEntity folder = folderService.getById(123170);
        List<DocumentVo> tcDocumentList = getTcDocumentList(folder.getRefId());
        for (DocumentVo documentVo : tcDocumentList) {
            System.out.println(JSONUtil.toJsonStr(documentVo));
        }

        tcsoaServiceFactory.logout();
    }


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

}
