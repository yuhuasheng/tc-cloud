package com.foxconn.plm.tcservice.issuemanagement.controller;

import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.tcservice.issuemanagement.param.AddDellIssueParam;
import com.foxconn.plm.tcservice.issuemanagement.param.AddHpIssueParam;
import com.foxconn.plm.tcservice.issuemanagement.param.AddIssueUpdatesParam;
import com.foxconn.plm.tcservice.issuemanagement.param.AddLenovoIssueParam;
import com.foxconn.plm.tcservice.issuemanagement.service.CreateIssueService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.List;

/**
 * 創建issue前端控制器
 *
 * @Description
 * @Author MW00442
 * @Date 2024/2/19 16:37
 **/
@RestController
@RequestMapping("/createIssue")
public class CreateIssueController {
    @Resource
    private CreateIssueService service;


    @PostMapping("dell")
    @ApiOperation("新增DellIssue数据")
    @ApiImplicitParam(name = "files",dataType = "_file",paramType = "form",dataTypeClass = String.class)
    public R createDellIssue(AddDellIssueParam param, @RequestPart(value = "files" ,required = false) @ApiParam("附件") List<MultipartFile> files){
        return service.createDellIssue(param,files);
    }

    @PostMapping("hp")
    @ApiOperation("新增HpIssue数据")
    @ApiImplicitParam(name = "files",dataType = "_file",paramType = "form",dataTypeClass = String.class)
    public R createHpIssue(AddHpIssueParam param, @RequestPart(value = "files" ,required = false) @ApiParam("附件") List<MultipartFile> files){
        return service.createHpIssue(param,files);
    }


    @PostMapping("lenovo")
    @ApiOperation("新增LenovoIssue数据")
    @ApiImplicitParam(name = "files",dataType = "_file",paramType = "form",dataTypeClass = String.class)
    public R createLenovoIssue(AddLenovoIssueParam param, @RequestPart(value = "files" ,required = false) @ApiParam("附件") List<MultipartFile> files){
        return service.createLenovoIssue(param,files);
    }


    @PostMapping("updates")
    @ApiOperation("新增IssueUpdates数据")
    @ApiImplicitParam(name = "files",dataType = "_file",paramType = "form",dataTypeClass = String.class)
    public R addIssueUpdates(AddIssueUpdatesParam param, @RequestPart(value = "files" ,required = false) @ApiParam("附件") List<MultipartFile> files){
        return service.addIssueUpdates(param,files);
    }
}
