package com.foxconn.plm.integrate.pnms.service;


import com.foxconn.dp.plm.privately.Access;
import com.foxconn.plm.integrate.pnms.mapper.AddCableInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service("addCableInfoServiceImpl")
public class AddCableInfoServiceImpl {

    @Autowired(required = false)
    private AddCableInfoMapper addCableInfoMapper;


    public int getCableCountByHHPN(String hhpn) {
        return addCableInfoMapper.getCableCountByHHPN(hhpn);
    }


    public String getHHPNEmptyByGroupId(String groupId) {

        return addCableInfoMapper.getHHPNEmptyByGroupId(Access.check(groupId));
    }


    public void addCableInfo(String hhpn, String designPN, String desc, String mfg,
                             String groupId, String creator, Date creationTime, Date modifyTime) {
        addCableInfoMapper.addCableInfo(hhpn, designPN, desc, mfg, groupId, creator, creationTime, modifyTime);
    }


    public void updateCableInfo(String hhpn, String designPN, String desc, String mfg, Date modifyTime, String id) {

        addCableInfoMapper.updateCableInfo(Access.check(hhpn), Access.check(designPN), Access.check(desc), Access.check(mfg), Access.check(modifyTime), Access.check(id));
    }
}
