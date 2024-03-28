package com.foxconn.plm.tcservice.issuemanagement.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.entity.response.RList;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.tcservice.issuemanagement.bean.AccountBean;
import com.foxconn.plm.tcservice.issuemanagement.bean.TcAccountUserBean;
import com.foxconn.plm.tcservice.issuemanagement.bean.TcUserBean;
import com.foxconn.plm.tcservice.issuemanagement.entity.SysAccountRel;
import com.foxconn.plm.tcservice.issuemanagement.entity.SysSecondAccount;
import com.foxconn.plm.tcservice.issuemanagement.param.*;
import com.foxconn.plm.tcservice.issuemanagement.response.AccountRes;
import com.foxconn.plm.tcservice.issuemanagement.response.SearchAccountRes;
import com.foxconn.plm.tcservice.issuemanagement.response.UserRes;
import com.foxconn.plm.tcservice.issuemanagement.service.SecondAccountService;
import com.foxconn.plm.tcservice.mapper.master.*;
import com.foxconn.plm.utils.tc.DataManagementUtil;
import com.foxconn.plm.utils.tc.FolderUtil;
import com.foxconn.plm.utils.tc.QueryUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.Folder;
import com.teamcenter.soa.client.model.strong.WorkspaceObject;
import com.teamcenter.soa.exceptions.NotLoadedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 接口實現類
 *
 * @Description
 * @Author MW00442
 * @Date 2023/11/24 17:27
 **/
@Service
public class SecondAccountServiceImpl implements SecondAccountService {
    @Resource
    private SysAccountRelMapper accountRelMapper;
    @Resource
    private SysSecondAccountMapper secondAccountMapper;
    @Resource
    private SysAccountCustomerMapper accountCustomerMapper;
    @Resource
    private SysAccountLibMapper accountLibMapper;
    @Resource
    private SysCustomerMapper customerMapper;
    @Resource
    private SysLibFolderMapper libFolderMapper;
    @Resource
    private TCSOAServiceFactory tcsoaServiceFactory;


    @Override
    public List<String> getUserByAccountUid(String accountUid) {
        if(StrUtil.isBlank(accountUid)){
            return Collections.emptyList();
        }
        List<String> list = accountRelMapper.getByUid(accountUid);
        return CollUtil.isNotEmpty(list) ? list : Collections.emptyList();
    }

    @Override
    public List<String> getUserByAccountNo(String accountNo) {
        if(StrUtil.isBlank(accountNo)){
            return Collections.emptyList();
        }
        List<String> list = accountRelMapper.getByNo(accountNo);
        return CollUtil.isNotEmpty(list) ? list : Collections.emptyList();
    }

    @Override
    public List<AccountBean> getAll() {
        List<AccountBean> list = accountRelMapper.getAll();
        return CollUtil.isNotEmpty(list) ? list : Collections.emptyList();
    }

    @Override
    public List<AccountBean> getUserAccountByUid(String uid) {
        if(StrUtil.isBlank(uid)){
            return Collections.emptyList();
        }
        List<AccountBean> list = accountRelMapper.getAccountByUid(uid);
        // 查詢超級管理員賬號
        List<AccountBean> list1 = secondAccountMapper.getAdminAccount();
        if(CollUtil.isNotEmpty(list1)){
            list.addAll(list1);
        }
        return CollUtil.isNotEmpty(list) ? list : Collections.emptyList();
    }

    @Override
    public RList<SearchAccountRes> searchAccount(SearchAccountParam param) {
        PageHelper.startPage(param.getPageNum(),param.getPageSize());
        Page<AccountBean> iPage = accountRelMapper.searchAccount(param);
        if(CollUtil.isNotEmpty(iPage.getResult())){
            List<SearchAccountRes> list = new ArrayList<>();
            for (AccountBean accountBean : iPage.getResult()) {
                SearchAccountRes res = new SearchAccountRes();
                BeanUtil.copyProperties(accountBean, res);
                if (StrUtil.isNotBlank(accountBean.getSecondAccountUid())) {
                    List<TcAccountUserBean> accountUser = secondAccountMapper.getAllTcAccountUser(accountBean.getSecondAccountUid());
                    if(CollUtil.isNotEmpty(accountUser)){
                        res.setAccountDesc(accountUser.get(0).getUserInfo());
                    }
                }
                if(StrUtil.isNotBlank(accountBean.getTcUid())){
                    List<TcUserBean> tcUser = secondAccountMapper.getAllTcUser(accountBean.getTcUid());
                    if(CollUtil.isNotEmpty(tcUser)){
                        res.setTcUserDesc(tcUser.get(0).getUserName() + "("+tcUser.get(0).getUserId()+")");
                    }
                }
                list.add(res);
            }
            return RList.ok(list,iPage.getTotal());
        }
        return RList.ok(Collections.emptyList(),0);
    }

