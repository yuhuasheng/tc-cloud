package com.foxconn.plm.cis.utils;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.cis.enumconfig.CISConstants;
import com.foxconn.plm.utils.collect.CollectUtil;
import com.foxconn.plm.utils.string.StringUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.query.SavedQueryService;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CISUtils {

    private static Log log = LogFactory.get();

    public static List<Item> getHHPN(SavedQueryService queryService, DataManagementService dmService, String hhpn, String mfg, String mfgPN) {
        List<Item> HHPNList = new ArrayList<>();
        try {

            Map<String, Object> queryResults = TCUtils.executeQuery(queryService, CISConstants.FIND_PARTS,
                    new String[]{CISConstants.HHPN, CISConstants.MFG, CISConstants.MFG_PN}, new String[]{hhpn, mfg, mfgPN});

            if (queryResults.get("succeeded") != null) {
                ModelObject[] objs = (ModelObject[]) queryResults.get("succeeded");
                if (CollectUtil.isNotEmpty(objs)) {
                    for (int i = 0; i < objs.length; i++) {
                        Item item = (Item) objs[i];
                        TCUtils.getProperty(dmService, item, "item_id");
                        String itemId = item.get_item_id();
                        int strNum = StringUtil.findStrNum(itemId, "-");
                        if (strNum != 2) {
                            continue;
                        }
                        String[] itemIdSplit = itemId.split("-");
                        if (itemIdSplit[0].length() != 9 || itemIdSplit[1].length() != 3 || itemIdSplit[2].length() != 1) {
                            continue;
                        }
                        HHPNList.add(item);
                    }
                } else {
                    log.info("MFG:" + mfg + ",MFG_PN:" + mfgPN + ".未查询到数据！");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("MFG:" + mfg + ",MFG_PN:" + mfgPN + ". 查询出错：" + e.getMessage());
        }
        return HHPNList;
    }





}
