package com.foxconn.plm.tcservice.mailtrack.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrSplitter;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSONObject;
import com.foxconn.plm.entity.pojo.ActualUserPojo;
import com.foxconn.plm.entity.response.SPASUser;
import com.foxconn.plm.feign.service.TcIntegrateClient;
import com.foxconn.plm.tcservice.mailtrack.domain.PoInfo;
import com.foxconn.plm.tcservice.mailtrack.domain.TrackResponse;
import com.foxconn.plm.tcservice.mailtrack.domain.UserPojo;
import com.foxconn.plm.utils.tc.ActualUserUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.query.SavedQueryService;
import com.teamcenter.services.strong.query._2006_03.SavedQuery;
import com.teamcenter.soa.client.FileManagementUtility;
import com.teamcenter.soa.client.GetFileResponse;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.Property;
import com.teamcenter.soa.client.model.strong.*;

import java.io.*;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;

public class MailTrackUtils {

    private static Log log = LogFactory.get();

    public static ModelObject[] taskQuery(SavedQueryService savedQueryService, String taskStateValue, String objectType, String templateName) throws Exception {
        ImanQuery query = null;
        SavedQuery.GetSavedQueriesResponse savedQueries = savedQueryService.getSavedQueries();
        for (int i = 0; i < savedQueries.queries.length; i++) {
            if (savedQueries.queries[i].name.equals("__D9_Find_Task_with_State")) {
                query = savedQueries.queries[i].query;
                break;
            }
        }
        if (query == null) {
            return null;
        }

        String[] entries = new String[]{"taskStateValue", "objectType", "templateName"};
        String[] values = new String[]{taskStateValue, objectType, templateName};
        return getModelObjects(savedQueryService, query, entries, values);
    }

    private static ModelObject[] getModelObjects(SavedQueryService savedQueryService, ImanQuery query, String[] entries, String[] values) {
        return null;
    }


    public static ModelObject[] subTaskQuery(SavedQueryService savedQueryService, String taskStateValue, String objectType, String templateName, String pcbaId, String pcbaRev, String wfStatevalue) throws Exception {
        ImanQuery query = null;
        SavedQuery.GetSavedQueriesResponse savedQueries = savedQueryService.getSavedQueries();
        for (int i = 0; i < savedQueries.queries.length; i++) {
            if (savedQueries.queries[i].name.equals("__D9_Find_Task_with_SubState")) {
                query = savedQueries.queries[i].query;
                break;
            }
        }
        if (query == null) {
            return null;
        }

        String[] entries = new String[]{"taskStateValue", "objectType", "templateName", "pcbaId", "pcbaRev", "wfStatevalue"};
        String[] values = new String[]{taskStateValue, objectType, templateName, pcbaId, pcbaRev, wfStatevalue};
        return getModelObjects(savedQueryService, query, entries, values);
    }

//3-EE/PI製作EBOM  节点签核人
    public static List<UserPojo> getTask3Tracker(DataManagementService dataManagementService, EPMTask task, FileManagementUtility fileManagementUtility, TcIntegrateClient tcIntegrate) throws Exception {
        List<UserPojo> tarckers = new ArrayList<>();
        dataManagementService.refreshObjects(new ModelObject[]{task});

        TCUtils.getProperty(dataManagementService, task, "root_target_attachments");
        ModelObject[] targerts = task.get_root_target_attachments();

        ItemRevision itemRev = null;
        for (ModelObject t : targerts) {
            if (!(t instanceof ItemRevision)) {
                continue;
            }
            itemRev = (ItemRevision) t;
            TCUtils.getProperty(dataManagementService, itemRev, "item_id");
            break;
        }
        if (itemRev == null) {
            updateTrackerMail(tcIntegrate, tarckers);
            return tarckers;
        }

        TCUtils.getProperty(dataManagementService, itemRev, "IMAN_external_object_link");
        ModelObject[] modelObject = itemRev.getPropertyObject("IMAN_external_object_link").getModelObjectArrayValue();
        if (modelObject == null || modelObject.length <= 0) {
            updateTrackerMail(tcIntegrate, tarckers);
            return tarckers;
        }
        List<ActualUserPojo> allActualUser = ActualUserUtil.getAllActualUser(dataManagementService, modelObject[0]);
        for (ActualUserPojo actualUserPojo : allActualUser) {
            if(actualUserPojo.getProcessNode().startsWith("3-")){
                List<String> userIds = StrSplitter.split(actualUserPojo.getActualUserId(), ",", true, true);
                List<String> mails = StrSplitter.split(actualUserPojo.getActualUserMail(), ",", true, true);
                if(userIds.size() > 0 && userIds.size() == mails.size()){
                    for (int i = 0; i < userIds.size(); i++) {
                        UserPojo up = new UserPojo();
                        up.setUserId(userIds.get(i));
                        up.setMail(mails.get(i));
                        tarckers.add(up);
                    }
                    updateTrackerMail(tcIntegrate, tarckers);
                }
            }
        }
        /*Dataset dataset = (Dataset) modelObject[0];
        dataManagementService.refreshObjects(new ModelObject[]{dataset});
        dataManagementService.getProperties(new ModelObject[]{dataset}, new String[]{"ref_list"});
        ModelObject[] dsfiles = dataset.get_ref_list();
        for (int i = 0; i < dsfiles.length; i++) {

            try {
                if (!(dsfiles[i] instanceof ImanFile)) {
                    continue;
                }

                ImanFile dsFile = (ImanFile) dsfiles[i];
                dataManagementService.refreshObjects(new ModelObject[]{dsFile});
                dataManagementService.getProperties(new ModelObject[]{dsFile},
                        new String[]{"original_file_name"});
                String fileName = dsFile.get_original_file_name();
                log.info("【INFO】 fileName: " + fileName);
                // 下载数据集
                GetFileResponse responseFiles = fileManagementUtility.getFiles(new ModelObject[]{dsFile});
                File[] fileinfovec = responseFiles.getFiles();
                File file = fileinfovec[0];
                List<String> lines=FileUtil.readLines(file,"BIG5");

                for (String line :lines) {
                    System.out.println(line);
                    log.info("【INFO】 line: " + line);
                    if (line.startsWith("3-")) {
                        try {
                            String t = line.substring(line.lastIndexOf(";") + 1);
                            t = t.substring(0, t.indexOf("##"));
                            String[] ut = t.split(",");
                            String mailTmp = line.substring(line.lastIndexOf("%%") + 2);
                            String[] mails = mailTmp.split(",");
                            for (int k = 0; k < ut.length; k++) {
                                UserPojo up = new UserPojo();
                                up.setUserId(ut[k].trim());
                                up.setMail(mails[k].trim());
                                tarckers.add(up);
                            }
                        } catch (Exception e) {
                        }
                    }
                }
                updateTrackerMail(tcIntegrate, tarckers);
            } catch(Exception e){}
        }*/
        return tarckers;

    }

