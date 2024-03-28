package com.foxconn.plm.integrate.mail.service.impl;

import com.foxconn.plm.entity.exception.BizException;
import com.foxconn.plm.integrate.mail.domain.MailGroup;
import com.foxconn.plm.integrate.mail.domain.MailItem;
import com.foxconn.plm.integrate.mail.domain.MailUser;
import com.foxconn.plm.integrate.mail.mapper.MailMapper;
import com.foxconn.plm.integrate.mail.service.MailGroupSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;


@Service("mailGroupSettingImpl")
public class MailGroupSettingImpl implements MailGroupSettingService {


    @Autowired(required = false)
    private MailMapper mailMapper;

    @Override
    public List<MailUser> getGroupUsersByName(String groupName) {
        return mailMapper.getGroupUsersByName(groupName);
    }

    @Override
    public List<MailUser> getGroupUsers(Long groupId) {
        return mailMapper.getGroupUsers(groupId);
    }

    @Override
    public void deleteGroup(Long id, String creator) {
        mailMapper.deleteGroup(id);
    }


    @Override
    public List<MailGroup> getMailGroups(Integer pageSize, Integer page, String userId, String bu) {
     try {
         if (bu == null || "".equalsIgnoreCase(bu)) {
             throw new BizException("bu 爲空");
         }

         if (userId == null || "".equalsIgnoreCase(userId)) {
             throw new BizException("用戶 爲空");
         }
         int startRow = (page - 1) * pageSize + 1;
         int endRow = page * pageSize;
         List<MailGroup> groups = mailMapper.getMailGroups(startRow, endRow, bu);
         for (MailGroup g : groups) {
             System.out.println(g.getId());
             if (userId.equalsIgnoreCase(g.getCreator())) {
                 g.setOperate("2");
             } else {
                 String us = g.getEditUser();
                 int f = 0;
                 if (us != null) {
                     String[] m = us.split(",");
                     for (String s : m) {
                         System.out.println("user======" + s);
                         if (s.equalsIgnoreCase(userId.trim())) {
                             f = 1;
                             break;
                         }
                     }
                 }
                 if (f == 1) {
                     g.setOperate("1");
                 } else {
                     g.setOperate("-1");
                 }
             }
             HashMap<String, List<String>> mps = new HashMap<String, List<String>>();
             List<MailItem> mailItems = mailMapper.getGroupItems(g.getId());
             for (MailItem item : mailItems) {
                 String itemCategory = item.getItemCategory();
                 List<String> ls;
                 if ("objectType".equalsIgnoreCase(itemCategory)) {
                     ls = mps.get(item.getItemId());
                     if (ls == null) {
                         ls = new ArrayList<>();
                         mps.put(item.getItemId(), ls);
                         ls.add("*");
                     }

                 } else {
                     String objType = item.getObjType();
                     ls = mps.get(objType);
                     if (ls == null) {
                         ls = new ArrayList<>();
                         mps.put(objType, ls);
                     }

                     if (ls.size() < 10) {
                         ls.add(item.getItemId());
                     }
                 }
             }
             if (mps.size() <= 0) {
                 continue;
             }
             Set<String> keys = mps.keySet();
             int i = 0;
             for (String key : keys) {
                 String itemids = "";
                 List<String> ls = mps.get(key);
                 for (String s : ls) {
                     itemids += s + ",";
                 }
                 if (itemids.endsWith(",")) {
                     itemids = itemids.substring(0, itemids.length() - 1);
                 }
                 if (i == 0) {
                     g.setItems(itemids);
                     g.setObjType(key);
                 } else {
                     MailGroup g2 = new MailGroup();
                     g2.setObjType(key);
                     g2.setItems(itemids);
                     g2.setId(g.getId());
                     g2.setBu(g.getBu());
                     g2.setCreated(g.getCreated());
                     g2.setCreatorName(g.getCreatorName());
                     g2.setDescription(g.getDescription());
                     g2.setGroupName(g.getGroupName());
                     g2.setOperate(g.getOperate());
                     groups.add(g2);
                 }
                 i++;
             }
         }
         return groups;
     }catch(Exception e){
         System.out.println(e);
         return null;
     }

    }

    @Override
    public Integer getMailGroupsCnt(String bu) {
        return mailMapper.getMailGroupsCnt(bu);
    }

    @Override
    public List<MailItem> getGroupItems(Long groupId) {
        return mailMapper.getGroupItems(groupId);
    }


}



