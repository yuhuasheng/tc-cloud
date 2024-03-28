package com.foxconn.plm.spas.service.impl;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.foxconn.plm.spas.config.dataSource.DataSource;
import com.foxconn.plm.spas.config.dataSource.DataSourceType;
import com.foxconn.plm.spas.bean.SynSpasChangeData;
import com.foxconn.plm.spas.config.properties.SpasPropertiesConfig;
import com.foxconn.plm.spas.mapper.SynSpasChangeDataMapper;
import com.foxconn.plm.spas.service.SynSpasChangeDataService;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;

/**
 * @Author {chen.zhang@foxconn.com}
 * @Date: 2022/12/08/ 16:14
 * @description
 */
@Service
public class SynSpasChangeDataServiceImpl implements SynSpasChangeDataService {

    @Resource
    private SpasPropertiesConfig spasPropertiesConfig;

    @Resource
    private SynSpasChangeDataMapper synSpasChangeDataMapper;

    @Override
    public List<SynSpasChangeData> querySynSpasChangeData(String startDate, String endDate) throws Exception {
        String url = spasPropertiesConfig.getUrl()
                + "/project-server/api/project/tc/stiProjectList?startDate="
                + startDate + "&endDate=" + endDate;
        return getSpasChangeData(url);
    }

    @Override
    public Integer querySynSpasChangeDataRecord(Integer id) throws Exception {
        return synSpasChangeDataMapper.querySynSpasChangeDataRecord(id);
    }

    @Override
    public void addSynSpasChangeData(List<SynSpasChangeData> synSpasChangeData) throws Exception {
        synSpasChangeDataMapper.addSynSpasChangeData(synSpasChangeData);
    }

    private List<SynSpasChangeData> getSpasChangeData(String url) throws Exception {
        String spasToken = getSpasToken();
        if (spasToken == null) {
            throw new Exception("获取 Token 失败！");
        }
        HashMap<String, String> tokenMap = new HashMap<>();
        tokenMap.put("token", spasToken);
        HttpResponse rp = HttpUtil.createGet(url).addHeaders(tokenMap).execute();
        System.out.print(rp.body());
        String result = rp.body();
        JSONObject obj = JSONObject.parseObject(result);
        return JSONArray.parseArray(obj.getJSONArray("data").toJSONString(), SynSpasChangeData.class);
    }

    private String getSpasToken() {
        String url = spasPropertiesConfig.getUrl() + "/user-server/api/user/sysSignIn";
        JSONObject parameterMap = new JSONObject();
        parameterMap.put("sysFlag", spasPropertiesConfig.getSysFlag());
        parameterMap.put("apiKey", spasPropertiesConfig.getApiKey());
        parameterMap.put("userName", spasPropertiesConfig.getUserName1());
        parameterMap.put("password", spasPropertiesConfig.getPassword1());
        JSONObject parameterJson = new JSONObject(parameterMap);
        String rs = HttpUtil.post(url, parameterJson.toString());
        JSONObject obj = JSONObject.parseObject(rs);
        if (200 == obj.getInteger("code")) {
            return obj.getString("data");
        }
        return null;
    }

}