    //指定任务的签核人
    public static List<UserPojo> getTaskTracker(DataManagementService dataManagementService, EPMTask task, FileManagementUtility fileManagementUtility, TcIntegrateClient tcIntegrate) throws Exception {
        List<UserPojo> tarckers = new ArrayList<>();
        dataManagementService.refreshObjects(new ModelObject[]{task});
        if (task instanceof EPMReviewTask) {
            EPMReviewTask reviewTask = (EPMReviewTask) task;
            TCUtils.getProperty(dataManagementService, reviewTask, "valid_signoffs");
            ModelObject[] uss = reviewTask.getPropertyObject("valid_signoffs").getModelObjectArrayValue();
            for (ModelObject u : uss) {
                Signoff sf = (Signoff) u;
                TCUtils.getProperty(dataManagementService, sf, "fnd0Performer");
                User us = sf.get_fnd0Performer();
                TCUtils.getProperty(dataManagementService, sf, "fnd0Status");
                String st = sf.get_fnd0Status();
                System.out.println(st);
                if ("No Decision".equalsIgnoreCase(st)) {
                    TCUtils.getProperty(dataManagementService, us, "user_name");
                    TCUtils.getProperty(dataManagementService, us, "user_id");
                    UserPojo up = new UserPojo();
                    up.setUserId(us.get_user_id());
                    up.setUserName(us.get_user_name());
                    tarckers.add(up);
                }
            }
        }

        if (task instanceof EPMConditionTask || task instanceof EPMAcknowledgeTask) {
            EPMConditionTask conTask = (EPMConditionTask) task;
            TCUtils.getProperty(dataManagementService, conTask, "fnd0Performer");
            User us = conTask.get_fnd0Performer();
            TCUtils.getProperty(dataManagementService, us, "user_name");
            TCUtils.getProperty(dataManagementService, us, "user_id");
            UserPojo up = new UserPojo();
            up.setUserId(us.get_user_id());
            up.setUserName(us.get_user_name());
            tarckers.add(up);
        }

        TCUtils.getProperty(dataManagementService, task, "object_string");
        String nameStr = task.get_object_string();

        TCUtils.getProperty(dataManagementService, task, "root_target_attachments");
        ModelObject[] targerts = task.get_root_target_attachments();

        ItemRevision itemRev = null;
        for (ModelObject t : targerts) {
            if (!(t instanceof ItemRevision)) {
                continue;
            }
            itemRev = (ItemRevision) t;
            TCUtils.getProperty(dataManagementService, itemRev, "item_id");
            break;
        }
        if (itemRev == null) {
            updateTrackerMail(tcIntegrate, tarckers);
            return tarckers;
        }

        TCUtils.getProperty(dataManagementService, itemRev, "IMAN_external_object_link");
        ModelObject[] modelObject = itemRev.getPropertyObject("IMAN_external_object_link").getModelObjectArrayValue();
        if (modelObject == null || modelObject.length <= 0) {
            updateTrackerMail(tcIntegrate, tarckers);
            return tarckers;
        }
        Dataset dataset = (Dataset) modelObject[0];
        dataManagementService.refreshObjects(new ModelObject[]{dataset});
        dataManagementService.getProperties(new ModelObject[]{dataset}, new String[]{"ref_list"});
        ModelObject[] dsfiles = dataset.get_ref_list();
        for (int i = 0; i < dsfiles.length; i++) {

            try {
                if (!(dsfiles[i] instanceof ImanFile)) {
                    continue;
                }

                ImanFile dsFile = (ImanFile) dsfiles[i];
                dataManagementService.refreshObjects(new ModelObject[]{dsFile});
                dataManagementService.getProperties(new ModelObject[]{dsFile},
                        new String[]{"original_file_name"});
                String fileName = dsFile.get_original_file_name();
                log.info("【INFO】 fileName: " + fileName);
                // 下载数据集
                GetFileResponse responseFiles = fileManagementUtility.getFiles(new ModelObject[]{dsFile});
                File[] fileinfovec = responseFiles.getFiles();
                File file = fileinfovec[0];
                List<String> lines=FileUtil.readLines(file,"BIG5");
                String nameTmp = nameStr.substring(0, 2);
                for (String line:lines) {
                    System.out.println(line);
                    log.info("【INFO】 line: " + line);
                    if (line.startsWith(nameTmp)) {
                        try {
                            String t = line.substring(line.lastIndexOf(";") + 1);
                            t = t.substring(0, t.indexOf("##"));
                            String[] ut = t.split(",");
                            String mailTmp = line.substring(line.lastIndexOf("%%") + 2);
                            String[] mails = mailTmp.split(",");
                            for (int k = 0; k < ut.length; k++) {
                                for (UserPojo u : tarckers) {
                                    if (u.getUserId().equalsIgnoreCase(ut[k].trim())) {
                                        u.setMail(mails[k].trim());
                                    }
                                }
                            }
                        } catch (Exception e) {
                        }
                    }
                }
                updateTrackerMail(tcIntegrate, tarckers);
            } catch(Exception e){

            }
        }
        return tarckers;

    }


