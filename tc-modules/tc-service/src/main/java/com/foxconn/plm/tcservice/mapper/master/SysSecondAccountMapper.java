package com.foxconn.plm.tcservice.mapper.master;

import com.foxconn.plm.tcservice.issuemanagement.bean.AccountBean;
import com.foxconn.plm.tcservice.issuemanagement.bean.TcAccountUserBean;
import com.foxconn.plm.tcservice.issuemanagement.bean.TcUserBean;
import com.foxconn.plm.tcservice.issuemanagement.entity.SysSecondAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 二級賬號mapper
 *
 * @Description
 * @Author MW00442
 * @Date 2023/11/24 16:53
 **/
@Mapper
public interface SysSecondAccountMapper {

    Long getId();

    Boolean insertEntity(SysSecondAccount bean);

    List<AccountBean> getAdminAccount();

    Boolean updateById(SysSecondAccount bean);

    List<SysSecondAccount> getAll();

    SysSecondAccount getbyId(@Param("accountId") String accountId);

    List<TcUserBean> getAllTcUser(@Param("puid") String puid);

    List<TcAccountUserBean> getAllTcAccountUser(@Param("puid") String puid);

    Boolean deleteById(@Param("id")Long id);
}
