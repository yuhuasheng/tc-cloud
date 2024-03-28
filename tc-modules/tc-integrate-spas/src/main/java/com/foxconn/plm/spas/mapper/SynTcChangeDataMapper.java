package com.foxconn.plm.spas.mapper;

import com.foxconn.plm.spas.bean.ManpowerPhaseInfo;
import com.foxconn.plm.spas.bean.SynSpasChangeData;
import com.foxconn.plm.spas.bean.SynSpasHandleResults;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @Author {chen.zhang@foxconn.com}
 * @Date: 2022/12/12/ 10:58
 * @description
 */
@Mapper
public interface SynTcChangeDataMapper {

    List<SynSpasChangeData> querySynSpasChangeData();

    void addSynSpasChangeDataHandleResults(SynSpasHandleResults synSpasHandleResults);

    List<ManpowerPhaseInfo> getManPowerFunction(String projectId)throws Exception ;

    Integer getHandleStatusCnt(Integer projectId)throws Exception ;

}
