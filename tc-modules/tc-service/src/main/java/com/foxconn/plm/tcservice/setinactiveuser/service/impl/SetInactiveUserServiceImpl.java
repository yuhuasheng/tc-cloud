package com.foxconn.plm.tcservice.setinactiveuser.service.impl;

import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.tcservice.mapper.master.SetInactiveUserMapper;
import com.foxconn.plm.tcservice.setinactiveuser.domain.UserBean;
import com.foxconn.plm.tcservice.setinactiveuser.service.SetInactiveUserService;
import com.foxconn.plm.utils.string.StringUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class SetInactiveUserServiceImpl implements SetInactiveUserService {

    @Resource
    SetInactiveUserMapper mapper;


    @Override
    public List<UserBean> getUserInfo(int days, List<String> excludeUsers) {
        return mapper.getUserInfo(days, excludeUsers);
    }

    @Override
    public List<UserBean> getUserInfoByIds(List<String> userIds) {
        return mapper.getUserInfoByIds(userIds);
    }

    @Override
    public void updateUserState(List<UserBean> users) {
        mapper.updateUserState(users);
    }

    @Override
    public void setUserState(List<UserBean> users) {
        TCSOAServiceFactory tCSOAServiceFactory = null;
        try {
            tCSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);

            TCUtils.byPass(tCSOAServiceFactory.getSessionService(), true);
            for (UserBean user : users) {
                try {
                    String userId = user.getUserId();
//                    ModelObject[] userMOArr = TCUtils.executequery(SavedQueryService.getService(tcsoaClientConfig.getConnection()), DataManagementService.getService(tcsoaClientConfig.getConnection()), "__WEB_find_user", new String[]{"User ID"}, new String[]{userId});
                    if (StringUtil.isNotEmpty(userId)) {
//                        TCUtils.setProperties(DataManagementService.getService(tcsoaClientConfig.getConnection()), userMOArr[0], "status", "1");
                        TCUtils.setUserProperties(tCSOAServiceFactory.getUserManagementService(), userId, "status", "1");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } finally {
            try {
                tCSOAServiceFactory.logout();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

}
