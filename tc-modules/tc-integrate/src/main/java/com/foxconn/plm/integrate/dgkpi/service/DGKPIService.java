package com.foxconn.plm.integrate.dgkpi.service;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSONObject;
import com.foxconn.plm.entity.constants.*;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.feign.service.TcServiceClient;
import com.foxconn.plm.integrate.dgkpi.mapper.DesignStandardMapper;
import com.foxconn.plm.integrate.dgkpi.service.ext.MEBomService;
import com.foxconn.plm.integrate.dgkpi.service.ext.PacDBomService;
import com.foxconn.plm.integrate.dgkpi.service.ext.URLService;
import com.foxconn.plm.rma.RemoteBaseObjectService;
import com.foxconn.plm.utils.string.StringUtil;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.locks.ReentrantLock;

@Service("dGKPIService")
public   class DGKPIService extends RemoteBaseObjectService {
    private static Log log = LogFactory.get();
    @Autowired(required = false)
    DesignStandardMapper designStandardMapper;
    private Cache<String,String> requestCache = CacheUtil.newFIFOCache(20);
    private ReentrantLock lock = new ReentrantLock(true);

    @Value("${kpi.hdfs}")
    private String hdfs;
    @Value("${kpi.fsurl}")
    private String fsurl;

    @Autowired(required = false)
    MEBomService mEBomService;


    @Autowired(required = false)
    PacDBomService pacDBomService;

    @Autowired(required = false)
    URLService uRLService;


    @Resource
    private TcServiceClient tcServiceClient;

    @Deprecated
    public R getMEDBOM(JSONObject paramJSONObject){
        return mEBomService.getMEDBOM(paramJSONObject);
    }

    @ApiOperation("查詢DGKPI BOM 結構數據")
    public R getBOM(JSONObject paramJSONObject){
        String  bomType=paramJSONObject.getString("bomType");
        String  function=paramJSONObject.getString("function");

        if("MEDBOM".equalsIgnoreCase(bomType)
         || ("ME".equalsIgnoreCase(function)&&"DesignBOM".equalsIgnoreCase(bomType))
         || ("ME".equalsIgnoreCase(function)&&"DBOM".equalsIgnoreCase(bomType))
        ){
            return  mEBomService.getMEDBOM(paramJSONObject);
        }
        if(("PAC".equalsIgnoreCase(function)&&"DBOM".equalsIgnoreCase(bomType))
        ){
            return  pacDBomService.getMEDBOM(paramJSONObject);
        }
       return R.error(HttpResultEnum.PARAM_ERROR.getCode(),"不支持的参数");
    }

    public R getURL(JSONObject paramJSONObject){
           return  uRLService.getURLByQuery(paramJSONObject);
    }


    public R getFeePer(JSONObject paramJSONObject) {
        if ("DCNFeePer".equalsIgnoreCase(paramJSONObject.getString("feeType"))) {
            String projectId = paramJSONObject.getString("spasProjId");
            if (StringUtil.isEmpty(projectId)) {
                return R.error(HttpResultEnum.PARAM_ERROR.getCode(), HttpResultEnum.PARAM_ERROR.getMsg());
            }
            return tcServiceClient.getDCNFeePerByProject(projectId, null);
        }
       return R.error(HttpResultEnum.API_NOT_FOUND.getCode(),"接口方法不存在");
    }


    @Override
    public String getObject() {
        return TCAPIConstant.DGKPI;
    }




}
