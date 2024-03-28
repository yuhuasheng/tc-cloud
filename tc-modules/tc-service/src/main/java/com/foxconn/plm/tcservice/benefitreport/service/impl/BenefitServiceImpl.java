package com.foxconn.plm.tcservice.benefitreport.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSON;
import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.entity.response.SPASProject;
import com.foxconn.plm.feign.service.TcIntegrateClient;
import com.foxconn.plm.tcservice.benefitreport.constant.ProjectDifficulty;
import com.foxconn.plm.tcservice.benefitreport.domain.RowDataBean;
import com.foxconn.plm.tcservice.benefitreport.service.BenefitService;
import com.foxconn.plm.tcservice.mapper.master.BenefitReportMapper;
import com.foxconn.plm.utils.collect.CollectUtil;
import com.foxconn.plm.utils.date.DateUtil;
import com.foxconn.plm.utils.string.StringUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author HuashengYu
 * @Date 2022/10/11 13:56
 * @Version 1.0
 */
@Service
public class BenefitServiceImpl implements BenefitService {
    private static Log log = LogFactory.get();
    @Resource
    private BenefitReportMapper benefitReportMapper;

    @Resource
    private TcIntegrateClient tcIntegrate;

    @Override
    public R getTCProject(String bu, String projectName) {
        List<Map> list = benefitReportMapper.getTCProject(projectName);
        if (CollectUtil.isEmpty(list)) {
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(), "含有 " + projectName + " 关键字的专案名在TC中不存在专案, 请重新选择！");
        }
        String result = tcIntegrate.getTCProjectByBu(JSON.toJSONString(list), bu);
        list = JSON.parseArray(result, Map.class);
        if (CollectUtil.isEmpty(list)) {
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(), "含有 " + projectName + " 关键字的专案名没有符合当前BU的记录");
        }
        return  R.success("含有 " + projectName + " 关键字的TC专案获取成功", list);
    }

    @Override
    public R getBenefitRowData(String bu, String startDate, String projectId) {
        try {
            List<SPASProject> list = null;
            if (StringUtil.isNotEmpty(startDate)) {
                Date start = new SimpleDateFormat("yyyy-MM-dd").parse(startDate);
                SimpleDateFormat sdf = new SimpleDateFormat("MM");
                String format = sdf.format(start);
                int month = Integer.parseInt(format);
                int year = Integer.parseInt(new SimpleDateFormat("yyyy").format(start));
                String firstDayOfMonth = DateUtil.getFirstDayOfMonth(month);
                firstDayOfMonth = year + firstDayOfMonth.substring(4);
                firstDayOfMonth = firstDayOfMonth.substring(0, 10);
                log.info(month + " 月第一天：" + firstDayOfMonth);
                String lastDayOfMonth = DateUtil.getLastDayOfMonth(month);
                lastDayOfMonth = year + lastDayOfMonth.substring(4);
                lastDayOfMonth = lastDayOfMonth.substring(0, 10);
                log.info(month + " 月的最后一天:" + lastDayOfMonth);
                list = tcIntegrate.getClosedProjectsByDate(firstDayOfMonth, lastDayOfMonth, bu);
                if (CollUtil.isEmpty(list)) {
                    return R.success( "效益的Row Data为空", new ArrayList<RowDataBean>());
                }
                removeInvalidRecord(list, bu);
            }

            if (CollectUtil.isEmpty(list)) {
                return R.success( "效益的Row Data为空", new ArrayList<RowDataBean>());
            }


            if (StringUtil.isNotEmpty(startDate)) {
                startDate = startDate.substring(0, startDate.lastIndexOf("-"));
            }
            List<RowDataBean> rowTotalDataList = new ArrayList<>();
            List<RowDataBean> rowData = benefitReportMapper.getRowData(bu, startDate, projectId);
            if (StringUtil.isNotEmpty(startDate)) {
                rowData = rowData.stream().filter(CollectUtil.distinctByKey(dataBean -> dataBean.getFunctionName() + dataBean.getProjectId() + dataBean.getBu() + dataBean.getLevels() + dataBean.getPhase())).collect(Collectors.toList()); // 按照功能名 + 专案ID + BU + 阶段去重
                if (CollectUtil.isNotEmpty(rowData)) {
                    for (RowDataBean dataBean : rowData) {
                        rowTotalDataList.addAll(benefitReportMapper.getRowDataByBUAndPhase(dataBean.getBu(), dataBean.getPhase(), dataBean.getFunctionName(), dataBean.getProjectId()));
                    }
                }

                List<RowDataBean> cisRowDataList = benefitReportMapper.getCisRowData(bu, startDate);
                cisRowDataList.removeIf(bean -> StringUtil.isEmpty(bean.getItemId()));

                if (CollectUtil.isNotEmpty(cisRowDataList) && CollectUtil.isEmpty(rowTotalDataList)) {
                    rowTotalDataList.addAll(cisRowDataList);
                } else if (CollectUtil.isNotEmpty(cisRowDataList) && CollectUtil.isNotEmpty(rowTotalDataList)) {
                    for (RowDataBean cisRowData : cisRowDataList) {
                        if (!checkRecord(rowTotalDataList, cisRowData)) {
                            rowTotalDataList.add(cisRowData);
                        }
                    }
                }
            }

            if (CollectUtil.isNotEmpty(list)) {
                rowTotalDataList = filterRowDataList(list, rowTotalDataList);
            }

            if (CollectUtil.isEmpty(rowTotalDataList)) {
                return R.success("效益的Row Data为空", new ArrayList<RowDataBean>());
            }
            return R.success("获取效益的Row Data成功", rowTotalDataList);
        } catch (Exception e) {
            e.printStackTrace();
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(), "效益的Row Data获取失败");
        }
    }

    /**
     * 判断记录是否已经存在
     *
     * @param rowTotalDataList
     * @param cisRowData
     * @return
     */
    private boolean checkRecord(List<RowDataBean> rowTotalDataList, RowDataBean cisRowData) {
        Optional<RowDataBean> findAny = rowTotalDataList.stream().filter(data -> {
            return (data.getFunctionName().equals(cisRowData.getFunctionName()) && data.getItemId().equals(cisRowData.getItemId()));
        }).findAny();

        if (findAny.isPresent()) { // 判断是否匹配成功
            return true;
        } else {
            return false;
        }
    }

    private void removeInvalidRecord(List<SPASProject> list, String bu) {
        list.removeIf(project -> project.getBu() == null || "N/A".equals(project.getBu()) | "".equals(project.getBu()) | StringUtil.isEmpty(project.getProjectId())); // 移除BU为NA, 项目ID为空的记录
        list.removeIf(project -> StringUtil.isEmpty(project.getLevels()) || project.getLevels().split(",").length < 2);
        list.removeIf(project -> !project.getLevels().split(",")[1].contains(ProjectDifficulty.E1.name()) &
                !project.getLevels().split(",")[1].toUpperCase().contains(ProjectDifficulty.E2.name()) &
                !project.getLevels().split(",")[1].toUpperCase().contains(ProjectDifficulty.E3.name()) &
                !project.getLevels().split(",")[1].toUpperCase().contains(ProjectDifficulty.A0.name()) &
                !project.getLevels().split(",")[1].toUpperCase().contains(ProjectDifficulty.A.name()) &
                !project.getLevels().split(",")[1].toUpperCase().contains(ProjectDifficulty.B.name()) &
                !project.getLevels().split(",")[1].toUpperCase().contains(ProjectDifficulty.C.name()) &
                !project.getLevels().split(",")[1].toUpperCase().contains(ProjectDifficulty.D.name()) &
                !project.getLevels().split(",")[1].toUpperCase().contains(ProjectDifficulty.E.name()) &
                !project.getLevels().split(",")[1].toUpperCase().contains(ProjectDifficulty.F.name()));  // 移除专案等级不是E1，E2，E3, A0, A, B, C, D, E, F
        if (StringUtil.isNotEmpty(bu)) {
            list.removeIf(project -> !bu.equals(project.getBu())); // 移除和当前记录不同的BU记录
        }
    }

    /**
     * 过滤掉不符条件的项目记录
     *
     * @param list
     * @param rowDataList
     */
    private List<RowDataBean> filterRowDataList(List<SPASProject> list, List<RowDataBean> rowDataList) {
        List<RowDataBean> rowTotalDataList = new ArrayList<>();
        rowDataList.forEach(bean -> {
            boolean flag = list.stream().anyMatch(spasProject -> ("p" + spasProject.getProjectId()).equalsIgnoreCase(bean.getProjectId())); // 检查是否至少匹配一个元素
            if (flag) {
                rowTotalDataList.add(bean);
            }
        });

        return rowTotalDataList;
    }

}
