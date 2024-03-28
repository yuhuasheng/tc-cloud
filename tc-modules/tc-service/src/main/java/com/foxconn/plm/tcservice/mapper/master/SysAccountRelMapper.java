package com.foxconn.plm.tcservice.mapper.master;

import com.foxconn.plm.tcservice.issuemanagement.bean.AccountBean;
import com.foxconn.plm.tcservice.issuemanagement.entity.SysAccountRel;
import com.foxconn.plm.tcservice.issuemanagement.param.SearchAccountParam;
import com.foxconn.plm.tcservice.issuemanagement.response.AccountRes;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 賬號對應關係表
 *
 * @Description
 * @Author MW00442
 * @Date 2023/11/24 16:57
 **/
@Mapper
public interface SysAccountRelMapper {
    Long getId();

    List<String> getByNo(@Param("no") String no);

    List<String> getByUid(@Param("uid") String uid);

    List<AccountBean> getAll();

    Boolean insertEntity(SysAccountRel bean);

    List<AccountBean> getAccountByUid(@Param("uid")String uid);

    Boolean updateById(SysAccountRel bean);

    Boolean deleteByAccountId(@Param("accountId") String accountId);

    Page<AccountBean> searchAccount(@Param("param") SearchAccountParam param);

    Integer countAccount(@Param("no") String no, @Param("tcUid") String tcUid);

    List<AccountBean> getByUids(@Param("uids") List<String> uids);
    List<AccountRes> get1stUser(@Param("customer") String customer);

    List<AccountRes> get2ndUser(@Param("customer") String customer);

    SysAccountRel getAccountByNoAndUid(@Param("no") String no, @Param("tcUid") String tcUid);

    Boolean deleteById(@Param("id")Long id);
}
