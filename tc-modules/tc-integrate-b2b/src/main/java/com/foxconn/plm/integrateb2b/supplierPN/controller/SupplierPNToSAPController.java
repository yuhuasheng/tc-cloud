package com.foxconn.plm.integrateb2b.supplierPN.controller;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSONArray;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.integrateb2b.supplierPN.bean.SyncSupplierRp;
import com.foxconn.plm.integrateb2b.supplierPN.service.SupplierPNToSAPService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 抛磚供應商資訊
 */
@RestController
@RequestMapping("supplierpn")
public class SupplierPNToSAPController {
    private static Log log = LogFactory.get();
    @Resource
    SupplierPNToSAPService mNTToSAPService;

    @PostMapping(value = "/syncSupplier")
    @ResponseBody
    public R<String> syncSupplier(@RequestBody List<SyncSupplierRp> syncSupplierRps) {
        log.info("begin syncSupplier");
        try {
            log.info(JSONArray.toJSONString(syncSupplierRps));
            for (SyncSupplierRp syncSupplierRp : syncSupplierRps) {
                mNTToSAPService.syncSupplier(syncSupplierRp.getChangeNum(), syncSupplierRp.getPlantCode());
            }
        } catch (Exception e) {
            log.info(e.getMessage());
            log.error(e.getMessage(), e);
        }
        log.info("end  syncSupplier");
        return R.success();
    }
}