    public static List<TrackResponse> getTrackResponse(SavedQueryService savedQueryService, DataManagementService dataManagementService, EPMTask task41, FileManagementUtility fileManagementUtility, TcIntegrateClient tcIntegrate) throws Exception {
        List<TrackResponse> trackResponses = new ArrayList<>();
        boolean needTrack = false;
        long realDelayHours = 0;
        long delayHours = 0;
        long realDelayHours2 = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("HH");
        int h = Integer.parseInt(sdf.format(new Date()));
        TCUtils.getProperty(dataManagementService, task41, "object_string");
        String task41NameStr = task41.get_object_string();
        log.info("coworktask name=======" + task41NameStr);
        TCUtils.getProperty(dataManagementService, task41, "root_target_attachments");
        ModelObject[] targerts = task41.get_root_target_attachments();

        TCUtils.getProperty(dataManagementService, task41, "owning_user");
        User ownerUs = (User) (task41.getPropertyObject("owning_user").getModelObjectValue());
        TCUtils.getProperty(dataManagementService, ownerUs, "user_name");
        String owner = ownerUs.get_user_name();
        log.info("coworktask owner=======" + owner);
        TCUtils.getProperty(dataManagementService, task41, "fnd0StartDate");
        Date dat = task41.getPropertyObject("fnd0StartDate").getCalendarValue().getTime();
        realDelayHours = (((new Date().getTime() - dat.getTime()) / 1000) / 60) / 60;

        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd");
        String ddd = sdf2.format(dat);
        ddd = ddd + " 9:00:00";
        SimpleDateFormat sdf3 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dat = sdf3.parse(ddd);

        delayHours = (((new Date().getTime() - dat.getTime()) / 1000) / 60) / 60;
        ItemRevision itemRev = null;
        Date dueDate = null;
        String urgcncy = "";
        String itemId = "";
        String actualUser = "";
        String itemRevId = "";
        for (ModelObject t : targerts) {
            if (!(t instanceof ItemRevision)) {
                continue;
            }
            ItemRevision rev = (ItemRevision) t;
            String objType = rev.get_object_type();
            if (!("D9_BOMRequestRevision".equalsIgnoreCase(objType))) {
                continue;
            }
            itemRev = rev;
            TCUtils.getProperty(dataManagementService, itemRev, "d9_DueDate");
            TCUtils.getProperty(dataManagementService, itemRev, "d9_Urgency");
            TCUtils.getProperty(dataManagementService, itemRev, "item_id");
            TCUtils.getProperty(dataManagementService, itemRev, "d9_ActualUserID");
            actualUser = itemRev.getPropertyObject("d9_ActualUserID").getStringValue();
            urgcncy = itemRev.getPropertyObject("d9_Urgency").getStringValue();
            dueDate = itemRev.getPropertyObject("d9_DueDate").getCalendarValue().getTime();
            itemId = itemRev.get_item_id();
            TCUtils.getProperty(dataManagementService, itemRev, "item_revision_id");
            itemRevId = itemRev.get_item_revision_id();
        }
        assert dueDate != null : "dueData is null";
        realDelayHours2 = (((new Date().getTime() - dueDate.getTime()) / 1000) / 60) / 60;
        if ("特急件".equalsIgnoreCase(urgcncy)) {
            if ((h == 10 || h == 15) && realDelayHours > 4) {
                needTrack = true;
            }
        } else if ("急件".equalsIgnoreCase(urgcncy)) {
            if (delayHours % 24 == 0) {
                needTrack = true;
            }

        } else {
            if (delayHours % 120 == 0) {
                needTrack = true;
            }
        }
        String dueDateStr = sdf3.format(dueDate);
        TrackResponse trackResponse = new TrackResponse();
        trackResponse.setUid(task41.getUid());
        trackResponse.setWorkflowName("FXN41");
        trackResponse.setStateName(task41NameStr);
        trackResponse.setItemId(itemId);
        trackResponse.setItemRev(itemRevId);
        PoInfo poInfo = new PoInfo();
        poInfo.setUid(task41.getUid());
        poInfo.setNeedTrack(needTrack);
        poInfo.setRealDelayHours(realDelayHours2);
        poInfo.setUrgcncy(urgcncy);
        poInfo.setDueDate(dueDateStr);
        poInfo.setOwner(owner);
        poInfo.setItemId(itemId);
        poInfo.setItemRev(itemRevId);
        poInfo.setStateName(task41NameStr);
        JSONObject pmo = new JSONObject();
        String pmUserId = actualUser.substring(actualUser.indexOf("(") + 1, actualUser.lastIndexOf(")"));
        String pmUserName = actualUser.substring(0, actualUser.indexOf("(") );
        pmo.put("empId", pmUserId);
        poInfo.setPmName(pmUserName);
        List<SPASUser> spasUser = tcIntegrate.getTeamRosterByEmpId(pmo.toJSONString());
        String pmMail = "";
        if (spasUser != null && spasUser.size() > 0) {
            pmMail = spasUser.get(0).getNotes();
            poInfo.setPmMail(pmMail);
        }
        trackResponse.setPoInfo(poInfo);

        if (task41NameStr.startsWith("1-")) {
            List<UserPojo> trackers = getTaskTracker(dataManagementService, task41, fileManagementUtility, tcIntegrate);
            trackResponse.setTrackers(trackers);
            trackResponses.add(trackResponse);
            return trackResponses;
        }

        if (task41NameStr.startsWith("0-") || task41NameStr.startsWith("2-")) {
            List<UserPojo> trackers = new ArrayList<>();
            UserPojo pmUser = new UserPojo();
            pmUser.setUserId(pmUserId);
            pmUser.setUserName(pmUserName);
            pmUser.setMail(pmMail);
            trackers.add(pmUser);
            trackResponse.setTrackers(trackers);
            trackResponses.add(trackResponse);
            return trackResponses;
        }

        if ( task41NameStr.startsWith("4-")) {
            TCUtils.getProperty(dataManagementService, itemRev, "D9_BOMReq_PCBA_REL");
            assert itemRev != null:"itemRev is null";
            Property d9_bomReq_pcba_rel = itemRev.getPropertyObject("D9_BOMReq_PCBA_REL");
            ModelObject[] itemRevs = d9_bomReq_pcba_rel.getModelObjectArrayValue();
            if (itemRevs == null || itemRevs.length <= 0) {
                trackResponses.add(trackResponse);
                return trackResponses;
            }


            List<ItemRevision>   hasNextRevs=new ArrayList<>();
            List<ItemRevision>   noNextRevs=new ArrayList<>();
            for (ModelObject m : itemRevs) {
                if (!(m instanceof ItemRevision)) {
                    continue;
                }
                ItemRevision pcbaRev = (ItemRevision) m;
                TCUtils.getProperty(dataManagementService, pcbaRev, "object_type");
                String objType = pcbaRev.get_object_type();
                if (!("D9_PCA_PartRevision".equalsIgnoreCase(objType))) {
                    continue;
                }
                TCUtils.getProperty(dataManagementService, pcbaRev, "item_id");
                TCUtils.getProperty(dataManagementService, pcbaRev, "item_revision_id");
                String pcbaRevisionId = pcbaRev.get_item_revision_id();
                TCUtils.getProperty(dataManagementService, pcbaRev, "items_tag");
                ItemRevision nextPcbaRev = getItemNextRevision(dataManagementService, pcbaRev.get_items_tag(), pcbaRevisionId);
                TCUtils.getProperty(dataManagementService, nextPcbaRev, "item_revision_id");
                String nextPcbaRevisionId = nextPcbaRev.get_item_revision_id();
                if (nextPcbaRevisionId.equalsIgnoreCase(pcbaRevisionId)) {
                    noNextRevs.add(pcbaRev);
                }else{
                    hasNextRevs.add(nextPcbaRev);
                }
            }

            if(noNextRevs.size()>0){
                    //跟催3节点的人
                List<UserPojo> trackers = getTask3Tracker(dataManagementService, task41, fileManagementUtility, tcIntegrate);
                TrackResponse r = new TrackResponse();
                r.setWorkflowName("FXN41");
                r.setUid(task41.getUid());
                r.setItemId(itemId);
                r.setStateName(task41NameStr);
                r.setTrackers(trackers);
                r.setItemRev(itemRevId);
                r.setPoInfo(poInfo);
                trackResponses.add(r);
            }
            boolean allComplete=true;
            for(ItemRevision nextPcbaRev:hasNextRevs) {
               TCUtils.getProperty(dataManagementService, nextPcbaRev, "item_revision_id");
               TCUtils.getProperty(dataManagementService, nextPcbaRev, "item_id");
               String pcbaId = nextPcbaRev.get_item_id();
               String nextPcbaRevisionId = nextPcbaRev.get_item_revision_id();
               boolean hasComplete=getSubTask4Trackers(nextPcbaRev, dataManagementService, savedQueryService, pcbaId, nextPcbaRevisionId, task41, fileManagementUtility, tcIntegrate, trackResponses, task41NameStr, poInfo);
               if(!hasComplete){
                   allComplete=false;
               }
            }

           if( allComplete  &&noNextRevs.size()<=0){
               List<UserPojo> trackers = getTaskTracker(dataManagementService, task41, fileManagementUtility, tcIntegrate);
               TrackResponse r = new TrackResponse();
               r.setWorkflowName("FXN41");
               r.setUid(task41.getUid());
               r.setItemId(itemId);
               r.setStateName(task41NameStr);
               r.setTrackers(trackers);
               r.setItemRev(itemRevId);
               r.setPoInfo(poInfo);
               trackResponses.add(r);
           }
        }


        if ( task41NameStr.startsWith("3-")) {
            TCUtils.getProperty(dataManagementService, itemRev, "D9_BOMReq_PCBA_REL");
            assert itemRev != null:"itemRev is null";
            Property d9_bomReq_pcba_rel = itemRev.getPropertyObject("D9_BOMReq_PCBA_REL");
            ModelObject[] itemRevs = d9_bomReq_pcba_rel.getModelObjectArrayValue();
            if (itemRevs == null || itemRevs.length <= 0) {
                trackResponses.add(trackResponse);
                return trackResponses;
            }
            List<ItemRevision>   hasNextRevs=new ArrayList<>();
            List<ItemRevision>   noNextRevs=new ArrayList<>();
            for (ModelObject m : itemRevs) {
                if (!(m instanceof ItemRevision)) {
                    continue;
                }
                ItemRevision pcbaRev = (ItemRevision) m;
                TCUtils.getProperty(dataManagementService, pcbaRev, "object_type");
                String objType = pcbaRev.get_object_type();
                if (!("D9_PCA_PartRevision".equalsIgnoreCase(objType))) {
                    continue;
                }
                TCUtils.getProperty(dataManagementService, pcbaRev, "item_id");
                TCUtils.getProperty(dataManagementService, pcbaRev, "item_revision_id");
                String pcbaRevisionId = pcbaRev.get_item_revision_id();
                TCUtils.getProperty(dataManagementService, pcbaRev, "items_tag");
                ItemRevision nextPcbaRev = getItemNextRevision(dataManagementService, pcbaRev.get_items_tag(), pcbaRevisionId);
                TCUtils.getProperty(dataManagementService, nextPcbaRev, "item_revision_id");
                String nextPcbaRevisionId = nextPcbaRev.get_item_revision_id();
                System.out.println("");
                if (nextPcbaRevisionId.equalsIgnoreCase(pcbaRevisionId)) {
                    noNextRevs.add(pcbaRev);
                }else{
                    hasNextRevs.add(nextPcbaRev);
                }
            }

            if(noNextRevs.size()>0) {
                List<UserPojo> trackers = getTaskTracker(dataManagementService, task41, fileManagementUtility, tcIntegrate);
                TrackResponse r = new TrackResponse();
                r.setWorkflowName("FXN41");
                r.setUid(task41.getUid());
                r.setItemId(itemId);
                r.setStateName(task41NameStr);
                r.setTrackers(trackers);
                r.setItemRev(itemRevId);
                r.setPoInfo(poInfo);
                trackResponses.add(r);
            }
            for(ItemRevision nextPcbaRev:hasNextRevs) {
                TCUtils.getProperty(dataManagementService, nextPcbaRev, "item_revision_id");
                TCUtils.getProperty(dataManagementService, nextPcbaRev, "item_id");
                String pcbaId = nextPcbaRev.get_item_id();
                String nextPcbaRevisionId = nextPcbaRev.get_item_revision_id();
                getSubTask3Trackers(nextPcbaRev, dataManagementService, savedQueryService, pcbaId, nextPcbaRevisionId, task41, fileManagementUtility, tcIntegrate, trackResponses, task41NameStr, poInfo);
            }

        }



        return trackResponses;
    }

