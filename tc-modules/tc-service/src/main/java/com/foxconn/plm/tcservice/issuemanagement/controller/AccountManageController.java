package com.foxconn.plm.tcservice.issuemanagement.controller;

import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.entity.response.RList;
import com.foxconn.plm.tcservice.issuemanagement.param.AddAccountParam;
import com.foxconn.plm.tcservice.issuemanagement.param.EditAccountParam;
import com.foxconn.plm.tcservice.issuemanagement.param.SearchAccountParam;
import com.foxconn.plm.tcservice.issuemanagement.response.SearchAccountRes;
import com.foxconn.plm.tcservice.issuemanagement.response.UserRes;
import com.foxconn.plm.tcservice.issuemanagement.service.SecondAccountService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.List;

/**
 * 二級賬號管理前端控制器
 *
 * @Description
 * @Author MW00442
 * @Date 2023/12/21 11:05
 **/
@RestController
@RequestMapping("/account")
public class AccountManageController {

    @Resource
    private SecondAccountService service;

    @GetMapping("search")
    @ApiOperation("根據條件搜索一級賬號白名單信息")
    public RList<SearchAccountRes> searchAccount(SearchAccountParam param){
        return service.searchAccount(param);
    }

    @GetMapping("tcUser")
    @ApiOperation("查詢TC中的所有一級賬號信息")
    public R<List<UserRes>> getAllUser(){
        return R.success(service.getAllUser());
    }

    @PostMapping("addAccount")
    @ApiOperation("新增一級賬號白名單數據")
    public R addAccount(@RequestBody AddAccountParam param){
        return service.addAccount(param);
    }

    @PostMapping("editAccount")
    @ApiOperation("修改一級賬號白名單數據")
    public R editAccount(@RequestBody EditAccountParam param){
        return service.editAccount(param);
    }

    @DeleteMapping("delAccount/{id}")
    @ApiOperation("刪除一級賬號白名單數據")
    public R delAccount(@PathVariable String id){
        return service.delAccount(id);
    }

    @PostMapping("import")
    @ApiOperation("批量导入一級賬號白名單数据")
    @ApiImplicitParam(name = "file",dataType = "_file",paramType = "form",dataTypeClass = String.class)
    public R importDetailData(@RequestPart("file") @ApiParam("導入的excel數據文件") MultipartFile file) {
        return service.importData(file);
    }

    @PostMapping("updateUid")
    @ApiOperation("批量同步二級賬號的uid")
    public R updateAccountUid(){
        return service.updateAccountUid();
    }

    @PostMapping("delByFile")
    @ApiOperation("批量刪除一級賬號白名單数据")
    @ApiImplicitParam(name = "file",dataType = "_file",paramType = "form",dataTypeClass = String.class)
    public R deleteAccountByFile(@RequestPart("file") @ApiParam("導入的excel數據文件") MultipartFile file) {
        return service.deleteAccountByFile(file);
    }

}
