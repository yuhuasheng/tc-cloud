package com.foxconn.plm.tcservice.ftebenefitreport.domain;

import com.foxconn.plm.tcservice.ftebenefitreport.constant.BUEnum;
import com.foxconn.plm.tcservice.ftebenefitreport.constant.FTEConstant;
import com.foxconn.plm.tcservice.ftebenefitreport.constant.FunctionNameEnum;
import com.foxconn.plm.utils.excel.ExcelUtil;
import com.foxconn.plm.utils.math.MathUtil;
import com.foxconn.plm.utils.string.StringUtil;
import lombok.Data;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @Author HuashengYu
 * @Date 2022/9/21 16:16
 * @Version 1.0
 */
@Data
public class FTERecordInfo {
    private String BU;
    private String functionName;
    private String productline;
    private String product;
    private String PN;
    private String costTransferNumber;
    private Integer reworkNumber;
    private Double reworkCost;
    private Date reworkDate;
    private String years;

    public static FTERecordInfo newFTERecordInfo(List<String> list, String reportType, int index) throws Exception {
        FTERecordInfo bean = new FTERecordInfo();
        String functionName = null;
        String BU = null;
        String product = null;
        String productline = null;
        String PN = null;
        String costTransferNumber = null;
        Double reworkCost = null;
        Integer reworkNumber = null;
        Date reworkDate = null;
        Boolean isRd = true;
        String str = "";
        try {
            productline = reportType;
            switch (reportType) {
                case FTEConstant.DT_L5:
                    functionName = FunctionNameEnum.REPAI_MOLD.functionNameCh();
                    BU = BUEnum.DT.name();
                    str = list.get(ExcelUtil.getColumIntByString("C"));
                    if (StringUtil.isNotEmpty(str) && !"null".equalsIgnoreCase(str)) {
                        str = StringUtil.replaceBlank(str);
                        reworkDate = new SimpleDateFormat("yyyy/MM/dd").parse(str);
                    }
                    str = list.get(ExcelUtil.getColumIntByString("M"));
                    reworkCost = getReworkCost(str);
                    break;
                case FTEConstant.DT_L6:
                    if (list.get((ExcelUtil.getColumIntByString("A"))).equals("516.0")) {
                        System.out.println(123);
                    }
                    str = list.get((ExcelUtil.getColumIntByString("N")));
                    if (!"是".equalsIgnoreCase(str)) {
                        isRd = false;
                        break;
                    }
                    functionName = FunctionNameEnum.REWORK.functionNameCh();
                    BU = BUEnum.DT.name();
                    product = list.get(ExcelUtil.getColumIntByString("C"));
                    PN = list.get(ExcelUtil.getColumIntByString("D"));
                    str = list.get(ExcelUtil.getColumIntByString("J"));
                    reworkCost = getReworkCost(str);
                    str = list.get(ExcelUtil.getColumIntByString("G"));
                    if (StringUtil.isNotEmpty(str) && !"null".equalsIgnoreCase(str)) {
                        str = StringUtil.replaceBlank(str);
                        reworkNumber = Integer.parseInt(str.split("\\.")[0]);
                    } else {
                        reworkNumber = 0;
                    }
                    str = list.get(ExcelUtil.getColumIntByString("F"));
                    if (StringUtil.isNotEmpty(str) && !"null".equalsIgnoreCase(str)) {
                        str = StringUtil.replaceBlank(str);
                        reworkDate = new SimpleDateFormat("yyyy/MM/dd").parse(str);
                    }
                    break;
                case FTEConstant.DT_L10:
                    str = list.get((ExcelUtil.getColumIntByString("J")));
                    if (!"是".equalsIgnoreCase(str)) {
                        isRd = false;
                        break;
                    }
                    str = list.get((ExcelUtil.getColumIntByString("K")));
                    if (StringUtil.isNotEmpty(str)) {
                        if (FunctionNameEnum.REWORK.functionNameCh().equals(str)) {
                            functionName = FunctionNameEnum.REWORK.functionNameCh();
                        } else if (FunctionNameEnum.STOPLINE.functionNameCh().equals(str)) {
                            functionName = FunctionNameEnum.STOPLINE.functionNameCh();
                        }
                    }
                    BU = BUEnum.DT.name();
                    costTransferNumber = list.get(ExcelUtil.getColumIntByString("B"));
                    str = list.get(ExcelUtil.getColumIntByString("D"));
                    reworkCost = getReworkCost(str);
                    str = list.get(ExcelUtil.getColumIntByString("H"));
                    if (StringUtil.isNotEmpty(str) && !"null".equalsIgnoreCase(str)) {
                        str = StringUtil.replaceBlank(str);
                        reworkDate = new SimpleDateFormat("yyyy/MM/dd").parse(str);
                    }
                    break;
                case FTEConstant.MNT_L5:
                    str = list.get(ExcelUtil.getColumIntByString("J"));
                    if (!"RD".equalsIgnoreCase(str)) {
                        isRd = false;
                        break;
                    }
                    functionName = FunctionNameEnum.REPAI_MOLD.functionNameCh();
                    BU = BUEnum.MNT.name();
                    product = list.get(ExcelUtil.getColumIntByString("C"));
                    str = list.get(ExcelUtil.getColumIntByString("F"));
                    reworkCost = getReworkCost(str);
                    str = list.get(ExcelUtil.getColumIntByString("E"));
                    if (StringUtil.isNotEmpty(str) && !"null".equalsIgnoreCase(str)) {
                        str = StringUtil.replaceBlank(str);
                        reworkDate = new SimpleDateFormat("yyyy/MM/dd").parse(str);
                    }
                    break;
                case FTEConstant.MNT_L6:
                    str = list.get(ExcelUtil.getColumIntByString("I"));
                    if (!"RD".equalsIgnoreCase(str)) {
                        isRd = false;
                        break;
                    }
                    functionName = FunctionNameEnum.REWORK.functionNameCh();
                    BU = BUEnum.MNT.name();
                    product = list.get(ExcelUtil.getColumIntByString("B"));
                    PN = list.get(ExcelUtil.getColumIntByString("C"));
                    str = list.get(ExcelUtil.getColumIntByString("F"));
                    reworkCost = getReworkCost(str);
                    str = list.get(ExcelUtil.getColumIntByString("D"));
                    if (StringUtil.isNotEmpty(str) && !"null".equalsIgnoreCase(str)) {
                        str = StringUtil.replaceBlank(str);
                        reworkNumber = Integer.parseInt(str.split("\\.")[0]);
                    } else {
                        reworkNumber = 0;
                    }
                    str = list.get(ExcelUtil.getColumIntByString("A"));
                    if (StringUtil.isNotEmpty(str) && !"null".equalsIgnoreCase(str)) {
                        str = StringUtil.replaceBlank(str);
                        reworkDate = new SimpleDateFormat("yyyy/MM/dd").parse(str);
                    }
                    break;
                case FTEConstant.MNT_L10:
                    str = list.get(ExcelUtil.getColumIntByString("M"));
                    if (!"RD".equalsIgnoreCase(str)) {
                        isRd = false;
                        break;
                    }
                    functionName = FunctionNameEnum.REWORK.functionNameCh();
                    BU = BUEnum.MNT.name();
                    product = list.get(ExcelUtil.getColumIntByString("G"));
                    str = list.get(ExcelUtil.getColumIntByString("O"));
                    reworkCost = getReworkCost(str);
                    str = list.get(ExcelUtil.getColumIntByString("D"));
                    if (StringUtil.isNotEmpty(str) && !"null".equalsIgnoreCase(str)) {
                        str = StringUtil.replaceBlank(str);
                        reworkDate = new SimpleDateFormat("yyyy/MM/dd").parse(str);
                    }
                    break;
            }
            if (!isRd) {
                return null;
            } else {
                bean.setBU(BU);
                bean.setFunctionName(functionName);
                bean.setProductline(productline);
                bean.setProduct(product == null ? null : StringUtil.replaceBlank(product).trim());
                bean.setPN(PN == null ? null : StringUtil.replaceBlank(PN).trim());
                bean.setCostTransferNumber(costTransferNumber == null ? null : StringUtil.replaceBlank(costTransferNumber).trim());
                bean.setReworkNumber(reworkNumber);
                bean.setReworkCost(reworkCost);
                bean.setReworkDate(reworkDate);
                return bean;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("第" + (index + 1) + "行" + e.getLocalizedMessage() + ", 解析异常");
        }
    }

    /**
     * 获取重工费用
     *
     * @param str
     * @return
     */
    private static Double getReworkCost(String str) {
        Double reworkCost = null;
        if (StringUtil.isNotEmpty(str)) {
            if (!"null".equalsIgnoreCase(str)) {
                str = StringUtil.replaceBlank(str);
                str = str.trim();
                Double d = new Double(str);
                reworkCost = Double.parseDouble(MathUtil.formatDecimal(str, 2));
            }
        } else {
            reworkCost = 0.0;
        }
        return reworkCost;
    }
}
