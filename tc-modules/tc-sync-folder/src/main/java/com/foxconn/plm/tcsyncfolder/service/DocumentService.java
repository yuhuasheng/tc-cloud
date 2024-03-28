package com.foxconn.plm.tcsyncfolder.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foxconn.plm.tcsyncfolder.entity.DocumentEntity;

/**
 * @ClassName: DocumentService
 * @Description:
 * @Author DY
 * @Create 2023/3/27
 */
public interface DocumentService extends IService<DocumentEntity> {

    Integer getId();
}
