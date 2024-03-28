package com.foxconn.plm.tcreport.drawcountreport.domain;

import com.foxconn.plm.tcapi.serial.SerialCloneable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author HuashengYu
 * @Date 2023/1/2 15:14
 * @Version 1.0
 */
@Data
@EqualsAndHashCode
public class DrawCountBean extends SerialCloneable implements Comparable<DrawCountBean> {

    private static final long serialVersionUID = 1L;
    private String bu = "";
    private String customer = "";
    private String productLine = "";
    private String projectSeries = "";
    private String projectName = "";
    private String designTreeType = ""; // 协同结构树类别
    private String designTreeName = ""; // 协同结构树名称
    private String owner = "";
    private String ownerGroup = "";
    private String practitioner = "";
    private String itemCode = ""; // 零件编码
    private String itemName = ""; // 零件名称
    private String itemType = ""; // 零件类别
    private Integer uploadNum = 0; // 应上传数量
    private Integer releaseNum = 0; // 已发布数量
    private String releaseProgress = ""; // 发布进度
    private Integer releaseModelNum = 0; // 发布3D模型数量
    private String itemCompleteness = ""; // 3D零件完整度
    private String drawCompleteness = ""; // 3D图档完整度
    private String projectId;
    private String phase;
    private String chassis;

    @Override
    public int compareTo(DrawCountBean o) {
        int i = this.bu.compareTo(o.getBu());
        if (i == 0) {
            int j = this.customer.compareTo(o.getCustomer());
            if (j == 0) {
                int k = this.productLine.compareTo(o.getProductLine());
                if (k == 0) {
                    int m = this.projectSeries.compareTo(o.getProjectSeries());
                    if (m == 0) {
                        int n = this.projectName.compareTo(o.getProjectName());
                        if (n == 0) {
                            int p = this.designTreeType.compareTo(o.getDesignTreeType());
                            if (p == 0) {
                                int q = this.designTreeName.compareTo(o.getDesignTreeName());
                                if (q == 0) {
                                    int r = this.itemCode.compareTo(o.getItemCode());
                                    if (r == 0) {
                                        int s = this.itemName.compareTo(o.getItemName());
                                        if (s == 0) {
                                            int t = this.itemType.compareTo(o.getItemType());
                                            if (t == 0) {
                                                return this.itemCode.compareTo(o.getItemCode());
                                            } else {
                                                return t;
                                            }
                                        } else {
                                            return s;
                                        }
                                    } else {
                                        return r;
                                    }
                                } else {
                                    return q;
                                }
                            } else {
                                return p;
                            }
                        } else {
                            return n;
                        }
                    } else {
                        return m;
                    }
                } else {
                    return k;
                }
            } else {
                return j;
            }
        }
        return i;
    }
}
