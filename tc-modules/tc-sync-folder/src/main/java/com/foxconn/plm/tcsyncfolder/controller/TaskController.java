package com.foxconn.plm.tcsyncfolder.controller;

import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.tcsyncfolder.service.TaskService;
import com.foxconn.plm.tcsyncfolder.vo.TaskInfoVo;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @ClassName: TaskController
 * @Description:
 * @Author DY
 * @Create 2023/4/6
 */
@CrossOrigin
@RestController
@RequestMapping("/sync/folder")
public class TaskController {

    @Resource
    private TaskService service;

    @GetMapping
    public R<TaskInfoVo> getTaskInfo(){
        TaskInfoVo info = service.getTaskInfo();
        return R.success(info);
    }

}
