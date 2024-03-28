package com.foxconn.plm.spas.mapper;

import com.foxconn.plm.spas.bean.*;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SynSpasWorkItemMapper {

    int saveWorkItem(SpasWorkItem workItem);
    int delete(String startDate,String endDate);
    List<String> tcProjectList();


}
