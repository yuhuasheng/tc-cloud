package com.foxconn.plm.utils.tc;

import cn.hutool.core.util.StrUtil;
import com.teamcenter.services.strong.core.LOVService;
import com.teamcenter.services.strong.core._2013_05.LOV;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Author HuashengYu
 * @Date 2023/3/9 16:54
 * @Version 1.0
 */
public class LOVUtil {

    /**
     * 获取LOV下拉值
     *
     * @param lovService   LOV服务类
     * @param boName
     * @param propertyName 属性名
     * @return
     * @throws Exception
     */
    public static List<Map<String, String[]>> getLovValue(LOVService lovService, String boName, String propertyName) throws Exception {
        List<Map<String, String[]>> list = new ArrayList<>();
        LOV.InitialLovData initLovData = new LOV.InitialLovData();
        LOV.LovFilterData LovFilterData = new LOV.LovFilterData();
        LovFilterData.filterString = "";
        LovFilterData.maxResults = 9999;
        LovFilterData.numberToReturn = 50;
        LovFilterData.order = 0;
        initLovData.filterData = LovFilterData;
        LOV.LOVInput lovInput = new LOV.LOVInput();
        lovInput.boName = boName;
        lovInput.operationName = "Create";
        initLovData.lovInput = lovInput;
        initLovData.propertyName = propertyName;
        LOV.LOVSearchResults response = lovService.getInitialLOVValues(initLovData);
        String result = TCUtils.getErrorMsg(response.serviceData);
        if (StrUtil.isNotEmpty(result)) {
            throw new Exception(result);
        }

        LOV.LOVValueRow[] lovValues = response.lovValues;
        if (lovValues == null || lovValues.length <= 0) {
            return null;
        }

        for (LOV.LOVValueRow row : lovValues) {
            Map<String, String[]> propDisplayValues = row.propDisplayValues;
            list.add(propDisplayValues);

        }
        list.removeIf(e -> e == null || e.size() <= 0);
        return list;
    }
}
