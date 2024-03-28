package com.foxconn.plm.integrate.mail.service;

import com.foxconn.plm.integrate.mail.domain.ItemInfo;
import com.foxconn.plm.integrate.mail.domain.MailGroup;
import com.foxconn.plm.integrate.mail.domain.MailItem;
import com.foxconn.plm.integrate.mail.domain.MailUser;
import com.foxconn.plm.integrate.mail.domain.rp.MailGroupRp;
import com.foxconn.plm.integrate.mail.domain.rp.MailItemRp;
import com.foxconn.plm.integrate.mail.domain.rp.MailUserRp;

import java.util.HashMap;
import java.util.List;

public interface MailGroupMnageService {

    public List<ItemInfo> searchItems(String keys);

    public List<String> searchItemObjType();


    public void addMailItem(MailItemRp mailItem);

    public void addMailGroup(MailGroupRp mailGroup);

    public void addMailUser(MailUserRp mailUser);


    public void updateGroup(MailGroupRp mailGroup);

    public List<ItemInfo> getItemInfos(String revIds);
}