    @Override
    public List<UserRes> getAllUser() {
        List<TcUserBean> userList = secondAccountMapper.getAllTcUser(null);
        if(CollUtil.isEmpty(userList)){
            return Collections.emptyList();
        }
        return userList.parallelStream().map(item ->{
            UserRes res = new UserRes();
            res.setUid(item.getPuid());
            res.setDesc(item.getUserName() + "("+ item.getUserId() +")");
            return res;
        }).sorted(Comparator.comparing(UserRes::getDesc)).collect(Collectors.toList());
    }

    @Override
    public R addAccount(AddAccountParam param) {
        // 查詢賬號是否已經存在
        Integer integer = accountRelMapper.countAccount(param.getNo(), param.getTcUid());
        if(ObjectUtil.isNotNull(integer) && integer > 0){
            return R.error("400","該賬號已經配置相同的一級賬號");
        }
        SysSecondAccount account = new SysSecondAccount();
        BeanUtil.copyProperties(param,account);
        account.setNo(param.getNo().trim().toLowerCase());
        account.setTcUid("");
        account.setId(secondAccountMapper.getId());
        // 查詢tc中的二級賬號
        tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS4);
        Map<String, Object> actualUsers = QueryUtil.executeQuery(tcsoaServiceFactory.getSavedQueryService(), "__D9_Find_Actual_User",
                new String[]{"item_id"}, new String[]{param.getNo()});
        if(ObjectUtil.isNotNull(actualUsers.get("succeeded"))){
            ModelObject[] md = (ModelObject[]) actualUsers.get("succeeded");
            if(md != null && md.length > 0){
                account.setTcUid(md[0].getUid());
            }
        }
        tcsoaServiceFactory.logout();
        account.setDelFlag("0");
        if(StrUtil.isBlank(param.getTcUid())){
            return R.success(secondAccountMapper.insertEntity(account));
        } else {
            SysAccountRel rel = new SysAccountRel();
            rel.setId(accountRelMapper.getId());
            rel.setAccountId(account.getId());
            rel.setUid(param.getTcUid());
            return R.success(secondAccountMapper.insertEntity(account) && accountRelMapper.insertEntity(rel));
        }
    }

    @Override
    public R editAccount(EditAccountParam param) {
        SysSecondAccount account = secondAccountMapper.getbyId(param.getAccountId());
        if(ObjectUtil.isNull(account)){
            return R.error("400","賬號id錯誤");
        }
        if(!account.getNo().equals(param.getNo().trim().toLowerCase())){
            // 需要修改二級賬號
            tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS4);
            Map<String, Object> actualUsers = QueryUtil.executeQuery(tcsoaServiceFactory.getSavedQueryService(), "__D9_Find_Actual_User",
                    new String[]{"item_id"}, new String[]{param.getNo().trim()});
            if(ObjectUtil.isNotNull(actualUsers.get("succeeded"))){
                ModelObject[] md = (ModelObject[]) actualUsers.get("succeeded");
                if(md != null && md.length > 0){
                    account.setTcUid(md[0].getUid());
                }else{
                    account.setTcUid("");
                }
            }else{
                account.setTcUid("");
            }
            tcsoaServiceFactory.logout();
        }
        account.setNo(param.getNo().trim().toLowerCase());
        account.setBu(param.getBu());
        account.setName(param.getName());
        account.setPlatform(param.getPlatform());
        account.setDept(param.getDept());
        secondAccountMapper.updateById(account);
        // 刪除
        accountRelMapper.deleteByAccountId(param.getAccountId());
        if(StrUtil.isBlank(param.getTcUid())){
            return R.success(Boolean.TRUE);
        } else {
            SysAccountRel rel = new SysAccountRel();
            rel.setId(accountRelMapper.getId());
            rel.setAccountId(account.getId());
            rel.setUid(param.getTcUid());
            return R.success(accountRelMapper.insertEntity(rel));
        }
    }

    @Override
    public R delAccount(String id) {
        SysSecondAccount account = secondAccountMapper.getbyId(id);
        if(ObjectUtil.isNull(account)){
            return R.error("400","賬號id錯誤");
        }
        account.setDelFlag("1");
        accountRelMapper.deleteByAccountId(id);
        return R.success(secondAccountMapper.updateById(account));
    }

    @Override
    public List<AccountBean> getByUids(List<String> uids) {
        if(CollUtil.isEmpty(uids)){
            return Collections.emptyList();
        }
        List<AccountBean> list = accountRelMapper.getByUids(uids);
        return CollUtil.isNotEmpty(list) ? list : Collections.emptyList();
    }

    @Override
    public List<AccountRes> getWorkList(String customer) {
        List<AccountRes>  firstUsers= accountRelMapper.get1stUser(customer);
        for(AccountRes a:firstUsers){
            a.setUid(a.getPuid());
            a.setDisabled(true);
        }
        List<AccountRes>  secondUsers= accountRelMapper.get2ndUser(customer);
        for(AccountRes a:secondUsers){
            a.setUid(a.getPuid());
            a.setDisabled(false);
        }
        Map<String, List<AccountRes>> map = new HashMap<String, List<AccountRes>>();
        Map<String, AccountRes> map2 = new HashMap<String, AccountRes>();
        for (AccountRes suser : secondUsers) {
            String keyUid = suser.getUid();
            map2.put(keyUid,suser);
            for(AccountRes fuser:firstUsers){
                if(fuser.getUid().equalsIgnoreCase(suser.getUser_uid())){
                    List<AccountRes> oList =map.get(keyUid);
                    if(oList==null){
                        oList=new ArrayList<>();
                        map.put(keyUid,oList);
                    }
                    oList.add(fuser);
                }
            }
        }
        List<AccountRes> list = new ArrayList<AccountRes>();
        for(String key : map.keySet()) {
            AccountRes accountRes=map2.get(key);
            List<AccountRes>  parents=map.get(key);
            accountRes.setParent(parents);
            list.add(accountRes);
        }
        return CollUtil.isNotEmpty(list) ? list : Collections.emptyList();
    }

    @Override
    public R contentToProject(ContentToProjectParam param) {
        try {
            tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS1);
            ModelObject object = DataManagementUtil.findObjectByUid(tcsoaServiceFactory.getDataManagementService(), param.getItemUid());
            // 查詢專案文件夾
            Map<String, Object> resultMap = QueryUtil.executeQuery(tcsoaServiceFactory.getSavedQueryService(), "__D9_Find_Project_Folder", new String[]{"d9_SPAS_ID"},
                    new String[]{param.getProjectId()});
            if (ObjectUtil.isNotNull(resultMap.get("succeeded"))) {
                ModelObject[] md = (ModelObject[]) resultMap.get("succeeded");
                if (md != null && md.length > 0) {
                    // 獲取專案文件夾
                    Folder folder = (Folder) md[0];
                    Folder folder1 = DataManagementUtil.getFolder(tcsoaServiceFactory.getDataManagementService(), folder, "D9_WorkAreaFolder", "產品設計協同工作區");
                    if(ObjectUtil.isNotNull(folder1)){
                        Folder folder2 = DataManagementUtil.getFolder(tcsoaServiceFactory.getDataManagementService(), folder1, "D9_WorkAreaFolder", "Issue協同工作區");
                        if(ObjectUtil.isNull(folder2)){
                            folder2 = FolderUtil.createFolder(tcsoaServiceFactory.getDataManagementService(),folder1,"D9_WorkAreaFolder", "Issue協同工作區",null);
                        }
                        TCUtils.addContents(tcsoaServiceFactory.getDataManagementService(), folder2, object);
                        return R.success(true);
                    }
                }
            }
            return R.success(false);
        } catch (Exception e) {
            return R.error("5001","查詢數據錯誤");
        } finally {
            tcsoaServiceFactory.logout();
        }
    }

    @Override
    public R importData(MultipartFile file) {
        ExcelReader reader = null;
        try {
            reader = ExcelUtil.getReader(file.getInputStream());
        } catch (IOException e) {
            return R.error(HttpResultEnum.NO_RESULT.getCode(),"讀取excel文件數據錯誤");
        }
        List<List<Object>> list = reader.read();
        if (list.size() <= 1) {
            return R.error(HttpResultEnum.NO_RESULT.getCode(),"No data found to be imported");
        }
        List<String> noNumList= new ArrayList<>();
        List<String> tcList = new ArrayList<>();
        List<String> inList = new ArrayList<>();
        // 查詢所有一級用戶信息
        List<TcUserBean> allTcUser = secondAccountMapper.getAllTcUser(null);
        Map<String, TcUserBean> tcUserMap = allTcUser.parallelStream().collect(Collectors.toMap(item -> item.getUserId().toLowerCase(), item -> item));
        // 查詢所有二級賬號信息
        List<TcAccountUserBean> allTcAccountUser = secondAccountMapper.getAllTcAccountUser(null);
        Map<String, TcAccountUserBean> tcAccountMap = allTcAccountUser.parallelStream().collect(Collectors.toMap(item -> item.getItemId().toLowerCase(), item -> item));
        Map<String,String> enNameMap = new HashMap<>();
        // 解析excel中的數據
        for (int i = 1; i < list.size(); i++) {
            List<Object> objects = list.get(i);
            // 獲取數據信息
            if(ObjectUtil.isNull(objects.get(1)) || ObjectUtil.isNull(objects.get(2)) ||
                    StrUtil.isBlank(String.valueOf(objects.get(1))) || StrUtil.isBlank(String.valueOf(objects.get(2)))){
                noNumList.add(String.valueOf(i+1));
                continue;
            }
            String userId = objects.get(1).toString().toLowerCase();
            if(ObjectUtil.isNull(tcUserMap.get(userId))){
                tcList.add(objects.get(1).toString());
                continue;
            }
            String secondUserId = objects.get(2).toString().toLowerCase();
            Integer count = accountRelMapper.countAccount(secondUserId,tcUserMap.get(userId).getPuid());
            if(ObjectUtil.isNotNull(count) && count > 0){
                inList.add(objects.get(1).toString() + "+" + objects.get(2).toString());
                continue;
            }
            // 新增數據
            SysSecondAccount account = new SysSecondAccount();
            account.setId(secondAccountMapper.getId());
            account.setNo(secondUserId);
            account.setName(ObjectUtil.isNotNull(objects.get(3)) ? objects.get(3).toString() : "");
            account.setBu(ObjectUtil.isNotNull(objects.get(5)) ? objects.get(5).toString() : "");
            account.setPlatform(ObjectUtil.isNotNull(objects.get(6)) ? objects.get(6).toString() : "");
            account.setDept(ObjectUtil.isNotNull(objects.get(7)) ? objects.get(7).toString() : "");
            account.setDelFlag("0");
            String enName = ObjectUtil.isNotNull(objects.get(4)) ? objects.get(4).toString() : "";
            if(ObjectUtil.isNotNull(tcAccountMap.get(secondUserId))){
                account.setTcUid(tcAccountMap.get(secondUserId).getPuid());
                if(StrUtil.isBlank(tcAccountMap.get(secondUserId).getEnName()) && StrUtil.isNotBlank(enName)){
                    enNameMap.put(tcAccountMap.get(secondUserId).getPuid(),enName);
                }
            }else{
                account.setTcUid("");
            }
            secondAccountMapper.insertEntity(account);

            SysAccountRel rel = new SysAccountRel();
            rel.setId(accountRelMapper.getId());
            rel.setAccountId(account.getId());
            rel.setUid(tcUserMap.get(userId).getPuid());
            accountRelMapper.insertEntity(rel);

        }
        StringBuilder sb = new StringBuilder();
        if(CollUtil.isNotEmpty(noNumList)){
            sb.append("第【").append(CollUtil.join(noNumList,",")).append("】行沒有填寫一級賬號或二級賬號；");
        }
        if(CollUtil.isNotEmpty(tcList)){
            sb.append("一級賬號").append(CollUtil.join(tcList,",")).append("不存在，請先創建一級賬號再導入白名單；");
        }
        if(CollUtil.isNotEmpty(inList)){
            sb.append(CollUtil.join(inList,",")).append("已存在，請勿重複導入；");
        }
        if(CollUtil.isNotEmpty(enNameMap)){
            // 更新actualUser的英文名
            tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS4);
            for (String actualUid : enNameMap.keySet()) {
                ModelObject object = DataManagementUtil.findObjectByUid(tcsoaServiceFactory.getDataManagementService(), actualUid);
                DataManagementUtil.setProperties(tcsoaServiceFactory.getDataManagementService(),object,"d9_EnglishName",enNameMap.get(actualUid));
            }
            tcsoaServiceFactory.logout();
        }
        return R.success(sb.toString(),Boolean.TRUE);
    }

    @Override
    public R updateAccountUid() {
        tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS2);
        try {

            List<SysSecondAccount> list = secondAccountMapper.getAll();
            List<SysSecondAccount> collect = list.parallelStream().filter(item -> StrUtil.isBlank(item.getTcUid())).collect(Collectors.toList());
            if(CollUtil.isNotEmpty(collect)){
                List<String> noList = collect.parallelStream().map(SysSecondAccount::getNo).collect(Collectors.toList());
                String nos = CollUtil.join(noList, ";");
                System.out.println(nos);
                // 更新所有賬號的uid
                Map<String, String> secondMap = new HashMap<>();
                // 查詢二級賬號
                Map<String, Object> actualUsers = TCUtils.executeQuery(tcsoaServiceFactory.getSavedQueryService(), "__D9_Find_Actual_User", new String[]{"item_id"}, new String[]{nos});
                if (ObjectUtil.isNotNull(actualUsers.get("succeeded"))) {
                    ModelObject[] md = (ModelObject[]) actualUsers.get("succeeded");
                    for (int i = 0; i < md.length; i++) {
                        String uid = md[i].getUid();
                        TCUtils.getProperty(tcsoaServiceFactory.getDataManagementService(), md[i], "item_id");
                        TCUtils.refreshObject(tcsoaServiceFactory.getDataManagementService(), md[i]);
                        String itemId = md[i].getPropertyObject("item_id").getStringValue();
                        secondMap.put(itemId.toLowerCase(), uid);
                    }
                }
                for (SysSecondAccount account : collect) {
                    String uid = secondMap.get(account.getNo().toLowerCase());
                    if (StrUtil.isNotBlank(uid)) {
                        account.setTcUid(uid);
                        account.setDelFlag("0");
                        secondAccountMapper.updateById(account);
                    }
                }
            }
            // 登出TC
            tcsoaServiceFactory.logout();
            return R.success(Boolean.TRUE);
        }catch (Exception e){
            return R.success(Boolean.FALSE);
        }
    }

    @Override
    public R deleteAccountByFile(MultipartFile file) {
        ExcelReader reader = null;
        try {
            reader = ExcelUtil.getReader(file.getInputStream());
        } catch (IOException e) {
            return R.error(HttpResultEnum.NO_RESULT.getCode(),"讀取excel文件數據錯誤");
        }
        List<List<Object>> list = reader.read();
        if (list.size() <= 1) {
            return R.error(HttpResultEnum.NO_RESULT.getCode(),"No data found to be imported");
        }
        // 查詢所有一級用戶信息
        List<TcUserBean> allTcUser = secondAccountMapper.getAllTcUser(null);
        Map<String, TcUserBean> tcUserMap = allTcUser.parallelStream().collect(Collectors.toMap(item -> item.getUserId().toLowerCase(), item -> item));
        List<String> noNumList = new ArrayList<>();
        List<String> tcList = new ArrayList<>();
        List<String> errorList = new ArrayList<>();
        for (int i = 1; i < list.size(); i++) {
            List<Object> objects = list.get(i);
            // 獲取數據信息
            if(ObjectUtil.isNull(objects.get(1)) || ObjectUtil.isNull(objects.get(2)) ||
                    StrUtil.isBlank(String.valueOf(objects.get(1))) || StrUtil.isBlank(String.valueOf(objects.get(2)))){
                noNumList.add(String.valueOf(i+1));
                continue;
            }
            String userId = objects.get(1).toString().toLowerCase();
            if(ObjectUtil.isNull(tcUserMap.get(userId))){
                tcList.add(objects.get(1).toString());
                continue;
            }
            // 查詢數據是否存在
            String secondUserId = objects.get(2).toString().toLowerCase();
            SysAccountRel accountRel = accountRelMapper.getAccountByNoAndUid(secondUserId, tcUserMap.get(userId).getPuid());
            if(ObjectUtil.isNotNull(accountRel)){
                // 刪除對應的數據
                secondAccountMapper.deleteById(accountRel.getAccountId());
                accountRelMapper.deleteById(accountRel.getId());
            }else{
                errorList.add(objects.get(1).toString() + "+" + objects.get(2).toString());
            }
        }
        StringBuilder sb = new StringBuilder();
        if(CollUtil.isNotEmpty(noNumList)){
            sb.append("第【").append(CollUtil.join(noNumList,",")).append("】行沒有填寫一級賬號或二級賬號；");
        }
        if(CollUtil.isNotEmpty(tcList)){
            sb.append("一級賬號").append(CollUtil.join(tcList,",")).append("不存在，請確認數據是否正確；");
        }
        if(CollUtil.isNotEmpty(errorList)){
            sb.append(CollUtil.join(errorList,",")).append("不存在；");
        }
        return R.success(sb.toString(),Boolean.TRUE);
    }
}
