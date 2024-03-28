package com.foxconn.plm.entity.param;

import com.foxconn.plm.entity.RequestParam;
import lombok.Data;

@Data
public class PageParam extends RequestParam {
    int pageIndex = 1;
    int pageSize = 10;
}
