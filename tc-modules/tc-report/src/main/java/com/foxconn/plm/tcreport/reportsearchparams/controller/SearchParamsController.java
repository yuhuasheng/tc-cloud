package com.foxconn.plm.tcreport.reportsearchparams.controller;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.tcreport.reportsearchparams.service.SearchParamsService;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @Author HuashengYu
 * @Date 2023/1/3 17:25
 * @Version 1.0
 */
@RestController
@RequestMapping("/searchParams")
public class SearchParamsController {
    private static Log log = LogFactory.get();
    @Resource
    private SearchParamsService searchParamsService;

    @GetMapping("/getLovList")
    @ApiOperation("下拉级联列表")
    public R getLovList() {
        log.info("==>> 开始执行 getLovList");
        try {
            return R.success(searchParamsService.getLovList());
        } catch (Exception e) {
            e.printStackTrace();
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(),e.getLocalizedMessage());
        }
    }
}
