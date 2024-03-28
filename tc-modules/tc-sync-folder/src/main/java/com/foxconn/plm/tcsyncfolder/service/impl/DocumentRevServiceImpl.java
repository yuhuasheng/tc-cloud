package com.foxconn.plm.tcsyncfolder.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foxconn.plm.tcsyncfolder.entity.DocumentRevEntity;
import com.foxconn.plm.tcsyncfolder.mapper.DocumentRevMapper;
import com.foxconn.plm.tcsyncfolder.service.DocumentRevService;
import com.foxconn.plm.tcsyncfolder.vo.FileVo;
import org.springframework.stereotype.Service;

/**
 * @ClassName: DocumentRevServiceImpl
 * @Description:
 * @Author DY
 * @Create 2023/3/27
 */
@Service
public class DocumentRevServiceImpl extends ServiceImpl<DocumentRevMapper, DocumentRevEntity> implements DocumentRevService {

    @Override
    public Integer getId() {
        return baseMapper.getId();
    }

    @Override
    public FileVo getFileInfo(Integer docRevId) {
        return baseMapper.getFileInfo(docRevId);
    }


}
