package com.foxconn.dp.plm.hdfs.controller;

import cn.hutool.json.JSONNull;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.dp.plm.hdfs.domain.rv.FunctionConfigRv;
import com.foxconn.dp.plm.hdfs.service.FunctionConfigService;
import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.response.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Api(tags = "Function配置")
@RestController()
@RequestMapping("/functionConfig")
public class FunctionConfigController {
    private static Log log = LogFactory.get();
    @Resource
    FunctionConfigService functionConfigService;


    @ApiOperation("获取已存在function配置")
    @PostMapping(value = "/getConfigList")
    public R<List<FunctionConfigRv>> getConfigList(@RequestBody FunctionConfigRv  functionConfigRv) {
        log.info("begin getConfigList "+ JSONUtil.toJsonStr(functionConfigRv));
        List<FunctionConfigRv> data=functionConfigService.getConfigList(functionConfigRv);
        log.info("end getConfigList "+ JSONUtil.toJsonStr(data));
        return R.success(data);
    }

    @ApiOperation("查询部門清單")
    @GetMapping(value = "/getFunctionList")
    public R<Map<String,List<FunctionConfigRv>>> getFunctionList( ) {
        log.info("begin getFunctionList ");
        Map<String, List<FunctionConfigRv>> functionList = functionConfigService.getFunctionList();
        log.info("end  getFunctionList "+ JSONUtil.toJsonStr(functionList));
        return R.success(functionList);
    }


    @ApiOperation("根據部門查詢group ")
    @GetMapping(value = "/getGroupList")
    public R<List<FunctionConfigRv>> getGroupList(Integer functionId ) {
        log.info("begin getGroupList " +functionId);
        List<FunctionConfigRv> data=functionConfigService.getGroupList(functionId);
        log.info("end  getGroupList " +JSONUtil.toJsonStr(data));
        return R.success(data);

    }

    @ApiOperation("根據部門查詢group ")
    @GetMapping(value = "/getTCGroupList")
    public R<List<FunctionConfigRv>> getTCGroupList(String functionName ) {
        log.info("begin getTCGroupList " +functionName);
        List<FunctionConfigRv> data=functionConfigService.getTCGroupList(functionName);
        log.info("end  getTCGroupList " +JSONUtil.toJsonStr(data));
        return R.success(data);

    }


    @ApiOperation("修改已存在function配置")
    @PostMapping(value = "/modify")
    public R<List<FunctionConfigRv>> modify(@RequestBody FunctionConfigRv  functionConfigRv) {
        log.info("begin modify "  +JSONUtil.toJsonStr(functionConfigRv));
        functionConfigService.modify(functionConfigRv);
        log.info("end modify ");
        return R.success("修改成功");
    }


    @ApiOperation("保存")
    @PostMapping(value = "/save")
    public R<String> save(@RequestBody FunctionConfigRv  functionConfigRv) {

        log.info("begin save "  +JSONUtil.toJsonStr(functionConfigRv));
        try {
            functionConfigService.insert(functionConfigRv);
        }catch (Exception e){
            return R.error(HttpResultEnum.PARAM_ERROR.getCode(),"配置信息已存在");
        }
        log.info("end save ");
        return R.success();
    }

    @ApiOperation("删除")
    @PostMapping(value = "/delete")
    public R<String> delete(@RequestBody FunctionConfigRv  functionConfigRv) {
        log.info("begin delete "  +JSONUtil.toJsonStr(functionConfigRv));
        functionConfigService.delete(functionConfigRv);
        log.info("end delete ");
        return R.success();
    }


}
