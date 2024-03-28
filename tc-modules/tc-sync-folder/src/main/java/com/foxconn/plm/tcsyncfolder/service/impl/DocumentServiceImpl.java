package com.foxconn.plm.tcsyncfolder.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foxconn.plm.tcsyncfolder.entity.DocumentEntity;
import com.foxconn.plm.tcsyncfolder.mapper.DocumentMapper;
import com.foxconn.plm.tcsyncfolder.service.DocumentService;
import org.springframework.stereotype.Service;

/**
 * @ClassName: DocumentServiceImpl
 * @Description:
 * @Author DY
 * @Create 2023/3/27
 */
@Service
public class DocumentServiceImpl extends ServiceImpl<DocumentMapper, DocumentEntity> implements DocumentService {
    @Override
    public Integer getId() {
        return baseMapper.getId();
    }
}
