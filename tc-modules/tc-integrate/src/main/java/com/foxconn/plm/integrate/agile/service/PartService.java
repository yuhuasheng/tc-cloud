package com.foxconn.plm.integrate.agile.service;


import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.foxconn.plm.integrate.agile.domain.HHPNPojo;
import com.foxconn.plm.integrate.pnms.service.WebServicesClient;
import com.foxconn.plm.integrate.sap.rfc.RfcService;
import com.foxconn.plm.integrate.sap.utils.DestinationUtils;
import com.foxconn.plm.utils.net.HttpUtil;
import com.foxconn.plm.utils.property.BaseUnitPropertitesUtil;
import com.foxconn.plm.utils.property.MaterialGroupPropertitesUtil;
import com.sap.conn.jco.JCoDestination;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class PartService {
    private static Log log = LogFactory.get();
    @Resource
    private WebServicesClient webSerivcesClient;

    @Resource
    private RfcService rfcService;

    /**
     * agile webservice 網址
     */
    @Value("${agile.partinfoWeb}")
    private String agileUrl;

    /**
     * 调用接口获取物料信息
     *
     * @param lists
     * @throws Exception
     */
    public void updatePartInfos(List<HHPNPojo> lists) throws Exception {

        String dataFrom = "";
        String plant = "";
        for (HHPNPojo hhpnPojo : lists) {
            //TC已有的物料过滤掉
            if (hhpnPojo.getIsExistInTC() == 0) {
                dataFrom = hhpnPojo.getDataFrom();
                plant = hhpnPojo.getPlant();
                break;
            }
        }

        if ("sap".equalsIgnoreCase(dataFrom)) {
            JCoDestination destination = DestinationUtils.getJCoDestination(plant);
            rfcService.upMaterialInfos(destination, lists, plant);
        } else if ("DT L5Agile".equalsIgnoreCase(dataFrom) || "DT L6Agile".equalsIgnoreCase(dataFrom) || "PRT Agile".equalsIgnoreCase(dataFrom)) {
            String rs = HttpUtil.post(agileUrl, JSONArray.toJSONString(lists));
            List<HHPNPojo> ls = JSONArray.parseArray(rs, HHPNPojo.class);
            lists.clear();
            lists.addAll(ls);
        } else if ("pnms".equalsIgnoreCase(dataFrom)) {
            for (HHPNPojo hhpnPojo : lists) {
                try {
                    if (hhpnPojo.getIsExistInTC() == 1) {
                        continue;
                    }
                    String itemId = hhpnPojo.getItemId();
                  //  if (itemId.endsWith("-H")) {
                      //  itemId = itemId.substring(0, itemId.length() - 2);
                      //  itemId = itemId + "-G";
                   // }

                    String rs = webSerivcesClient.queryHHPnByEnv(itemId, hhpnPojo.getPnms()==0);
                    if (rs == null || "".equalsIgnoreCase(rs.trim())) {
                        continue;
                    }
                    JSONObject obj = JSONObject.parseObject(rs);
                    String des = obj.getString("des") == null ? "" : obj.getString("des");
                    hhpnPojo.setDescr(des);

                    String enDes = obj.getString("enDes") == null ? "" : obj.getString("enDes");
                    hhpnPojo.setEnDescr(enDes);


                    String cnDes = obj.getString("cnDes") == null ? "" : obj.getString("cnDes");
                    hhpnPojo.setCnDescr(cnDes);


                    //供应商料号
                    String mfgpn = obj.getString("mfgpn") == null ? "" : obj.getString("mfgpn");
                    hhpnPojo.setMfgPN(mfgpn);

                    //供应商
                    String mfg = obj.getString("mfg") == null ? "" : obj.getString("mfg");
                    hhpnPojo.setMfg(mfg);

                    String mg = MaterialGroupPropertitesUtil.props.getProperty(hhpnPojo.getItemId().substring(0, 4));
                    if (mg == null) {
                        mg = "";
                    }
                    hhpnPojo.setMaterialGroup(mg);

                    String baseUnit = BaseUnitPropertitesUtil.props.getProperty(mg);
                    if (baseUnit == null) {
                        baseUnit = "";
                    }
                    hhpnPojo.setUnit(baseUnit);
                } catch (Exception e) {
                }
            }

        }


    }


}
