package com.foxconn.plm.integrate.mail.service.impl;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.entity.exception.BizException;
import com.foxconn.plm.integrate.mail.domain.ItemInfo;
import com.foxconn.plm.integrate.mail.domain.MailItem;
import com.foxconn.plm.integrate.mail.domain.rp.*;
import com.foxconn.plm.integrate.mail.mapper.MailMapper;
import com.foxconn.plm.integrate.mail.service.MailGroupMnageService;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.services.loose.core.SessionService;
import com.teamcenter.services.strong.administration.PreferenceManagementService;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.query.SavedQueryService;
import com.teamcenter.services.strong.query._2006_03.SavedQuery;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service("mailGroupManageServiceImpl")
public class MailGroupManageServiceImpl implements MailGroupMnageService {
    private static Log log = LogFactory.get();

    @Autowired(required = false)
    private MailMapper mailMapper;


    @Override
    public void addMailGroup(MailGroupRp mailGroup) {
        Integer cnt = mailMapper.getMailGroupCnt(mailGroup.getGroupName().toUpperCase(Locale.ENGLISH));
        if (cnt.intValue() > 0) {
            throw new BizException("群组名称重复");
        }
        String creator = mailGroup.getCreator();
        if (creator == null) {
            throw new BizException("创建人不能为空");
        }
        mailGroup.setCreatorName(getUserName(creator));
        mailMapper.addMailGroup(mailGroup);
    }


    @Override
    public void addMailUser(MailUserRp mailUser) {
        List<MailUserInner> mailUsers = mailUser.getList();
        if (mailUsers == null || mailUsers.size() <= 0) {
            return;
        }
        mailMapper.deleteMailUser(mailUser.getGroupId());

        for (MailUserInner mu : mailUsers) {
            try {
                if (mu.getCreator() == null) {
                    throw new BizException("创建人不能为空");
                }
                mu.setCreatorName(mu.getCreator());
                mailMapper.addMailUser(mu);
            }catch(Exception e){
                try {
                    log.error("Add mail user faield !" + mu.getEmpId() + " " + mu.getMail());
                }catch(Exception e0){}
            }
        }
    }


    @Override
    public void updateGroup(MailGroupRp mailGroup) {
        String updateBy = mailGroup.getUpdateBy();
        if (updateBy == null) {
            throw new BizException("修改人不能爲空");
        }
        mailGroup.setUpdateByName(updateBy);
        mailMapper.updateGroup(mailGroup);
    }


    public static ModelObject findObjectByUid(DataManagementService dataManagementService, String uid) {
        ServiceData sd = dataManagementService.loadObjects(new String[]{uid});
        return sd.getPlainObject(0);
    }


    @Override
    public List<ItemInfo> searchItems(String keys) {
        TCSOAServiceFactory tcSOAServiceFactory = null;
        try {
            tcSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
            List<ItemInfo> ls = new ArrayList<>();
            SavedQueryService savedQueryService = tcSOAServiceFactory.getSavedQueryService();
            DataManagementService dataManagementService = tcSOAServiceFactory.getDataManagementService();
            ImanQuery query = null;
            SavedQuery.GetSavedQueriesResponse savedQueries = savedQueryService.getSavedQueries();
            for (int i = 0; i < savedQueries.queries.length; i++) {
                if (savedQueries.queries[i].name.equals("__D9_Find_ItemRev")) {
                    query = savedQueries.queries[i].query;
                    break;
                }
            }
            if (query == null) {
                return ls;
            }
            dataManagementService.refreshObjects(new ModelObject[]{query});
            PreferenceManagementService preferenceManagementService = tcSOAServiceFactory.getPreferenceManagementService();
            preferenceManagementService.refreshPreferences();
            String[] ps = TCUtils.getTCPreferences(preferenceManagementService, "D9_Mailgroup_Type");
            String t = "";
            for (String s : ps) {
                t = t + s + ";";
            }
            if (t.endsWith(";")) {
                t = t.substring(0, t.length() - 1);
            }

            String[] entries = new String[]{"ItemID2", "Type2"};
            String[] values = new String[]{keys, t};
            com.teamcenter.services.strong.query._2007_06.SavedQuery.SavedQueryInput[] savedQueryInput = new com.teamcenter.services.strong.query._2007_06.SavedQuery.SavedQueryInput[1];
            savedQueryInput[0] = new com.teamcenter.services.strong.query._2007_06.SavedQuery.SavedQueryInput();
            savedQueryInput[0].query = query;
            savedQueryInput[0].entries = entries;
            savedQueryInput[0].values = values;
            savedQueryInput[0].maxNumToReturn = 50;
            com.teamcenter.services.strong.query._2007_06.SavedQuery.ExecuteSavedQueriesResponse savedQueryResult = savedQueryService.executeSavedQueries(savedQueryInput);
            com.teamcenter.services.strong.query._2007_06.SavedQuery.SavedQueryResults found = savedQueryResult.arrayOfResults[0];
            if (found.numOfObjects <= 0) {
                return ls;
            }

            ModelObject[] tasks = found.objects;
            for (ModelObject m : tasks) {
                ItemRevision rev = (ItemRevision) findObjectByUid(dataManagementService, m.getUid());
                TCUtils.getProperties(dataManagementService, rev, new String[]{"item_id", "object_name", "object_type"});
                String itemId = rev.get_item_id();
                String objType = rev.getPropertyObject("object_type").getDisplayableValue();
                String itemName = rev.get_object_name();
                ItemInfo itemInfo = new ItemInfo();
                itemInfo.setUuid(m.getUid());
                itemInfo.setItemId(itemId);
                itemInfo.setObjType(objType);
                itemInfo.setItemName(itemName);
                ls.add(itemInfo);
            }
            return ls;
        } catch (Exception e) {
            throw new BizException("查詢失敗");
        } finally {
            try {
                if (tcSOAServiceFactory != null) {
                    tcSOAServiceFactory.logout();
                }
            } catch (Exception e) {
            }
        }
    }


