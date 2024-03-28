package com.foxconn.plm.tcservice.issuemanagement.service;

import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.entity.response.RList;
import com.foxconn.plm.tcservice.issuemanagement.bean.AccountBean;
import com.foxconn.plm.tcservice.issuemanagement.param.*;
import com.foxconn.plm.tcservice.issuemanagement.response.AccountRes;
import com.foxconn.plm.tcservice.issuemanagement.response.SearchAccountRes;
import com.foxconn.plm.tcservice.issuemanagement.response.UserRes;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 業務邏輯接口類
 *
 * @Description
 * @Author MW00442
 * @Date 2023/11/24 17:25
 **/
public interface SecondAccountService {
    List<String> getUserByAccountUid(String accountUid);

    List<String> getUserByAccountNo(String accountNo);

    List<AccountBean> getAll();

    List<AccountBean> getUserAccountByUid(String uid);

    RList<SearchAccountRes> searchAccount(SearchAccountParam param);

    List<UserRes> getAllUser();

    R addAccount(AddAccountParam param);

    R editAccount(EditAccountParam param);

    R delAccount(String id);

    List<AccountBean> getByUids(List<String> uids);

    R contentToProject(ContentToProjectParam param);

    List<AccountRes> getWorkList(String customer);

    R importData(MultipartFile file);

    R updateAccountUid();

    R deleteAccountByFile(MultipartFile file);
}
