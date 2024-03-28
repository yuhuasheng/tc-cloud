package com.foxconn.plm.integrate.mail.service.impl;

import com.foxconn.plm.integrate.mail.domain.MailUser;
import com.foxconn.plm.integrate.mail.service.SapsUserService;
import com.foxconn.plm.integrate.spas.mapper.SpasMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("sapasUserServiceImpl")
public class SapasUserServiceImpl implements SapsUserService {


    @Autowired(required = false)
    private SpasMapper spasMapper;

    @Override
    public List<MailUser> findMailUsers(String keyWords) {

        return spasMapper.findMailUsers(keyWords);
    }
}
