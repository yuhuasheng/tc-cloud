package com.foxconn.dp.plm.hdfs.controller;

import com.foxconn.dp.plm.hdfs.domain.entity.LOVEntity;
import com.foxconn.dp.plm.hdfs.domain.rp.DelRp;
import com.foxconn.dp.plm.hdfs.domain.rp.SaveBuRp;
import com.foxconn.dp.plm.hdfs.domain.rv.LOVRv;
import com.foxconn.dp.plm.hdfs.domain.rv.ProductLineRv;
import com.foxconn.dp.plm.hdfs.service.BUService;
import com.foxconn.dp.plm.hdfs.service.MainService;
import com.foxconn.plm.entity.param.BUListRp;
import com.foxconn.plm.entity.response.BURv;
import com.foxconn.plm.entity.response.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Api(tags = "BU管理")
@RestController()
@RequestMapping("/buManage")
public class BUController {

    @Resource
    BUService buService;

    @Resource
    MainService mainService;

    @ApiOperation("获取LOV")
    @GetMapping(value = "/getLov")
    public R<HashMap<String, Object>> getLov() {
        List<LOVRv> customer = buService.getLovList("customer");
        List<ProductLineRv> productLine = buService.getProductLineList();
        List<LOVRv> buList = new ArrayList<>();
        List<LOVEntity> bu = mainService.getLOV("BU");
        for (LOVEntity lovEntity : bu) {
            buList.add(new LOVRv(lovEntity.getId(), lovEntity.getCode()));
        }
        HashMap<String, Object> map = new HashMap<>();
        map.put("customer", customer);
        map.put("productLine", productLine);
        map.put("bu", buList);
        return R.success(map);
    }

    @ApiOperation("查询BU清单")
    @GetMapping(value = "/buList")
    public R<List<BURv>> buList(BUListRp rp) {
        return R.success(buService.getBUList(rp));
    }

    @ApiOperation("保存")
    @PostMapping(value = "/save")
    public R<String> save(@RequestBody SaveBuRp rp) {
        buService.save(rp);
        return R.success();
    }

    @ApiOperation("删除")
    @PostMapping(value = "/delete")
    public R<String> delete(@RequestBody DelRp rp, HttpServletRequest request) {
        String empId = request.getHeader("empId");
        buService.delete(rp.getId(), empId);
        return R.success();
    }


}
