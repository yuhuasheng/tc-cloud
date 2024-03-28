package com.foxconn.dp.plm.hdfs;


import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@SpringBootApplication(scanBasePackages = {"com.foxconn"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.foxconn.plm"})
//@EnableFeignClients()
@MapperScan("com.foxconn.dp.plm.hdfs.dao")
//@EnableScheduling
public class TcHDFSApp {

    public static void main(String[] args) {
        try {
            SpringApplication.run(TcHDFSApp.class, args);
        } catch (Exception e) {
            e.printStackTrace();
        }

//        makeDeptSpasToTC("C:\\Users\\Oz\\Downloads\\文档管理系统部门与SPAS部门对应关系--Update20220813 (1).xlsx");

//        makeBu("C:\\Users\\Oz\\Downloads\\bu.properties");

    }

//    public static void makeBu(String txtPath) throws Exception {
//
//        List<String> list = readUserTxt(txtPath,0);
//        list.removeIf(Util::isEmptyString);
//        System.out.println();
//    }

//    public static List<String> readUserTxt(String filePath, int skip) throws IOException {
//        List<String> list = new ArrayList<>();
//        try (FileInputStream fin = new FileInputStream(filePath); InputStreamReader reader = new InputStreamReader(fin)) {
//            BufferedReader buffReader = new BufferedReader(reader);
//            String strTmp;
//            int total = 0;
//            while ((strTmp = buffReader.readLine()) != null) {
//                total++;
//                if (total > skip) {
//                    list.add(strTmp);
//                }
//            }
//            buffReader.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return list;
//    }
//
//    public static void makeDeptSpasToTC(String xlsPath) throws Exception {
//        List<String> list = new ArrayList<>();
//        XSSFWorkbook workbook = null;
//        //读取第一个工作簿
//        try {
//            workbook = new XSSFWorkbook(new FileInputStream(xlsPath));
//            XSSFSheet sheet = workbook.getSheet("Dell");
//            for (int i = 1; i <= 999; i++) {
//                XSSFRow row = sheet.getRow(i);
//                if (isEmptyRow(row)) {
//                    break;
//                }
//                String dept = row.getCell(1).getStringCellValue();
//                String stringCellValue = sheet.getRow(i).getCell(2).getStringCellValue();
//                String hasTC = sheet.getRow(i).getCell(3).getStringCellValue();
//                String[] split = stringCellValue.split("\n");
//                for (String s : split) {
//                    list.add(dept + "<==>" + s + "<==>" + hasTC);
//                }
//            }
//
//            sheet = workbook.getSheet("HP");
//            for (int i = 1; i <= 999; i++) {
//                XSSFRow row = sheet.getRow(i);
//                if (isEmptyRow(row)) {
//                    break;
//                }
//                String dept = row.getCell(0).getStringCellValue();
//                String stringCellValue = row.getCell(1).getStringCellValue();
//                String hasTC = row.getCell(2).getStringCellValue();
//                String[] split = stringCellValue.split("\n");
//                for (String s : split) {
//                    list.add(dept + "<==>" + s + "<==>" + hasTC);
//                }
//            }
////
//            sheet = workbook.getSheet("Lenovo");
//            for (int i = 1; i <= 999; i++) {
//                XSSFRow row = sheet.getRow(i);
//                if (isEmptyRow(row)) {
//                    break;
//                }
//                String dept = row.getCell(0).getStringCellValue();
//                String stringCellValue = sheet.getRow(i).getCell(1).getStringCellValue();
//                String hasTC = sheet.getRow(i).getCell(2).getStringCellValue();
//                String[] split = stringCellValue.split("\n");
//                for (String s : split) {
//                    list.add(dept + "<==>" + s + "<==>" + hasTC);
//                }
//            }
//
//            sheet = workbook.getSheet("Lenovo L5");
//            for (int i = 1; i <= 999; i++) {
//                XSSFRow row = sheet.getRow(i);
//                if (isEmptyRow(row)) {
//                    break;
//                }
//                String dept = sheet.getRow(i).getCell(0).getStringCellValue();
//                String stringCellValue = sheet.getRow(i).getCell(1).getStringCellValue();
//                String hasTC = sheet.getRow(i).getCell(2).getStringCellValue();
//                String[] split = stringCellValue.split("\n");
//                for (String s : split) {
//                    list.add(dept + "<==>" + s + "<==>" + hasTC);
//                }
//            }
//
//            sheet = workbook.getSheet("MNT");
//            for (int i = 1; i <= 999; i++) {
//                XSSFRow row = sheet.getRow(i);
//                if (isEmptyRow(row)) {
//                    break;
//                }
//                String dept = sheet.getRow(i).getCell(0).getStringCellValue();
//                String stringCellValue = sheet.getRow(i).getCell(1).getStringCellValue();
//                String hasTC = sheet.getRow(i).getCell(2).getStringCellValue();
//                String[] split = stringCellValue.split("\n");
//                for (String s : split) {
//                    list.add(dept + "<==>" + s + "<==>" + hasTC);
//                }
//            }
//
//            sheet = workbook.getSheet("PRT");
//            for (int i = 1; i <= 999; i++) {
//                XSSFRow row = sheet.getRow(i);
//                if (isEmptyRow(row)) {
//                    break;
//                }
//                String dept = sheet.getRow(i).getCell(0).getStringCellValue();
//                String stringCellValue = sheet.getRow(i).getCell(1).getStringCellValue();
//                String hasTC = sheet.getRow(i).getCell(2).getStringCellValue();
//                String[] split = stringCellValue.split("\n");
//                for (String s : split) {
//                    list.add(dept + "<==>" + s + "<==>" + hasTC);
//                }
//            }
//
//            int id = 1255;
//            for (String s : list) {
//                String[] split = s.split("<==>");
//                String value = split[1];
//                String code = split[0];
//                code += "<->" + (split[2].equals("是") ? 1 : 0);
//                id++;
//                System.out.printf("INSERT INTO CONFIG (ID,NAME,VALUE,SORT,CODE) VALUES (%d,'DeptSpasToTC','%s',(select max(sort)+1 from config c where c.name = 'DeptSpasToTC'),'%s');%n", id, value, code);
//            }
//
//        } finally {
//            if (workbook != null) {
//                workbook.close();
//            }
//        }
//    }
//
//    private static boolean isEmptyRow(XSSFRow row) {
//        if (row == null) {
//            return true;
//        }
//        for (int i = 0; i < 3; i++) {
//            XSSFCell cell = row.getCell(i);
//            if (cell == null) {
//                continue;
//            }
//            if (!cell.getStringCellValue().isEmpty()) {
//                return false;
//            }
//        }
//        return true;
//    }


}
