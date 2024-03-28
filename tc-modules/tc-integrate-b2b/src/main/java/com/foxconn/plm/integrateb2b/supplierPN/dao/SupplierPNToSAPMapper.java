package com.foxconn.plm.integrateb2b.supplierPN.dao;

import com.foxconn.plm.entity.param.MakerPNRp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SupplierPNToSAPMapper {

    List<MakerPNRp> getMaterialList(@Param("changeNum") String changeNum, @Param("plant") String plant);

}
