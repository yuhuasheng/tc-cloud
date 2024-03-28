package com.foxconn.plm.feign.service;


import com.foxconn.plm.entity.exception.BizException;
import com.foxconn.plm.entity.param.BUListRp;
import com.foxconn.plm.entity.response.BURv;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.feign.fallback.HDFSClientFallback;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name="tc-hdfs",fallback = HDFSClientFallback.class)
public interface HDFSClient {

    @GetMapping(value = "buManage/buList")
    R<List<BURv>> buList(@SpringQueryMap BUListRp rp) throws BizException;

}
