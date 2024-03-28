package com.foxconn.plm.tcservice.tclicensereport.controller;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.tcservice.mapper.master.FunctionMapper;
import com.foxconn.plm.tcservice.tclicensereport.domain.DateRecordInfo;
import com.foxconn.plm.utils.collect.CollectUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author HuashengYu
 * @Date 2022/9/20 11:02
 * @Version 1.0
 */
@Api(tags = "TC License 稼动率")
@RestController(value = "TCLicenseController")
@RequestMapping("tclicense/report")
public class ReportController {
    private static Log log = LogFactory.get();
    @Resource
    private FunctionMapper functionMapper;

    @ApiOperation("保存Date Record Info")
    @PostMapping("/saveDateInfo")
    public R saveDateRecordInfo(@RequestBody List<DateRecordInfo> list) {
        try {
            list.stream().filter(CollectUtil.distinctByKey(info -> info.getRecordDate())).collect(Collectors.toList());
            functionMapper.insertOrUpdateDateRecord(list);
        } catch (Exception e) {
            e.printStackTrace();
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(), "导入失败");
        }
        return R.success("导入成功");
    }
}
