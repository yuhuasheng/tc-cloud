import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.TcReportApp;
import com.foxconn.plm.entity.constants.TCSearchEnum;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.tcreport.drawcountreport.domain.DrawCountEntity;
import com.foxconn.plm.tcreport.drawcountreport.service.DrawCountService;
import com.foxconn.plm.tcreport.mapper.TcProjectMapper;
import com.foxconn.plm.tcreport.reportsearchparams.domain.LovBean;
import com.foxconn.plm.tcreport.schedule.DrawCountSchedule;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.query.SavedQueryService;
import com.teamcenter.soa.client.FileManagementUtility;
import com.teamcenter.soa.client.GetFileResponse;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.*;
import com.teamcenter.soa.exceptions.NotLoadedException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @ClassName: VerifyTest
 * @Description:
 * @Author DY
 * @Create 2023/4/11
 */
//@RunWith(SpringRunner.class)
//@SpringBootTest(classes = TcReportApp.class)
public class VerifyTest {
    private static Log log = LogFactory.get();
    @Resource
    private TCSOAServiceFactory tcsoaServiceFactory;
    @Resource
    private DrawCountService service;
    @Resource
    private DrawCountSchedule schedule;
    @Resource
    private TcProjectMapper tcProjectMapper;

    @Test
    public void test() throws NotLoadedException {
        String str = "D9_MNT_DCNRevision=D9_MNT_DCNForm;100";
        System.out.println(str.split("=")[1].split(";")[1]);
    }

    @Test
    public void drawCount() {
        schedule.drawCount();
    }

