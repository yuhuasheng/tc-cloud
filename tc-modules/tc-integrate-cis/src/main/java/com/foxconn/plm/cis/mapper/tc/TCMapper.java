package com.foxconn.plm.cis.mapper.tc;

import com.foxconn.plm.cis.domain.EE3DProjectBean;
import com.foxconn.plm.cis.domain.EE3DReportBean;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

@Mapper
public interface TCMapper {

    List<String> getChildFolderUidByName(@Param("parentFolderUid") String parentFolderUid, @Param("folderName") String folderName);

    int batchInsertEE3DReportBean(@Param("list") List<EE3DReportBean> list);

    void deleteAllEE3DReportBean();

    List<EE3DReportBean> selectEE3DReportBean(EE3DProjectBean bean);

    Set<String> selectConditionProject(EE3DProjectBean projectBean);
}
