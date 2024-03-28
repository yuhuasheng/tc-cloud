package com.foxconn.plm.feign.service;

import com.foxconn.plm.entity.exception.BizException;
import com.foxconn.plm.feign.config.MultipartSupportConfig;
import com.foxconn.plm.feign.fallback.TcMailClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @Author HuashengYu
 * @Date 2022/8/2 14:05
 * @Version 1.0
 */
@FeignClient(name = "tc-mail", fallback = TcMailClientFallback.class, configuration = MultipartSupportConfig.class)
public interface TcMailClient {

    @GetMapping(value = "/teamcenter/sendMail3", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    String sendMail3Method(@RequestParam("data") String data, @RequestPart(value = "file", required = false) MultipartFile... files)  throws BizException;

    @GetMapping(value = "/teamcenter/sendMail4", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    String sendMail4Method(@RequestParam("data") String data, @RequestPart(value = "file", required = false) MultipartFile... files)  throws BizException;

    @GetMapping(value = "/teamcenter/sendMail5", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    String sendMail5Method(@RequestParam("data") String data, @RequestPart(value = "file", required = false) List<MultipartFile> files)  throws BizException;
}
