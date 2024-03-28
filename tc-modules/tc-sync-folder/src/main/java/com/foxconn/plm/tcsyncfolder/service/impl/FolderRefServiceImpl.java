package com.foxconn.plm.tcsyncfolder.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foxconn.plm.tcsyncfolder.entity.FolderRefEntity;
import com.foxconn.plm.tcsyncfolder.mapper.FolderRefMapper;
import com.foxconn.plm.tcsyncfolder.service.FolderRefService;
import org.springframework.stereotype.Service;

/**
 * @ClassName: FolderRefServiceImpl
 * @Description:
 * @Author DY
 * @Create 2023/3/27
 */
@Service
public class FolderRefServiceImpl extends ServiceImpl<FolderRefMapper, FolderRefEntity> implements FolderRefService {
    @Override
    public Integer getId() {
        return baseMapper.getId();
    }
}
