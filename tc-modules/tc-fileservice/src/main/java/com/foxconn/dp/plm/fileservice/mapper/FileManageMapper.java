package com.foxconn.dp.plm.fileservice.mapper;

import com.foxconn.dp.plm.fileservice.domain.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


@Mapper
public interface FileManageMapper {


    /**
     * 删除文档版本下的所有文件
     *
     * @param docRevId 附档版本ID
     * @throws Exception
     */
    public abstract void deleteDocRevFiles(@Param("docRevId") Long docRevId);

    /**
     * 获取文档下 未删除的文件
     *
     * @param docId
     * @throws Exception
     */
    public abstract Integer getDocFileCnt(@Param("docId") Long docId);

    /**
     * 删除文档版本
     *
     * @param docRevId
     */
    public abstract void deleteDocRev(@Param("docRevId") Long docRevId);


    /**
     * 删除文档
     *
     * @param docId
     */
    public abstract void deleteDoc(@Param("docId") Long docId);


    /**
     * 新增档案
     *
     * @param documentEntity
     * @throws Exception
     */
    public abstract void addDoc(DocumentEntity documentEntity);


    /**
     * 新增档案版本
     *
     * @param documentRevEntity
     * @throws Exception
     */
    public abstract void addDocRev(DocumentRevEntity documentRevEntity);

    /**
     * 新增文件
     *
     * @param fileEntity
     * @throws Exception
     */
    public abstract void addFile(FileEntity fileEntity);


    /**
     * 新增文件版本
     *
     * @param fileVersionEntity
     * @throws Exception
     */
    public abstract void addFileVersion(FileVersionEntity fileVersionEntity);


    /**
     * 查询文档所有版本
     *
     * @param docId
     * @return
     * @throws Exception
     */
    public abstract List<String> getDocRevNums(@Param("docId") Long docId);

    /**
     * 新增文件操作历史
     *
     * @param fileHisEntity
     * @throws Exception
     */
    public abstract void addFileHistory(FileHisEntity fileHisEntity);

    public abstract Long getObjFileSqe();

    public abstract FileEntity getFileInfo(@Param("fileVersionId") Long fileVersionId);

    public abstract DocumentRevEntity getDocRevInfo(@Param("docRevId") Long docRevId);


    public abstract void quickReleaseDocRev(@Param("docRevId") Long docRevId);


    public abstract List<FileEntity> getDeletedFileInfo(@Param("docRevId") Long docRevId);


    public abstract List<ServerListEntity> getServerList();

    /**
     * 查询文档下未发行版本数量
     *
     * @param docId
     * @return
     */
    public abstract Integer getUnReleaseCnt(@Param("docId") Long docId);


    public abstract Integer getDocCnt(@Param("folderId") Long folderId, @Param("docName") String docName);

    public abstract void updateDocName(@Param("docRevId") Long docRevId, @Param("newDocName") String newDocName);


}
