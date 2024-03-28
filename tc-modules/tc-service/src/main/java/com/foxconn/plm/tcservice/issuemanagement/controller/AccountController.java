package com.foxconn.plm.tcservice.issuemanagement.controller;

import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.tcservice.issuemanagement.bean.AccountBean;
import com.foxconn.plm.tcservice.issuemanagement.param.AddDellIssueParam;
import com.foxconn.plm.tcservice.issuemanagement.param.ContentToProjectParam;
import com.foxconn.plm.tcservice.issuemanagement.response.AccountRes;
import com.foxconn.plm.tcservice.issuemanagement.service.SecondAccountService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.List;

/**
 * 问题管理 二级账号管理模块
 *
 * @Description
 * @Author MW00442
 * @Date 2023/11/27 11:08
 **/
@RestController
@RequestMapping("/issueManagement")
public class AccountController {
    @Resource
    private SecondAccountService service;

    @GetMapping("getAll")
    public R<List<AccountBean>> getAll(){
        List<AccountBean> list = service.getAll();
        return R.success(list);
    }

    @GetMapping("getUserByAccountUid")
    public R<List<String>> getUserByAccountUid(String accountUid){
        List<String> list = service.getUserByAccountUid(accountUid);
        return R.success(list);
    }

    @GetMapping("getUserByAccountNo")
    public R<List<String>> getUserByAccountNo(String accountNo){
        List<String> list = service.getUserByAccountNo(accountNo);
        return R.success(list);
    }

    @GetMapping("getUserAccountByUid")
    public R<List<AccountBean>> getUserAccountByUid(String uid){
        List<AccountBean> list = service.getUserAccountByUid(uid);
        return R.success(list);
    }

    @PostMapping("getByUids")
    public R<List<AccountBean>> getByUids(@RequestBody List<String> uids){
        List<AccountBean> list = service.getByUids(uids);
        return R.success(list);
    }

    @GetMapping("getWorkList")
    public R<List<AccountRes>> getWorkList(String customer){
        List<AccountRes> list = service.getWorkList(customer);
        return R.success(list);
    }

    @PostMapping("contents")
    public R contentToProject(@RequestBody ContentToProjectParam param){
        return service.contentToProject(param);
    }





}
