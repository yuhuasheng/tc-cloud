package com.foxconn.plm.tcreport.drawcountreport.controller;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.tcreport.drawcountreport.domain.DrawCountRes;
import com.foxconn.plm.tcreport.drawcountreport.domain.QueryBean;
import com.foxconn.plm.tcreport.drawcountreport.service.DrawCountReportService;
import com.foxconn.plm.tcreport.reportsearchparams.service.SearchParamsService;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @Author HuashengYu
 * @Date 2023/1/3 14:03
 * @Version 1.0
 */
@RestController
@RequestMapping("/drawCountReport")
public class DrawCountReportController {
    private static Log log = LogFactory.get();
    @Resource
    private SearchParamsService searchParamsService;

    @Resource
    private DrawCountReportService drawCountReportService;

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

    @GetMapping("/searchDrawCountRecord")
    @ApiOperation("查询系統机构&电子3D图档记录")
    public R getDrawCountRecordList(QueryBean queryBean) {
        try {
            List<DrawCountRes> list = drawCountReportService.getDrawCountRecordList(queryBean);
            return R.success(list);
        } catch (Exception e) {
            e.printStackTrace();
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(),e.getLocalizedMessage());
        }
    }

    @GetMapping("exportDrawCountRecordList")
    @ApiOperation("导出统计issue的status、category、impact的原始数据")
    public void exportDrawCountRecordList(HttpServletResponse response, QueryBean queryBean) {
        drawCountReportService.exportDrawCountRecordList(response, queryBean);
    }
}
