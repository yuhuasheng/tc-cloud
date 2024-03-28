package com.foxconn.plm.tcservice.benefitreport.service;


import com.foxconn.plm.entity.response.R;
import org.apache.poi.ss.usermodel.Workbook;

import java.text.ParseException;
import java.util.Map;

/**
 * @Author HuashengYu
 * @Date 2022/7/13 10:12
 * @Version 1.0
 */
public interface SpasProjectService {

    R getSpasProjectByDate(String startDate, String bu, Workbook wb, Map<String,Integer> sheetIndexMap);

    R getSingleSpasProject(String projectId, String projectName, String bu);

    void wirteExcel(Workbook wb, String bu, String startDate) throws ParseException;
    void wirteExcelFor2022(Workbook wb, String bu, String startDate) throws ParseException;
}
