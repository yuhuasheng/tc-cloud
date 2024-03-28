package com.foxconn.plm.integrate.log.contoller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.integrate.log.domain.ActionLogRp;
import com.foxconn.plm.integrate.log.domain.ItemRev2Info;
import com.foxconn.plm.integrate.log.service.ActionLogServiceImpl;
import com.foxconn.plm.integrate.log.service.ProjectInfoServiceImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Api(tags = "供应商管理")
@RestController
@RequestMapping("/actionlog")
public class ActionLogController {
    private static Log log = LogFactory.get();
    @Resource
    private ActionLogServiceImpl actionLogServiceImpl;


    @Resource
    private ProjectInfoServiceImpl projectInfoServiceImpl;

    @ApiOperation("保存action log")
    @PostMapping("/addlog")
    public R<Long> searchMakerInfo(@RequestBody List<ActionLogRp> actionLogRps) {
        log.info("==========================begin add action log=========================");
        actionLogServiceImpl.addLog(actionLogRps);
        log.info("==========================end add action log=========================");
        return R.success(1L);
    }


    @ApiOperation("refresh report")
    @PostMapping("/freshReport")
    public R<Long> freshReport() {
       try {
         projectInfoServiceImpl.synProjectInfo();
       }catch(Exception e){}
        return R.success(1L);
    }

    @PostMapping("/add2log")
    public R<Long> add2log(@RequestBody List<ItemRev2Info> itemRev2Infos) {

        actionLogServiceImpl.add2log(itemRev2Infos);

        return R.success(1L);
    }


    @PostMapping("/getProjIntc")
    public R<List<String>> getProjIntc() {
        List<String> datas = new ArrayList<>();
        try {
            datas = projectInfoServiceImpl.getProjsIntc();
        } catch (Exception e) {
        }
        return R.success(datas);
    }


    @PostMapping("/getActualUsers")
    public R<List<String>> getActualUsers() {
        List<String> datas = new ArrayList<>();
        try {
            datas = projectInfoServiceImpl.getActualUsers();
        } catch (Exception e) {
        }
        return R.success(datas);
    }


    @PostMapping("/getFolderDiff")
    public R<String> getFolderDiff() {
        String html="";
        try {
            html=  projectInfoServiceImpl.getFolderDiff();
        } catch (Exception e) {
            log.error(e.getMessage(),e);
        }
        return R.success(html);
    }

    @PostMapping("/saveFolderDiff")
    public R<String> saveFolderDiff(@RequestBody String path) {
        String html="";
        try {
            html=  projectInfoServiceImpl.saveFolderDiff(path);
        } catch (Exception e) {
            log.error(e.getMessage(),e);
        }
        return R.success(html);
    }


    @PostMapping("/getCisRecord")
    public R<Long> getCISActionLogRecord(@RequestBody ActionLogRp pctionLogRp) {
        Integer record = actionLogServiceImpl.getCISActionLogRecord(pctionLogRp);
        return R.success(ObjectUtil.isNotNull(record) ? record : 0L);
    }

    @PostMapping("/insertCISPart")
    public R<Boolean> insertCISPart(@RequestBody ActionLogRp pctionLogRp) {
        try {
            actionLogServiceImpl.insertCISPart(pctionLogRp);
            return R.success(true);
        }catch (Exception e){
            return R.error(HttpResultEnum.NO_RESULT.getCode(),"保存数据失败");
        }

    }
}
