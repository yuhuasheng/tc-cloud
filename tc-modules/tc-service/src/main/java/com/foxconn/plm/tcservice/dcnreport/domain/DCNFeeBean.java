package com.foxconn.plm.tcservice.dcnreport.domain;

import lombok.Data;

/**
 * @Author MW00333
 * @Date 2023/5/8 14:15
 * @Version 1.0
 */
@Data
public class DCNFeeBean implements Comparable<DCNFeeBean> {

    private String index = "0";
    private String projectId;
    private String projectName;
    private String userId;
    private String userName;
    private String dcnNo;
    private String dcnVersion;
    private String reason = "N/A";
    private String hhpn;
    private String modelNo;
    private String modelNoVersion;
    private String partName;
    private String dcnFee = "0";
    private String newMoldFee = "0";
    private String dcnFeePer = "0.0";
    private String total;

    @Override
    public int compareTo(DCNFeeBean o) {
        int i = this.index.compareTo(o.getIndex());
        if (i == 0) {
            int j = this.userId.compareTo(o.getUserId());
            if (j == 0) {
                int k = this.userName.compareTo(o.getUserName());
                if (k == 0) {
                    return this.reason.compareTo(o.getReason());
                } else {
                    return k;
                }
            } else {
                return j;
            }
        } else {
            return i;
        }
    }
}
