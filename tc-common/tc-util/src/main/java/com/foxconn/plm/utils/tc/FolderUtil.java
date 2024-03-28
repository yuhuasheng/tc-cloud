package com.foxconn.plm.utils.tc;

import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.constants.TCFolderConstant;
import com.teamcenter.services.loose.core.SessionService;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.core._2008_06.DataManagement;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.Folder;
import com.teamcenter.soa.client.model.strong.WorkspaceObject;

import java.util.*;

/**
 * @Author HuashengYu
 * @Date 2023/3/8 14:00
 * @Version 1.0
 */
public class FolderUtil {
    private static Log log = LogFactory.get();

    //创建文件夹
    public static Folder createReferenceFolder(DataManagementService dmService, Folder parentFolder, String folderName, String folderType) throws Exception {
        Folder folder = null;
        Map<String, String> propMap = new HashMap<>();
        propMap.put(TCFolderConstant.PROPERTY_OBJECT_NAME, folderName);
        DataManagement.CreateResponse response = TCUtils.createObjects(dmService, folderType, propMap);
        ServiceData serviceData = response.serviceData;
        String result = TCUtils.getErrorMsg(serviceData);
        if (StrUtil.isNotEmpty(result)) {
            throw new Exception(result);
        }
        if (serviceData.sizeOfPartialErrors() <= 0) {
            ModelObject[] folders = response.output[0].objects;
            folder = (Folder) folders[0];
            TCUtils.addContents(dmService, parentFolder, folder);
        }
        return folder;
    }

    /**
     * @param parentFolder
     * @param childFolerName
     * @param dataManagementService
     * @return
     * @throws Exception
     */
    public static boolean isExistChildFolder(Folder parentFolder, String childFolerName, DataManagementService dataManagementService) throws Exception {
        boolean flag = false;
        dataManagementService.refreshObjects(new ModelObject[]{parentFolder});
        TCUtils.getProperty(dataManagementService, parentFolder, "contents");
        WorkspaceObject[] contents = parentFolder.get_contents();
        for (int i = 0; i < contents.length; i++) {
            WorkspaceObject content = contents[i];
            if (content instanceof Folder) {
                Folder folder = (Folder) content;
                TCUtils.getProperty(dataManagementService, folder, "object_name");
                String folderName = folder.get_object_name();
                if (folderName.equalsIgnoreCase(childFolerName)) {
                    flag = true;
                    break;
                }
            }
        }
        return flag;
    }

    public static boolean isExistPhaseFolder(Folder parentFolder, String phaseFolerName, DataManagementService dataManagementService) throws Exception {
        boolean flag = false;
        String temp = phaseFolerName;
        if (temp.length() > 2) {
            temp = temp.substring(0, 2);
        }
        dataManagementService.refreshObjects(new ModelObject[]{parentFolder});
        TCUtils.getProperty(dataManagementService, parentFolder, "contents");
        WorkspaceObject[] contents = parentFolder.get_contents();
        for (int i = 0; i < contents.length; i++) {
            WorkspaceObject content = contents[i];
            if (content instanceof Folder) {
                Folder folder = (Folder) content;
                TCUtils.getProperty(dataManagementService, folder, "object_name");
                String folderName = folder.get_object_name();
                if (folderName.toLowerCase(Locale.ENGLISH).startsWith(temp.toLowerCase(Locale.ENGLISH))) {
                    flag = true;
                    break;
                }
            }
        }
        return flag;
    }


    public static Set<Folder> findChildFolders(Folder parentFolder, DataManagementService dataManagementService) throws Exception {
        Set<Folder> childFolders = new HashSet<>();
        dataManagementService.refreshObjects(new ModelObject[]{parentFolder});
        TCUtils.getProperty(dataManagementService, parentFolder, "contents");
        WorkspaceObject[] contents = parentFolder.get_contents();
        for (int i = 0; i < contents.length; i++) {
            WorkspaceObject content = contents[i];
            if (content instanceof Folder) {
                Folder folder = (Folder) content;
                childFolders.add(folder);
            }
        }
        return childFolders;
    }

