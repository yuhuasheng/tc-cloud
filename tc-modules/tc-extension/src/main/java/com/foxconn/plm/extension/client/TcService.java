package com.foxconn.plm.extension.client;


import com.foxconn.plm.tcapi.domain.dto.AjaxResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


/**
 * @author Robert
 */
@FeignClient("tc-service")
public interface TcService {

    @GetMapping(value = "/materialInfo/getMaterialGroupAndBaseUnit")
    AjaxResult getMaterialGroupAndBaseUnit(@RequestParam("materialNum") String materialNum);
}
