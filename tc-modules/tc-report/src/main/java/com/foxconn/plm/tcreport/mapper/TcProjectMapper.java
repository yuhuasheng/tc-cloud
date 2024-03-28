package com.foxconn.plm.tcreport.mapper;

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
public interface TcProjectMapper {

    String getFolderIdBySpasIdAndName(@Param("spasId") String spasId,@Param("folderName") String folderName);

    String getChildFolderIdByName(@Param("puid")String puid,@Param("folderName") String folderName);

    List<String> getChildFolderIdByNames(@Param("puid")String puid,@Param("nameList") List<String> nameList);

    String getFolderIdBySpasId(@Param("spasId") String spasId);
}