    public static Folder findChildFolder(Folder parentFolder, String childFolderName, DataManagementService dataManagementService) throws Exception {
        dataManagementService.refreshObjects(new ModelObject[]{parentFolder});
        TCUtils.getProperty(dataManagementService, parentFolder, "contents");
        WorkspaceObject[] contents = parentFolder.get_contents();
        for (int i = 0; i < contents.length; i++) {
            WorkspaceObject content = contents[i];
            if (content instanceof Folder) {
                Folder folder = (Folder) content;
                TCUtils.getProperty(dataManagementService, folder, "object_name");
                String folderName = folder.get_object_name();
                if (folderName.equals(childFolderName)) {
                    return folder;
                }
            }
        }
        return null;
    }


    public static void isEmmptyFolder(DataManagementService dataManagementService, Folder folder, List<WorkspaceObject> data) throws Exception {
        dataManagementService.refreshObjects(new ModelObject[]{folder});
        TCUtils.getProperty(dataManagementService, folder, "contents");
        WorkspaceObject[] contents = folder.get_contents();
        for (int i = 0; i < contents.length; i++) {
            WorkspaceObject content = contents[i];
            if (content instanceof Folder) {
                Folder childFolder = (Folder) content;
                isEmmptyFolder(dataManagementService, childFolder, data);
            } else {
                data.add(content);
            }
        }
    }

    public static void deleteFolderRelation(DataManagementService dmService, Folder parentFolder,
                                            Folder childFolder) throws Exception {
        com.teamcenter.services.strong.core._2006_03.DataManagement.Relationship[] relationships =
                new com.teamcenter.services.strong.core._2006_03.DataManagement.Relationship[1];
        relationships[0] = new com.teamcenter.services.strong.core._2006_03.DataManagement.Relationship();
        relationships[0].clientId = "";
        relationships[0].primaryObject = parentFolder;
        relationships[0].secondaryObject = childFolder;
        relationships[0].relationType = TCFolderConstant.REL_CONTENTS;
        dmService.deleteRelations(relationships);
    }


    public static void deleteFolderAndAllChildFolder(DataManagementService dmService, SessionService sessionservice, Folder parentFolder,
                                                     Folder folder) throws Exception {
        TCUtils.byPass(sessionservice, true);
        List<Folder> folders = new ArrayList<>();
        deleteFolderAllRelated(dmService, folder, folders);
        for (Folder f : folders) {
            dmService.deleteObjects(new ModelObject[]{f});
        }

        com.teamcenter.services.strong.core._2006_03.DataManagement.Relationship[] relationships =
                new com.teamcenter.services.strong.core._2006_03.DataManagement.Relationship[1];
        relationships[0] = new com.teamcenter.services.strong.core._2006_03.DataManagement.Relationship();
        relationships[0].clientId = "";
        relationships[0].primaryObject = parentFolder;
        relationships[0].secondaryObject = folder;
        relationships[0].relationType = TCFolderConstant.REL_CONTENTS;
        dmService.deleteRelations(relationships);
        dmService.deleteObjects(new ModelObject[]{folder});

        dmService.refreshObjects(new ModelObject[]{parentFolder});
    }


    public static void deleteFolderSoft(DataManagementService dmService, SessionService sessionservice, Folder parentFolder,
                                                     Folder folder,String desc) throws Exception {
        TCUtils.byPass(sessionservice, true);

        com.teamcenter.services.strong.core._2006_03.DataManagement.Relationship[] relationships =
                new com.teamcenter.services.strong.core._2006_03.DataManagement.Relationship[1];
        relationships[0] = new com.teamcenter.services.strong.core._2006_03.DataManagement.Relationship();
        relationships[0].clientId = "";
        relationships[0].primaryObject = parentFolder;
        relationships[0].secondaryObject = folder;
        relationships[0].relationType = TCFolderConstant.REL_CONTENTS;
        dmService.deleteRelations(relationships);
        dmService.refreshObjects(new ModelObject[]{parentFolder});

        Map<String, com.teamcenter.services.strong.core._2007_01.DataManagement.VecStruct> map = new HashMap<>();
        com.teamcenter.services.strong.core._2007_01.DataManagement.VecStruct vecStruce = new com.teamcenter.services.strong.core._2007_01.DataManagement.VecStruct();
        vecStruce.stringVec = new String[]{desc};
        map.put("object_desc", vecStruce);
        dmService.setProperties(new ModelObject[]{folder}, map);
        dmService.refreshObjects(new ModelObject[]{folder});

    }


