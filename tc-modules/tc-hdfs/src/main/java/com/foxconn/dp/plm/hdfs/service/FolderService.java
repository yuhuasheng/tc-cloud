package com.foxconn.dp.plm.hdfs.service;


import com.foxconn.dp.plm.hdfs.domain.entity.FolderEntity;
import com.foxconn.dp.plm.hdfs.domain.entity.ItemRevEntity;
import com.foxconn.dp.plm.hdfs.domain.rp.*;
import com.github.pagehelper.PageInfo;

import java.util.List;

public interface FolderService {


    PageInfo<FolderEntity> getProjectFolder(FolderListRp rp);

    List<FolderEntity> getSubFolder(SubFolderListRp rp);

    PageInfo<ItemRevEntity> getItem(ItemListRp rp);

    PageInfo<ItemRevEntity> getAllItemRevisionByDocId(ItemRevisionListRp rp);

    long createFolder(CreateFolderRp rp);

    void delFolder(DelRp rp);

    void modifyFolder(ModifyFolderRp rp);

    public void modifyDocName(String docNum, String docRev, String docName, long folderId);

}
