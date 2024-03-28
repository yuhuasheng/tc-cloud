package com.foxconn.plm.integrateb2b.dataExchange.mapper;


import com.foxconn.plm.integrateb2b.dataExchange.domain.BOMActionInfo;
import com.foxconn.plm.integrateb2b.dataExchange.domain.MaterialInfo;
import com.foxconn.plm.integrateb2b.dataExchange.domain.TransferOrder;
import com.foxconn.plm.integrateb2b.dataExchange.domain.TransferOrderResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.HashMap;
import java.util.List;


@Mapper
public interface DataExchangeMapper {

    public void updateTransferOrder(TransferOrderResp transferOrderResp) throws Exception;

    public List<TransferOrder> getTransferOrders(List<String> list) throws Exception;


    public void updateSynFlag(TransferOrder transferOrder) throws Exception;

    void getBOMAction(@Param("map") HashMap map) throws Exception;

    public List<MaterialInfo> getMaterialInfo(@Param("changeNum") String changeNum) throws Exception;

    public List<BOMActionInfo> getBomMaterialInfo(@Param("changeNum") String changeNum) throws Exception;
}
