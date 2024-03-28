package com.foxconn.dp.plm.fileservice.service;

import com.foxconn.dp.plm.fileservice.domain.entity.DocumentEntity;
import com.foxconn.dp.plm.fileservice.domain.entity.FileEntity;
import com.foxconn.dp.plm.fileservice.domain.entity.FileHisEntity;

public interface IDownloadService {

    /**
     * 下载文件
     *
     * @throws Exception
     */
    public FileEntity downloadFile(Long fileVersionId);


    /**
     * 从其他Site 同步文件
     *
     * @throws Exception
     */
    public FileEntity syncFileFromOtherSite(Long fileVersionId);


}
