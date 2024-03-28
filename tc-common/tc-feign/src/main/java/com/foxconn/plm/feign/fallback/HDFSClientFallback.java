package com.foxconn.plm.feign.fallback;

import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.exception.BizException;
import com.foxconn.plm.entity.param.BUListRp;
import com.foxconn.plm.entity.response.BURv;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.feign.service.HDFSClient;



import java.util.List;

/**
 * @ClassName: HDFSClientFallback
 * @Description:
 * @Author DY
 * @Create 2023/2/1
 */
public class HDFSClientFallback implements HDFSClient {
    @Override
    public R<List<BURv>> buList(BUListRp rp) throws BizException {
        throw new BizException(HttpResultEnum.NET_ERROR.getCode(),"feign遠程調用HDFS接口失敗");
    }
}
