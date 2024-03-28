package com.foxconn.dp.plm.hdfs.controller;

import com.foxconn.dp.plm.hdfs.domain.entity.TCProjectEntity;
import com.foxconn.dp.plm.hdfs.domain.rp.ProjectListRp;
import com.foxconn.dp.plm.hdfs.service.ProjectService;
import com.foxconn.plm.entity.response.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@Api(tags = "专案")
@RestController
@Validated
public class ProjectController {

    @Resource
    ProjectService service;


    @ApiOperation("獲取所有專案")
    @PostMapping(value = "/getProjectsByEmpId")
    public R<List<TCProjectEntity>> getProjectsByEmpId(@RequestBody @Valid ProjectListRp rp) {
        return service.getProjectsByEmpId(rp);
    }

}
