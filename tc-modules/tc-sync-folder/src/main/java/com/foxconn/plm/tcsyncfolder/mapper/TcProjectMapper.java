package com.foxconn.plm.tcsyncfolder.mapper;

import com.foxconn.plm.tcsyncfolder.vo.FolderVo;
import com.foxconn.plm.tcsyncfolder.vo.ProjectVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @ClassName: TcProjectMapper
 * @Description:
 * @Author DY
 * @Create 2023/3/27
 */
@Mapper
public interface TcProjectMapper  {

    ProjectVo getTcProjectInfo(@Param("spasId") String spasId);

    List<ProjectVo> getAllProject();

    List<FolderVo> getChildFolder(@Param("parentId") String parentId);

    FolderVo getByUid(@Param("puid") String puid);
}
