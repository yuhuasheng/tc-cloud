package com.foxconn.plm.integrate.sap.maker.controller;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSONArray;
import com.foxconn.plm.entity.param.MakerPNRp;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.integrate.sap.customPN.utils.ConnectPoolUtils;
import com.foxconn.plm.integrate.sap.customPN.utils.SAPConstants;
import com.foxconn.plm.integrate.sap.maker.domain.MakerInfoEntity;
import com.foxconn.plm.integrate.sap.maker.domain.MakerInfor;
import com.foxconn.plm.integrate.sap.maker.domain.rp.SearchMakerRp;
import com.foxconn.plm.integrate.sap.maker.service.MakerService;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@Api(tags = "供应商管理")
@RestController
@RequestMapping("/maker")
public class MakerManageController {
    private static Log log = LogFactory.get();
    @Resource
    private MakerService makerService;

    @ApiOperation("查询供应商信息")
    @PostMapping("/searchMakerInfo")
    public R<List<MakerInfoEntity>> searchMakerInfo(@RequestBody SearchMakerRp searchMakerRp) {
        log.info("searchMakerInfo=========================" + searchMakerRp.getMakerCode() + "  " + searchMakerRp.getMakerName());


        List<MakerInfoEntity> makerInfoEntitys = makerService.searchMakerInfo(searchMakerRp);
        return R.success(makerInfoEntitys);
    }


    @ApiOperation("抛轉供應商信息")
    @PostMapping("/postMakerPN")
    public R<String> postMakerPN(@RequestBody List<MakerPNRp> makerPNRps) {
        JCoDestination destination = null;
        JCoDestination destination888 = null;
        JCoDestination destination868 = null;
        String msg = "";
        try {
            log.info("begin post maker info");
            if (SAPConstants.SAP_IP == null || "".equalsIgnoreCase(SAPConstants.SAP_IP)) {
                throw new Exception("ahost is null");
            }
            log.info(JSONArray.toJSONString(makerPNRps));
            List<MakerInfor> makeinfos = null;
            destination = JCoDestinationManager.getDestination(ConnectPoolUtils.ABAP_AS_POOLED);
            destination888 = JCoDestinationManager.getDestination(ConnectPoolUtils.ABAP_AS_POOLED_888);
            destination868 = JCoDestinationManager.getDestination(ConnectPoolUtils.ABAP_AS_POOLED_868);
            makeinfos = makerService.postMakerPN(destination, destination888, destination868, makerPNRps);

            return R.success(JSONArray.toJSONString(makeinfos));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            msg = e.getMessage();
        }
        log.info("end post maker info");
        return R.success(msg);
    }

}
