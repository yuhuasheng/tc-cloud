package com.foxconn.dp.plm.hdfs.dao.xplm;

import com.foxconn.dp.plm.hdfs.domain.entity.*;
import com.foxconn.dp.plm.hdfs.domain.rp.CreateFolderRp;
import com.foxconn.dp.plm.hdfs.domain.rp.ItemListRp;
import com.foxconn.dp.plm.hdfs.domain.rp.ModifyFolderRp;
import com.foxconn.dp.plm.hdfs.domain.rp.SubFolderListRp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FolderMapper {

    List<FolderEntity> getFolderById(long folderId);
    long nextFolderSeq();
    long nextFolderStructSeq();

    List<FolderEntity> getProjectFolder(String projectId);

    List<FolderEntity> getSubFolder(SubFolderListRp rp);

    List<ItemRevEntity> getItem(ItemListRp rp);

    List<ItemRevEntity> getAllItemRevisionByDocId(long docId);

    int insertFolder(CreateFolderRp rp);

    int existFolder(CreateFolderRp rp);

    int insertFolderStruct(CreateFolderRp rp);

    int delFolder(long id);

    int delFolderStruct(@Param("parentId") long parentId, @Param("fldId") long fldId);

    int updateFolder(ModifyFolderRp rp);

    int folderCount(long fid);

    int documentCount(long fid);

    int getParentId(long childId);

    List<DatasetEntity> getFileList(Long docRevId);

    void modifyDocName(@Param("docId") String docId, @Param("docName") String docName);

    String getDocSn(@Param("docId") String docId);

    void modifyDocRevName(@Param("docSn") String docSn, @Param("docRev") String docRev, @Param("docName") String docName);

    public abstract FileEntity getFileInfo(@Param("fileVersionId") Long fileVersionId);

}
