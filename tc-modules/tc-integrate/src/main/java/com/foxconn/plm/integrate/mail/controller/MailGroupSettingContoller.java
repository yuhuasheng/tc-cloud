package com.foxconn.plm.integrate.mail.controller;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.entity.response.RList;
import com.foxconn.plm.integrate.mail.domain.MailGroup;
import com.foxconn.plm.integrate.mail.domain.MailItem;
import com.foxconn.plm.integrate.mail.domain.MailUser;
import com.foxconn.plm.integrate.mail.service.SapsUserService;
import com.foxconn.plm.integrate.mail.service.impl.MailGroupSettingImpl;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@RestController
@Scope("request")
@RequestMapping("/mailgroupsetting")
public class MailGroupSettingContoller {
    private static Log log = LogFactory.get();

    @Autowired(required = false)
    private MailGroupSettingImpl mailGroupSettingImpl;


    @Autowired(required = false)
    private SapsUserService sapasUserServiceImpl;


    @ApiOperation("查询用户")
    @PostMapping("/findMailUser")
    public R<List<MailUser>> findMailUser(String keyWords) {
        List<MailUser> users = new ArrayList<>();
        log.info("begin findMailUser");
        log.info(keyWords);
        users = sapasUserServiceImpl.findMailUsers(keyWords);
        log.info("end findMailUser");
        return R.success(users);
    }


    @ApiOperation("查询群組裏的對象")
    @PostMapping("/getGroupItems")
    public RList<MailItem> getGroupItems(Long groupId) {
        log.info("begin getGroupItems");

        List<MailItem> items = mailGroupSettingImpl.getGroupItems(groupId);

        log.info("end getGroupItems");
        return RList.ok(items, items.size());
    }

    @ApiOperation("查询邮件群组")
    @PostMapping("/getMailGroup")
    public RList<MailGroup> getMailGroup(Integer pageSize, Integer page, String userId, String bu) {
        log.info("begin addMailGroup");
        log.info("pageSize:" + pageSize + ",page:" + page + " ,userId :" + userId);
        List<MailGroup> groups = mailGroupSettingImpl.getMailGroups(pageSize, page, userId, bu);
        int cnt = mailGroupSettingImpl.getMailGroupsCnt(bu);
        log.info("end addMailGroup");
        return RList.ok(groups, cnt);
    }


    @ApiOperation("删除邮件群组")
    @PostMapping("/deleteMailGroup")
    public R<Long> deleteMailGroup(Long groupId, String updateBy) {
        log.info("begin deleteMailGroup");
        log.info("id:" + groupId + ",updateBy:" + updateBy);
        mailGroupSettingImpl.deleteGroup(groupId, updateBy);
        log.info("end deleteMailGroup");
        return R.success(groupId);
    }


    @ApiOperation("获取邮件群组成员")
    @PostMapping("/getGroupUsers")
    public R<List<MailUser>> getGroupUsers(Long groupId) {
        log.info("begin getGroupUsers");
        log.info("group id :" + groupId);
        List<MailUser> user = mailGroupSettingImpl.getGroupUsers(groupId);
        return R.success(user);
    }

/*
    private void updateMailbox(List<MailUser> spasUsers) {
        if (spasUsers.size() == 0) {
            return;
        }
        String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
        String DB_URL = "jdbc:mysql://10.203.162.106:3306/user_center?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

        String USER = "TCUser";
        String PASS = "123456";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            Class.forName(JDBC_DRIVER);
            System.out.println("连接数据库...");
            conn = DriverManager.getConnection(DB_URL, USER, PASS);

            StringBuilder sql = new StringBuilder();
            sql.append("SELECT work_id,name,notes FROM view_user where work_id in (");
            for (int i = 0; i < spasUsers.size(); i++) {
                MailUser spasUser = spasUsers.get(i);
                String workId = spasUser.getEmpId();
                if ((i + 1) == spasUsers.size()) {
                    sql.append("'" + workId + "'");
                } else {
                    sql.append("'" + workId + "'" + ",");
                }
            }
            sql.append(")");
            System.out.println("sql:" + sql.toString());
            ps = conn.prepareStatement(sql.toString());
            ResultSet rs = ps.executeQuery();

            // 展开结果集数据库
            while (rs.next()) {
                // 通过字段检索
                String work_id = rs.getString("work_id");
                String name = rs.getString("name");
                String notes = rs.getString("notes");

                for (int i = 0; i < spasUsers.size(); i++) {
                    MailUser spasUser = spasUsers.get(i);
                    String workId = spasUser.getEmpId();
                    if (workId != null && workId.equals(work_id)) {
                        spasUser.setMail(notes);
                    }
                }
                // 输出数据
                System.out.print("work_id: " + work_id);
                System.out.print("name: " + name);
                System.out.print("notes: " + notes);
                System.out.print("\n");
            }
            // 完成后关闭
            rs.close();
            ps.close();
            conn.close();
        } catch (SQLException se) {
            // 处理 JDBC 错误
            se.printStackTrace();
        } catch (Exception e) {
            // 处理 Class.forName 错误
            e.printStackTrace();
        } finally {
            // 关闭资源
            try {
                if (ps != null) ps.close();
            } catch (SQLException se2) {
            }// 什么都不做
            try {
                if (conn != null) conn.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
        System.out.println("Goodbye!");
    }
*/

}
