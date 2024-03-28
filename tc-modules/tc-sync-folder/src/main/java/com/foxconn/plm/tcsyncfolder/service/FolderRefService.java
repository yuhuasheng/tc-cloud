package com.foxconn.plm.tcsyncfolder.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foxconn.plm.tcsyncfolder.entity.FolderRefEntity;

/**
 * @ClassName: FolderRefService
 * @Description:
 * @Author DY
 * @Create 2023/3/27
 */
public interface FolderRefService extends IService<FolderRefEntity> {
    Integer getId();
}
