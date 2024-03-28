package com.foxconn.plm.tcserviceawc.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.tcserviceawc.param.PersonalFolderParam;
import com.foxconn.plm.tcserviceawc.param.TaskUidsParam;
import com.foxconn.plm.tcserviceawc.service.PersonalFolderService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 個人工作區前端控制器
 *
 * @Description
 * @Author MW00442
 * @Date 2024/1/23 17:06
 **/
@CrossOrigin
@RestController
@RequestMapping("personalFolder")
public class PersonalFolderController {
    @Resource
    private PersonalFolderService service;

    @PostMapping("getFolderUid")
    public R<String> getPersonalFolderUid(@RequestBody PersonalFolderParam param){
        String uid = service.getPersonalFolderUid(param);
        return StrUtil.isNotBlank(uid) ? R.success(HttpResultEnum.SUCCESS.getMsg(),uid) : R.error(HttpResultEnum.NO_RESULT.getCode(),HttpResultEnum.NO_RESULT.getMsg());
    }


    @PostMapping("getTaskFolderUid")
    public R<String> getTaskFolderUid(@RequestBody PersonalFolderParam param){
        String uid = service.getTaskFolderUid(param);
        return StrUtil.isNotBlank(uid) ? R.success(HttpResultEnum.SUCCESS.getMsg(),uid) : R.error(HttpResultEnum.NO_RESULT.getCode(),HttpResultEnum.NO_RESULT.getMsg());
    }

    @PostMapping("getTaskUids")
    public R<List<String>> getTaskUids(@RequestBody TaskUidsParam param){
        List<String> uids = service.getTaskUids(param);
        return R.success(uids);
    }
}
