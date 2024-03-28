package com.foxconn.plm.tcservice.common;

import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.tcservice.project.ProjectBean;
import com.foxconn.plm.tcservice.project.ProjectService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "TC基础功能")
@RestController
@RequestMapping("/common")
public class CommonController {
    @Resource
    ProjectService projectService;

    @ApiOperation("根據版本規則獲得下一個版本號")
    @GetMapping("/getRevNextIdByRule")
    public R<List<ProjectBean>> getRevNextIdByRule(String rule,String itemType) {
        TCSOAServiceFactory tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
        String id = "";
        com.teamcenter.services.strong.core._2013_05.DataManagement.GenerateNextValuesIn[] ins = new com.teamcenter.services.strong.core._2013_05.DataManagement.GenerateNextValuesIn[1];
        com.teamcenter.services.strong.core._2013_05.DataManagement.GenerateNextValuesIn in = new com.teamcenter.services.strong.core._2013_05.DataManagement.GenerateNextValuesIn();
        ins[0] = in;
        in.businessObjectName = itemType;
        in.clientId = "AutoAssignRAC";
        in.operationType = 1;
        Map<String, String> map = new HashMap<>();
        String prefix = "&quot;";
        String suffix = "-&quot;NNNNN";
        String mode = prefix + rule + suffix;
        map.put("item_id", mode);
        in.propertyNameWithSelectedPattern = map;
        com.teamcenter.services.strong.core._2013_05.DataManagement.GenerateNextValuesResponse response = tcsoaServiceFactory.getDataManagementService().generateNextValues(ins);
        com.teamcenter.services.strong.core._2013_05.DataManagement.GeneratedValuesOutput[] outputs = response.generatedValues;
        for (com.teamcenter.services.strong.core._2013_05.DataManagement.GeneratedValuesOutput result : outputs) {
            Map<String, com.teamcenter.services.strong.core._2013_05.DataManagement.GeneratedValue> resultMap = result.generatedValues;
            com.teamcenter.services.strong.core._2013_05.DataManagement.GeneratedValue generatedValue = resultMap.get("item_id");
            id = generatedValue.nextValue;
        }
        tcsoaServiceFactory.logout();
        return R.success(id);
    }
}
