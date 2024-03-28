package com.foxconn.dp.plm.hdfs.service;


import com.foxconn.dp.plm.hdfs.domain.rp.SaveBuRp;
import com.foxconn.dp.plm.hdfs.domain.rv.LOVRv;
import com.foxconn.dp.plm.hdfs.domain.rv.ProductLineRv;
import com.foxconn.plm.entity.param.BUListRp;
import com.foxconn.plm.entity.response.BURv;

import java.util.List;

public interface BUService {


    List<BURv> getBUList(BUListRp rp);

    List<LOVRv> getLovList(String name);

    List<ProductLineRv> getProductLineList();

    void save(SaveBuRp rp);

    void delete(long id, String user);


}