    public static ItemRevision getItemNextRevision(DataManagementService dmService, Item item, String curRevId) {
        try {
            ModelObject[] objects = {item};
            String[] atts = {"revision_list"};
            dmService.getProperties(objects, atts);
            ModelObject[] itemRevs = item.get_revision_list();
            for (int i = 0; i < itemRevs.length; i++) {
                ItemRevision tmpRev = (ItemRevision) itemRevs[i];
                TCUtils.getProperty(dmService, tmpRev, "item_revision_id");
                String revId = tmpRev.get_item_revision_id();
                if (curRevId.equalsIgnoreCase(revId)) {
                    if (i <= itemRevs.length - 2) {
                        return (ItemRevision) itemRevs[i + 1];
                    } else {
                        return tmpRev;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private static boolean  getSubTask4Trackers(ItemRevision nextPcbaRev, DataManagementService dataManagementService, SavedQueryService savedQueryService, String pcba, String nextPcbaRevid, EPMTask task41, FileManagementUtility fileManagementUtility, TcIntegrateClient tcIntegrate, List<TrackResponse> trackResponses, String task41NameStr, PoInfo poInfo) throws Exception {
        boolean hasComplete=false;
        boolean has31 = false;
        TCUtils.getProperty(dataManagementService, nextPcbaRev, "fnd0AllWorkflows");
        ModelObject[] subtasks = nextPcbaRev.getPropertyObject("fnd0AllWorkflows").getModelObjectArrayValue();
        if (subtasks != null && subtasks.length > 0) {
            for (ModelObject m : subtasks) {
                EPMTask t = (EPMTask) m;
                TCUtils.getProperty(dataManagementService, t, "object_string");
                String nameStr = t.get_object_string();
                if (nameStr.startsWith("FXN31")) {
                    has31 = true;
                }
            }
        }
        ModelObject[] tasks = MailTrackUtils.subTaskQuery(savedQueryService, "4", "EPMReviewTask;EPMConditionTask;EPMAcknowledgeTask", "FXN31*", pcba, nextPcbaRevid, "4");
        if (tasks == null || tasks.length <= 0) {
            // 未起31流程
            if (!has31) {
                List<UserPojo> trackers = getTask3Tracker(dataManagementService, task41, fileManagementUtility, tcIntegrate);
                TrackResponse r = new TrackResponse();
                r.setWorkflowName("FXN41");
                r.setUid(task41.getUid());
                r.setItemId(poInfo.getItemId());
                r.setStateName(task41NameStr);
                r.setPoInfo(poInfo);
                r.setTrackers(trackers);
                r.setItemRev(poInfo.getItemRev());
                trackResponses.add(r);
            } else {//31流程关闭
                tasks = MailTrackUtils.subTaskQuery(savedQueryService, "4", "EPMReviewTask;EPMConditionTask;EPMAcknowledgeTask", "FXN38*", pcba, nextPcbaRevid, "4");
                if (tasks == null || tasks.length <= 0) {
                    boolean has38 = false;
                    TCUtils.getProperty(dataManagementService, nextPcbaRev, "fnd0AllWorkflows");
                    ModelObject[] subtasks38 = nextPcbaRev.getPropertyObject("fnd0AllWorkflows").getModelObjectArrayValue();
                    if (subtasks38 != null && subtasks38.length > 0) {
                        for (ModelObject m : subtasks38) {
                            EPMTask t = (EPMTask) m;
                            TCUtils.getProperty(dataManagementService, t, "object_string");
                            String nameStr = t.get_object_string();
                            if (nameStr.startsWith("FXN38")) {
                                has38 = true;
                            }
                        }
                    }
                    //未起38流程
                    if(!has38){
                        List<UserPojo> trackers = getTask3Tracker(dataManagementService, task41, fileManagementUtility, tcIntegrate);
                        TrackResponse r = new TrackResponse();
                        r.setWorkflowName("FXN41");
                        r.setUid(task41.getUid());
                        r.setItemId(poInfo.getItemId());
                        r.setStateName(task41NameStr);
                        r.setPoInfo(poInfo);
                        r.setTrackers(trackers);
                        r.setItemRev(poInfo.getItemRev());
                        trackResponses.add(r);

                    }else {
                       //38 已經走完了
                        hasComplete=true;
                    }
                } else {
                    for (ModelObject t : tasks) {
                        EPMTask task38 = (EPMTask) t;
                        List<UserPojo> tarckers38 = getTaskTracker(dataManagementService, task38, fileManagementUtility, tcIntegrate);
                        if (tarckers38 != null && tarckers38.size() > 0) {
                            TCUtils.getProperty(dataManagementService, task38, "object_string");
                            String task38NameStr = task38.get_object_string();
                            TrackResponse r = new TrackResponse();
                            r.setUid(task38.getUid());
                            r.setItemId(pcba);
                            r.setStateName(task38NameStr);
                            r.setTrackers(tarckers38);
                            r.setItemRev(nextPcbaRevid);
                            r.setWorkflowName("FXN38");
                            r.setPoInfo(poInfo);
                            trackResponses.add(r);
                        }
                    }
                }
            }
        } else {
            for (ModelObject t : tasks) {
                EPMTask task31 = (EPMTask) t;
                List<UserPojo> tarckers31 = getTaskTracker(dataManagementService, task31, fileManagementUtility, tcIntegrate);
                if (tarckers31 != null && tarckers31.size() > 0) {
                    TCUtils.getProperty(dataManagementService, task31, "object_string");
                    String task31NameStr = task31.get_object_string();
                    TrackResponse r = new TrackResponse();
                    r.setUid(task31.getUid());
                    r.setItemId(pcba);
                    r.setWorkflowName("FXN31");
                    r.setStateName(task31NameStr);
                    r.setTrackers(tarckers31);
                    r.setPoInfo(poInfo);
                    r.setItemRev(nextPcbaRevid);
                    trackResponses.add(r);
                }
            }
        }
        return hasComplete;
    }



    private static void getSubTask3Trackers(ItemRevision nextPcbaRev, DataManagementService dataManagementService, SavedQueryService savedQueryService, String pcba, String nextPcbaRevid, EPMTask task41, FileManagementUtility fileManagementUtility, TcIntegrateClient tcIntegrate, List<TrackResponse> trackResponses, String task41NameStr, PoInfo poInfo) throws Exception {
        boolean has31 = false;
        TCUtils.getProperty(dataManagementService, nextPcbaRev, "fnd0AllWorkflows");
        ModelObject[] subtasks = nextPcbaRev.getPropertyObject("fnd0AllWorkflows").getModelObjectArrayValue();
        if (subtasks != null && subtasks.length > 0) {
            for (ModelObject m : subtasks) {
                EPMTask t = (EPMTask) m;
                TCUtils.getProperty(dataManagementService, t, "object_string");
                String nameStr = t.get_object_string();
                if (nameStr.startsWith("FXN31")) {
                    has31 = true;
                }
            }
        }
        ModelObject[] tasks = MailTrackUtils.subTaskQuery(savedQueryService, "4", "EPMReviewTask;EPMConditionTask;EPMAcknowledgeTask", "FXN31*", pcba, nextPcbaRevid, "4");
        if (tasks == null || tasks.length <= 0) {
            // 未起31流程
            if (!has31) {
                List<UserPojo> trackers = getTaskTracker(dataManagementService, task41, fileManagementUtility, tcIntegrate);
                TrackResponse r = new TrackResponse();
                r.setWorkflowName("FXN41");
                r.setUid(task41.getUid());
                r.setItemId(poInfo.getItemId());
                r.setStateName(task41NameStr);
                r.setPoInfo(poInfo);
                r.setTrackers(trackers);
                r.setItemRev(poInfo.getItemRev());
                trackResponses.add(r);
            } else {//31流程关闭
                tasks = MailTrackUtils.subTaskQuery(savedQueryService, "4", "EPMReviewTask;EPMConditionTask;EPMAcknowledgeTask", "FXN38*", pcba, nextPcbaRevid, "4");
                if (tasks == null || tasks.length <= 0) {
                    //未起38流程 或者 38 已經走完了
                    List<UserPojo> trackers = getTaskTracker(dataManagementService, task41, fileManagementUtility, tcIntegrate);
                    TrackResponse r = new TrackResponse();
                    r.setUid(task41.getUid());
                    r.setWorkflowName("FXN41");
                    r.setItemId(poInfo.getItemId());
                    r.setStateName(task41NameStr);
                    r.setPoInfo(poInfo);
                    r.setTrackers(trackers);
                    r.setItemRev(poInfo.getItemRev());
                    trackResponses.add(r);

                } else {
                    for (ModelObject t : tasks) {
                        EPMTask task38 = (EPMTask) t;
                        List<UserPojo> tarckers38 = getTaskTracker(dataManagementService, task38, fileManagementUtility, tcIntegrate);
                        if (tarckers38 != null && tarckers38.size() > 0) {
                            TCUtils.getProperty(dataManagementService, task38, "object_string");
                            String task38NameStr = task38.get_object_string();
                            TrackResponse r = new TrackResponse();
                            r.setUid(task38.getUid());
                            r.setItemId(pcba);
                            r.setStateName(task38NameStr);
                            r.setTrackers(tarckers38);
                            r.setItemRev(nextPcbaRevid);
                            r.setWorkflowName("FXN38");
                            r.setPoInfo(poInfo);
                            trackResponses.add(r);
                        }
                    }
                }
            }
        } else {
            for (ModelObject t : tasks) {
                EPMTask task31 = (EPMTask) t;
                List<UserPojo> tarckers31 = getTaskTracker(dataManagementService, task31, fileManagementUtility, tcIntegrate);
                if (tarckers31 != null && tarckers31.size() > 0) {
                    TCUtils.getProperty(dataManagementService, task31, "object_string");
                    String task31NameStr = task31.get_object_string();
                    TrackResponse r = new TrackResponse();
                    r.setUid(task31.getUid());
                    r.setItemId(pcba);
                    r.setWorkflowName("FXN31");
                    r.setStateName(task31NameStr);
                    r.setTrackers(tarckers31);
                    r.setPoInfo(poInfo);
                    r.setItemRev(nextPcbaRevid);
                    trackResponses.add(r);
                }
            }
        }
    }

    private static void updateTrackerMail(TcIntegrateClient tcIntegrate, List<UserPojo> tarckers) throws Exception {
        for (UserPojo u : tarckers) {

            JSONObject o = new JSONObject();
            o.put("empId", u.getUserId());
            List<SPASUser> spasUser = tcIntegrate.getTeamRosterByEmpId(o.toJSONString());
            if (spasUser != null && spasUser.size() > 0) {
                if(u.getUserName()==null||"".equalsIgnoreCase(u.getUserName())){
                    u.setUserName(spasUser.get(0).getName());
                }
                if (u.getMail() == null || "".equalsIgnoreCase(u.getMail())) {
                    u.setMail(spasUser.get(0).getNotes());
                }
            }
        }
    }


    public static String genTrackMailHtml(TrackResponse trackResponse, String userName) {
        String html = "";
        PoInfo poInfo = trackResponse.getPoInfo();
        long delayHours2 = poInfo.getRealDelayHours();
        if (delayHours2 > 0) {
            html = "<html><head><style>div{margin:10px;}table{margin:20px;border-spacing: 0px;}th{border:solid 1px #000000;height: 35px;padding:6px;} td{border-left:solid 1px #000000;border-bottom:solid 1px #000000;border-right:solid 1px #000000;height: 35px;padding:6px;}</style></head><body><div>尊敬的 " + userName + ":</div><div>&nbsp;&nbsp;這是TeamCenter系統發出的PCBA BOM製作申請[" + poInfo.getItemId() + "/" + poInfo.getItemRev() + "],由" + poInfo.getOwner() + "發起。已逾期" + (delayHours2 + 48) + "小時,請及時簽核。</div>";
        } else {
            if (Math.abs(delayHours2) < 48) {
                html = "<html><head><style>div{margin:10px;}table{margin:20px;border-spacing: 0px;}th{border:solid 1px #000000;height: 35px;padding:6px;} td{border-left:solid 1px #000000;border-bottom:solid 1px #000000;border-right:solid 1px #000000;height: 35px;padding:6px;}</style></head><body><div>尊敬的 " + userName + ":</div><div>&nbsp;&nbsp;這是TeamCenter系統發出的PCBA BOM製作申請[" + poInfo.getItemId() + "/" + poInfo.getItemRev() + "],由" + poInfo.getOwner() + "發起。已逾期" + (48 - Math.abs(delayHours2)) + "小時,請及時簽核。</div>";
            } else {
                html = "<html><head><style>div{margin:10px;}table{margin:20px;border-spacing: 0px;}th{border:solid 1px #000000;height: 35px;padding:6px;} td{border-left:solid 1px #000000;border-bottom:solid 1px #000000;border-right:solid 1px #000000;height: 35px;padding:6px;}</style></head><body><div>尊敬的 " + userName + ":</div><div>&nbsp;&nbsp;這是TeamCenter系統發出的PCBA BOM製作申請[" + poInfo.getItemId() + "/" + poInfo.getItemRev() + "],由" + poInfo.getOwner() + "發起。距離逾期還剩" + (Math.abs(delayHours2) - 48) + "小時,請及時簽核。</div>";
            }
        }
        String wf = trackResponse.getWorkflowName();
        if (wf.startsWith("FXN31") || wf.startsWith("FXN38")) {
            html += "<div>&nbsp;&nbsp;PCBA板" + trackResponse.getItemId() + "/" + trackResponse.getItemRev() + "," + wf + "/" + trackResponse.getStateName() + ",負責人:" + userName + "</div>";
        }
        html += "</body></html>";
        return html;
    }


}
