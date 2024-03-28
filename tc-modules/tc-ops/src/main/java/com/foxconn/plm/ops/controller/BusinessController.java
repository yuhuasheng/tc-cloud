package com.foxconn.plm.ops.controller;

import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.entity.response.RList;
import com.foxconn.plm.ops.param.AddRecordParam;
import com.foxconn.plm.ops.param.DisableParam;
import com.foxconn.plm.ops.param.EditRecordParam;
import com.foxconn.plm.ops.param.SearchRecordParam;
import com.foxconn.plm.ops.response.SearchRecordRes;
import com.foxconn.plm.ops.service.BusinessService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 業務操作前端控制器
 *
 * @Description
 * @Author MW00442
 * @Date 2024/1/2 11:26
 **/
@Validated
@CrossOrigin
@RestController
@RequestMapping("/business")
public class BusinessController {
    @Resource
    private BusinessService service;

    @PostMapping("addRecord")
    public R addRecord(@Validated @RequestBody AddRecordParam param){
        return service.addRecord(param);
    }

    @PutMapping("editRecord")
    public R editRecord(@Validated @RequestBody EditRecordParam param){
        return service.editRecord(param);
    }

    @DeleteMapping("delRecord")
    public R delRecord(@Validated @RequestBody @NotEmpty(message = "刪除的參數列表不能為空") List<String> ids){
        return service.delRecord(ids);
    }

    @GetMapping("search")
    public RList<SearchRecordRes> searchRecord(SearchRecordParam param){
        return service.searchRecord(param);
    }


    @PutMapping("disable")
    public R disable(@Validated @RequestBody DisableParam param){
        return service.disable(param);
    }

    @PostMapping("execute")
    public R execute(@Validated @RequestBody @NotEmpty(message = "刪除的參數列表不能為空") List<String> ids){
        return service.execute(ids);
    }

}
