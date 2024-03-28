package com.foxconn.plm.integrate.lbs.service;

import com.foxconn.plm.integrate.lbs.domain.SaveParam;
import com.foxconn.plm.integrate.lbs.domain.SyncRes;

import java.util.List;

/**
 * @ClassName: LBSService
 * @Description:
 * @Author DY
 * @Create 2022/12/15
 */
public interface LBSService {
    boolean saveEntity(SaveParam param);

    List<SyncRes> getList();

    boolean batchDelete(List<String> ids);
}
