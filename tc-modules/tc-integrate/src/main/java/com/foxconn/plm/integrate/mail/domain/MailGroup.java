package com.foxconn.plm.integrate.mail.domain;

import io.swagger.annotations.ApiModel;
import lombok.Data;

@ApiModel
@Data
public class MailGroup {

    private Long id;
    private String groupName;
    private String description;
    private String creator;
    private String creatorName;
    private String updateBy;
    private String updateByName;
    private String bu;
    private String created;
    private String lastUpd;
    private String objType;
    private String items;
    private String operate; //"-1" 查看  1 查看 編輯       2 查看  編輯  刪除
    private String editUser;

}
