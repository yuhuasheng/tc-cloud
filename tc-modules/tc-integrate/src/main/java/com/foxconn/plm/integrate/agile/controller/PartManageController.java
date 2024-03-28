package com.foxconn.plm.integrate.agile.controller;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.foxconn.plm.integrate.agile.domain.HHPNPojo;
import com.foxconn.plm.integrate.agile.service.PartService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.List;

@Api(tags = "物料管理")
@RestController
@RequestMapping("/agile/partManage")
public class PartManageController {
    private static Log log = LogFactory.get();
    @Resource
    private PartService partService;

    @ApiOperation("获取物料信息")
    @PostMapping("/getPartInfo")
    public String getPartInfo(@RequestBody String json) {
        try {
            log.info("begin getPartInfo");
            log.info("getHHPNInfo=========================" + json);
            List<HHPNPojo> hhpnPojos = JSON.parseArray(json, HHPNPojo.class);
            partService.updatePartInfos(hhpnPojos);
            log.info("part info =========================" + JSONArray.toJSONString(hhpnPojos));
            log.info("end getPartInfo");

            return JSONArray.toJSONString(hhpnPojos);
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e);
        }
        log.info("end getPartInfo");
        return "";
    }


}
