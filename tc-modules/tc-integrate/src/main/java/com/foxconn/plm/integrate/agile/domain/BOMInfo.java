package com.foxconn.plm.integrate.agile.domain;

import com.foxconn.plm.entity.constants.TCPropName;
import com.teamcenter.soa.client.model.strong.BOMLine;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import lombok.Data;

import java.util.List;

@Data
public class BOMInfo {

    //1021
    //@TCPropName("1021")
    private String pnRev;//版本
    //1334
    private String bomRev;
    //1003
    private String customer;//客户
    //1307
    private String project;//专案
    //1303
    private String phase;//阶段
    //1011
    //@TCPropName("1011")
    private String partNum;//料号
    //1012
    @TCPropName("1012")
    private String findNum;//查找编号
    //1020
    // @TCPropName("1020")
    private String description;//描述
    //2199
    @TCPropName("2199")
    private String unit;//单位

    private String materialType;//物料类型
    private String materialGroup;//物料群组

    private String supplier;//厂商
    private String supplierPN;//厂商料号
    //1035
    @TCPropName("1035")
    private String qty;//数量
    //1341
    @TCPropName("1341")
    private String location;//位号
    //1640
    @TCPropName("1640")
    private String ccl;
    //1636
    @TCPropName("1636")
    private String altCode;//主料 ||替代料
    //2175
    @TCPropName("2175")
    private String altGroup;
    //1638
    @TCPropName("1638")
    private String usagProb;

    private ItemRevision itemRev;
    private List<BOMInfo> substitute;
    private List<BOMInfo> child;
    //D9_VirtualPart
    private boolean isVirtualPart;

    private BOMLine bomLine;
}
