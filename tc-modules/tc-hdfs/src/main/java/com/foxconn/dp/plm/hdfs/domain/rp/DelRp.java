package com.foxconn.dp.plm.hdfs.domain.rp;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.List;

@Data
@ApiModel
public class DelRp {
    List<Long> idList;
    Long id;
}
