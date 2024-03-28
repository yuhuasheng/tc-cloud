package com.foxconn.plm.integrate.sap.rfc.controller;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.param.PartPNRp;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.integrate.agile.domain.HHPNPojo;
import com.foxconn.plm.integrate.sap.rfc.RfcService;
import com.foxconn.plm.integrate.sap.rfc.domain.rp.PNSupplierInfo;
import com.foxconn.plm.integrate.sap.rfc.mapper.SAPSupplierMapper;
import com.foxconn.plm.integrate.sap.utils.DestinationUtils;
import com.sap.conn.jco.JCoDestination;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

@Api(tags = "call sap rfc")
@RestController
@RequestMapping("/rfc")
@Scope("request")
public class RfcController {
    private static Log log = LogFactory.get();
    @Resource
    private RfcService rfcService;


    @Resource
    private SAPSupplierMapper sapSupplierMapper;


    @ApiOperation("從SAP獲取物料信息")
    @PostMapping("/getPartInfo")
    public String getPartInfo(@RequestBody String json) {
        try {
            log.info("getHHPNInfo=========================" + json);
            List<HHPNPojo> hhpnPojos = JSON.parseArray(json, HHPNPojo.class);
            HashMap<String, List<HHPNPojo>> mp = new HashMap<String, List<HHPNPojo>>();
            for (HHPNPojo h : hhpnPojos) {
                String plant = h.getPlant();
                if (plant == null || "".equalsIgnoreCase(plant)) {
                    continue;
                }
                List<HHPNPojo> tmps = mp.get(plant);
                if (tmps == null) {
                    tmps = new ArrayList<>();
                    mp.put(plant, tmps);
                }
                tmps.add(h);
            }

            Set<String> keys = mp.keySet();
            for (String plant : keys) {
                List<HHPNPojo> ls = mp.get(plant);
                JCoDestination destination = DestinationUtils.getJCoDestination(plant);
                rfcService.upMaterialInfos(destination, ls, plant);
            }

            return JSONArray.toJSONString(hhpnPojos);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    @PostMapping("/compareSupplerInfo")
    public R<List<PNSupplierInfo>> compareSupplerInfo(@RequestBody List<PNSupplierInfo> supplerList, String plant) {
        try {
            if (supplerList != null && supplerList.size() > 0) {
                List<PNSupplierInfo> sapList = rfcService.getSAPSupplierByTCDB(supplerList, plant);
                List<PNSupplierInfo> resultList = rfcService.compareSupplierInfo(supplerList, sapList);
                return R.success(resultList);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(), e.getLocalizedMessage());
        }
        return R.success();
    }


    @PostMapping("/isExistInSAP")
    public List<PartPNRp> isExistInSAP(@RequestBody List<PartPNRp> parts) {
        try {
            return rfcService.isExistInSAP(parts);
        } catch (Exception e) {
            log.info(e.getLocalizedMessage());
            log.error(e.getLocalizedMessage(),e);
            return null;
        }
    }


    @PostMapping("/batchUpPartInfo")
    public String batchUpPartInfo() {
        try {
            log.info("batchUpPartInfo=========================");


            rfcService.batchUpPartInfo();


        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

}
