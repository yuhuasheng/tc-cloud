package com.foxconn.plm.integrate.lbs.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.integrate.lbs.domain.SaveParam;
import com.foxconn.plm.integrate.lbs.domain.SyncRes;
import com.foxconn.plm.integrate.lbs.service.LBSService;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
 * @ClassName: LBSController
 * @Description:
 * @Author DY
 * @Create 2022/12/15
 */
@Api(tags = "LBS")
@RestController
@RequestMapping("/lbs")
public class LBSController {
    private static Log log = LogFactory.get();
    @Resource
    private LBSService lbsService;

    @PostMapping("save")
    public R<Boolean> saveEntity(@RequestBody SaveParam param) {
        boolean flag = lbsService.saveEntity(param);
        return flag ? R.success(true) : R.error(HttpResultEnum.SERVER_ERROR.getCode(),"保存數據失敗");
    }

    @GetMapping("list")
    public R<List<SyncRes>> getList() {
        List<SyncRes> list = lbsService.getList();
        return CollUtil.isNotEmpty(list) ? R.success(list) : R.success(Collections.emptyList());
    }

    @PostMapping("batchDelete")
    public R<Boolean> batchDelete(@RequestBody List<String> ids) {
        boolean flag = lbsService.batchDelete(ids);
        return flag ? R.success(true) : R.error(HttpResultEnum.SERVER_ERROR.getCode(),"刪除數據失敗");
    }

}
