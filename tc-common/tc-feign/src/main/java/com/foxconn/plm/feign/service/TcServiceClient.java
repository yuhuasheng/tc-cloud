package com.foxconn.plm.feign.service;

import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.feign.fallback.TcServiceClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @Author MW00333
 * @Date 2023/5/11 16:44
 * @Version 1.0
 */
@FeignClient(name = "tc-service", fallback = TcServiceClientFallback.class)
public interface TcServiceClient {

    @GetMapping("/DCNReport/getDCNFeePerByProject")
    R getDCNFeePerByProject(@RequestParam("projectId") String projectId, @RequestParam(value = "owner", required = false) String owner);
}
