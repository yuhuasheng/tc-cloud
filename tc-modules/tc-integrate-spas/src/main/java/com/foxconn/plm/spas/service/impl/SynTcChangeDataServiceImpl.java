package com.foxconn.plm.spas.service.impl;

import com.foxconn.plm.spas.bean.SynSpasChangeData;
import com.foxconn.plm.spas.bean.SynSpasHandleResults;
import com.foxconn.plm.spas.mapper.SynTcChangeDataMapper;
import com.foxconn.plm.spas.service.SynTcChangeDataService;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Author {chen.zhang@foxconn.com}
 * @Date: 2022/12/12/ 10:43
 * @description
 */
@Service("synTcChangeDataServiceImpl")
public class SynTcChangeDataServiceImpl extends SynTcChangeDataService {

    @Resource
    private SynTcChangeDataMapper synTcChangeDataMapper;

    @Override
    public List<SynSpasChangeData> querySynSpasChangeData() {
        return synTcChangeDataMapper.querySynSpasChangeData();
    }

    ;

    @Override
    public void addSynSpasChangeDataHandleResults(SynSpasHandleResults synSpasHandleResults) {
        synTcChangeDataMapper.addSynSpasChangeDataHandleResults(synSpasHandleResults);
    }



    @Override
    public void synSpasDataToTc(TCSOAServiceFactory tCSOAServiceFactory, SynSpasChangeData synSpasChangeData) throws Exception {

    }


}
