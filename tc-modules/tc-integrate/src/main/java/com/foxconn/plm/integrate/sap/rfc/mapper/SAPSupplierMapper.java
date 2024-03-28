package com.foxconn.plm.integrate.sap.rfc.mapper;

import com.foxconn.plm.integrate.sap.rfc.domain.rp.MaterialRp;
import com.foxconn.plm.integrate.sap.rfc.domain.rp.PNSupplierInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SAPSupplierMapper {

    void batchInsert(List<PNSupplierInfo> list);

    void deleteAll();

    List<PNSupplierInfo> selectInPartPn(@Param("list") List<PNSupplierInfo> list, @Param("plant") String plant);


}
