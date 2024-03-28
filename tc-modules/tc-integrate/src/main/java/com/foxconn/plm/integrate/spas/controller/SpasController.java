package com.foxconn.plm.integrate.spas.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.shaded.com.google.gson.Gson;
import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.param.BUListRp;
import com.foxconn.plm.entity.response.BURv;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.feign.service.HDFSClient;
import com.foxconn.plm.integrate.spas.domain.ReportPojo;
import com.foxconn.plm.integrate.spas.domain.SPASUser;
import com.foxconn.plm.integrate.spas.domain.STIProject;
import com.foxconn.plm.integrate.spas.service.impl.SpasServiceImpl;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.sql.*;
import java.util.*;

@Controller
@RequestMapping("/spas")
public class SpasController {
    private static Log log = LogFactory.get();

    @Value("${kpi.hdfs}")
    private String nameSpace;

    @Value("${spring.cloud.nacos.discovery.namespace}")
    private String env;

    @Autowired(required = false)
    private SpasServiceImpl spasServiceImpl;


    @Resource
    private HDFSClient hdfsClient;

    @RequestMapping(value = "/getSPASUser")
    @ResponseBody
    public List<SPASUser> getSPASUser() {

        return spasServiceImpl.selectSPASUser();
    }

    @RequestMapping(value = "/getTeamRoster")
    @ResponseBody
    public List<SPASUser> getTeamRoster(@RequestParam String data) {
        Map<String, String> dataMap = new HashMap<String, String>();
        Gson gson = new Gson();
        dataMap = gson.fromJson(data, dataMap.getClass());
        String platformFoundIds = dataMap.get("platformFoundIds");
        System.out.println("platformFoundIds:" + platformFoundIds);
        String[] ids = platformFoundIds.split(",");
        List<SPASUser> spasUsers = spasServiceImpl.queryTeamRoster(ids);
        if("poc".equalsIgnoreCase(env)){
            updateNote(spasUsers);
        }
        return spasUsers;
    }


    @RequestMapping(value = "/getTeamRosterByEmpId")
    @ResponseBody
    public List<SPASUser> getTeamRosterByEmpId(@RequestParam String data) {

        Map<String, String> dataMap = new HashMap<String, String>();
        Gson gson = new Gson();
        dataMap = gson.fromJson(data, dataMap.getClass());
        String empId = dataMap.get("empId");
        System.out.println("empId:" + empId);

        List<SPASUser> spasUsers = spasServiceImpl.queryTeamRosterByEmpId(empId);
        if("poc".equalsIgnoreCase(env)){
            updateNote(spasUsers);
        }
        return spasUsers;
    }


    private void updateNote( List<SPASUser> spasUsers){

        Connection conn=null;
        try{
            Class.forName("oracle.jdbc.driver.OracleDriver");
            conn=DriverManager.getConnection("jdbc:oracle:thin:@10.203.163.223:1526:tcprd","XPLM","PASSW0RDPLM");
            Statement st=null;
            for(SPASUser u:spasUsers){
               st= conn.createStatement();
                ResultSet rs= st.executeQuery("select notes from spas_user  where work_id='"+u.getWorkId()+"'");
                while(rs.next()){
                   u.setNotes(rs.getString(1)+".123");
                }
               st.close();
            }
        }catch(Exception e){
            System.out.print(e);
        }finally {
            try{
                if(conn!=null){
                    conn.close();
                }
            }catch(Exception e){}
        }


    }



    @ApiOperation("查询时间段范围类，有关闭阶段的专案信息")
    @RequestMapping(value = "/getClosedProjectsByDate")
    @ResponseBody
    public List<ReportPojo> getClosedProjectsByDate(@RequestParam String startDate, String endDate, String buName) {
        try {
            log.info("begin getClosedProjectsByDate");
            log.info("startDate " + startDate + " endDate " + endDate + " buName " + buName);
            return spasServiceImpl.searchPojects(startDate, endDate, buName);
        } catch (Exception e) {
            return null;
        }
    }

    @ApiOperation("查询专案的阶段信息")
    @RequestMapping(value = "/getProjectPhase")
    @ResponseBody
    public ReportPojo getProjectPhase(@RequestParam String projectId) {
        try {
            return spasServiceImpl.getPhases(projectId);
        } catch (Exception e) {
            return null;
        }
    }


    @ApiOperation("查询专案的所有阶段")
    @RequestMapping(value = "/getAllPhases")
    @ResponseBody
    public R<List<ReportPojo>> getAllPhases(@RequestParam String projectId) {
        try {
            List<ReportPojo> list= spasServiceImpl.getAllPhases(projectId);
            return R.success(list);
        } catch (Exception e) {
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(), e.getLocalizedMessage());
        }
    }


    @RequestMapping(value = "/getTCProjectByBu")
    @ResponseBody
    public String getTCProjectByBu(String projectList, String BUName) {
        List<Map> list = JSON.parseArray(projectList, Map.class);
        List<Map> result = spasServiceImpl.getCurBUTCProject(list, BUName);
        return JSON.toJSONString(result);
    }


    @RequestMapping(value = "/getSTIProjectInfo")
    @ResponseBody
    public String getSTIProjectInfo(String projId) {
        STIProject sTIProject = spasServiceImpl.getProjectInfo(projId);
        if (sTIProject == null) {
            return "";
        }
        return JSON.toJSONString(sTIProject);
    }

    @RequestMapping(value = "/getProjectBu")
    @ResponseBody
    public String getProjectBu(String projId) {
        if (projId.toUpperCase(Locale.ENGLISH).startsWith("P")) {
            projId = projId.substring(1);
        }
        STIProject sTIProject = spasServiceImpl.getProjectInfo(projId);
        if (sTIProject != null) {

            BUListRp rp = new BUListRp();
            rp.setCustomer(sTIProject.getCustomerName());
            rp.setProductLine(sTIProject.getPlatformFoundProductLine());
            R<List<BURv>> r = hdfsClient.buList(rp);
            List<BURv> list = r.getData();
            if (list != null && list.size() > 0) {
                return list.get(0).getBu();
            }
        }
        return "";
    }


    @GetMapping(value = "/queryProjectById/{projId}")
    @ResponseBody
    public List<ReportPojo> queryProjectById(@PathVariable String projId) {
        List<ReportPojo> list = spasServiceImpl.queryProjectById(projId);
        return CollUtil.isNotEmpty(list) ? list : Collections.emptyList();
    }




}
