
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrBuilder;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.cis.Application;
import com.foxconn.plm.cis.domain.PartEntity;
import com.foxconn.plm.cis.enumconfig.CISConstants;
import com.foxconn.plm.cis.enumconfig.CISType;
import com.foxconn.plm.cis.scheduled.CISSynScheduledTask;
import com.foxconn.plm.cis.service.ICISService;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.query.SavedQueryService;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.Item;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @ClassName: VerifyTest
 * @Description:
 * @Author DY
 * @Create 2023/4/11
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class VerifyTest {
    private static Log log = LogFactory.get();
    @Resource
    private TCSOAServiceFactory tcsoaServiceFactory;
    @Resource(name = "CISServiceImpl")
    private ICISService cisService;
    @Resource(name = "CISDellServiceImpl")
    private ICISService cisDellService;
    @Value("${cis.globalPath}")
    private String defaultPath;
    @Value("${cis.dellPath}")
    private String dellPath;
    @Resource
    private CISSynScheduledTask task;

    @Test
    public void verify() {
        task.syncCIS();
    }


    @Test
    public void test() throws Exception {
        List<PartEntity> list = cisService.getNotSyncPart();
        List<PartEntity> list1 = cisDellService.getNotSyncPart();

        /*tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS4);
        List<PartEntity> collect = list.parallelStream().filter(item -> StrUtil.isNotBlank(item.getModifiedDrawingFile())).collect(Collectors.toList());

        syncFile(collect, CISType.CIS, tcsoaServiceFactory);
        List<PartEntity> collect1 = list1.parallelStream().filter(item -> StrUtil.isNotBlank(item.getModifiedDrawingFile())).collect(Collectors.toList());

        syncFile(collect1,CISType.CIS_DELL,tcsoaServiceFactory);


        tcsoaServiceFactory.logout();*/
    }

    private void syncFile(List<PartEntity> list, CISType type, TCSOAServiceFactory tcsoaServiceFactory) throws Exception {
        for (PartEntity partEntity : list) {
            String standardPn = partEntity.getStandardPN();
            String modifiedDrawingFile = partEntity.getModifiedDrawingFile();
            String foxconnPartNumberNodt = partEntity.getFoxconnPartNumberNodt();
            String category = transCategory(partEntity.getCategory());
            System.out.println("-----------------");
            System.out.println(standardPn);
            System.out.println(modifiedDrawingFile);
            System.out.println(foxconnPartNumberNodt);
            System.out.println(category);
            System.out.println("-----------------");
            // 1、先確認item是否存在
            // 将3D模型挂载到standardPN下
            try {
                updateObject(tcsoaServiceFactory, standardPn, modifiedDrawingFile, type, category);
            } catch (Exception e) {
                e.printStackTrace();
            }
            // 将3D模型挂载到foxconnPartNodt下
            if (StrUtil.isNotBlank(foxconnPartNumberNodt) && !foxconnPartNumberNodt.equals(standardPn)) {
                try {
                    updateObject(tcsoaServiceFactory, foxconnPartNumberNodt, modifiedDrawingFile, type, category);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 将文件夹类型转换成对应的首字母大写其他字母小写的形式
     *
     * @param category
     * @return
     */
    private String transCategory(String category) {
        if (category.contains("_")) {
            String[] list = category.split("_");
            StrBuilder sb = new StrBuilder();
            for (String s : list) {
                sb.append(StrUtil.upperFirst(s.toLowerCase())).append(" ");
            }
            return sb.subString(0, sb.length() - 1);
        } else {
            return StrUtil.upperFirst(category.toLowerCase());
        }
    }

    /**
     * 根据对象名称创建item或者item版本，根据3D数据模型判断是否上传模型数据
     *
     * @param objName         item的名称
     * @param drawingFileName 3D模型的名称
     * @param type            模型类型，CIS全局库还是DELL库
     * @throws Exception
     */
    private void updateObject(TCSOAServiceFactory tcsoaServiceFactory, String objName, String drawingFileName, CISType type, String category) throws Exception {
        if (StrUtil.isNotBlank(objName)) {
            Item item = getItem(objName, tcsoaServiceFactory.getSavedQueryService());
            if(ObjectUtil.isNull(item)){
                item = TCUtils.createDocument(tcsoaServiceFactory.getDataManagementService(), objName, CISConstants.TYPE_EDA_COM_PART, objName, "A", new HashMap<>());
            }
            ItemRevision itemRevision = TCUtils.getItemLatestRevision(tcsoaServiceFactory.getDataManagementService(), item);
            ItemRevision drawingItemRevision = relateDesign(itemRevision,drawingFileName,tcsoaServiceFactory.getSavedQueryService(),tcsoaServiceFactory.getDataManagementService());
            if (StrUtil.isNotBlank(drawingFileName) && ObjectUtil.isNotNull(drawingItemRevision)) {
                TCUtils.getProperty(tcsoaServiceFactory.getDataManagementService(), drawingItemRevision, "IMAN_specification");
                TCUtils.refreshObject(tcsoaServiceFactory.getDataManagementService(), drawingItemRevision);
                List<ModelObject> modelObjectList = drawingItemRevision.getPropertyObject("IMAN_specification").getModelObjectListValue();
                if (CollUtil.isNotEmpty(modelObjectList)) {
                    for (ModelObject modelObject : modelObjectList) {
                        String name = modelObject.getTypeObject().getName();
                        TCUtils.getProperty(tcsoaServiceFactory.getDataManagementService(), modelObject, "object_name");
                        TCUtils.refreshObject(tcsoaServiceFactory.getDataManagementService(), modelObject);
                        String objectName = modelObject.getPropertyObject("object_name").getStringValue();
                        if ("ProPrt".equalsIgnoreCase(name) && objectName.equals(drawingItemRevision.get_item_id())) {
                            return;
                        }
                    }
                }
                // 掛載3D模型
                File modelFile = getModel(drawingFileName, type, category);
                if (ObjectUtil.isNotNull(modelFile)) {
                    try {
                        TCUtils.uploadDataset(tcsoaServiceFactory.getDataManagementService(), tcsoaServiceFactory.getFileManagementUtility(), drawingItemRevision,
                                modelFile.getAbsolutePath(), "PrtFile", drawingItemRevision.get_item_id(), "ProPrt");
                        TCUtils.createNewProcess(tcsoaServiceFactory.getWorkflowService(), drawingItemRevision.get_item_id(), CISConstants.PROCESS_FXN30_PARTS_BOM_FAST_RELEASE_PROCESS, new ModelObject[]{drawingItemRevision});
                    } catch (Exception e) {
                        LogFactory.get().error("上传数据集文件失败，错误原因：" + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * 根据CIS类型 获取3D模型的文件
     *
     * @param name 3D模型文件名称
     * @param type CIS类型，全局库还是DELL库
     * @return
     */
    private File getModel(String name, CISType type, String category) {
        String path = "";
        if (CISType.CIS.equals(type)) {
            path = defaultPath + category;
        } else if (CISType.CIS_DELL.equals(type)) {
            path = dellPath + category;
        }
        List<File> files = FileUtil.loopFiles(path, new FileFilter() {
            @Override
            public boolean accept(File file) {
                return name.equals(file.getName());
            }
        });
        return CollUtil.isNotEmpty(files) ? files.get(0) : null;
    }

    private Item getItem(String itemId, SavedQueryService savedQueryService) {
        Map<String, Object> queryResults = TCUtils.executeQuery(savedQueryService, "Item_Name_or_ID", new String[]{CISConstants.ATTR_ITEM_ID}, new String[]{itemId});

        ModelObject[] md = (ModelObject[]) queryResults.get("succeeded");
        if (md != null && md.length > 0) {
            return (Item) md[0];
        }
        return null;
    }

    private ItemRevision relateDesign(ModelObject itemRevision, String drawingFileName, SavedQueryService savedQueryService,
                                      DataManagementService dataManagementService) throws Exception {
        if (StrUtil.isBlank(drawingFileName)) {
            return null;
        }
        String itemName = drawingFileName.substring(0, drawingFileName.lastIndexOf("."));
        if (itemName.indexOf("_") == 0) {
            return null;
        }
        // 查詢版本下是否能查詢到關係
        TCUtils.getProperties(dataManagementService, itemRevision, new String[]{"TC_Is_Represented_By", "d9_EnglishDescription"});
        TCUtils.refreshObject(dataManagementService, itemRevision);
        List<ModelObject> objectList = itemRevision.getPropertyObject("TC_Is_Represented_By").getModelObjectListValue();
        if (CollUtil.isNotEmpty(objectList)) {
            for (ModelObject modelObject : objectList) {
                TCUtils.getProperties(dataManagementService,modelObject,new String[]{"item_id"});
                TCUtils.refreshObject(dataManagementService, modelObject);
                String itemId = modelObject.getPropertyObject("item_id").getStringValue();
                if(itemId.equals(itemName)){
                    return (ItemRevision) modelObject;
                }
            }
        }
        Item item = getItem(itemName, savedQueryService);
        if(ObjectUtil.isNull(item)) {
            // 創建對象並關聯關係
            item = TCUtils.createDocument(dataManagementService, itemName, "D9_EDADesign", itemName, "A", new HashMap<>());
        }
        ItemRevision itemLatestRevision = TCUtils.getItemLatestRevision(dataManagementService, item);
        String description = itemRevision.getPropertyObject("d9_EnglishDescription").getStringValue();
        TCUtils.setProperties(dataManagementService, itemLatestRevision, "d9_EnglishDescription", description);
        TCUtils.addRelation(dataManagementService, itemRevision, itemLatestRevision, "TC_Is_Represented_By");
        return itemLatestRevision;
    }
}
