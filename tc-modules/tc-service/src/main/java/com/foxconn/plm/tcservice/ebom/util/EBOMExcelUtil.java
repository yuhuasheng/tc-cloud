package com.foxconn.plm.tcservice.ebom.util;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.foxconn.plm.entity.constants.TCPropName;
import com.foxconn.plm.entity.response.MegerCellEntity;
import com.foxconn.plm.tcservice.ebom.domain.MergedRegionInfo;
import com.foxconn.plm.utils.string.StringUtil;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.util.ReflectionUtils;


/**
 * @author Robert
 */
public class EBOMExcelUtil {
    /**
     * Robert 2022��4��11��
     *
     * @param fileName
     * @return
     */
    public Workbook getWorkbook(String fileName) {
        Workbook workbook = null;
        InputStream in = null;
        InputStream inputStream=null;
        try {
            inputStream= this.getClass().getClassLoader().getResourceAsStream(fileName);
            in = Objects.requireNonNull(inputStream, "文件未找到" + fileName);
            if (Pattern.matches(".*(xls|XLS)$", fileName)) {
                workbook = new HSSFWorkbook(in);
            } else {
                workbook = new XSSFWorkbook(in);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            }catch (IOException e){}
            try {
                if (in != null) {
                    in.close();
                }
            }catch (IOException e){}
        }
        return workbook;
    }