    private static void deleteFolderAllRelated(DataManagementService dmService, Folder folder, List<Folder> folders) throws Exception {
        dmService.refreshObjects(new ModelObject[]{folder});
        TCUtils.getProperty(dmService, folder, TCFolderConstant.REL_CONTENTS);
        WorkspaceObject[] contents = folder.get_contents();
        for (int i = 0; i < contents.length; i++) {
            WorkspaceObject content = contents[i];
            if (content instanceof Folder) {
                Folder childFolder = (Folder) content;
                folders.add(childFolder);
                com.teamcenter.services.strong.core._2006_03.DataManagement.Relationship[] relationships =
                        new com.teamcenter.services.strong.core._2006_03.DataManagement.Relationship[1];
                relationships[0] = new com.teamcenter.services.strong.core._2006_03.DataManagement.Relationship();
                relationships[0].clientId = "";
                relationships[0].primaryObject = folder;
                relationships[0].secondaryObject = childFolder;
                relationships[0].relationType = TCFolderConstant.REL_CONTENTS;
                dmService.deleteRelations(relationships);
                deleteFolderAllRelated(dmService, childFolder, folders);
            }
        }
    }

    public static Folder createFolder(DataManagementService dataManagementService, Folder projectFolder, String folderType, String folderName,String descr) throws Exception {
        Folder mntFolder = null;
        Map<String, String> propMap = new HashMap<>();
        propMap.put(TCFolderConstant.PROPERTY_OBJECT_NAME, folderName);
        propMap.put(TCFolderConstant.PROPERTY_OBJECT_DESC, descr==null?"":descr);
        DataManagement.CreateResponse response = TCUtils.createObjects(dataManagementService, folderType, propMap);
        ServiceData serviceData = response.serviceData;
        if (serviceData.sizeOfPartialErrors() == 0) {
            ModelObject[] folders = response.output[0].objects;
            mntFolder = (Folder) folders[0];
            TCUtils.addContents(dataManagementService, projectFolder, mntFolder);
            dataManagementService.refreshObjects(new ModelObject[]{mntFolder});
            log.info("創建文件夾成功===》" + folderName);
        } else {
            throw new Exception("创建【" + folderName + "】文件夹失败：" + serviceData.getPartialError(0).getErrorValues()[0].getMessage());
        }
        return mntFolder;
    }

    public static Folder createFolder(DataManagementService dataManagementService, String folderType, String folderName,String descr) throws Exception {
        Folder mntFolder = null;
        Map<String, String> propMap = new HashMap<>();
        propMap.put(TCFolderConstant.PROPERTY_OBJECT_NAME, folderName);
        propMap.put(TCFolderConstant.PROPERTY_OBJECT_DESC, descr==null?"":descr);
        DataManagement.CreateResponse response = TCUtils.createObjects(dataManagementService, folderType, propMap);
        ServiceData serviceData = response.serviceData;
        if (serviceData.sizeOfPartialErrors() == 0) {
            ModelObject[] folders = response.output[0].objects;
            mntFolder = (Folder) folders[0];
            dataManagementService.refreshObjects(new ModelObject[]{mntFolder});
            log.info("創建文件夾成功===》" + folderName);
        } else {
            throw new Exception("创建【" + folderName + "】文件夹失败：" + serviceData.getPartialError(0).getErrorValues()[0].getMessage());
        }
        return mntFolder;
    }

}
