import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Snowflake;
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
import com.foxconn.plm.tcsyncfolder.entity.FolderEntity;
import com.foxconn.plm.tcsyncfolder.entity.FolderRefEntity;
import com.foxconn.plm.tcsyncfolder.entity.ProjectEntity;
import com.foxconn.plm.tcsyncfolder.mapper.TcProjectMapper;
import com.foxconn.plm.tcsyncfolder.service.DocumentService;
import com.foxconn.plm.tcsyncfolder.service.FolderRefService;
import com.foxconn.plm.tcsyncfolder.service.FolderService;
import com.foxconn.plm.tcsyncfolder.service.ProjectService;
import com.foxconn.plm.tcsyncfolder.vo.FolderVo;
import com.foxconn.plm.tcsyncfolder.vo.ProjectVo;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.services.strong.administration.PreferenceManagementService;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.Folder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.sql.Date;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName: SyncProjectInfoTest
 * @Description:
 * @Author DY
 * @Create 2023/3/27
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class SyncProjectInfoTest {
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

    @Test
    public void syncDeptFolderId() {
        tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS2);
        List<ProjectEntity> list = projectService.list();
        for (ProjectEntity projectEntity : list) {
            if(StrUtil.isBlank(projectEntity.getRefId())){
                log.error(JSONUtil.toJsonStr(projectEntity) +"--------->项目没有同步refId");
                continue;
            }
            List<FolderEntity> deptFolder = folderService.getChildFolder(projectEntity.getFolderId());
            List<FolderVo> tcChildrenFolders = tcProjectMapper.getChildFolder(projectEntity.getRefId());
            for (FolderEntity folderEntity : deptFolder) {
                List<FolderVo> collect = tcChildrenFolders.parallelStream().filter(item -> item.getFolderName().equals(folderEntity.getFldName())).collect(Collectors.toList());
                if (CollUtil.isEmpty(collect)) {
                    log.error("未查询到TC中文件夹信息：------------>" + JSONUtil.toJsonStr(folderEntity));
                    //deleteFolder(folderEntity.getFldSn());
                    continue;
                }
                if (collect.size() == 1) {
                    folderEntity.setRefId(collect.get(0).getPuid());
                    folderService.updateById(folderEntity);
                } else {
                    log.error(JSONUtil.toJsonStr(folderEntity) + "----->文件夹下有重复的文件夹名：" + folderEntity.getFldName());
                }
            }
        }

        tcsoaServiceFactory.logout();
    }

    @Test
    public void syncProjectInfo() {
        List<ProjectEntity> list = projectService.list();
        for (ProjectEntity projectEntity : list) {
            String projName = projectEntity.getProjName();
            String spasId = projectEntity.getProjSpasId();
            ProjectVo projectVo = tcProjectMapper.getTcProjectInfo(spasId);
            if(ObjectUtil.isNull(projectVo)){
                log.error(JSONUtil.toJsonStr(projectEntity) +"--------->项目文件夹在TC中没有找到");
                //projectService.removeById(projectEntity.getProjSn());
                //deleteFolder(projectEntity.getFolderId());
                continue;
            }
            if (ObjectUtil.isNotNull(projectVo)) {
                projectEntity.setRefId(projectVo.getPuid());
                if (!projectVo.getProjectName().trim().equals(projName.trim())) {
                    projectEntity.setProjName(projectVo.getProjectName().trim());
                }
            }
            if (ObjectUtil.isNull(projectEntity.getCreated())) {
                projectEntity.setCreated(LocalDateTime.now());
            }
        }
        projectService.updateBatchById(list);
    }

    @Test
    public void syncFolderRefId() {
        tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS2);
        List<ProjectEntity> list = projectService.list();
        for (ProjectEntity projectEntity : list) {
            if(StrUtil.isBlank(projectEntity.getRefId())){
                log.error(JSONUtil.toJsonStr(projectEntity) +"--------->项目没有同步refId");
                continue;
            }
            List<FolderEntity> deptFolder = folderService.getChildFolder(projectEntity.getFolderId());
            List<FolderVo> tcChildrenFolders = tcProjectMapper.getChildFolder(projectEntity.getRefId());
            for (FolderEntity folderEntity : deptFolder) {
                List<FolderVo> collect = tcChildrenFolders.parallelStream().filter(item -> item.getFolderName().equals(folderEntity.getFldName())).collect(Collectors.toList());
                if (CollUtil.isEmpty(collect)) {
                    log.error("未查询到TC中文件夹信息：------------>" + JSONUtil.toJsonStr(folderEntity));
                    //deleteFolder(folderEntity.getFldSn());
                    continue;
                }
                if (collect.size() == 1) {
                    folderEntity.setRefId(collect.get(0).getPuid());
                    folderService.updateById(folderEntity);
                } else {
                    log.error(JSONUtil.toJsonStr(folderEntity) + "----->文件夹下有重复的文件夹名：" + folderEntity.getFldName());
                }
                // 同步子文件夹id
                syncChildFolderRefId(folderEntity);
            }
        }

        tcsoaServiceFactory.logout();
    }

    @Test
    public void syncDocumentRefId() throws Exception {
        tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS2);
        List<DocumentEntity> list = documentService.list();
        for (DocumentEntity documentEntity : list) {
            Map<String, Object> queryResults = TCUtils.executeQuery(tcsoaServiceFactory.getSavedQueryService(), "Item...",
                    new String[]{"item_id"}, new String[]{documentEntity.getDocNum()});
            if (queryResults.get("succeeded") == null) {
                throw new Exception("未查询到项目信息");
            }
            ModelObject[] objs = (ModelObject[]) queryResults.get("succeeded");
            if (objs.length  == 1) {
                documentEntity.setRefId(objs[0].getUid());
                documentService.updateById(documentEntity);
            }else{
                log.error("设置文档数据出错--------------> ：" + JSONUtil.toJsonStr(documentEntity));
            }
        }

        tcsoaServiceFactory.logout();
    }

    private void syncChildFolderRefId(FolderEntity folderEntity) {
        // 查询外挂系统中的子文件夹
        List<FolderEntity> childFolder = folderService.getChildFolder(folderEntity.getFldSn());
        if (CollUtil.isEmpty(childFolder)) {
            return;
        }
        List<FolderVo> tcChildrenFolders = tcProjectMapper.getChildFolder(folderEntity.getRefId());

        for (FolderEntity entity : childFolder) {
            List<FolderVo> collect1 = tcChildrenFolders.parallelStream()
                    .filter(item -> item.getFolderName().equals(entity.getFldName()))
                    .collect(Collectors.toList());
            if (CollUtil.isEmpty(collect1)) {
                log.error("未查询到TC中文件夹信息：------------>" + JSONUtil.toJsonStr(folderEntity));
                //deleteFolder(folderEntity.getFldSn());
                continue;
            }
            if (collect1.size() == 1) {
                entity.setRefId(collect1.get(0).getPuid());
                folderService.updateById(entity);
            } else {
                log.error(JSONUtil.toJsonStr(folderEntity) + "----->文件夹下有重复的文件夹名：" + entity.getFldName());
            }
            syncChildFolderRefId(entity);
        }
    }

    @Test
    public void  test(){
        List<ProjectEntity> list = projectService.list();
        List<Integer> rootFolderIds = list.parallelStream().map(ProjectEntity::getFolderId).collect(Collectors.toList());
        List<FolderEntity> folderList = folderService.list(new QueryWrapper<FolderEntity>().lambda()
                .eq(FolderEntity::getRefType,1)
                .isNull(FolderEntity::getRefId)
                .notIn(FolderEntity::getFldSn, rootFolderIds)
        );
        sync(folderList);
    }

    private void sync(List<FolderEntity> folderList) {
        tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
        Map<String, Set<String>> noAccountDeptMap = getNoAccountDeptMap(tcsoaServiceFactory.getPreferenceManagementService());

        for (FolderEntity folderEntity : folderList) {
            List<Integer> folderIds = new ArrayList<>();
            getparentFolderIds(folderEntity.getFldSn(), folderIds);
            if (CollUtil.isEmpty(folderIds)) {
                deleteFolder(folderEntity.getFldSn());
                continue;
            }
            // 项目文件夹
            Integer rootFolderId = folderIds.get(folderIds.size() - 1);
            Integer deptFolderId = null;
            if (folderIds.size() > 2) {
                deptFolderId = folderIds.get(folderIds.size() - 2);
            } else {
                deptFolderId = folderEntity.getFldSn();
            }
            // 判断部门是有账号还是无账号部门
            ProjectEntity projectEntity = projectService.getOne(new QueryWrapper<ProjectEntity>().lambda()
                    .eq(ProjectEntity::getFolderId, rootFolderId));
            if (ObjectUtil.isNull(projectEntity)) {
                deleteFolder(folderEntity.getFldSn());
                continue;
            }
            if(StrUtil.isBlank(projectEntity.getRefId())){
                continue;
            }
            FolderEntity deptFolder = folderService.getById(deptFolderId);
            String bu = getProjectBu(projectEntity.getProjSpasId());
            Boolean flag = true;
            Set<String> deptSet = noAccountDeptMap.get(bu);
            if (CollUtil.isNotEmpty(deptSet) && deptSet.contains(deptFolder.getFldName())) {
                // 表示从外挂系统同步到TC系统
                flag = false;
            }
            if (flag) {
                // 查询TC中文件夹的puid
                int index = folderIds.size() - 2;
                String uPid = projectEntity.getRefId();
                while (index >= 0) {
                    FolderEntity childFolder = folderService.getById(folderIds.get(index));
                    List<FolderVo> tcChildrenFolders = tcProjectMapper.getChildFolder(uPid);
                    for (FolderVo folderVo : tcChildrenFolders) {
                        if (childFolder.getFldName().equals(folderVo.getFolderName())) {
                            if (StrUtil.isBlank(childFolder.getRefId())) {
                                childFolder.setRefId(folderVo.getPuid());
                                folderService.updateById(childFolder);
                            }
                            uPid = folderVo.getPuid();
                            index--;
                            break;
                        }
                    }
                }
                Boolean delFlag = true;
                List<FolderVo> tcChildrenFolders = tcProjectMapper.getChildFolder(uPid);
                if (CollUtil.isNotEmpty(tcChildrenFolders)) {
                    for (FolderVo folderVo : tcChildrenFolders) {
                        if (folderEntity.getFldName().equals(folderVo.getFolderName())) {
                            folderEntity.setRefId(uPid);
                            folderService.updateById(folderEntity);
                            delFlag = false;
                        }
                    }
                }
                if (delFlag) {
                    // TC中没有 需要将其删除
                    deleteFolder(folderEntity.getFldSn());
                }
            }
        }
        // 登出TC
        tcsoaServiceFactory.logout();
    }

    private void deleteFolder(Integer parentId) {
        Set<Integer> relIds = new HashSet<>();
        Set<Integer> folderIds = new HashSet<>();
        folderIds.add(parentId);
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

        if (CollUtil.isNotEmpty(relIds)) {
            folderRefService.removeByIds(relIds);
        }
        if (CollUtil.isNotEmpty(folderIds)) {
            folderService.removeByIds(folderIds);
        }
    }

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

    private String getProjectBu(String spasId) {
        try {
            Map<String, Object> queryResults = TCUtils.executeQuery(tcsoaServiceFactory.getSavedQueryService(), "__D9_Find_Series_Folder",
                    new String[]{"D9_PlatformFoundFolder:contents.d9_SPAS_ID"}, new String[]{spasId});
            if (queryResults.get("succeeded") == null) {
                throw new Exception("【" + spasId + "】专案未找到BU.");
            }
            ModelObject[] queryResult = (ModelObject[]) queryResults.get("succeeded");
            Folder folder = (Folder) queryResult[0];
            return TCUtils.getPropStr(tcsoaServiceFactory.getDataManagementService(), folder, "object_desc");
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Set<String>> getNoAccountDeptMap(PreferenceManagementService preferenceManagementService) {
        Map<String, Set<String>> map = new HashMap<>();
        try {
            String[] noTcAccDept = TCUtils.getTCPreferences(preferenceManagementService, "D9_TC_NoAccount_Department");
            for (int i = 0; i < noTcAccDept.length; i++) {
                String[] buAndDept = noTcAccDept[i].split("=");
                String bu = buAndDept[0];
                String dept = buAndDept[1];
                Set<String> deptList = CollUtil.newHashSet(Arrays.asList(dept.split(",")));
                map.put(bu, deptList);
            }
        } catch (Exception e) {
            log.error("查询无账号部门失败");
        }
        return map;
    }

    private void getparentFolderIds(Integer folderId, List<Integer> folderIds) {
        FolderRefEntity parent = folderRefService.getOne(new QueryWrapper<FolderRefEntity>().lambda()
                .eq(FolderRefEntity::getFldChildId, folderId));
        if (ObjectUtil.isNotNull(parent)) {
            folderIds.add(parent.getFldId());
            getparentFolderIds(parent.getFldId(), folderIds);
        }
    }
}
