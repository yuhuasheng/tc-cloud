package com.foxconn.dp.plm.fileservice.service;

import com.foxconn.dp.plm.fileservice.domain.entity.DocumentEntity;
import com.foxconn.dp.plm.fileservice.domain.entity.FileEntity;
import com.foxconn.dp.plm.fileservice.domain.entity.FileHisEntity;

public interface IUploadService {


    /**
     * 上传文件
     *
     * @throws Exception
     */
    public void uploadFile(DocumentEntity documentEntity, String docName, FileEntity fileEntity, FileHisEntity fileHisEntity);


    /**
     * 文件身板
     *
     * @throws Exception
     */
    public void rivieseFile(Long docId, String docName, FileEntity fileEntity, FileHisEntity fileHisEntity);


}
