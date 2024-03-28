package com.foxconn.plm.tcsyncfolder.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foxconn.plm.tcsyncfolder.entity.FolderEntity;
import com.foxconn.plm.tcsyncfolder.mapper.FolderMapper;
import com.foxconn.plm.tcsyncfolder.service.FolderService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @ClassName: FolderServiceImpl
 * @Description:
 * @Author DY
 * @Create 2023/3/27
 */
@Service
public class FolderServiceImpl extends ServiceImpl<FolderMapper, FolderEntity> implements FolderService {
    @Override
    public List<FolderEntity> getChildFolder(Integer pldSn) {
        return baseMapper.getChildFolder(pldSn);
    }

    @Override
    public Integer getId() {
        return baseMapper.getId();
    }
}
