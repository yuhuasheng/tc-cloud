package com.foxconn.plm.feign.fallback;

import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.exception.BizException;
import com.foxconn.plm.feign.service.TcMailClient;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @ClassName: TcMailClientFallback
 * @Description:
 * @Author DY
 * @Create 2023/2/1
 */
public class TcMailClientFallback implements TcMailClient {
    @Override
    public String sendMail3Method(String data, MultipartFile... files)  throws BizException {
        throw new BizException(HttpResultEnum.NET_ERROR.getCode(),"feign遠程調用發送郵件接口失敗");
    }

    @Override
    public String sendMail4Method(String data, MultipartFile... files) throws BizException {
        throw new BizException(HttpResultEnum.NET_ERROR.getCode(),"feign遠程調用發送郵件接口失敗");
    }

    @Override
    public String sendMail5Method(String data, List<MultipartFile> files) throws BizException {
        throw new BizException(HttpResultEnum.NET_ERROR.getCode(),"feign遠程調用發送郵件接口失敗");
    }
}
