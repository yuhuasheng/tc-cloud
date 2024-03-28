package com.foxconn.plm.tcservice.mapper.master;

import com.foxconn.plm.tcservice.certificate.ProjectPojo;
import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface ProjectPMMapper {

    ProjectPojo getProjectPMInfo(String pId);

    String getUserMail(String userName);
}
