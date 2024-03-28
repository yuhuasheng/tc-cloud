package com.foxconn.plm.integrate.spas.service.impl;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.dp.plm.privately.Access;
import com.foxconn.plm.entity.param.BUListRp;
import com.foxconn.plm.entity.response.BURv;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.feign.service.HDFSClient;
import com.foxconn.plm.integrate.spas.domain.PhasePojo;
import com.foxconn.plm.integrate.spas.domain.ReportPojo;
import com.foxconn.plm.integrate.spas.domain.SPASUser;
import com.foxconn.plm.integrate.spas.domain.STIProject;
import com.foxconn.plm.integrate.spas.mapper.SpasMapper;
import com.foxconn.plm.integrate.spas.service.SpasService;

import com.foxconn.plm.utils.collect.CollectUtil;
import com.foxconn.plm.utils.string.StringUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;


@Service("reportServiceImpl")
public class SpasServiceImpl implements SpasService {

    private static Log log = LogFactory.get();
    @Resource
    private SpasMapper spasMapper;

    @Resource
    private HDFSClient hDFSClient;

    @Override
    public List<ReportPojo> searchPojects(String startDate, String endDate,String buName) throws Exception {
        if("MNT".equalsIgnoreCase(buName)||"PRT".equalsIgnoreCase(buName)){
            if(startDate.compareToIgnoreCase("2022-06-13")<0){
                startDate="2022-06-13" ;
            }
        }
        List<ReportPojo> projects = spasMapper.queryProjects(startDate, endDate);
        for (ReportPojo r : projects) {
            BUListRp buListRp=new BUListRp();
            buListRp.setCustomer(r.getCustomer());
            buListRp.setProductLine(r.getProductLine());
            R<List<BURv>> buRv= hDFSClient.buList(buListRp);
            String bu=null;
            List<BURv> data= buRv.getData();
            if(data!=null&&data.size()>0){
                bu=data.get(0).getBu();
            }
            if (bu == null) {
                bu = "N/A";
            }
            r.setBu(bu);
        }
        return projects;
    }

    /**获取专案的阶段信息 */
    @Override
    public ReportPojo getPhases(String projectId) throws Exception {

        List<ReportPojo> projects = spasMapper.queryProjectById(Access.check(projectId));
        ReportPojo project = projects.get(0);

        BUListRp buListRp=new BUListRp();
        buListRp.setCustomer(project.getCustomer());
        buListRp.setProductLine(project.getProductLine());
        R<List<BURv>>  buRv= hDFSClient.buList(buListRp);
        String bu=null;
        List<BURv> data= buRv.getData();
        if(data!=null&&data.size()>0){
            bu=data.get(0).getBu();
        }
        if (bu == null) {
            bu = "N/A";
        }
        project.setBu(bu);

        List<PhasePojo> phases = spasMapper.getPhases(Access.check(projectId));
        project.setPhases(phases);
        return project;
    }

    @Override
    public List<ReportPojo> getAllPhases(String projectId) throws Exception {

        List<ReportPojo> projects = spasMapper.queryProjectPhases(Access.check(projectId));

        return projects;
    }


    @Override
    public List<Map> getCurBUTCProject(List<Map> list, String BUName) {
        List<Map> resultList = Collections.synchronizedList(new ArrayList<Map>());
        list.forEach( e -> {
            String projectId = e .get("projectId") == null ? "" : e .get("projectId").toString().
                                trim().replace("p", "").replace("P", "");
            try {

                List<ReportPojo> projects = spasMapper.queryProjectById(Access.check(projectId));
                if (CollectUtil.isEmpty(projects)) {
                    return;
                }
                ReportPojo project = projects.get(0);

                BUListRp buListRp=new BUListRp();
                buListRp.setCustomer(project.getCustomer());
                buListRp.setProductLine(project.getProductLine());
                R<List<BURv>>  buRv= hDFSClient.buList(buListRp);
                String bu=null;
                List<BURv> data= buRv.getData();
                if(data!=null&&data.size()>0){
                    bu=data.get(0).getBu();
                }

//                String bu = TCUtils.getBUName(project.getCustomer(), project.getProductLine());
                log.info("==>> 专案ID为: " + e.get("projectId") + ", 所属BU为: " + bu);
                if (BUName!=null &&bu !=null && BUName.toUpperCase(Locale.ENGLISH).equals(bu.toUpperCase(Locale.ENGLISH))) {
                    resultList.add(e);
                }
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });
        return resultList;
    }

    @Override
    public STIProject getProjectInfo(String projId) {
        return spasMapper.getProjectInfo(projId);
    }

    @Override
    public List<SPASUser> queryTeamRoster(String[] platformFoundIds) {
        for(int i=0;i< platformFoundIds.length;i++){
            String id=platformFoundIds[i];
            if(id.toLowerCase(Locale.ENGLISH).startsWith("p")){
                id=id.toLowerCase(Locale.ENGLISH).substring(1);
                platformFoundIds[i]=id;
            }
        }
        return spasMapper.queryTeamRoster(platformFoundIds);
    }

    @Override
    public List<SPASUser> queryTeamRosterByEmpId(String empId) {

        return spasMapper.queryTeamRosterByEmpId(empId);
    }

    @Override
    public List<SPASUser> selectSPASUser() {
        return spasMapper.selectSPASUser();
    }

    public List<ReportPojo> queryProjectById(String projId) {
        return spasMapper.queryProjectById(projId);
    }
}
