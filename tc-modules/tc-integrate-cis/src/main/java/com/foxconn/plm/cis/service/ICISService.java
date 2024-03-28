package com.foxconn.plm.cis.service;

import com.foxconn.plm.cis.domain.PartEntity;
import com.foxconn.plm.cis.domain.ThreeDDrawingBean;
import com.foxconn.plm.entity.param.ActionLogRp;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.query.SavedQueryService;

import java.util.List;

public interface ICISService {
     List<PartEntity> getNotSyncPart();

     List<PartEntity> getPartById(int id);

     void updateSync(int id);

     List<ThreeDDrawingBean> getThreeDDrawingRecord();

     List<ActionLogRp> recordCisSyncTC(DataManagementService dataManagementService, SavedQueryService savedQueryService);
}
