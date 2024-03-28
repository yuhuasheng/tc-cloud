package com.foxconn.plm.integrateb2b.dataExchange.controller;


import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.integrateb2b.dataExchange.constants.BUConstants;
import com.foxconn.plm.integrateb2b.dataExchange.core.ext.MNTL6DataExchangeListener;
import com.foxconn.plm.integrateb2b.dataExchange.domain.TransferOrder;
import com.foxconn.plm.integrateb2b.dataExchange.domain.TransferOrderResp;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dataexchange")
public class DataExchangeController {
    private static Log log = LogFactory.get();

    @Autowired
    private MNTL6DataExchangeListener mntDataExchangeListener;

    @RequestMapping(value = "/test")
    @ResponseBody
    public void test() {
        TCSOAServiceFactory tCSOAServiceFactory = null;
        try {
            log.info("==========================begin post MNTDCN =========================");


        } catch (Exception e) {

        } finally {

        }
    }

}
