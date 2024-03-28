package com.foxconn.plm.tcservice.connandcable.domain;

import com.foxconn.plm.entity.constants.TCPropName;
import com.foxconn.plm.utils.excel.ExcelUtil;
import com.foxconn.plm.utils.string.StringUtil;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static com.foxconn.plm.tcservice.connandcable.constant.ConnCableConstant.*;

/**
 * @Author HuashengYu
 * @Date 2022/10/6 16:50
 * @Version 1.0
 */
@Data
public class CableBean implements Comparable<CableBean> {

    @TCPropName(tcProperty = "", cell = 0)
    private Integer groupId;

    @TCPropName(tcProperty = "item_id")
    private String HHPN;

    @TCPropName(tcProperty = "d9_EnglishDescription")
    private String description;

    @TCPropName(tcProperty = "d9_ManufacturerID")
    private String supperlier;

    private String type;

    @TCPropName(tcProperty = "object_type")
    private String objectType;

    private String creator = "admin";

    private Boolean filterFlag = false; // 是否过滤掉的标识

    public static List<CableBean> newCableBean(List<String> list, int index, List<String> msgList) throws Exception {
        try {
            List<CableBean> beanList = new ArrayList<>();
            CableBean connBean = new CableBean();
            CableBean cableBean = new CableBean();
            // Conn
            String str = "";
            str = list.get(ExcelUtil.getColumIntByString("A"));
            if (StringUtil.isNotEmpty(str)) {
                str = StringUtil.replaceBlank(str);
                connBean.setGroupId(Integer.valueOf(str));
                cableBean.setGroupId(Integer.valueOf(str));
            }
            str = list.get(ExcelUtil.getColumIntByString("B"));
            if (StringUtil.isNotEmpty(str)) {
                str = StringUtil.replaceBlank(str);
                connBean.setHHPN(str);
            }
            str = list.get(ExcelUtil.getColumIntByString("C"));
            if (StringUtil.isNotEmpty(str)) {
                str = StringUtil.replaceBlank(str);
                connBean.setDescription(str);
            }

            str = list.get(ExcelUtil.getColumIntByString("D"));
            if (StringUtil.isNotEmpty(str)) {
                str = StringUtil.replaceBlank(str);
                connBean.setSupperlier(str);
            }
            connBean.setType(CONN);
            connBean.setObjectType(CONNTYPE);
            if (checkEmpty(connBean)) {
                connBean.setFilterFlag(true);
            } else if (checkAnyEmpty(connBean)) {
                msgList.add("【ERROR】第" + (index + 1) + "行" + ", 数据存在问题");
                connBean.setFilterFlag(true);
            }
            beanList.add(connBean);
            // Cable
            str = list.get(ExcelUtil.getColumIntByString("E"));
            if (StringUtil.isNotEmpty(str)) {
                str = StringUtil.replaceBlank(str);
                cableBean.setHHPN(str);
            }
            str = list.get(ExcelUtil.getColumIntByString("F"));
            if (StringUtil.isNotEmpty(str)) {
                str = StringUtil.replaceBlank(str);
                cableBean.setDescription(str);
            }
            str = list.get(ExcelUtil.getColumIntByString("G"));
            if (StringUtil.isNotEmpty(str)) {
                str = StringUtil.replaceBlank(str);
                cableBean.setSupperlier(str);
            }
            cableBean.setType(CABLE);
            cableBean.setObjectType(CABLETYPE);
            if (checkEmpty(cableBean)) {
                cableBean.setFilterFlag(true);
            } else if (checkAnyEmpty(cableBean)) {
                msgList.add("【ERROR】第" + (index + 1) + "行" + ", 数据存在问题");
                cableBean.setFilterFlag(true);
            }

            beanList.add(cableBean);
            return beanList;
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("【ERROR】第" + (index + 1) + "行" + e.getLocalizedMessage() + ", 解析异常");
        }

    }

    private static boolean checkEmpty(CableBean bean) {
        if (StringUtil.isEmpty(bean.getHHPN()) && StringUtil.isEmpty(bean.getDescription()) && StringUtil.isEmpty(bean.getSupperlier())) {
            return true;
        }

        if ("null".equals(bean.getHHPN()) && "null".equals(bean.getDescription()) && "null".equals(bean.getSupperlier())) {
            return true;
        }
        return false;
    }


    private static boolean checkAnyEmpty(CableBean bean) {
        if (StringUtil.isEmpty(bean.getHHPN()) || StringUtil.isEmpty(bean.getDescription()) || StringUtil.isEmpty(bean.getSupperlier())) {
            return true;
        }
        return false;
    }


    @Override
    public int compareTo(CableBean o) {
        int i = this.groupId.compareTo(o.getGroupId());
        if (i == 0) {
            return o.getType().compareTo(this.type);
        }
        return i;
    }
}
