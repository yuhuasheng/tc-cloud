package com.foxconn.plm.spas.service;

import com.foxconn.plm.spas.bean.SynSpasChangeData;
import com.foxconn.plm.spas.bean.SynSpasHandleResults;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.teamcenter.soa.client.model.strong.Folder;

import java.util.List;

/**
 * @Author {chen.zhang@foxconn.com}
 * @Date: 2022/12/12/ 8:59
 * @description
 */
public abstract class SynTcChangeDataService {

    public List<SynSpasChangeData> querySynSpasChangeData() {
        return null;
    }

    public void addSynSpasChangeDataHandleResults(SynSpasHandleResults synSpasHandleResults) {
    }



    public abstract void synSpasDataToTc(TCSOAServiceFactory tCSOAServiceFactory, SynSpasChangeData synSpasChangeData) throws Exception;



}
