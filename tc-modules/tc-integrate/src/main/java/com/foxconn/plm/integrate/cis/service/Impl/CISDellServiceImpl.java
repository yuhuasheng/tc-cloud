package com.foxconn.plm.integrate.cis.service.Impl;

import com.foxconn.plm.integrate.cis.domain.PartEntity;
import com.foxconn.plm.integrate.cis.domain.ThreeDDrawingBean;
import com.foxconn.plm.integrate.cis.mapper.cisdell.CISDellMapper;
import com.foxconn.plm.integrate.cis.service.ICISService;
import com.foxconn.plm.integrate.config.dataSource.DataSource;
import com.foxconn.plm.integrate.config.dataSource.DataSourceType;
import com.foxconn.plm.integrate.log.domain.ActionLogRp;
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
        return cisDellMapper.getNotSyncPart();
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
