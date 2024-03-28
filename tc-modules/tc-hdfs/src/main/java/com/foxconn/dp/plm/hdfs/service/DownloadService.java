package com.foxconn.dp.plm.hdfs.service;


import com.foxconn.dp.plm.hdfs.domain.entity.DatasetEntity;
import com.foxconn.dp.plm.hdfs.domain.entity.DownloadEntity;
import com.foxconn.dp.plm.hdfs.domain.entity.FileEntity;

import java.util.List;

public interface DownloadService {

    /**
     * 下载文件
     *
     * @throws Exception
     */
    public FileEntity downloadFile(String refId, String site);


    public List<DatasetEntity> getFileList(Long fileVersionId);
    public FileEntity getFileInfo(Long docRevId);

}
