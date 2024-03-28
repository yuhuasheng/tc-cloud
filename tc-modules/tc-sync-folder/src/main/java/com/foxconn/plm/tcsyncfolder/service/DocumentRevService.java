package com.foxconn.plm.tcsyncfolder.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foxconn.plm.tcsyncfolder.entity.DocumentRevEntity;
import com.foxconn.plm.tcsyncfolder.vo.FileVo;

/**
 * @ClassName: DocumentRevService
 * @Description:
 * @Author DY
 * @Create 2023/3/27
 */
public interface DocumentRevService extends IService<DocumentRevEntity> {

    Integer getId();

    FileVo getFileInfo(Integer docRevId);
}
