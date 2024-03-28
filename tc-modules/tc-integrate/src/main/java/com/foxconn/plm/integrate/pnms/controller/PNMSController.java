package com.foxconn.plm.integrate.pnms.controller;

import com.foxconn.plm.integrate.pnms.service.UpdateItemServices;
import com.foxconn.plm.integrate.pnms.service.WebServicesClient;
import org.springframework.context.annotation.Scope;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

@RestController
@RequestMapping("/pnms")
@Scope("request")
public class PNMSController {

    @Resource
    private WebServicesClient webSerivcesClient;

    @Resource
    private UpdateItemServices updateItemServices;

    @GetMapping("/getHHPNInfo")
    public String getHHPNInfo(String hhpn) {
        if (StringUtils.hasLength(hhpn)) {
            return webSerivcesClient.queryHHPn(hhpn);
        }
        return "";
    }

    @GetMapping("/getHHPNInfoByMfg")
    public String getHHPNInfoByMfg(String mfgPn, String mfg) {
        if (StringUtils.hasLength(mfgPn)) {
            return webSerivcesClient.queryHHPn(mfg, mfgPn);
        }
        return "";
    }


    @PostMapping("/updateTCItem")
    public String updateTCItem(@RequestBody Map<String, String> requestMap) throws Exception {
        System.out.println("requestMap :: " + requestMap);
        String itemID = requestMap.get("bupn");
        requestMap.put("bupn", "");
        return updateItemServices.updateItem(itemID, requestMap);
    }
}
