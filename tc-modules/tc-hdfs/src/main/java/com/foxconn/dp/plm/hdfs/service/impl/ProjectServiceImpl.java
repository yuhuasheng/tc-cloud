package com.foxconn.dp.plm.hdfs.service.impl;

import com.foxconn.dp.plm.hdfs.dao.xplm.ConfigMapper;
import com.foxconn.dp.plm.hdfs.dao.xplm.MailGroupUserMapper;
import com.foxconn.dp.plm.hdfs.dao.xplm.ProjectMapper;
import com.foxconn.dp.plm.hdfs.dao.xplm.UserMapper;
import com.foxconn.dp.plm.hdfs.domain.entity.PhaseEntity;
import com.foxconn.dp.plm.hdfs.domain.entity.TCProjectEntity;
import com.foxconn.dp.plm.hdfs.domain.entity.UserEntity;
import com.foxconn.dp.plm.hdfs.domain.rp.ProjectListRp;
import com.foxconn.dp.plm.hdfs.service.ProjectService;
import com.foxconn.plm.entity.exception.BizException;
import com.foxconn.plm.entity.param.BUListRp;
import com.foxconn.plm.entity.response.BURv;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.feign.service.HDFSClient;
import com.foxconn.plm.utils.batch.BatchProcessor;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

import com.foxconn.dp.plm.privately.Access;

@Service
public class ProjectServiceImpl implements ProjectService {


    private static String admin;

    static {
        try {
            Properties properties = PropertiesLoaderUtils.loadAllProperties("config.properties");
            admin = properties.getProperty("admin");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    @Resource
    ProjectMapper projectMapper;

    @Resource
    UserMapper userMapper;

    @Resource
    ConfigMapper configMapper;
    @Resource
    MailGroupUserMapper mailGroupUserMapper;

    @Resource
    com.foxconn.dp.plm.hdfs.dao.xplm.ProjectMapper xmplProjectMapper;

    @Resource
    HDFSClient hdfsClient;

    @Override
    public R<List<TCProjectEntity>> getProjectsByEmpId(ProjectListRp rp) {


        if ("1".equals(rp.getPdm())) {
            List<TCProjectEntity> virtualProjectList = xmplProjectMapper.getVirtualProjectList();
            int exist = mailGroupUserMapper.existInGroup(rp.getEmpId());
            virtualProjectList.removeIf(next -> "v1002".equals(next.getId()) && !admin.equals(rp.getEmpId()) && exist == 0);
            return R.success(virtualProjectList);
        }

        if (admin.equals(rp.getEmpId())) {
            return getProjectsForAdmin();
        }


        R<List<BURv>> listR = hdfsClient.buList(new BUListRp());
        List<BURv> buList = listR.getData();

        List<UserEntity> userInfoInSpas = userMapper.getUserInfoInSpas(Collections.singletonList(rp.getEmpId()));
        if (userInfoInSpas.isEmpty()) {
            throw new BizException("查無此人");
        }

        UserEntity userEntity = userInfoInSpas.get(0);
        boolean isManager = userEntity.isManager();

        String empId = isManager ? admin : rp.getEmpId();
        List<TCProjectEntity> projectList = projectMapper.getProjectIDsInSpas(Access.check(empId));

        for (TCProjectEntity tcProjectEntity : projectList) {
            tcProjectEntity.setBu(findBu(buList, tcProjectEntity.getCustomerId(), tcProjectEntity.getProductLineId()));
        }

        // 去掉BU檢查
//        String bu = userEntity.getBu();
//        projectList.removeIf(projectEntity -> !bu.equals(projectEntity.getBu()));

        List<TCProjectEntity> creatorList = new ArrayList<>();
        BatchProcessor.batch(projectList, 1000, list -> {
            List<TCProjectEntity> l = xmplProjectMapper.getProjectList(Access.check(list));
            creatorList.addAll(l);
        });

        Iterator<TCProjectEntity> iterator = projectList.iterator();
        while (iterator.hasNext()) {
            TCProjectEntity left = iterator.next();
            TCProjectEntity right = findProject(creatorList, left);
            if (right == null) {
                iterator.remove();
                continue;
            }
            left.setFolderId(right.getFolderId());
            left.setName(right.getName());
        }

        return R.success(projectList);
    }

    public R<List<TCProjectEntity>> getProjectsForAdmin() {
        List<TCProjectEntity> projectList = projectMapper.getProjectIDsInSpas(Access.check(admin));

        List<TCProjectEntity> creatorList = new ArrayList<>();
        BatchProcessor.batch(projectList, 1000, list -> {
            List<TCProjectEntity> l = xmplProjectMapper.getProjectList(Access.check(list));
            creatorList.addAll(l);
        });

        Iterator<TCProjectEntity> iterator = projectList.iterator();
        while (iterator.hasNext()) {
            TCProjectEntity left = iterator.next();
            TCProjectEntity right = findProject(creatorList, left);
            if (right == null) {
                iterator.remove();
                continue;
            }
            left.setFolderId(right.getFolderId());
            left.setName(right.getName());
        }

//        projectList.addAll(xmplProjectMapper.getVirualProjectList());

        return R.success(projectList);
    }

    String findBu(List<BURv> buList, long customerId, long productLineId) {
        for (BURv rv : buList) {
            if (Objects.equals(rv.getCustomerId(), customerId) && Objects.equals(rv.getProductLineId(), productLineId)) {
                return rv.getBu();
            }
        }
        return null;
    }

    private TCProjectEntity findProject(List<TCProjectEntity> list, TCProjectEntity p) {
        for (TCProjectEntity tcProject : list) {
            if (tcProject.equals(p)) {
                return tcProject;
            }
        }
        return null;
    }

}
