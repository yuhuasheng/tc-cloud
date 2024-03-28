package com.foxconn.plm.extension.avl.mapper;

import com.foxconn.plm.extension.avl.domain.PartModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PartMapper {

    @Select("select *  from view_items_tc where  lastmodifieddate >= #{formDate} and  UPPER(in_cis) != 'Y'  ")
    List<PartModel> findPartsByDate(LocalDateTime formDate);


    @Select("select *  from view_items_tc where  UPPER(in_cis) != 'Y'  ")
    List<PartModel> findAllParts();
}
