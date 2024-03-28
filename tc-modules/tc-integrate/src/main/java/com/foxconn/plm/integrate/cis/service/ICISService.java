package com.foxconn.plm.integrate.cis.service;

import com.foxconn.plm.integrate.cis.domain.PartEntity;
import com.foxconn.plm.integrate.cis.domain.ThreeDDrawingBean;
import com.foxconn.plm.integrate.log.domain.ActionLogRp;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.query.SavedQueryService;

import java.util.List;

public interface ICISService {
    public List<PartEntity> getNotSyncPart();

    public List<PartEntity> getPartById(int id);

    public void updateSync(int id);

    public List<ThreeDDrawingBean> getThreeDDrawingRecord();

    List<ActionLogRp> recordCisSyncTC(DataManagementService dataManagementService, SavedQueryService savedQueryService);
}
