package com.foxconn.plm.feign.service;


import com.foxconn.plm.entity.exception.BizException;
import com.foxconn.plm.entity.param.ActionLogRp;
import com.foxconn.plm.entity.param.MakerPNRp;
import com.foxconn.plm.entity.param.PartPNRp;
import com.foxconn.plm.entity.pojo.ReportPojo;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.entity.response.SPASProject;
import com.foxconn.plm.entity.response.SPASUser;
import com.foxconn.plm.feign.fallback.TcIntegrateClientFallback;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;



/**
 * @author Robert
 */
@FeignClient(name = "tc-integrate",fallback = TcIntegrateClientFallback.class)
public interface TcIntegrateClient {

    @PostMapping(value = "/maker/postMakerPN")
    String  postMakerPN(@RequestBody List<MakerPNRp> makerPNRps) throws BizException;

    @PostMapping(value = "/rfc/isExistInSAP")
    List<PartPNRp>  isExistInSAP(@RequestBody List<PartPNRp> parts) throws BizException;

    @GetMapping(value = "/spas/getClosedProjectsByDate")
    List<SPASProject> getClosedProjectsByDate(@RequestParam("startDate") String startDate, @RequestParam("endDate") String endDate, @RequestParam("buName") String buName) throws BizException;

    @GetMapping(value = "/spas/getProjectPhase")
    SPASProject getProjectPhase(@RequestParam("projectId") String projectId) throws BizException;

    @GetMapping(value = "/spas/getTCProjectByBu")
    String getTCProjectByBu(@RequestParam("projectList") String projectList, @RequestParam("BUName") String BUName) throws BizException;

    @GetMapping(value = "/spas/getTeamRosterByEmpId")
    List<SPASUser> getTeamRosterByEmpId(@RequestParam("data") String data) throws BizException;

    @GetMapping(value = "/spas/getSTIProjectInfo")
    String getSTIProjectInfo(@RequestParam("projId") String projId) throws BizException;

    @GetMapping(value = "/spas/getProjectBu")
    String getProjectBu(@RequestParam("projId") String projId) throws BizException;

    @PostMapping("/actionlog/addlog")
    R<Long> addlog(@RequestBody List<ActionLogRp> actionLogRps) throws BizException;

    @PostMapping("/actionlog/getCisRecord")
    R<Long> getCISActionLogRecord(@RequestBody ActionLogRp pctionLogRp) throws BizException;

    @PostMapping("/actionlog/insertCISPart")
    R<Boolean> insertCISPart(@RequestBody ActionLogRp pctionLogRp) throws BizException;

    @PostMapping("/actionlog/freshReport")
    R<Long> freshReport() throws BizException;

    @GetMapping("/spas/queryProjectById/{projId}")
    List<ReportPojo> queryProjectById(@PathVariable("projId") String projId) throws BizException;
}
