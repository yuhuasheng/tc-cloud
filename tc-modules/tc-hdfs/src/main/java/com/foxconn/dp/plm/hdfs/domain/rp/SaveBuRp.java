package com.foxconn.dp.plm.hdfs.domain.rp;

import lombok.Data;

@Data
public class SaveBuRp {
    private String id;
    private long customerId;
    private long productLineId;
    private long buId;
}
