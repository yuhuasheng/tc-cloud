package com.foxconn.plm.rma;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
  TC对外接口工程类 ，根据api id  获取对应的接口服务
 * @Class: DGKPIFactory

 * @Version: 1.0
 * @date 2020/8/29 18:59
 */
@Service("remoteObjectServiceFactory")
public class RemoteObjectServiceFactory {


    @Autowired(required=false)
    Map<String, RemoteBaseObjectService> remoteBaseObjectServiceMap;


    public RemoteBaseObjectService getObjectService(String object) throws Exception {

        Set<String> keys=remoteBaseObjectServiceMap.keySet();
        for(String key:keys){
            RemoteBaseObjectService baseTCIntegrateService=remoteBaseObjectServiceMap.get(key);
            if(baseTCIntegrateService!=null && baseTCIntegrateService.getObject().equalsIgnoreCase(object)){
                return baseTCIntegrateService;
            }
        }
        return null;
    }





}