    public void setRowCellVaule(Object bean, Sheet sheet, CellStyle cellStyle)
            throws IllegalArgumentException, IllegalAccessException {
        if (bean != null) {
            Field[] fields = bean.getClass().getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                ReflectionUtils.makeAccessible(fields[i]);
                TCPropName tcProp = fields[i].getAnnotation(TCPropName.class);
                if (tcProp != null) {
                    int row = tcProp.row();
                    int col = tcProp.cell();
                    if (row > -1 && col > -1) {
                        String val = (String) fields[i].get(bean);
                        Cell cell = sheet.getRow(row).getCell(col);
                        cell.setCellValue(val);
                        if (cellStyle != null) {
                            cell.setCellStyle(cellStyle);
                        }
                    }
                }
            }
        }
    }

    public void setMerged(MergedRegionInfo mrInfo, Class cls, Sheet sheet, int row)
            throws NoSuchFieldException, SecurityException {
        String fieldName = mrInfo.getFieldName();
        int lenth = mrInfo.getLenth();
        Field field = cls.getDeclaredField(fieldName);
        ReflectionUtils.makeAccessible(field);
        TCPropName tcProp = field.getAnnotation(TCPropName.class);
        if (tcProp != null) {
            int cell = tcProp.cell();
            if (lenth > 0) {
                sheet.addMergedRegion(new CellRangeAddress(row, row + lenth, cell, cell));
            }
        }
    }

    public void setCellValue(List beans, int startRow, Sheet sheet, CellStyle cellStyle)
            throws IllegalArgumentException, IllegalAccessException {
        for (Object bean : beans) {
            Row row = sheet.createRow(startRow);
            startRow++;
            if (bean != null) {
                Field[] fields = bean.getClass().getDeclaredFields();
                for (int i = 0; i < fields.length; i++) {
                    ReflectionUtils.makeAccessible(fields[i]);
                    TCPropName tcProp = fields[i].getAnnotation(TCPropName.class);
                    if (tcProp != null) {
                        int col = tcProp.cell();
                        if (col >= 0) {
                            Cell cell = row.createCell(tcProp.cell());
                            String val = (String) fields[i].get(bean);
                            cell.setCellValue(val);
                            if (cellStyle != null) {
                                cell.setCellStyle(cellStyle);
                            }
                        }
                    }
                }
            }
        }
    }

    public void setRichCellValue(List beans, int startRow, Sheet sheet, CellStyle cellStyle)
            throws IllegalArgumentException, IllegalAccessException {
        Pattern p = Pattern.compile("(?<=<del>).*(?=</del>)");
        Pattern p2 = Pattern.compile("(?<=<add>).*(?=</add>)");
        for (Object bean : beans) {
            Row row = sheet.createRow(startRow);
            startRow++;
            if (bean != null) {
                Field[] fields = bean.getClass().getDeclaredFields();
                for (int i = 0; i < fields.length; i++) {
                    ReflectionUtils.makeAccessible(fields[i]);
                    TCPropName tcProp = fields[i].getAnnotation(TCPropName.class);
                    if (tcProp != null) {
                        int col = tcProp.cell();
                        if (col >= 0) {
                            Cell cell = row.createCell(tcProp.cell());
                            if (cellStyle != null) {
                                Font ft = sheet.getWorkbook().createFont();
                                ft.setFontHeightInPoints((short) 12);
                                ft.setFontName("Microsoft YaHei Light");
                                cellStyle.setFont(ft);
                                cell.setCellStyle(cellStyle);
                            }
                            String val = (String) fields[i].get(bean);
                            if (val != null && val.length() > 0) {
                                Matcher m = p.matcher(val);
                                if (m.find()) {
                                    Font ft = sheet.getWorkbook().createFont();
                                    ft.setFontHeightInPoints((short) 12);
                                    ft.setFontName("Microsoft YaHei Light");
                                    ft.setStrikeout(true);
                                    ft.setColor(Font.COLOR_RED);
                                    RichTextString textString = new XSSFRichTextString(
                                            val.replace("<del>", "").replace("</del>", ""));
                                    int start = m.start() - 5;
                                    int end = m.end() - 5;
                                    textString.applyFont(start, end, ft);
                                    cell.setCellValue(textString);
                                    continue;
                                }
                                Matcher m2 = p2.matcher(val);
                                if (m2.find()) {
                                    Font ft = sheet.getWorkbook().createFont();
                                    ft.setFontHeightInPoints((short) 12);
                                    ft.setFontName("Microsoft YaHei Light");
                                    ft.setColor(Font.COLOR_RED);
                                    RichTextString textString = new XSSFRichTextString(
                                            val.replace("<add>", "").replace("</add>", ""));
                                    int start = m2.start() - 5;
                                    int end = m2.end() - 5;
                                    textString.applyFont(start, end, ft);
                                    cell.setCellValue(textString);
                                    continue;
                                }
                                cell.setCellValue(val);
                            } else {
                                cell.setCellValue(val);
                            }
                        }
                    }
                }
            }
        }
    }

    public void setCellValue2(List beans, int startRow, int collength, Sheet sheet, CellStyle cellStyle)
            throws IllegalArgumentException, IllegalAccessException {
        for (Object bean : beans) {
            Row row = sheet.createRow(startRow);
            for (int i = 0; i < collength; i++) {
                row.createCell(i);
                row.getCell(i).setCellStyle(cellStyle);
            }
            startRow++;
            if (bean != null) {
                Field[] fields = bean.getClass().getDeclaredFields();
                for (int i = 0; i < fields.length; i++) {
                    ReflectionUtils.makeAccessible(fields[i]);
                    TCPropName tcProp = fields[i].getAnnotation(TCPropName.class);
                    if (tcProp != null) {
                        int col = tcProp.cell();
                        if (col >= 0) {
                            // Cell cell = row.createCell(tcProp.cell());
                            Cell cell = row.getCell(tcProp.cell());
                            String val = "";
                            // System.out.println(fields[i].getType());
                            if (fields[i].getType() == Integer.class) {
                                if (fields[i].get(bean) != null) {
                                    val = String.valueOf(fields[i].get(bean));
                                }
                            } else {
                                val = (String) fields[i].get(bean);
                            }
                            if (val != null) {
                                cell.setCellValue(val);
                            }
                            // if (cellStyle != null)
                            // {
                            // cell.setCellStyle(cellStyle);
                            // }
                        }
                    }
                }
            }
        }
    }

    /**
     * ����ĳһ�е�Ԫ�������
     *
     * @param sheet
     * @param startRow
     * @param cellNum
     * @param value
     */
    public void updateCellValue(Sheet sheet, int startRow, int cellNum, String value) {
        int count = sheet.getLastRowNum(); // ��ȡ���һ��
        for (int i = startRow; i <= count; i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                break;
            }
            Cell cell = row.getCell(cellNum);
            if (cell == null) {
                break;
            }
            if (StringUtil.isEmpty(removeBlank(getCellValue(cell)))) {
                continue;
            }
            if (cell.getCellStyle() != null) {
                cell.setCellValue(value);
            }
        }
    }

    public void setCellValue(Sheet sheet, int rowIndex, int colIndex, String value) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            return;
        }

        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            return;
        }

        cell.setCellValue(value);
    }

    public static CellStyle getCellStyle(Workbook workbook) {
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setBorderBottom(BorderStyle.THIN); // �±߿�
        cellStyle.setBorderLeft(BorderStyle.THIN);// ��߿�
        cellStyle.setBorderTop(BorderStyle.THIN);// �ϱ߿�
        cellStyle.setBorderRight(BorderStyle.THIN);// �ұ߿�
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        return cellStyle;
    }

    public CellStyle getCellStyleFont(Workbook wb) {
        CellStyle style = getCellStyle(wb);
        Font font = wb.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setFontName("宋体");
        font.setBold(true);
        style.setFont(font);
        style.setWrapText(true);
        return style;
    }

    public CellStyle getCellStyle2(Workbook wb) {
        CellStyle style = getCellStyle(wb);
        Font font = wb.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setFontName("宋体");
        style.setFont(font);
        style.setWrapText(true);
        return style;
    }

    /**
     * ��ȡ��Ԫ������
     *
     * @param cell
     * @return
     */
    public static String getCellValue(Cell cell) {
        String value = "";
        if (null == cell) {
            return value;
        }
        switch (cell.getCellType().name()) {
            case "STRING":
                value = cell.getRichStringCellValue().getString();
                break;
            case "NUMERIC":
                value = cell.getNumericCellValue() + "";
                break;
            case "BOOLEAN":
                value = String.valueOf(cell.getBooleanCellValue());
                break;
            case "BLANK":
                value = null;
                break;
            case "ERROR":
                value = null;
                break;
            case "FORMULA":
                value = cell.getCellFormula() + "";
                break;
            default:
                value = cell.toString();
                break;
        }
        return value;
    }

    /**
     * ȥ���ַ����е�ǰ��ո񡢻س������з����Ʊ�� value
     *
     * @return
     */
    public static String removeBlank(String value) {
        String result = "";
        if (value != null) {
            Pattern p = Pattern.compile("|\t|\r|\n");
            Matcher m = p.matcher(value);
            result = m.replaceAll("");
            result = result.trim();
        } else {
            result = value;
        }
        return result;
    }

    public int getColumIntByString(String strColum) {
        int intColum = 0;
        int lenth = strColum.length();
        for (int i = 0; i < lenth; i++) {
            // 公式：26^指数*字母的位数
            intColum = intColum + (int) (Math.pow(26, lenth - 1 - i) * (strColum.charAt(i) - 64));
        }
        return (intColum - 1);
    }

    public List<MegerCellEntity> scanMegerCells(List resps, String fliedName, int startLine)
            throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException {
        List<MegerCellEntity> megerList = new ArrayList<>();
        int startRow = 0;
        String lastValue = "";
        for (int i = 0; i < resps.size(); i++) {
            Object obj = resps.get(i);
            Class c = obj.getClass();
            Field field = c.getDeclaredField(fliedName);
            ReflectionUtils.makeAccessible(field);
            String value = "";
            if (field.getType() != String.class) {
                value = String.valueOf(field.get(obj));
            } else {
                value = (String) field.get(obj);
            }

            if ("".equals(lastValue)) {
                startRow = i + 1;
                lastValue = value;
                continue;
            }
            if (i == resps.size() - 1) {
                if (lastValue.equals(value)) {
                    megerList.add(new MegerCellEntity(startRow + startLine, i + 1 + startLine));
                }
            }
            if (!lastValue.equals(value)) {
                lastValue = value;
                if (startRow != i) {
                    megerList.add(new MegerCellEntity(startRow + startLine, i + startLine));
                }
                startRow = i + 1;
            }
        }
        return megerList;
    }
}
