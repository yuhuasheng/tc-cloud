package com.foxconn.plm.tcservice.project;

import com.foxconn.plm.entity.response.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Api(tags = "专案")
@RestController
@RequestMapping("/project")
public class ProjectController {

    @Resource
    ProjectService projectService;

    @ApiOperation("查询指定用户参与的特权专案Id")
    @GetMapping("/queryProjectByPrivilegeUser")
    public R<List<ProjectBean>> queryProjectByPrivilegeUser(String userId) {
        return R.success(projectService.queryProjectByPrivilegeUser(userId));
    }
}
