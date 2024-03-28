package com.foxconn.plm.feign.fallback;

import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.exception.BizException;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.feign.service.TcServiceClient;

/**
 * @Author MW00333
 * @Date 2023/5/11 16:45
 * @Version 1.0
 */
public class TcServiceClientFallback implements TcServiceClient {
    @Override
    public R getDCNFeePerByProject(String projectId, String owner) throws BizException {
        throw new BizException(HttpResultEnum.NET_ERROR.getCode(),"feign遠程調用TcService服務的getDCNFeePerByProject接口失敗");
    }
}
