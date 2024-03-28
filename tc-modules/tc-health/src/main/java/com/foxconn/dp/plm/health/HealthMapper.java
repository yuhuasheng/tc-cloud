package com.foxconn.dp.plm.health;


import com.foxconn.plm.entity.response.HealthStatusRv;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface HealthMapper {

    @Select("select * from tc_health")
    List<HealthStatusRv> getList();

    @Select("select count(1) from tc_health h where h.service = #{service}")
    int count(String service);

    @Insert("insert into tc_health (service)values(#{service})")
    int insert(String service);

    @Update("update tc_health set status = '正常',jvmMaxMemory=#{jvmMaxMemory},jvmTotalMemory=#{jvmTotalMemory},jvmUsedMemory=#{jvmUsedMemory},jvmThreadNum=#{jvmThreadNum},faultTime = null where service = #{service}")
    int success(HealthStatusRv rp);

    @Select("select count(1) from tc_health h where h.service = #{service} and status = '故障'")
    int faultCount(String service);

    @Update("update tc_health set status = '故障',faultTime = sysdate ,jvmMaxMemory=null,jvmTotalMemory=null,jvmUsedMemory=null,jvmThreadNum=null where service = #{service}")
    int fault(String service);


}
