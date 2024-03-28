package com.foxconn.plm.integrate.mail.mapper;

import com.foxconn.plm.integrate.mail.domain.ItemInfo;
import com.foxconn.plm.integrate.mail.domain.MailGroup;
import com.foxconn.plm.integrate.mail.domain.MailItem;
import com.foxconn.plm.integrate.mail.domain.MailUser;
import com.foxconn.plm.integrate.mail.domain.rp.*;
import io.swagger.models.auth.In;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MailMapper {

    public List<MailUser> getGroupUsersByName(@Param("groupName") String groupName);

    public List<MailItem> getGroupItems(@Param("groupId") Long groupId);


    public List<MailUser> getGroupUsers(@Param("groupId") Long groupId);

    public Integer getMailUserCnt(@Param("groupId") Long groupId, @Param("empId") String empId);

    public Integer getMailGroupCnt(@Param("groupName") String groupName);

    public void addMailGroup(MailGroupRp mailGroup);

    public void addMailUser(MailUserInner mailUser);

    public void deleteGroup(@Param("id") Long id);

    public void deleteMailUser(@Param("groupId") Long groupId);


    public void addMailItem(MailItemInner mailItem);

    public void deleteMailItem(@Param("id") Long id);


    public void updateGroup(MailGroupRp mailGroup);

    public List<MailGroup> getMailGroups(@Param("rowStart") Integer rowStart, @Param("rowEnd") Integer rowEnd, @Param("bu") String bu);

    public Integer getMailGroupsCnt(@Param("bu") String bu);
}
