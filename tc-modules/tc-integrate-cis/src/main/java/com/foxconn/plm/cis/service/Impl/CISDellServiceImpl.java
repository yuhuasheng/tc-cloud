package com.foxconn.plm.cis.service.Impl;

import cn.hutool.core.date.DateTime;
import com.foxconn.plm.cis.domain.PartEntity;
import com.foxconn.plm.cis.domain.ThreeDDrawingBean;
import com.foxconn.plm.cis.mapper.cisdell.CISDellMapper;
import com.foxconn.plm.cis.service.ICISService;
import com.foxconn.plm.cis.config.dataSource.DataSource;
import com.foxconn.plm.cis.config.dataSource.DataSourceType;
import com.foxconn.plm.entity.param.ActionLogRp;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.query.SavedQueryService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@DataSource(value = DataSourceType.CISDELL)
@Service
public class CISDellServiceImpl implements ICISService {

    @Resource
    CISDellMapper cisDellMapper;

    @Override
    public List<PartEntity> getNotSyncPart() {
        DateTime date = cn.hutool.core.date.DateUtil.date();
        DateTime startTime = cn.hutool.core.date.DateUtil.beginOfDay(date);
        DateTime endTime = cn.hutool.core.date.DateUtil.endOfDay(date);
        return cisDellMapper.getNotSyncPart(startTime,endTime);
    }


    @Override
    public List<PartEntity> getPartById(int id) {
        return cisDellMapper.getPartById(id);
    }

    @Override
    public void updateSync(int id) {
        cisDellMapper.updateSync(id);
    }

    @Override
    public List<ThreeDDrawingBean> getThreeDDrawingRecord() {
        return cisDellMapper.getThreeDDrawingRecord();
    }

    @Override
    public List<ActionLogRp> recordCisSyncTC(DataManagementService dataManagementService, SavedQueryService savedQueryService) {
        return null;
    }
}
