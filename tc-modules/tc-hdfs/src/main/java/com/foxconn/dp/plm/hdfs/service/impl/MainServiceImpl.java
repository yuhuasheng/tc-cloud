package com.foxconn.dp.plm.hdfs.service.impl;

import com.foxconn.dp.plm.hdfs.dao.xplm.ConfigMapper;
import com.foxconn.dp.plm.hdfs.domain.entity.LOVEntity;
import com.foxconn.dp.plm.hdfs.service.MainService;
import com.foxconn.plm.utils.string.StringUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MainServiceImpl implements MainService {

    @Resource
    ConfigMapper configMapper;

    @Override
    public Map<String, List<LOVEntity>> getLOV(List<String> list) {
        Map<String, List<LOVEntity>> result = new HashMap<>();
        for (String name : list) {
            List<LOVEntity> lov = configMapper.getLOV(name);
            result.put(name, lov);
        }
        return result;
    }

    @Override
    public List<LOVEntity> getLOV(String name) {
        ArrayList<String> list = new ArrayList<>();
        list.add(name);
        Map<String, List<LOVEntity>> lov = getLOV(list);
        return lov.get(name);
    }

    @Override
    public List<String> getAllDept() {
        return configMapper.getAllDept();
    }

    @Override
    public String getLoginSite(String ip) {
        if (StringUtil.isEmpty(ip)) {
            return "WH";
        }
        String[] split = ip.split("\\.");
        String ipTop2 = split[0] + '.' + split[1];
        List<LOVEntity> siteIpConfig = getLOV("SiteIpConfig");
        for (LOVEntity lovEntity : siteIpConfig) {
            String[] site = lovEntity.getValue().split("=");
            String siteCode = site[0];
            String siteIps = site[1];
            split = siteIps.split("/");
            for (String ipPatten : split) {
                if (ipPatten.startsWith(ipTop2)) {
                    return siteCode;
                }
            }
        }
        return "WH";
    }


}
