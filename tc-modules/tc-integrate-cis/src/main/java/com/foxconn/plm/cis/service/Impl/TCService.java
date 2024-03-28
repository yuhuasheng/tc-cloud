package com.foxconn.plm.cis.service.Impl;

import cn.hutool.json.JSONUtil;
import com.foxconn.plm.cis.config.dataSource.DataSource;
import com.foxconn.plm.cis.config.dataSource.DataSourceType;
import com.foxconn.plm.cis.domain.EE3DProjectBean;
import com.foxconn.plm.cis.domain.EE3DReportBean;
import com.foxconn.plm.cis.mapper.tc.TCMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@DataSource(value = DataSourceType.XPLM)
public class TCService {

    @Resource
    private TCMapper tcMapper;

    public String getChildFolderUidByName(String parentFolderUid, String folderName) {
        List<String> list = tcMapper.getChildFolderUidByName(parentFolderUid, folderName);
        if (list.size() > 0) {
            return list.get(0);
        }
        return null;
    }

    public List<EE3DReportBean> selectEE3DReportList(EE3DProjectBean bean) {
        if (bean == null) {
            bean = new EE3DProjectBean();
        }
        List<EE3DReportBean> dataList = tcMapper.selectEE3DReportBean(bean);
        dataList.sort(Comparator.comparing(e -> e.getBu() + e.getCustomer() + e.getProjectSeries() + e.getProjectName() + e.getPhase() + e.getVersion() + e.getCategory() + e.getPartType()));
        return dataList;
    }

    public int batchInsertEE3DReport(List<EE3DReportBean> list) {
        if (list != null && list.size() > 0) {
            return tcMapper.batchInsertEE3DReportBean(list);
        }
        return 0;
    }

    public void deleteAllEE3DReport() {
        tcMapper.deleteAllEE3DReportBean();
    }

    public Set<String> selectConditionProject(EE3DProjectBean projectBean) {
        if (projectBean != null && StringUtils.hasLength(projectBean.getValue())) {
            return tcMapper.selectConditionProject(projectBean);
        }
        System.out.println(" illegal parameter : " + JSONUtil.toJsonStr(projectBean));
        return null;
    }

}