    @Test
    public void verifyDocument() throws NotLoadedException {
        tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS2);
        ItemRevision itemRev = (ItemRevision) TCUtils.findObjectByUid(tcsoaServiceFactory.getDataManagementService(), "xdiNf1E7ppJG1D");
        TCUtils.getProperty(tcsoaServiceFactory.getDataManagementService(), new ModelObject[]{itemRev}, new String[]{"owning_user", "owning_group", "d9_ActualUserID"});
        TCUtils.refreshObject(tcsoaServiceFactory.getDataManagementService(), itemRev);
        ModelObject owning_user = itemRev.getPropertyObject("owning_user").getModelObjectValue();
        TCUtils.getProperty(tcsoaServiceFactory.getDataManagementService(), new ModelObject[]{owning_user}, new String[]{"user_name", "user_id"});
        TCUtils.refreshObject(tcsoaServiceFactory.getDataManagementService(), owning_user);
        System.out.println("userName:" + owning_user.getPropertyObject("user_name").getStringValue());
        System.out.println("userId:" + owning_user.getPropertyObject("user_id").getStringValue());
        ModelObject owning_group = itemRev.getPropertyObject("owning_group").getModelObjectValue();
        TCUtils.getProperty(tcsoaServiceFactory.getDataManagementService(), owning_group, "full_name");
        TCUtils.refreshObject(tcsoaServiceFactory.getDataManagementService(), owning_group);
        System.out.println("group:" + owning_group.getPropertyObject("full_name").getStringValue());
        String d9_ActualUserID = itemRev.getPropertyObject("d9_ActualUserID").getStringValue();
        System.out.println("d9_ActualUserID:" + d9_ActualUserID);
        tcsoaServiceFactory.logout();
    }

    @Test
    public void parseEmail() throws Exception {
        tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS2);
        DataManagementService dmService = tcsoaServiceFactory.getDataManagementService();
        FileManagementUtility fileManagementUtility = tcsoaServiceFactory.getFileManagementUtility();
        ItemRevision itemRev = (ItemRevision) TCUtils.findObjectByUid(tcsoaServiceFactory.getDataManagementService(), "wdgRHJzF4VtjAC");
        List<String> list = new ArrayList<>();
        TCUtils.getProperty(tcsoaServiceFactory.getDataManagementService(), itemRev, "IMAN_external_object_link");
        ModelObject[] modelObject = itemRev.getPropertyObject("IMAN_external_object_link").getModelObjectArrayValue();
        if (modelObject == null || modelObject.length <= 0) {
            return;
        }
        Dataset dataset = (Dataset) modelObject[0];
        dmService.refreshObjects(new ModelObject[]{dataset});
        dmService.getProperties(new ModelObject[]{dataset}, new String[]{"ref_list"});
        ModelObject[] dsfiles = dataset.get_ref_list();
        for (ModelObject dsfile : dsfiles) {
            InputStreamReader inputStreamReader = null;
            FileInputStream fileInputStream = null;
            BufferedReader br = null;
            try {
                if (!(dsfile instanceof ImanFile)) {
                    continue;
                }

                ImanFile dsFile = (ImanFile) dsfile;
                dmService.refreshObjects(new ModelObject[]{dsFile});
                dmService.getProperties(new ModelObject[]{dsFile}, new String[]{"original_file_name"});
                String fileName = dsFile.get_original_file_name();
                log.info("【INFO】 fileName: " + fileName);
                // 下载数据集
                GetFileResponse responseFiles = fileManagementUtility.getFiles(new ModelObject[]{dsFile});
                File[] fileinfovec = responseFiles.getFiles();
                File file = fileinfovec[0];
                fileInputStream = new FileInputStream(file);
                inputStreamReader = new InputStreamReader(fileInputStream, "UTF-8");
                br = new BufferedReader(inputStreamReader);
                String line;

                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                    log.info("【INFO】 line: " + line);
                    if (line.startsWith("审核")) {
                        try {
                            String t = line.substring(line.lastIndexOf(";") + 1);
                            t = t.substring(0, t.indexOf("##"));
                            String[] ut = t.split(",");
                            String[] names = line.substring(line.indexOf("##") + 2, line.lastIndexOf("%%")).split(",");
                            String mailTmp = line.substring(line.lastIndexOf("%%") + 2);
                            String[] mails = mailTmp.split(",");
//                            if (ut.length == names.length && ut.length == mails.length) {
//                                for (int k = 0; k < ut.length; k++) {
//                                    list.add(ut[k].trim() + "|" + names[k].trim() + "|" + mails[k].trim());
//                                }
//                            } else if (ut.length != names.length && names.length == mails.length) {
//
//                            }

                            for (int k = 0; k < names.length; k++) {
                                if (ut.length > 0 && ut.length <= k) {
                                    list.add(ut[ut.length - 1].trim() + "|" + names[k].trim() + "|" + mails[k].trim());
                                } else {
                                    list.add(ut[k].trim() + "|" + names[k].trim() + "|" + mails[k].trim());
                                }

                            }

                        } catch (Exception e) {
                            // nothing
                        }
                    }
                }
            } finally {
                try {
                    if (fileInputStream != null) {
                        fileInputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    if (inputStreamReader != null) {
                        inputStreamReader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    if (br != null) {
                        br.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Test
    public void getProjectInfo() throws NotLoadedException {
        tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS2);
        DataManagementService dmService = tcsoaServiceFactory.getDataManagementService();
        FileManagementUtility fileManagementUtility = tcsoaServiceFactory.getFileManagementUtility();
        ItemRevision itemRev = (ItemRevision) TCUtils.findObjectByUid(tcsoaServiceFactory.getDataManagementService(), "RXvR4z$r4VtjAC");
        String projects_list = TCUtils.getPropStr(dmService, itemRev, "projects_list");
        System.out.println("==>> projects_list: " + projects_list);
    }

    @Test
    public void modifyDate() {
        String date = "2021/8/9";
        try {
            Runtime runtime = Runtime.getRuntime();
            String command = "cmd.exe /c date" + " " + (date);
            runtime.exec(command);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testExecuteQuery() throws Exception {
        Pattern pattern = Pattern.compile("^.+?_P\\d+$"); // 以_P+数字结尾
        Matcher matcher = pattern.matcher("test_p2142".toUpperCase()); // 判断itemID是否以是"_P+数字结尾"
        System.out.println(matcher.matches());
    }

    public static boolean areListsEqual(List<String> list1, List<String> list2) {
        return list1.equals(list2);
    }
    public static boolean areListsEqualIgnoringOrder(List<String> list1, List<String> list2) {
        return list1.stream().sorted().collect(Collectors.toList()).equals(list2.stream().sorted().collect(Collectors.toList()));
    }

    @Test
    public void testListEqual() {
        String str = "P1979,P2042";
        String[] split = str.split(",");
        List<String> list1 = List.of("1", "2", "3");
        List<String> list2 = List.of("1", "2", "3");
        List<String> list3 = List.of("3", "2", "1");

        List<String> collect = list1.stream().sorted().collect(Collectors.toList());
        List<String> collect1 = list2.stream().sorted().collect(Collectors.toList());

        System.out.println("list1 and list2 are equal: " + areListsEqual(list1, list2)); // true
        System.out.println("list1 and list3 are equal: " + areListsEqual(list1, list3)); // false

        System.out.println("list1 and list2 are equal (ignoring order): " + areListsEqualIgnoringOrder(list1, list2)); // true
        System.out.println("list1 and list3 are equal (ignoring order): " + areListsEqualIgnoringOrder(list1, list3)); // true
    }

    @Test
    public void testMap() {
        Map param = new HashMap();
        param.put("a","123");
        param.put("a","456");

        System.out.println(param.get("a"));
    }
}
