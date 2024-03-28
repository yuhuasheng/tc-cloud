package com.foxconn.dp.plm.hdfs.dao.xplm;


import com.foxconn.dp.plm.hdfs.domain.entity.LOVEntity;
import com.foxconn.dp.plm.hdfs.domain.rp.SaveBuRp;
import com.foxconn.dp.plm.hdfs.domain.rv.LOVRv;
import com.foxconn.dp.plm.hdfs.domain.rv.ProductLineRv;

import com.foxconn.plm.entity.param.BUListRp;
import com.foxconn.plm.entity.response.BURv;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BUMapper {

    List<BURv> getBUList(BUListRp rp);

    List<LOVRv> getCustomerList();

    List<ProductLineRv> getProductLineList();

    int modify(SaveBuRp rp);

    int insert(SaveBuRp rp);

    int delete(long id, String userId);

    int exits(SaveBuRp rp);


}
