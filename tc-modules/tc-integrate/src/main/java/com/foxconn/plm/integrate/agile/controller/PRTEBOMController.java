package com.foxconn.plm.integrate.agile.controller;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSONObject;
import com.foxconn.dp.plm.privately.PrivaFileUtis;
import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.integrate.agile.domain.BOMInfo;
import com.foxconn.plm.integrate.agile.service.DTEBOMServiceImpl;
import com.foxconn.plm.integrate.agile.service.PRTEBOMServiceImpl;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
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

@Api(tags = "prt BOM管理")
@RestController
@RequestMapping("/prt")
@Scope("request")
public class PRTEBOMController {
    private static Log log = LogFactory.get();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Resource
    private PRTEBOMServiceImpl pRTEBOMServiceImpl;


    @RequestMapping("syncPrtBOM")
    public String  syncBOMByAgile(String uid) {
        try {
                new SyncBomThread(uid).start();
        }catch(Exception e){
            log.error(e.getMessage(),e);
            System.out.println(e);
            return e.getMessage()==null?"sync failed !":e.getMessage();
        }
        return  "success";
    }



    private class  SyncBomThread extends Thread {
        private String uid;
        public SyncBomThread(String uid){
                this.uid=uid;
        }
        @Override
        public void run() {
                pRTEBOMServiceImpl.buildAgileBOM(uid);
        }


    }


}
