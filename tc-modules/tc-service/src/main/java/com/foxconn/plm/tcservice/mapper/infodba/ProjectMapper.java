package com.foxconn.plm.tcservice.mapper.infodba;

import com.foxconn.plm.tcservice.project.ProjectBean;
import com.foxconn.plm.tcservice.tclicensereport.domain.LovEntity;
import com.foxconn.plm.tcservice.tclicensereport.domain.QueryRp;
import com.foxconn.plm.tcservice.tclicensereport.domain.ReportVO;
import com.foxconn.plm.tcservice.tclicensereport.domain.UserInfoVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * @Author HuashengYu
 * @Date 2022/7/13 9:06
 * @Version 1.0
 */
@Mapper
public interface ProjectMapper {

    List<ProjectBean> queryProjectByPrivilegeUser(String userId);
}
