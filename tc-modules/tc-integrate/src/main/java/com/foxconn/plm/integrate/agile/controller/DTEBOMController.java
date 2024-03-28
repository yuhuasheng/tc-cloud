package com.foxconn.plm.integrate.agile.controller;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSONObject;
import com.foxconn.dp.plm.privately.Access;
import com.foxconn.dp.plm.privately.PrivaFileUtis;
import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.integrate.agile.domain.BOMInfo;
import com.foxconn.plm.integrate.agile.service.DTEBOMServiceImpl;
import com.foxconn.plm.utils.file.FileUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

@Api(tags = "BOM管理")
@RestController
@RequestMapping("/DT")
@Scope("request")
public class DTEBOMController {
    private static Log log = LogFactory.get();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Resource
    private DTEBOMServiceImpl DTEBOMServiceImpl;


    @RequestMapping("syncAgileBOM")
    public String syncBOMByAgile(String ecnNumber) {
        return DTEBOMServiceImpl.buildAgileBOM(ecnNumber);

    }

    @ApiOperation("上传EBOM")
    @PostMapping("/uploadEBOM")
    public R<Long> uploadEBOM(MultipartFile multipartFile, String json) {
        System.err.println("==================构建【DT L6 EBOM】开始：" + dateFormat.format(new Date()) + "==================");
        log.info("==================构建【DT L6 EBOM】开始：" + dateFormat.format(new Date()) + "==================");
        File targetFile = null;
        try {
            log.info("json data :" + json);
            String tmpdir = PrivaFileUtis.getTmpdir();

            String filePath = tmpdir + multipartFile.getOriginalFilename();
            FileUtil.checkSecurePath(filePath);
            targetFile = new File(filePath);
            multipartFile.transferTo(targetFile);
            BOMInfo bomInfo = JSONObject.parseObject(json, BOMInfo.class);
            DTEBOMServiceImpl.uploadEBOM(targetFile, bomInfo);
        } catch (Exception e) {
            e.printStackTrace();
            log.info(e.getMessage(), e);
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(), e.getMessage());
        } finally {
            if (targetFile != null) {
                if (targetFile.exists()) {
                    targetFile.delete();
                }
            }
        }
        log.info("==================构建【DT L6 EBOM】完成：" + dateFormat.format(new Date()) + "==================");
        System.err.println("==================构建【DT L6 EBOM】完成：" + dateFormat.format(new Date()) + "==================");
        return R.success();
    }
}
