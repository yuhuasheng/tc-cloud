package com.foxconn.plm.integrate.sap.customPN.controller;

import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.integrate.sap.customPN.domain.ApplyCustomPnResponse;
import com.foxconn.plm.integrate.sap.customPN.domain.rp.CustomPartRp;
import com.foxconn.plm.integrate.sap.customPN.service.CustomPNService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Api(tags = "自编料号管理")
@RestController
@RequestMapping("/custompn")
@Scope("request")
public class CustomPNController {
    private static Log log = LogFactory.get();
    @Resource
    private CustomPNService customPNService;

    @ApiOperation("apply Custom PN")
    @RequestMapping ("/applyCustomPN")
    public R<String> applyCustomPN(@RequestBody List<CustomPartRp> customPartRps) {

        List<ApplyCustomPnResponse> mMResponses = new ArrayList<>();
        String msg = "";
        try {
            log.info("begin apply custom pn");
            log.info(JSONUtil.toJsonStr(customPartRps));
            msg = customPNService.applyCustomPNs(customPartRps);
            log.info("end apply custom pn");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            msg = e.getMessage();
        }
        return R.success(msg,msg);
    }







    @ApiOperation("post Custom PN")
    @PostMapping("/postCustomPN")
    public R<String> postCustomPN(@RequestBody List<CustomPartRp> customPartRps) {

        List<ApplyCustomPnResponse> mMResponses = new ArrayList<>();
        String msg = "";
        try {
            log.info("begin post custom pn");
            msg = customPNService.postCustomPNs(customPartRps);
            log.info("end post custom pn");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            msg = e.getMessage();
        }
        return R.success(msg,msg);
    }


}
