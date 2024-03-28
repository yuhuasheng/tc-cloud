package com.foxconn.plm.integrate.mail.service;

import com.foxconn.plm.integrate.mail.domain.MailUser;
import com.foxconn.plm.integrate.mail.domain.rp.MailUserRp;

import java.util.List;

public interface SapsUserService {


    public List<MailUser> findMailUsers(String keyWords);


}
