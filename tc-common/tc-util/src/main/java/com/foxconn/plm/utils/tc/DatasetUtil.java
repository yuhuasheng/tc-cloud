package com.foxconn.plm.utils.tc;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.constants.TCDatasetConstant;
import com.teamcenter.schemas.soa._2006_03.exceptions.ServiceException;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.core._2006_03.DataManagement.CreateDatasetsResponse;
import com.teamcenter.services.strong.core._2008_06.DataManagement;
import com.teamcenter.soa.client.FileManagementUtility;
import com.teamcenter.soa.client.GetFileResponse;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.Dataset;
import com.teamcenter.soa.client.model.strong.ImanFile;
import com.teamcenter.soa.client.model.strong.ItemRevision;


import com.teamcenter.services.loose.core._2006_03.FileManagement;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.exceptions.NotLoadedException;


import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class DatasetUtil {
    private static Log log = LogFactory.get();


    public static String getDataSetName(DataManagementService dataManagementService, ModelObject mo) throws Exception {
        String[] type_atts = {"object_name"};
        dataManagementService.getProperties(new ModelObject[]{mo}, type_atts);
        return mo.getPropertyDisplayableValue("object_name");
    }


    public static Dataset getDateSet(DataManagementService dataManagementService, String dateSetUid) {
        ModelObject modelObject = TCUtils.findObjectByUid(dataManagementService, dateSetUid);
        return (Dataset) modelObject;
    }


    /**
     * 创建数据集
     *
     * @param dataManagementService 工具类
     * @param itemRevision          对象版本
     * @param dsname                数据集名称
     * @param type                  数据集类型
     * @param relationType          数据集类型
     * @return
     */
    public static Dataset createDataset(DataManagementService dataManagementService, ItemRevision itemRevision, String dsname,
                                        String type, String relationType) {
        DataManagement.DatasetProperties2 props = new DataManagement.DatasetProperties2();
        props.clientId = "datasetClientId";
        props.type = type;
        props.name = dsname;
        props.description = "Create dataset object";
        props.container = itemRevision;
        props.relationType = relationType;
        DataManagement.DatasetProperties2[] currProps = {props};
        CreateDatasetsResponse response = dataManagementService.createDatasets2(currProps);
        if (response.serviceData.sizeOfPartialErrors() > 0) {
            int errorMsg = response.serviceData.getPartialError(0).getErrorValues()[0].getCode();
            if (errorMsg == 515239) {
                log.error("【error】 用户infodba被锁死，请联系管理员！");
            }
            return null;
        }
        return response.output[0].dataset;
    }


    public static File[] getDataSetFiles(DataManagementService dmService, Dataset dataset, FileManagementUtility fmsFileManagement) {
        try {
            TCUtils.getProperty(dmService, dataset, "ref_list");
            ModelObject[] dsfilevec = dataset.get_ref_list();
            ImanFile dsFile = null;
            if (dsfilevec.length > 0) {
                if (dsfilevec[0] instanceof ImanFile) {
                    dsFile = (ImanFile) dsfilevec[0];
                }

                // getProperty(dmService, dsFile, "original_file_name");
                GetFileResponse getFileResponse = fmsFileManagement.getFiles(dsfilevec);
                return getFileResponse.getFiles();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public static File getImanFile(ImanFile dsFile, DataManagementService dmService, FileManagementUtility fmsFileManagement) {
        try {
            ModelObject[] dsfilevec = new ModelObject[1];
            if (dsFile != null) {
                dsfilevec[0] = dsFile;
                GetFileResponse getFileResponse = fmsFileManagement.getFiles(dsfilevec);
                File[] files = getFileResponse.getFiles();
                if (files.length > 0)
                    return getFileResponse.getFiles()[0];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public static Dataset updateDataset(Dataset dataset, String refName, String filePath, DataManagementService dmService, FileManagementUtility fMSFileManagement) {
        try {
            ModelObject[] objects2 = {dataset};
            String[] attributes2 = {"ref_list"};
            dmService.refreshObjects(objects2);
            dmService.getProperties(objects2, attributes2);
            ModelObject[] dsFileVec = dataset.get_ref_list();

            dmService.deleteObjects(dsFileVec);

            dmService.refreshObjects(new ModelObject[]{dataset});

            FileManagement.DatasetFileInfo[] fileInfos = new FileManagement.DatasetFileInfo[1];
            FileManagement.DatasetFileInfo fileInfo = new FileManagement.DatasetFileInfo();

            fileInfo.fileName = filePath;
            fileInfo.allowReplace = true;
            fileInfo.isText = false;
            fileInfo.namedReferencedName = refName;
            fileInfos[0] = fileInfo;

            FileManagement.GetDatasetWriteTicketsInputData[] inputDatas = new FileManagement.GetDatasetWriteTicketsInputData[1];
            FileManagement.GetDatasetWriteTicketsInputData inputData = new FileManagement.GetDatasetWriteTicketsInputData();

            inputData.dataset = dataset;
            inputData.createNewVersion = false;
            inputData.datasetFileInfos = fileInfos;
            inputDatas[0] = inputData;
            ServiceData response = fMSFileManagement.putFiles(inputDatas);
            dmService.refreshObjects(new ModelObject[]{dataset});
        } catch (Exception e) {
            e.printStackTrace();
        }


        return dataset;
    }


    public static boolean addDatasetFile(FileManagementUtility fMSFileManagement, DataManagementService dataManagementService,
                                         Dataset dataset, String fileName, String refName, boolean isText) {
        try {
            dataManagementService.refreshObjects(new ModelObject[]{dataset});
            dataManagementService.getProperties(new ModelObject[]{dataset},
                    new String[]{"ref_list"});
            ModelObject[] dsFileVec = dataset.get_ref_list();
            // 删除数据集命名的引用下的文件
            dataManagementService.deleteObjects(dsFileVec);
            dataManagementService.refreshObjects(new ModelObject[]{dataset});

            FileManagement.DatasetFileInfo[] fileInfos = new FileManagement.DatasetFileInfo[1];
            FileManagement.DatasetFileInfo fileInfo = new FileManagement.DatasetFileInfo();
            File file = new File(fileName);
            if (!file.exists()) {
                return false;
            }

            fileInfo.fileName = file.getAbsolutePath();
            ;
            fileInfo.allowReplace = true;
//	         fileInfo.isText = false;
            fileInfo.isText = isText;
            fileInfo.namedReferencedName = refName;
            fileInfos[0] = fileInfo;

            FileManagement.GetDatasetWriteTicketsInputData[] inputDatas = new FileManagement.GetDatasetWriteTicketsInputData[1];
            FileManagement.GetDatasetWriteTicketsInputData inputData = new FileManagement.GetDatasetWriteTicketsInputData();

            inputData.dataset = dataset;
            inputData.createNewVersion = false;
            inputData.datasetFileInfos = fileInfos;
            inputDatas[0] = inputData;

            ServiceData response = fMSFileManagement.putFiles(inputDatas);
            if (response.sizeOfPartialErrors() > 0) {
                return false;
            }
            dataManagementService.refreshObjects(new ModelObject[]{dataset});
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    /**
     * 数据集移除文件
     * @param dmService
     * @param dataset 数据集
     * @param type 类型
     * @return
     * @throws NotLoadedException
     */
    public static boolean removeFileFromDataset(DataManagementService dmService, Dataset dataset, String type) throws NotLoadedException {
        dmService.refreshObjects(new ModelObject[]{dataset});
        dmService.getProperties(new ModelObject[]{dataset}, new String[]{TCDatasetConstant.REL_REF_LIST});
        ModelObject[] files = dataset.get_ref_list();
        for (int i = 0; i < files.length; i++) {
            com.teamcenter.services.strong.core._2007_09.DataManagement.NamedReferenceInfo[] nrInfo = new com.teamcenter.services.strong.core._2007_09.DataManagement.NamedReferenceInfo[1];
            nrInfo[0] = new com.teamcenter.services.strong.core._2007_09.DataManagement.NamedReferenceInfo();
            nrInfo[0].clientId = files[i].getUid();
            nrInfo[0].deleteTarget = true;
            nrInfo[0].type = type;
            nrInfo[0].targetObject = files[i];
            com.teamcenter.services.strong.core._2007_09.DataManagement.RemoveNamedReferenceFromDatasetInfo datasetinfo[] = new com.teamcenter.services.strong.core._2007_09.DataManagement.RemoveNamedReferenceFromDatasetInfo[1];
            datasetinfo[0] = new com.teamcenter.services.strong.core._2007_09.DataManagement.RemoveNamedReferenceFromDatasetInfo();
            datasetinfo[0].clientId = dataset.getUid();
            datasetinfo[0].dataset = dataset;
            datasetinfo[0].nrInfo = nrInfo;
            ServiceData data = dmService.removeNamedReferenceFromDataset(datasetinfo);
            if (data.sizeOfPartialErrors() > 0) {
                for (int j = 0; j < data.sizeOfPartialErrors(); i++) {
                    log.error("【ERROR】remove file from Dataset response error info : " + Arrays.toString(data.getPartialError(i).getMessages()));
                    return false;
                }
            }
            System.out.println("AAAAA源文件已经删除");
        }
        return true;
    }


    public static boolean addDatasetFile(FileManagementUtility fMSFileManagement, DataManagementService dataManagementService,
                                         Dataset dataset, File file, String refName, boolean isText) {
        try {
            dataManagementService.refreshObjects(new ModelObject[]{dataset});
            dataManagementService.getProperties(new ModelObject[]{dataset},
                    new String[]{"ref_list"});
            ModelObject[] dsFileVec = dataset.get_ref_list();
            // 删除数据集命名的引用下的文件
            dataManagementService.deleteObjects(dsFileVec);
            dataManagementService.refreshObjects(new ModelObject[]{dataset});

            FileManagement.DatasetFileInfo[] fileInfos = new FileManagement.DatasetFileInfo[1];
            FileManagement.DatasetFileInfo fileInfo = new FileManagement.DatasetFileInfo();

            if (!file.exists()) {
                return false;
            }

            fileInfo.fileName = file.getAbsolutePath();
            ;
            fileInfo.allowReplace = true;
//	         fileInfo.isText = false;
            fileInfo.isText = isText;
            fileInfo.namedReferencedName = refName;
            fileInfos[0] = fileInfo;

            FileManagement.GetDatasetWriteTicketsInputData[] inputDatas = new FileManagement.GetDatasetWriteTicketsInputData[1];
            FileManagement.GetDatasetWriteTicketsInputData inputData = new FileManagement.GetDatasetWriteTicketsInputData();

            inputData.dataset = dataset;
            inputData.createNewVersion = false;
            inputData.datasetFileInfos = fileInfos;
            inputDatas[0] = inputData;

            ServiceData response = fMSFileManagement.putFiles(inputDatas);
            if (response.sizeOfPartialErrors() > 0) {
                return false;
            }
            dataManagementService.refreshObjects(new ModelObject[]{dataset});
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }


    public static List<String> getFileType(String extension) {
        String dsType, refType;
        switch (extension.toLowerCase(Locale.ENGLISH)) {
            case "pdf":
                dsType = "PDF";
                refType = "PDF_Reference";
                break;
            case "png":
                dsType = "Image";
                refType = "Image";
                break;
            case "7z":
                dsType = "D9_7Z";
                refType = "D9_7Z";
                break;
            case "zip":
                dsType = "Zip";
                refType = "ZIPFILE";
                break;
            case "bmp":
                dsType = "Bitmap";
                refType = "Image";
                break;
            case "stp":
                dsType = "D9_STEP";
                refType = "D9_STEP";
                break;
            case "dwg":
                dsType = "D9_AutoCAD";
                refType = "D9_AutoCAD";
            case "txt":
                dsType = "Text";
                refType = "Text";
                break;
            case "docx":
                dsType = "MSWordX";
                refType = "word";
                break;
            case "doc":
                dsType = "MSWord";
                refType = "word";
                break;
            case "pptx":
                dsType = "MSPowerPointX";
                refType = "powerpoint";
                break;
            case "ppt":
                dsType = "MSPowerPoint";
                refType = "powerpoint";
                break;
            case "xlsx":
                dsType = "MSExcelX";
                refType = "excel";
                break;
            case "xls":
                dsType = "MSExcel";
                refType = "excel";
                break;
            default:
                dsType = "JPEG";
                refType = "JPEG_Reference";
                break;
        }
        return CollUtil.newArrayList(dsType, refType);
    }

    public static Dataset uploadDataset(DataManagementService dmService, FileManagementUtility fmuService, ItemRevision itemRev, String filePath,
                                        String refName, String datasetName, String datasetType,String relationType) throws ServiceException {
        Dataset dataset = null;
        DataManagement.DatasetProperties2[] datasetProps = new DataManagement.DatasetProperties2[1];
        DataManagement.DatasetProperties2 datasetProp = new DataManagement.DatasetProperties2();

        datasetProp.clientId = "datasetWriteTixTestClientId";
        datasetProp.type = datasetType;
        datasetProp.name = datasetName;
        datasetProp.description = "";
        datasetProps[0] = datasetProp;
        com.teamcenter.services.strong.core._2006_03.DataManagement.CreateDatasetsResponse dsResp = dmService.createDatasets2(datasetProps);
        dataset = dsResp.output[0].dataset;

        com.teamcenter.services.strong.core._2006_03.DataManagement.Relationship[] relationships = new com.teamcenter.services.strong.core._2006_03.DataManagement.Relationship[1];
        com.teamcenter.services.strong.core._2006_03.DataManagement.Relationship relationship = new com.teamcenter.services.strong.core._2006_03.DataManagement.Relationship();

        relationship.clientId = "";
        relationship.primaryObject = itemRev;
        relationship.secondaryObject = dataset;
        relationship.relationType = relationType;
        relationship.userData = null;
        relationships[0] = relationship;
        com.teamcenter.services.strong.core._2006_03.DataManagement.CreateRelationsResponse crResponse = dmService.createRelations(relationships);
        ServiceData crServiceData = crResponse.serviceData;
        if (crServiceData.sizeOfPartialErrors() > 0) {
            throw new ServiceException(crServiceData.getPartialError(0).toString());
        }

        com.teamcenter.services.loose.core._2006_03.FileManagement.DatasetFileInfo[] datasetFileInfos = new com.teamcenter.services.loose.core._2006_03.FileManagement.DatasetFileInfo[1];
        com.teamcenter.services.loose.core._2006_03.FileManagement.DatasetFileInfo datasetFileInfo = new com.teamcenter.services.loose.core._2006_03.FileManagement.DatasetFileInfo();

        datasetFileInfo.fileName = filePath;
        datasetFileInfo.allowReplace = true;
        datasetFileInfo.isText = false;
        datasetFileInfo.namedReferencedName = refName;
        datasetFileInfos[0] = datasetFileInfo;

        com.teamcenter.services.loose.core._2006_03.FileManagement.GetDatasetWriteTicketsInputData[] inputDatas = new com.teamcenter.services.loose.core._2006_03.FileManagement.GetDatasetWriteTicketsInputData[1];
        com.teamcenter.services.loose.core._2006_03.FileManagement.GetDatasetWriteTicketsInputData inputData = new com.teamcenter.services.loose.core._2006_03.FileManagement.GetDatasetWriteTicketsInputData();
        inputData.dataset = dataset;
        inputData.createNewVersion = false;
        inputData.datasetFileInfos = datasetFileInfos;
        inputDatas[0] = inputData;

        ServiceData fmuResponse = fmuService.putFiles(inputDatas);
        if (fmuResponse.sizeOfPartialErrors() > 0) {
            throw new ServiceException(fmuResponse.getPartialError(0).toString());
        }
        dmService.refreshObjects(new ModelObject[]{dataset});
        return dataset;
    }
}
