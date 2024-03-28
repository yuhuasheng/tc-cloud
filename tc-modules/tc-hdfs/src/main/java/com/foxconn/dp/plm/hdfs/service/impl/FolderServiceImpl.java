package com.foxconn.dp.plm.hdfs.service.impl;

import com.foxconn.dp.plm.hdfs.dao.xplm.FolderMapper;
import com.foxconn.dp.plm.hdfs.dao.xplm.ProjectMapper;
import com.foxconn.dp.plm.hdfs.dao.xplm.UserMapper;
import com.foxconn.dp.plm.hdfs.domain.entity.FolderEntity;
import com.foxconn.dp.plm.hdfs.domain.entity.ItemRevEntity;
import com.foxconn.dp.plm.hdfs.domain.entity.TCProjectEntity;
import com.foxconn.dp.plm.hdfs.domain.entity.UserEntity;
import com.foxconn.dp.plm.hdfs.domain.rp.*;
import com.foxconn.dp.plm.hdfs.service.FolderService;
import com.foxconn.dp.plm.privately.Access;
import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.entity.exception.BizException;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.tc.TCUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.teamcenter.services.strong.query._2007_06.SavedQuery;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.Item;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class FolderServiceImpl implements FolderService {

    @Resource
    FolderMapper folderMapper;

    @Resource
    UserMapper userMapper;

    @Resource
    ProjectMapper projectMapper;

    @Override
    public PageInfo<FolderEntity> getProjectFolder(FolderListRp rp) {
        PageHelper.startPage(rp.getPageIndex(), rp.getPageSize());
        return new PageInfo<>(folderMapper.getProjectFolder(rp.getProjectId()));
    }

    @Override
    public List<FolderEntity> getSubFolder(SubFolderListRp rp) {
        String projectId = rp.getProjectId();
        if (projectId == null) {
            throw new BizException("參數不完整！");
        }
        TCProjectEntity project = projectMapper.getProjectById(rp.getProjectId());
        if ("admin".equals(rp.getEmpId()) || rp.isVirtual()) {
            rp.setShowAll(1);
        } else {
            rp.setShowAll(rp.getEmpId().equals(project.getCreatorId()) ? 1 : 0);
        }
        List<FolderEntity> subFolder = folderMapper.getSubFolder(rp);
        if ("v1001".equals(rp.getProjectId()) && !"admin".equals(rp.getEmpId())) {
            // 不展示其他迁移文件
            subFolder.removeIf(next -> "其他迁移文件".equals(next.getName()));
        }
        return subFolder;
    }

    @Override
    public PageInfo<ItemRevEntity> getItem(ItemListRp rp) {
        PageHelper.startPage(rp.getPageIndex(), rp.getPageSize());
        List<ItemRevEntity> itemList = folderMapper.getItem(rp);
        return new PageInfo<>(itemList);
    }


    @Override
    public PageInfo<ItemRevEntity> getAllItemRevisionByDocId(ItemRevisionListRp rp) {
        List<ItemRevEntity> list = new ArrayList<>();
        List<ItemRevEntity> itemList = folderMapper.getAllItemRevisionByDocId(rp.getDocId());


        for (ItemRevEntity itemRevEntity : itemList) {
            if (!contains(list, itemRevEntity)) {
                list.add(itemRevEntity);
            }
        }
        return new PageInfo<>(list);


    }

    private boolean contains(List<ItemRevEntity> itemList, ItemRevEntity item) {
        for (ItemRevEntity itemRevEntity : itemList) {
            String flag = itemRevEntity.getNum() + itemRevEntity.getVerNum();
            String flag2 = item.getNum() + item.getVerNum();
            if (flag.equals(flag2)) {
                return true;
            }
        }
        return false;
    }

    private UserEntity findUser(List<UserEntity> list, String uid) {
        for (UserEntity user : list) {
            if (user.getId().equals(uid)) {
                return user;
            }
        }
        return null;
    }

    @Override
    public long createFolder(CreateFolderRp rp) {
        //检查是否存在
        int i = folderMapper.existFolder(rp);
        if (i > 0) {
            throw new BizException("相同父文件夾下有同名的文件夾，不能創建。");
        }
        rp.setFid(folderMapper.nextFolderSeq());
        i = folderMapper.insertFolder(rp);
        if (i == 0) {
            throw new BizException("創建文件夾失敗");
        }
        rp.setFsId(folderMapper.nextFolderStructSeq());
        i = folderMapper.insertFolderStruct(rp);
        if (i == 0) {
            throw new BizException("創建文件夾失敗");
        }
        return rp.getFid();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delFolder(DelRp rp) {
        List<Long> idList = rp.getIdList();
        for (Long fid : idList) {
            if (folderMapper.folderCount(fid) > 0 || folderMapper.documentCount(fid) > 0) {
                throw new BizException(HttpResultEnum.SERVER_ERROR.getCode(), "只能刪除空的文件夾");
            }
            int parentId = folderMapper.getParentId(Access.check(fid));
            folderMapper.delFolder(Access.check(fid));
            folderMapper.delFolderStruct(Access.check(parentId), Access.check(fid));
        }
    }

    @Override
    public void modifyFolder(ModifyFolderRp rp) {
        if (folderMapper.updateFolder(rp) == 0) {
            throw new BizException(HttpResultEnum.SERVER_ERROR.getCode(), "修改文件夾失敗");
        }
    }

    @Override
    public void modifyDocName(String docNum, String docRev, String docName, long folderId) {

        // 校验文件名重复
        ItemListRp rp = new ItemListRp();
        rp.setDocName(docName);
        ArrayList<Long> folderIds = new ArrayList<>();
        folderIds.add(folderId);
        rp.setFolderIds(folderIds);
        List<ItemRevEntity> itemList = folderMapper.getItem(rp);
        for (ItemRevEntity itemRevEntity : itemList) {
            if (docName.equals(itemRevEntity.getDocName())) {
                throw new BizException("文檔名稱重複");
            }
        }

        folderMapper.modifyDocName(docNum, docName);
        String docSn = folderMapper.getDocSn(docNum);
        folderMapper.modifyDocRevName(docSn, docRev, docName);
        TCSOAServiceFactory tCSOAServiceFactory = null;
        try {
            tCSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS1);
            SavedQuery.ExecuteSavedQueriesResponse savedQueryResult = TCUtils.execute2Query(tCSOAServiceFactory.getSavedQueryService()
                    , "Item_Name_or_ID", new String[]{"item_id"}, new String[]{docNum});
            ModelObject[] objs = savedQueryResult.arrayOfResults[0].objects;
            if (objs.length > 0) {
                Item item = (Item) objs[0];
                TCUtils.setProperties(tCSOAServiceFactory.getDataManagementService()
                        , item, "object_name", docName);
                ItemRevision itemRev = TCUtils.getItemLatestRevision(tCSOAServiceFactory.getDataManagementService(), item);
                TCUtils.setProperties(tCSOAServiceFactory.getDataManagementService()
                        , itemRev, "object_name", docName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (tCSOAServiceFactory != null) {
                    tCSOAServiceFactory.logout();
                }
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }
}