    @Override
    public List<String> searchItemObjType() {
        TCSOAServiceFactory tcSOAServiceFactory = null;
        try {
            tcSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
            PreferenceManagementService preferenceManagementService = tcSOAServiceFactory.getPreferenceManagementService();
            preferenceManagementService.refreshPreferences();
            //PCA Part Revision TECH Revision
            String[] ps = TCUtils.getTCPreferences(preferenceManagementService, "D9_Mailgroup_Type");
            List<String> ls = new ArrayList<>();
            for (String s : ps) {
                ls.add(s);
            }
            return ls;
        } catch (Exception e) {
            throw new BizException("查詢失敗");
        } finally {
            try {
                if (tcSOAServiceFactory != null) {
                    tcSOAServiceFactory.logout();
                }
            } catch (Exception e) {
            }
        }
    }


    @Override
    public void addMailItem(MailItemRp mailItem) {
        List<MailItemInner> mailItems = mailItem.getList();
        List<MailItemInner> addList = new ArrayList<>();
        List<MailItem> deleteList = new ArrayList<>();
        List<MailItem> oldItems = mailMapper.getGroupItems(mailItem.getGroupId());
        for (MailItemInner nm : mailItems) {
            int f = 0;
            for (MailItem om : oldItems) {
                if (nm.getItemId().equalsIgnoreCase(om.getItemId())) {
                    f = 1;
                    break;
                }
            }
            if (f == 0) {
                addList.add(nm);
            }
        }
        for (MailItem om : oldItems) {
            int f = 0;
            for (MailItemInner nm : mailItems) {
                if (nm.getItemId().equalsIgnoreCase(om.getItemId())) {
                    f = 1;
                    break;
                }
            }
            if (f == 0) {
                deleteList.add(om);
            }
        }

        for (MailItemInner mi : addList) {
            mi.setCreatorName(mi.getCreator());
            String itemCategory = mi.getItemCategory();
            if ("objectType".equalsIgnoreCase(itemCategory)) {
                mi.setItemName("");
            }
            mailMapper.addMailItem(mi);
        }
        new UpdateItemThread(addList, mailItem.getGroupId()).start();
        for (MailItem i : deleteList) {
            mailMapper.deleteMailItem(i.getId());
        }
    }


    class UpdateItemThread extends Thread {
        private List<MailItemInner> addList;
        private Long groupId;

        public UpdateItemThread(List<MailItemInner> addList, Long groupId) {
            this.addList = addList;
            this.groupId = groupId;

        }


