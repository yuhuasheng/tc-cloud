package com.foxconn.plm.tcsyncfolder.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foxconn.plm.tcsyncfolder.entity.FolderEntity;

import java.util.List;

/**
 * @ClassName: FolderService
 * @Description:
 * @Author DY
 * @Create 2023/3/27
 */
public interface FolderService extends IService<FolderEntity> {
    List<FolderEntity> getChildFolder(Integer pldSn);

    Integer getId();
}
