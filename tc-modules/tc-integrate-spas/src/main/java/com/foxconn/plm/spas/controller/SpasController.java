package com.foxconn.plm.spas.controller;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.spas.service.impl.SynSpasDBServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/spas")
public class SpasController {
    private static Log log = LogFactory.get();

    @Autowired(required = false)
    private SynSpasDBServiceImpl synSpasDBServiceImpl;


    @RequestMapping(value = "/synCustomerProductLine")
    @ResponseBody
    public R<String> synCustomerProductLine() {
        try {
            log.info("同步SPAS客户、产品线开始.");
            SynSpasDBServiceImpl.updateDate();
            synSpasDBServiceImpl.addCustomerData();
            synSpasDBServiceImpl.addProductLineData();
            log.info("同步SPAS客户、产品线结束.");
        } catch (Exception e) {
            e.printStackTrace();
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(),e.getMessage());
        }
        return R.success("success");
    }

}
