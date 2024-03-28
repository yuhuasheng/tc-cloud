package com.foxconn.dp.plm.fileservice.service;

import com.foxconn.dp.plm.fileservice.domain.entity.DocumentEntity;
import com.foxconn.dp.plm.fileservice.domain.entity.FileHisEntity;
import com.foxconn.dp.plm.fileservice.domain.entity.FileEntity;

import java.io.File;

public interface IFileService {


    /**
     * 删除文档版本
     *
     * @throws Exception
     */
    public void deleteDocRev(Long docRevId, FileHisEntity fileHisEntity);


    /**
     * 文件快速发行文档版本
     *
     * @throws Exception
     */
    public void quickReleaseDocRev(Long docRevId, FileHisEntity fileHisEntity);

    /**
     * 将删除的文件放到回收区
     *
     * @throws Exception
     */
    public void recycleFile(Long fileVersionId);

}
