package com.foxconn.plm.integrate.mail.utils;

import com.foxconn.plm.integrate.agile.domain.BOMInfo;
import com.foxconn.plm.integrate.mail.domain.ItemInfo;
import com.foxconn.plm.utils.net.HttpUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.services.strong.administration.PreferenceManagementService;

import java.util.HashMap;
import java.util.List;


public class MailSupport {

    /**
     * L6 Agile EBOM 邮件通知
     */
    public String genL6EBOMMailBody(String userName, BOMInfo bomInfo) {
        String project = bomInfo.getProject();
        String pcarev = bomInfo.getPnRev();
        String pcapa = bomInfo.getPartNum();
        String html = "<html><head><style>div{margin:10px;}td{padding:2px;}</style></head><body><div>Dear :" + userName + "</div><div>&nbsp;&nbsp;" + pcapa + " has sync to TC </div><table>";
        html += "<tr><td>Project Name:</td></tr>";
        html += "<tr><td>" + project + "</td></tr>";
        html += "<tr><td>&nbsp;</td></tr>";
        html += "<tr><td>PCA P/N:</td></tr>";
        html += "<tr><td>" + pcapa + "</td></tr>";
        html += "<tr><td>&nbsp;</td></tr>";


        html += "<tr><td>PCA Version:</td></tr>";
        html += "<tr><td>" + pcarev + "</td></tr>";
        html += "<tr><td>&nbsp;</td></tr>";

        html += "<tr><td>Description of PCA:</td></tr>";
        html += "<tr><td>" + bomInfo.getDescription() + "</td></tr>";
        html += "<tr><td>&nbsp;</td></tr>";

        html += "</table></body></html>";


        return html;
    }


    /**
     * 技术文档发行 邮件通知
     *
     * @return
     */
    public String genMailBody(PreferenceManagementService preferenmanagementservice, List<ItemInfo> itemInfos, String descr) {

        String html = "";
        try {
            String url = TCUtils.getTCPreferences(preferenmanagementservice, "D9_SpringCloud_URL")[0];
            html = "<html><head><style>div{margin:10px;}table{margin:20px;border-spacing: 0px;}th{border:solid 1px #000000;height: 35px;padding:6px;} td{border-left:solid 1px #000000;border-bottom:solid 1px #000000;border-right:solid 1px #000000;height: 35px;padding:6px;}</style></head><body><div>Dear All:</div><div>&nbsp;&nbsp;The following documents have been released, you can copy the link to download and view.</div><table>";
            html += "<tr><td style='border:solid 1px #000000;'>Summary</td><td style='border:solid 1px #000000;border-left:none;'>" + descr + "</td></tr>";
            for (ItemInfo i : itemInfos) {
                if (i.getDataSet() == null || "".equalsIgnoreCase(i.getDataSet())) {
                    continue;
                }
                String path = url + "/tc-hdfs/downloadFile?refId=" + i.getDataSet() + "&site=wh";
                html += "<tr><td> <a href='" + path + "'>" + i.getItemName() + "</a></td><td style='border-left:none;'>" + path + "</td></tr>";
            }
            html += "</table></body></html>";
        } catch (Exception e) {

        } finally {

        }
        return html;
    }


    /**
     * 零件废弃邮件通知
     *
     * @return
     */
    public String genMailBody(String revId, List<String> revIds) {

        String html = "<html><head><style>div{margin:10px;}table{margin:20px;border-spacing: 0px;}th{border:solid 1px #000000;height: 35px;padding:6px;} td{border-left:solid 1px #000000;border-bottom:solid 1px #000000;border-right:solid 1px #000000;height: 35px;padding:6px;}</style></head><body><div>Dear All:</div><div>&nbsp;&nbsp;以下零件已废弃,请确认.</div><table>";
        html += "<tr><td style='border:solid 1px #000000;'>废弃的零件</td><td style='border:solid 1px #000000;border-left:none;'>影响的装配</td></tr>";
        for (String s : revIds) {
            html += "<tr><td> " + revId + "</td><td style='border-left:none;'>" + s + "</td></tr>";
        }
        html += "</table></body></html>";

        return html;
    }


    public void sendMai(PreferenceManagementService preferenmanagementservice, HashMap<String, String> map) {
        try {
            String url = TCUtils.getTCPreferences(preferenmanagementservice, "D9_SpringCloud_URL")[0];
            map.put("requestPath", url);
            String msg = HttpUtil.httpPost(map);
            System.out.print(msg);
        } catch (Exception e) {
            System.out.print(e);
        }
    }

}