        @Override
        public void run() {
            TCSOAServiceFactory tcSOAServiceFactory = null;
            try {
                tcSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
                DataManagementService dataManagementService = tcSOAServiceFactory.getDataManagementService();
                SessionService sessionService = tcSOAServiceFactory.getSessionService();
                for (MailItemInner mi : addList) {
                    mi.setCreatorName(mi.getCreator());
                    String itemCategory = mi.getItemCategory();
                    if ("objectType".equalsIgnoreCase(itemCategory)) {
                        mi.setItemName("");
                    }
                    if ("objectType".equalsIgnoreCase(itemCategory)) {
                        continue;
                    }

                    String revId = mi.getUuid();
                    if (revId == null || "".equalsIgnoreCase(revId)) {
                        throw new BizException("uid 爲空");
                    }

                    ServiceData sdDataset = dataManagementService.loadObjects(new String[]{revId});
                    ItemRevision irv = (ItemRevision) sdDataset.getPlainObject(0);
                    TCUtils.getProperty(dataManagementService, irv, "d9_MailGroupID");
                    String oldGroupIds = "";
                    try {
                        oldGroupIds = irv.getPropertyObject("d9_MailGroupID").getStringValue();
                    } catch (Exception e) {
                        continue;
                    }
                    if (oldGroupIds == null) {
                        oldGroupIds = "";
                    }
                    int f = 0;
                    String[] m = oldGroupIds.split(",");
                    for (String str : m) {
                        if (str.equalsIgnoreCase(groupId + "")) {
                            f = 1;
                        }
                    }
                    if (f == 1) {
                        continue;
                    }
                    if (oldGroupIds.endsWith(",")) {
                        oldGroupIds = oldGroupIds.substring(0, oldGroupIds.length() - 1);
                    }
                    oldGroupIds = oldGroupIds + "," + groupId;

                    TCUtils.byPass(sessionService, true);
                    TCUtils.setProperties(dataManagementService, irv, "d9_MailGroupID", oldGroupIds);
                }
            } catch (Exception e) {
                log.error("Update item failed " + e.getMessage(), e);
            } finally {
                try {
                    if (tcSOAServiceFactory != null) {
                        tcSOAServiceFactory.logout();
                    }
                } catch (Exception e) {
                }
            }
        }

    }

    @Override
    public List<ItemInfo> getItemInfos(String revIds) {
        TCSOAServiceFactory tcSOAServiceFactory = null;
        List<ItemInfo> itemInfos = new ArrayList<>();
        try {
            tcSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
            String[] m = revIds.split(",");
            DataManagementService dataManagementService = tcSOAServiceFactory.getDataManagementService();
            for (String revId : m) {
                ServiceData sdDataset = dataManagementService.loadObjects(new String[]{revId});
                DocumentRevision irv = (DocumentRevision) sdDataset.getPlainObject(0);
                try {
                    TCUtils.getProperty(dataManagementService, irv, "item_id");
                    String itemId = irv.getPropertyObject("item_id").getStringValue();
                    ItemInfo itemInfo = new ItemInfo();
                    itemInfo.setUuid(revId);
                    itemInfo.setItemId(itemId);
                    itemInfos.add(itemInfo);
                    dataManagementService.getProperties(new ModelObject[]{irv}, new String[]{"IMAN_specification"});
                    ModelObject[] ws = irv.getPropertyObject("IMAN_specification").getModelObjectArrayValue();
                    for (ModelObject w : ws) {
                        if (!(w instanceof Dataset)) {
                            continue;
                        }
                        Dataset ds = (Dataset) w;
                        TCUtils.getProperty(dataManagementService, ds, "object_name");
                        itemInfo.setItemName(ds.get_object_name());
                        itemInfo.setDataSet(ds.getUid());
                    }
                } catch (Exception e) {
                    System.out.print(e);
                }
            }
        } finally {
            try {
                if (tcSOAServiceFactory != null) {
                    tcSOAServiceFactory.logout();
                }
            } catch (Exception e) {
            }
        }
        return itemInfos;
    }


    private String getUserName(String empId) {
        TCSOAServiceFactory tcSOAServiceFactory = null;
        try {
            tcSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);

            DataManagementService dataManagementService = tcSOAServiceFactory.getDataManagementService();
            SavedQueryService savedQueryService = tcSOAServiceFactory.getSavedQueryService();
            Map<String, Object> queryResults = TCUtils.executeQuery(savedQueryService, "__WEB_find_user",
                    new String[]{"user_id"}, new String[]{(empId)});
            if (queryResults.get("succeeded") == null) {
                throw new Exception("获取用户信息失败");
            }
            ModelObject[] md = (ModelObject[]) queryResults.get("succeeded");
            if (md == null || md.length <= 0) {
                throw new Exception("获取用户信息失败");
            }
            ModelObject iv = md[0];
            ServiceData sd = dataManagementService.loadObjects(new String[]{iv.getUid()});
            User u = (User) sd.getPlainObject(0);
            TCUtils.getProperty(dataManagementService, u, "user_name");
            return u.get_user_name();
        } catch (Exception e) {
            throw new BizException("获取用户信息失败");
        } finally {
            try {
                if (tcSOAServiceFactory != null) {
                    tcSOAServiceFactory.logout();
                }
            } catch (Exception e) {
            }
        }

    }

}
