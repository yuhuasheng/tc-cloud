package com.foxconn.dp.plm.hdfs.domain.rv;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductLineRv {
    private long id;
    private long customerId;
    private String name;
}
