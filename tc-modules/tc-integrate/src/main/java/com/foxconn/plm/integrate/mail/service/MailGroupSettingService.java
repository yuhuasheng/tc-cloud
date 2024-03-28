package com.foxconn.plm.integrate.mail.service;

import com.foxconn.plm.integrate.mail.domain.ItemInfo;
import com.foxconn.plm.integrate.mail.domain.MailGroup;
import com.foxconn.plm.integrate.mail.domain.MailItem;
import com.foxconn.plm.integrate.mail.domain.MailUser;
import com.foxconn.plm.integrate.mail.domain.rp.MailGroupRp;
import com.foxconn.plm.integrate.mail.domain.rp.MailUserRp;
import org.apache.ibatis.annotations.Param;

import java.util.HashMap;
import java.util.List;

public interface MailGroupSettingService {


    public List<MailUser> getGroupUsersByName(String groupName);

    public List<MailUser> getGroupUsers(Long groupId);


    public void deleteGroup(Long id, String creator);


    public List<MailGroup> getMailGroups(Integer pageSize, Integer page, String userId, String bu);


    public Integer getMailGroupsCnt(String bu);


    public List<MailItem> getGroupItems(Long groupId);


}
