package com.foxconn.plm.ops.service;

import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.entity.response.RList;
import com.foxconn.plm.ops.param.AddRecordParam;
import com.foxconn.plm.ops.param.DisableParam;
import com.foxconn.plm.ops.param.EditRecordParam;
import com.foxconn.plm.ops.param.SearchRecordParam;
import com.foxconn.plm.ops.response.SearchRecordRes;

import java.util.List;

/**
 *
 *
 * @Description
 * @Author MW00442
 * @Date 2024/1/2 11:27
 **/
public interface BusinessService {
    R addRecord(AddRecordParam param);

    R editRecord(EditRecordParam param);

    R delRecord(List<String> ids);

    RList<SearchRecordRes> searchRecord(SearchRecordParam param);

    R disable(DisableParam param);

    R execute(List<String> ids);
}
