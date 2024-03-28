package com.foxconn.plm.spas.service;

import com.foxconn.plm.spas.bean.SynSpasChangeData;

import java.util.List;

/**
 * @Author {chen.zhang@foxconn.com}
 * @Date: 2022/12/08/ 16:06
 * @description
 */
public interface SynSpasChangeDataService {

    List<SynSpasChangeData> querySynSpasChangeData(String startDate, String endDate) throws Exception;

    Integer querySynSpasChangeDataRecord(Integer id) throws Exception;

    void addSynSpasChangeData(List<SynSpasChangeData> synSpasChangeData) throws Exception;

}
