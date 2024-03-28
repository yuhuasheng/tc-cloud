/**
 *
 */
package com.foxconn.plm.utils.excel;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import cn.hutool.poi.excel.ExcelReader;
import com.foxconn.plm.entity.constants.TCPropName;
import com.foxconn.plm.entity.response.MegerCellEntity;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.formula.functions.T;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressBase;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**

 */
public class ExcelUtil {


    public static XSSFWorkbook getWorkbook(File f) throws IOException {
        FileInputStream in = null;
        XSSFWorkbook wb = null;
        try {
            in = new FileInputStream(f);
            wb = new XSSFWorkbook(in);
        } catch (FileNotFoundException e0) {
            e0.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
        return wb;
    }


    public static XSSFSheet getSheet(XSSFWorkbook wb, String name) {
        return wb.getSheet(name);
    }


    public static Workbook getLocalWorkbook(String fileName) {
        Workbook workbook = null;
        InputStream in = null;
        try {
            in = new FileInputStream(new File(fileName));
            if (Pattern.matches(".*(xls|XLS)$", fileName)) {
                workbook = new HSSFWorkbook(in);
            } else {
                workbook = new XSSFWorkbook(in);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        return workbook;
    }


    public static Workbook getWorkbookNew(String fileName) {
        Workbook workbook = null;
        InputStream in = null;
        try {
            ClassPathResource classPathResource = new ClassPathResource(fileName);
            in = classPathResource.getInputStream();
            if (Pattern.matches(".*(xls|XLS)$", fileName)) {
                workbook = new HSSFWorkbook(in);
            } else {
                workbook = new XSSFWorkbook(in);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return workbook;
    }


    /**
     * 检查文件夹是否存在，如果不存在创建文件夹
     *
     * @param filePath
     */
    private static void checkDirs(String filePath) {
        File f = new File(filePath);
        if (f.isDirectory()) {
            if (!f.exists()) {
                f.mkdirs();
            }
            return;
        }
        f = f.getAbsoluteFile().getParentFile();
        if (!f.exists()) {
            f.mkdirs();
        }
    }

    /**
     * 输出xcel文件
     *
     * @param wb
     * @param filePath 文件路径
     * @throws Exception
     */
    public static boolean writeExcel(Workbook wb, String filePath) {
        checkDirs(filePath);
        OutputStream os = null;
        try {
            File file = new File(filePath);
            os = new FileOutputStream(file);
            wb.write(os);
            return true;
        } catch (Exception e) {

            return false;
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
            } catch (Exception e) {
            }
        }
    }


    public static Cell setCellStyleAndValue(Cell cell, Object value, XSSFCellStyle cellStyle) {
        cell.setCellStyle(cellStyle);
        if (value instanceof String) {
            cell.setCellValue(((String) value));
        } else if (value instanceof Integer) {
            cell.setCellValue(((Integer) value));
        } else if (value instanceof Double) {
            cell.setCellValue(((Double) value));
        } else if (value instanceof Date) {
            cell.setCellValue(((Date) value));
        } else if (value instanceof Calendar) {
            cell.setCellValue(((Calendar) value));
        } else if (value instanceof RichTextString) {
            cell.setCellValue(((RichTextString) value));
        } else if (value instanceof Boolean) {
            cell.setCellValue(((Boolean) value));
        }
        return cell;
    }

    public static int getNotBlankRowCount(Workbook wb, int sheetIx) {
        Sheet sheet = wb.getSheetAt(sheetIx);
        CellReference cellReference = new CellReference("A1");
        boolean flag = false;

        for (int i = cellReference.getRow(); i <= sheet.getLastRowNum(); ) {
            Row r = sheet.getRow(i);
            if (r == null && (sheet.getLastRowNum() >= i + 1)) {
                // 如果是空行(即没有任何数据、格式)，直接把它以下的数据往上移动
                sheet.shiftRows(i + 1, sheet.getLastRowNum(), -1);
                continue;
            }

            flag = false;

            if (r != null) {
                for (Cell c : r) {
                    if (c.getCellType() != CellType.BLANK) {
                        flag = true;
                        break;
                    }
                }
            }

            if (flag) {
                i++;
                continue;
            } else {
                //如果是空白行(即可能没有数据，但是有一定格式)
                if (i == sheet.getLastRowNum()) {//如果到了最后一行，直接将那一行remove掉
                    if (r != null)
                        sheet.removeRow(r);
                    else {
                        i++;
                        continue;
                    }
                } else//如果还没到最后一行，则数据往上移一行
                    sheet.shiftRows(i + 1, sheet.getLastRowNum(), -1);
            }
        }

        return sheet.getLastRowNum() > 0 ? sheet.getLastRowNum() + 1 : sheet.getLastRowNum();
    }

    /**
     * 返回sheet 中的行数
     *
     * @param sheetIx 指定 Sheet 页，从 0 开始
     * @return
     */
    public static int getRowCount(Workbook wb, int sheetIx) {
        Sheet sheet = wb.getSheetAt(sheetIx);
        if (sheet.getPhysicalNumberOfRows() == 0) {
            return 0;
        }
        return sheet.getLastRowNum() + 1;
    }

    /**
     * 返回sheet 中的行数
     *
     * @return
     */
    public static int getRowCount(Workbook wb, String sheetName) {
        Sheet sheet = wb.getSheet(sheetName);
        if (sheet.getPhysicalNumberOfRows() == 0) {
            return 0;
        }
        return sheet.getLastRowNum() + 1;
    }

    /**
     * 返回所在行的列数
     *
     * @param sheetIx  指定 Sheet 页，从 0 开始
     * @param rowIndex 指定行，从0开始
     * @return 返回-1 表示所在行为空
     */
    public static int getColumnCount(Workbook wb, int sheetIx, int rowIndex) {
        Sheet sheet = wb.getSheetAt(sheetIx);
        Row row = sheet.getRow(rowIndex);
        return row == null ? -1 : row.getLastCellNum();

    }

    /**
     * 返回指定行的值的集合
     *
     * @param sheetIx  指定 Sheet 页，从 0 开始
     * @param rowIndex 指定行，从0开始
     * @return
     */
    public List<String> getRowValue(Workbook wb, int sheetIx, int rowIndex) {
        Sheet sheet = wb.getSheetAt(sheetIx);
        Row row = sheet.getRow(rowIndex);
        List<String> list = new ArrayList<String>();
        if (row == null) {
            list.add(null);
        } else {
            for (int i = 0; i < row.getLastCellNum(); i++) {
                list.add(getCellValueToString(row.getCell(i)));
            }
        }
        return list;
    }

    /**
     * 读取指定sheet 页指定行数据
     *
     * @param sheetIx 指定 sheet 页，从 0 开始
     * @param start   指定开始行，从 0 开始
     * @param end     指定结束行，从 0 开始
     * @return
     * @throws Exception
     */
    public static List<List<String>> read(Workbook wb, int sheetIx, int start, int end) throws Exception {
        Sheet sheet = wb.getSheetAt(sheetIx);
        List<List<String>> list = new ArrayList<List<String>>();

//		System.out.println(getRowCount(wb, sheetIx));
        if (end > getRowCount(wb, sheetIx)) {
            end = getRowCount(wb, sheetIx);
        }

        int cols = sheet.getRow(0).getLastCellNum(); // 第一行总列数
        FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();

        for (int i = start; i <= end; i++) {
            List<String> rowList = new ArrayList<String>();
            Row row = sheet.getRow(i);
            for (int j = 0; j < cols; j++) {
                if (row == null) {
                    rowList.add(null);
                    continue;
                }
                if (isMergedRegion(sheet, i, j)) {
                    rowList.add(getMergedRegionValue(sheet, i, j, evaluator));
                } else {
                    rowList.add(getCellValueToString(row.getCell(j)));
                }
            }
            list.add(rowList);
        }

        return list;
    }


    public static List<List<String>> read(Sheet sheet, int start, int end) throws Exception {
        List<List<String>> list = new ArrayList<List<String>>();
        if (end > sheet.getLastRowNum() || end == -1) {
            end = sheet.getLastRowNum();
        }
        for (int i = start; i <= end; i++) {
            List<String> rowList = new ArrayList<String>();
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            for (int j = 0; j < row.getLastCellNum(); j++) {
                Cell cell = row.getCell(j);
                if (cell != null) {
                    rowList.add(getCellValue(row.getCell(j)) + "");
                }
            }
            list.add(rowList);
        }
        return list;
    }


    /**
     * 读取指定sheet 页指定行数据
     *
     * @param start 指定开始行，从 0 开始
     * @param end   指定结束行，从 0 开始
     * @return
     * @throws Exception
     */
    public static List<List<String>> read(Workbook wb, String sheetName, int start, int end) throws Exception {
        Sheet sheet = wb.getSheet(sheetName);
        List<List<String>> list = new ArrayList<List<String>>();

//		System.out.println(getRowCount(wb, sheetIx));
        if (end > getRowCount(wb, sheetName)) {
            end = getRowCount(wb, sheetName);
        }

        int cols = sheet.getRow(0).getLastCellNum(); // 第一行总列数
        FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();

        for (int i = start; i <= end; i++) {
            List<String> rowList = new ArrayList<String>();
            Row row = sheet.getRow(i);
            for (int j = 0; j < cols; j++) {
                if (row == null) {
                    rowList.add(null);
                    continue;
                }
                if (isMergedRegion(sheet, i, j)) {
                    rowList.add(getMergedRegionValue(sheet, i, j, evaluator));
                } else {
                    rowList.add(getCellValueToString(row.getCell(j)));
                }
            }
            list.add(rowList);
        }

        return list;
    }

    /**
     * 读取指定sheet 页指定行数据
     *
     * @return
     * @throws Exception
     */
    public static List<List<String>> read(Workbook wb, String sheetName, int startRow, int startCol, int endRow, int endCol) throws Exception {
        Sheet sheet = wb.getSheet(sheetName);
        List<List<String>> list = new ArrayList<List<String>>();

//		System.out.println(getRowCount(wb, sheetIx));
        if (endCol > getRowCount(wb, sheetName)) {
            endCol = getRowCount(wb, sheetName);
        }

//		int cols = sheet.getRow(0).getLastCellNum(); // 第一行总列数
//		FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
        FormulaEvaluator evaluator = new XSSFFormulaEvaluator((XSSFWorkbook) wb);

        for (int i = startRow; i <= endRow; i++) {
            List<String> rowList = new ArrayList<String>();
            Row row = sheet.getRow(i);
            for (int j = startCol; j < endCol; j++) {
                if (row == null) {
                    rowList.add(null);
                    continue;
                }
                if (isMergedRegion(sheet, i, j)) {
                    rowList.add(getMergedRegionValue(sheet, i, j, evaluator));
                } else {
                    rowList.add(getCellValueToString(row.getCell(j), evaluator));
                }
            }

            System.out.println(rowList.toString());
            list.add(rowList);
        }

        return list;
    }


    /**
     * 返回 row 和 column 位置的单元格值
     *
     * @param sheetIx  指定 Sheet 页，从 0 开始
     * @param rowIndex 指定行，从0开始
     * @param colIndex 指定列，从0开始
     * @return
     */
    public static String getValueAt(Workbook wb, int sheetIx, int rowIndex, int colIndex) {
        Sheet sheet = wb.getSheetAt(sheetIx);
        return getCellValueToString(sheet.getRow(rowIndex).getCell(colIndex));
    }

    /**
     * 转换单元格的类型为String 默认的 <br>
     * 默认的数据类型：CELL_TYPE_BLANK(3), CELL_TYPE_BOOLEAN(4),
     * CELL_TYPE_ERROR(5),CELL_TYPE_FORMULA(2), CELL_TYPE_NUMERIC(0),
     * CELL_TYPE_STRING(1)
     *
     * @param cell
     * @return
     */
    public static String getCellValueToString(Cell cell) {
        String strCell = "";
        if (cell == null) {
            return null;
        }
        switch (cell.getCellTypeEnum()) {
            case BOOLEAN:
                strCell = String.valueOf(cell.getBooleanCellValue());
                break;
            case NUMERIC:
                if (HSSFDateUtil.isCellDateFormatted(cell)) {
                    if (HSSFDateUtil.isCellDateFormatted(cell)) { // 判断日期类型
                        strCell = new SimpleDateFormat("yyyy-MM-dd").format(cell.getDateCellValue());
                    } else { // 否
                        strCell = new DecimalFormat("#.######").format(cell.getNumericCellValue());
                    }
                    if ("".equals(strCell) || strCell == null) {
                        strCell = "0";
                    }

                    break;
                }
                // 不是日期格式，则防止当数字过长时以科学计数法显示
                cell.setCellType(CellType.STRING);
                strCell = cell.toString();
                break;
            case STRING:
                strCell = cell.getStringCellValue();
                break;
            case FORMULA:
                strCell = cell.getCellFormula() + "";
                break;
            default:
                break;
        }
        return strCell;
    }


    public static String getCellValue(Sheet sheet, Cell cell, String dateFormat) {
        int row = cell.getRowIndex();
        int column = cell.getColumnIndex();
        int sheetMergeCount = sheet.getNumMergedRegions();
        for (int i = 0; i < sheetMergeCount; i++) {
            CellRangeAddress ca = sheet.getMergedRegion(i);
            int firstColumn = ca.getFirstColumn();
            int lastColumn = ca.getLastColumn();
            int firstRow = ca.getFirstRow();
            int lastRow = ca.getLastRow();

            if (row >= firstRow && row <= lastRow) {
                if (column >= firstColumn && column <= lastColumn) {
                    Row fRow = sheet.getRow(firstRow);
                    Cell fCell = fRow.getCell(firstColumn);
                    return getCellValueToString(fCell, dateFormat);
                }
            }
        }
        return getCellValue(cell) + "";
    }


    public static String getCellValueToString(Cell cell, String dateFormat) {
        String strCell = "";
        if (cell == null) {
            return null;
        }
        switch (cell.getCellTypeEnum()) {
            case BOOLEAN:
                strCell = String.valueOf(cell.getBooleanCellValue());
                break;
            case NUMERIC:
                if (HSSFDateUtil.isCellDateFormatted(cell)) {
                    if (HSSFDateUtil.isCellDateFormatted(cell)) { // 判断日期类型
                        strCell = new SimpleDateFormat(dateFormat).format(cell.getDateCellValue());
                    } else { // 否
                        strCell = new DecimalFormat("#.######").format(cell.getNumericCellValue());
                    }
                    if ("".equals(strCell) || strCell == null) {
                        strCell = "0";
                    }

                    break;
                }
                // 不是日期格式，则防止当数字过长时以科学计数法显示
                cell.setCellType(CellType.STRING);
                strCell = cell.toString();
                break;
            case STRING:
                strCell = cell.getStringCellValue();
                break;
            case FORMULA:
                strCell = cell.getCellFormula() + "";
                break;
            default:
                break;
        }
        return strCell;
    }

    public static String getCellValueToString(Cell cell, FormulaEvaluator evaluator) {
        String strCell = "";
        if (cell == null) {
            return null;
        }
        switch (cell.getCellTypeEnum()) {
            case BOOLEAN:
                strCell = String.valueOf(cell.getBooleanCellValue());
                break;
            case NUMERIC:
                if (HSSFDateUtil.isCellDateFormatted(cell)) {
                    if (HSSFDateUtil.isCellDateFormatted(cell)) { // 判断日期类型
                        strCell = new SimpleDateFormat("yyyy-MM-dd").format(cell.getDateCellValue());
                    } else { // 否
                        strCell = new DecimalFormat("#.######").format(cell.getNumericCellValue());
                    }
                    if ("".equals(strCell) || strCell == null) {
                        strCell = "0";
                    }

                    break;
                }
                // 不是日期格式，则防止当数字过长时以科学计数法显示
                cell.setCellType(CellType.STRING);
                strCell = cell.toString();
                float b1 = Float.parseFloat(strCell);
                int num = (int) b1;
                strCell = String.valueOf(num);
                break;
            case STRING:
                strCell = cell.getStringCellValue();
                break;
            case FORMULA:
                String value = "";
                switch (cell.getCachedFormulaResultType()) {
                    case NUMERIC:
                        CellValue evaluate = evaluator.evaluate(cell);
                        value = evaluate.formatAsString();
                        System.out.println("Last evaluated as: " + value);
                        break;
                    case STRING:
                        value = evaluator.evaluate(cell).formatAsString();
                        System.out.println("Last evaluated as \"" + value + "\"");
                        break;
                }
                strCell = value;
                break;
            default:
                break;
        }
        return strCell;
    }

    public static String getCellValueToString(XSSFCell cell) {
        String strCell = "";
        if (cell == null) {
            return null;
        }
        switch (cell.getCellTypeEnum()) {
            case BOOLEAN:
                strCell = String.valueOf(cell.getBooleanCellValue());
                break;
            case NUMERIC:
                if (HSSFDateUtil.isCellDateFormatted(cell)) {
                    Date date = cell.getDateCellValue();
                    strCell = date.toString();

                    break;
                }
                // 不是日期格式，则防止当数字过长时以科学计数法显示
                cell.setCellType(CellType.STRING);
                strCell = cell.toString();
                break;
            case STRING:
                strCell = cell.getStringCellValue();
                break;
            default:
                break;
        }
        return strCell;
    }


    public static int getColumIntByString(String strColum) {
        int intColum = 0;
        int lenth = strColum.length();
        for (int i = 0; i < lenth; i++) {
            // 公式：26^指数*字母的位数
            intColum = intColum + (int) (Math.pow(26, lenth - 1 - i) * (strColum.charAt(i) - 64));
        }
        return (intColum - 1);
    }

    /**
     * 判断指定的单元格是否是合并单元格
     *
     * @param sheet
     * @param row    行下标
     * @param column 列下标
     * @return
     */
    public static boolean isMergedRegion(Sheet sheet, int row, int column) {
        int sheetMergeCount = sheet.getNumMergedRegions();
        for (int i = 0; i < sheetMergeCount; i++) {
            CellRangeAddress range = sheet.getMergedRegion(i);
            int firstColumn = range.getFirstColumn();
            int lastColumn = range.getLastColumn();
            int firstRow = range.getFirstRow();
            int lastRow = range.getLastRow();
            if (row >= firstRow && row <= lastRow) {
                if (column >= firstColumn && column <= lastColumn) {
                    return true;
                }
            }
        }
        return false;
    }


    public static List<Integer> isMergedRowRegion(Sheet sheet, int row) {
        int sheetMergeCount = sheet.getNumMergedRegions();
        List<Integer> regionList = new ArrayList<>();
        for (int i = 0; i < sheetMergeCount; i++) {
            CellRangeAddress range = sheet.getMergedRegion(i);
            int firstRow = range.getFirstRow();
            int lastRow = range.getLastRow();
            if (row >= firstRow && row <= lastRow) {
                regionList.add(i);
            }
        }
        return regionList;
    }


    /**
     *  该方法只取第一个合并的值
     * @param sheet
     * @param row
     * @param column
     * @return
     */
    public static CellRangeAddress getMergedRegion(Sheet sheet, int row, int column) {
        int sheetMergeCount = sheet.getNumMergedRegions();
        for (int i = 0; i < sheetMergeCount; i++) {
            CellRangeAddress range = sheet.getMergedRegion(i);
            int firstColumn = range.getFirstColumn();
            int firstRow = range.getFirstRow();
            if (row == firstRow && column == firstColumn) {
                return range;
            }
        }
        return null;
    }


    /**
     * 获取合并单元格的值
     *
     * @param sheet
     * @param row
     * @param column
     * @return
     */
    public static String getMergedRegionValue(Sheet sheet, int row, int column, FormulaEvaluator evaluator) {
        int sheetMergeCount = sheet.getNumMergedRegions();

        for (int i = 0; i < sheetMergeCount; i++) {
            CellRangeAddress ca = sheet.getMergedRegion(i);
            int firstColumn = ca.getFirstColumn();
            int lastColumn = ca.getLastColumn();
            int firstRow = ca.getFirstRow();
            int lastRow = ca.getLastRow();

            if (row >= firstRow && row <= lastRow) {
                if (column >= firstColumn && column <= lastColumn) {
                    Row fRow = sheet.getRow(firstRow);
                    Cell fCell = fRow.getCell(firstColumn);
                    return getCellValueToString(fCell, evaluator);
                }
            }
        }

        return null;
    }

    /**
     * 获取excel 中sheet 总页数
     *
     * @return
     */
    public int getSheetCount(Workbook wb) {
        return wb.getNumberOfSheets();
    }

    /**
     * 获取 sheet名称
     *
     * @param sheetIx 指定 Sheet 页，从 0 开始
     * @return
     * @throws IOException
     */
    public String getSheetName(Workbook wb, int sheetIx) throws IOException {
        Sheet sheet = wb.getSheetAt(sheetIx);
        return sheet.getSheetName();
    }

    public static Sheet getSheet(Workbook wb, String name) {
        return wb.getSheet(name);
    }

    public static boolean setValueAtForString(Sheet sheet, int rowIndex, int colIndex, String value) throws IOException {
        sheet.getRow(rowIndex).getCell(colIndex).setCellValue(value);
        return true;
    }

    public static boolean setValueAtForDouble(Sheet sheet, int rowIndex, int colIndex, double value) throws IOException {
//        sheet.getRow(rowIndex).getCell(colIndex).setCellStyle(getCellStyle()); // 设置自动
        sheet.getRow(rowIndex).getCell(colIndex).setCellValue(value);
        return true;
    }

    public static void setCellValue(List beans, int startRow, int collength, Sheet sheet, CellStyle cellStyle) throws IllegalAccessException {

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
                        }
                    }
                }
            }
        }

    }

    public static CellStyle getCellStyle(Workbook wb) {
        CellStyle cellStyle = wb.createCellStyle();
        cellStyle.setBorderBottom(BorderStyle.THIN); // 下边框
        cellStyle.setBorderLeft(BorderStyle.THIN);// 左边框
        cellStyle.setBorderTop(BorderStyle.THIN);// 上边框
        cellStyle.setBorderRight(BorderStyle.THIN);// 右边框
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        Font font = wb.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setFontName("宋体");
        cellStyle.setFont(font);
        cellStyle.setWrapText(true);
        return cellStyle;
    }


    public static List<MegerCellEntity> scanMegerCells2(List resps, String fliedName, int startLine) throws NoSuchFieldException,
            IllegalAccessException, ClassNotFoundException {
        List<MegerCellEntity> megerList = new ArrayList<>();
        int startRow = 0;
        String lastValue = "未初始化";
        int continuousQty = 0;
        for (int i = 0; i < resps.size(); i++) {
            Object obj = resps.get(i);
            Class c = obj.getClass();
            Field field = c.getDeclaredField(fliedName);
            ReflectionUtils.makeAccessible(field);
            String value = (String) field.get(obj);
            if (lastValue.equals(value) && i != resps.size() - 1) {
                continuousQty++;
            } else {
                if (continuousQty > 1) {
                    megerList.add(new MegerCellEntity(startRow, startRow + continuousQty - 1));
                }
                startRow = i + startLine;
                continuousQty = 1;
                lastValue = value;
            }
        }
        return megerList;
    }

    public static void scanMegerCells4(Sheet sheet, List resps, int startLine, int column, String... fliedNames) throws NoSuchFieldException,
            IllegalAccessException {
        List<MegerCellEntity> megerList = new ArrayList<>();
        int startRow = 0;
        String lastValue = "";
        for (int i = 0; i < resps.size(); i++) {
            Object obj = resps.get(i);
            Class c = obj.getClass();
            StringBuilder value = new StringBuilder();
            for (String filedName : fliedNames) {
                Field field = c.getDeclaredField(filedName);
                ReflectionUtils.makeAccessible(field);
                value.append((String) field.get(obj));
            }
            if ("".equals(lastValue)) {
                startRow = i + 1;
                lastValue = value.toString();
                continue;
            }
            if (i == resps.size() - 1) {
                if (lastValue.equals(value.toString())) {
                    megerList.add(new MegerCellEntity(startRow + startLine, i + 1 + startLine));
                }
            }
            if (!lastValue.equals(value.toString())) {
                lastValue = value.toString();
                if (startRow != i) {
                    megerList.add(new MegerCellEntity(startRow + startLine, i + startLine));
                }
                startRow = i + 1;
            }
        }
        for (MegerCellEntity megerCellEntity : megerList) {
            sheet.addMergedRegion(new CellRangeAddress(megerCellEntity.startRow, megerCellEntity.endRow, column, column));
        }
    }

    public static void scanMegerCells3(Sheet sheet, List resps, String fliedName, int startLine, int column) throws NoSuchFieldException,
            IllegalAccessException {
        List<MegerCellEntity> megerList = new ArrayList<>();
        int startRow = 0;
        String lastValue = "";
        for (int i = 0; i < resps.size(); i++) {
            Object obj = resps.get(i);
            Class c = obj.getClass();
            Field field = c.getDeclaredField(fliedName);
            ReflectionUtils.makeAccessible(field);
            String value = (String) field.get(obj);
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
        for (MegerCellEntity megerCellEntity : megerList) {
            sheet.addMergedRegion(new CellRangeAddress(megerCellEntity.startRow, megerCellEntity.endRow, column, column));
        }
    }

    public static List<MegerCellEntity> scanMegerCells(List resps, String fliedName, int startLine) throws NoSuchFieldException,
            IllegalAccessException, ClassNotFoundException {
        List<MegerCellEntity> megerList = new ArrayList<>();
        int startRow = 0;
        String lastValue = "";
        for (int i = 0; i < resps.size(); i++) {
            Object obj = resps.get(i);
            Class c = obj.getClass();
            Field field = c.getDeclaredField(fliedName);
            ReflectionUtils.makeAccessible(field);
            String value = (String) field.get(obj);
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

    /**
     * sheet 数据复制入口方法
     * <p>
     * Robert 2022年9月21日
     *
     * @param startRow 开始复制的行
     * @param endRow   结束复制的行 如果大于 fromSheet 则以用 sheet 的row
     */
    public static void copySheetData(Sheet fromSheet, int startRow, int endRow, Sheet toSheet) {
        mergerRegion(fromSheet, toSheet, startRow);
        int toLastRowNuw = toSheet.getLastRowNum();
        int fromLenth = fromSheet.getLastRowNum();
        if (fromLenth > endRow) {
            fromLenth = endRow;
        }
        for (int r = startRow; r <= fromLenth; r++) {
            toLastRowNuw++;
            Row tmpRow = fromSheet.getRow(r);
            Row newRow = toSheet.createRow(toLastRowNuw);
            newRow.setHeight(tmpRow.getHeight());
            copyRow(tmpRow, newRow);
        }
    }

    public static void mergerRegion(Sheet fromSheet, Sheet toSheet, int offset) {
        int sheetMergerCount = fromSheet.getNumMergedRegions();
        int toLastRowNuw = toSheet.getLastRowNum() - offset + 1;
        for (int i = 0; i < sheetMergerCount; i++) {
            CellRangeAddress mergedRegionAt = fromSheet.getMergedRegion(i);
            int mergedFirstRow = mergedRegionAt.getFirstRow();
            if (mergedFirstRow >= offset) {
                int mergedLastRow = mergedRegionAt.getLastRow() + toLastRowNuw;
                mergedRegionAt.setFirstRow(mergedFirstRow + toLastRowNuw);
                mergedRegionAt.setLastRow(mergedLastRow);
                toSheet.addMergedRegion(mergedRegionAt);
            }
        }
    }


    public static void copyRow(Row fromRow, Row toRow) {
        for (Iterator cellIt = fromRow.cellIterator(); cellIt.hasNext(); ) {
            Cell tmpCell = (Cell) cellIt.next();
            Cell newCell = toRow.createCell(tmpCell.getColumnIndex()); // (tmpCell.getCellNum());
            copyCell(tmpCell, newCell);
        }
    }

    public static void copyCell(Cell srcCell, Cell distCell) {
        // 样式
        CellStyle newstyle = distCell.getRow().getSheet().getWorkbook().createCellStyle();
        Font eFont = srcCell.getRow().getSheet().getWorkbook().getFontAt(srcCell.getCellStyle().getFontIndexAsInt());// 获取字体
        copyCellStyle(srcCell.getCellStyle(), newstyle);
        newstyle.setFont(eFont);
        // 评论
        if (srcCell.getCellComment() != null) {
            distCell.setCellComment(srcCell.getCellComment());
        }
        // 不同数据类型处理
        CellType srcCellType = srcCell.getCellType();
        if (srcCellType.equals(CellType.NUMERIC)) {
            newstyle.cloneStyleFrom(srcCell.getCellStyle());
            if (HSSFDateUtil.isCellDateFormatted(srcCell)) {
                distCell.setCellValue(srcCell.getDateCellValue());
            } else {
                distCell.setCellValue(srcCell.getNumericCellValue());
            }
        } else if (srcCellType.equals(CellType.STRING)) {
            distCell.setCellValue(srcCell.getRichStringCellValue());
        } else if (srcCellType.equals(CellType.BLANK)) {
            // nothing21
        } else if (srcCellType.equals(CellType.BOOLEAN)) {
            distCell.setCellValue(srcCell.getBooleanCellValue());
        } else if (srcCellType.equals(CellType.ERROR)) {
            distCell.setCellErrorValue(srcCell.getErrorCellValue());
        } else if (srcCellType.equals(CellType.FORMULA)) {
            switch (srcCell.getCachedFormulaResultType()) {
                case NUMERIC:
                    newstyle.cloneStyleFrom(srcCell.getCellStyle());
                    distCell.setCellValue(srcCell.getNumericCellValue());
                    break;
                case STRING:
                    distCell.setCellValue(srcCell.getRichStringCellValue());
                    break;
                default:
                    break;
            }
        } else { // nothing29
        }
        distCell.setCellStyle(newstyle);
    }


    public static Object getCellValue(Cell srcCell) {
        Object value = null;
        CellType srcCellType = srcCell.getCellType();
        if (srcCellType.equals(CellType.NUMERIC)) {
            if (HSSFDateUtil.isCellDateFormatted(srcCell)) {
                value = new SimpleDateFormat("yyyy/MM/dd").format(srcCell.getDateCellValue());
            } else {
                value = srcCell.getNumericCellValue();
            }
        } else if (srcCellType.equals(CellType.STRING)) {
            value = (srcCell.getRichStringCellValue());
        } else if (srcCellType.equals(CellType.BLANK)) {
            // nothing21
        } else if (srcCellType.equals(CellType.BOOLEAN)) {
            value = (srcCell.getBooleanCellValue());
        } else if (srcCellType.equals(CellType.FORMULA)) {
            switch (srcCell.getCachedFormulaResultType()) {
                case NUMERIC:
                    if (HSSFDateUtil.isCellDateFormatted(srcCell)) {
                        value = new SimpleDateFormat("yyyy/MM/dd").format(srcCell.getDateCellValue());
                    } else {
                        value = (srcCell.getNumericCellValue());
                    }
                    break;
                case STRING:
                    value = (srcCell.getRichStringCellValue());
                    break;
                default:
                    break;
            }
        } else { // nothing29
        }
        return value;
    }


    /**
     * sheet 数据复制入口方法
     * <p>
     * Robert 2022年9月21日
     *
     * @param startRow 开始复制的行
     * @param endRow   结束复制的行 如果大于 fromSheet 则以用 sheet 的row
     */
    public static void copySheetData(Workbook wb, Sheet fromSheet, int startRow, int endRow, Sheet toSheet) {
        FormulaEvaluator evaluator = new XSSFFormulaEvaluator((XSSFWorkbook) wb);
        mergerRegion(fromSheet, toSheet, startRow);
        int toLastRowNuw = toSheet.getLastRowNum();
        int fromLenth = fromSheet.getLastRowNum();
        if (fromLenth > endRow) {
            fromLenth = endRow;
        }
        for (int r = startRow; r <= fromLenth; r++) {
            toLastRowNuw++;
            Row tmpRow = fromSheet.getRow(r);
            Row newRow = toSheet.createRow(toLastRowNuw);
            newRow.setHeight(tmpRow.getHeight());
            copyRow(tmpRow, newRow, evaluator);
        }
    }

    public static void copyRow(Row fromRow, Row toRow, FormulaEvaluator evaluator) {
        for (Iterator cellIt = fromRow.cellIterator(); cellIt.hasNext(); ) {
            Cell tmpCell = (Cell) cellIt.next();
            Cell newCell = toRow.createCell(tmpCell.getColumnIndex()); // (tmpCell.getCellNum());
            copyCell(tmpCell, newCell, evaluator);
        }
    }

    public static void copyCell(Cell srcCell, Cell distCell, FormulaEvaluator evaluator) {
        // 样式
        CellStyle newstyle = distCell.getRow().getSheet().getWorkbook().createCellStyle();
        Font eFont = srcCell.getRow().getSheet().getWorkbook().getFontAt(srcCell.getCellStyle().getFontIndexAsInt());// 获取字体
        copyCellStyle(srcCell.getCellStyle(), newstyle);
        newstyle.setFont(eFont);
        // 评论
        if (srcCell.getCellComment() != null) {
            distCell.setCellComment(srcCell.getCellComment());
        }
        // 不同数据类型处理
        CellType srcCellType = srcCell.getCellType();
        if (srcCellType.equals(CellType.NUMERIC)) {
            newstyle.cloneStyleFrom(srcCell.getCellStyle());
            if (HSSFDateUtil.isCellDateFormatted(srcCell)) {
                distCell.setCellValue(srcCell.getDateCellValue());
            } else {
                distCell.setCellValue(srcCell.getNumericCellValue());
            }
        } else if (srcCellType.equals(CellType.STRING)) {
            distCell.setCellValue(srcCell.getRichStringCellValue());
        } else if (srcCellType.equals(CellType.BLANK)) {
            // nothing21
        } else if (srcCellType.equals(CellType.BOOLEAN)) {
            distCell.setCellValue(srcCell.getBooleanCellValue());
        } else if (srcCellType.equals(CellType.ERROR)) {
            distCell.setCellErrorValue(srcCell.getErrorCellValue());
        } else if (srcCellType.equals(CellType.FORMULA)) {
            switch (srcCell.getCachedFormulaResultType()) {
                case NUMERIC:
                    CellValue evaluate = evaluator.evaluate(srcCell);
                    newstyle.cloneStyleFrom(srcCell.getCellStyle());
//                    distCell.setCellValue(srcCell.getNumericCellValue());
                    distCell.setCellValue(evaluate.formatAsString());
                    break;
                case STRING:
                    distCell.setCellValue(evaluator.evaluate(srcCell).formatAsString());
                    break;
                default:
                    break;
            }
        } else { // nothing29
        }
        distCell.setCellStyle(newstyle);
    }

    /**
     * @auth Robert
     * @param list
     * @param startRow
     * @param cls
     * @return List<CellRangeAddress>
     * @throws IllegalAccessException
     */
    public static <T> List<CellRangeAddress> generateCellRangeAddress(List<T> list, int startRow, Class<T> cls) throws IllegalAccessException {
        Field[] fields = cls.getDeclaredFields();
        Map<Integer, List<String>> colValListMap = new HashMap<>();
        for (T t : list) {
            for (Field field : fields) {
                ReflectionUtils.makeAccessible(field);
                TCPropName tcProp = field.getAnnotation(TCPropName.class);
                if (tcProp != null && tcProp.isMerge()) {
                    int column = tcProp.cell();
                    String value = (String) field.get(t);
                    List<String> colList = null;
                    if (colValListMap.containsKey(column)) {
                        colList = colValListMap.get(column);
                    } else {
                        colList = new ArrayList<>();
                        colValListMap.put(column, colList);
                    }
                    colList.add(value);
                }
            }
        }
        List<CellRangeAddress> rangeList = new ArrayList<>();
        Map<Field, Integer> fieldCountMap = new HashMap<>();
        for (int i = 1; i < list.size(); i++) {
            for (Field field : fields) {
                ReflectionUtils.makeAccessible(field);
                TCPropName tcProp = field.getAnnotation(TCPropName.class);
                if (tcProp != null && tcProp.isMerge()) {
                    int column = tcProp.cell();
                    //
                    int tempColumn = column;
                    String value = "";
                    String previousValue = "";
                    while (tempColumn >= 0 && colValListMap.containsKey(tempColumn)) {
                        List<String> colValList = colValListMap.get(tempColumn);
                        value = colValList.get(i) + value;
                        previousValue = colValList.get(i - 1) + previousValue;
                        --tempColumn;
                    }
                    int count = fieldCountMap.getOrDefault(field, 0);
                    int lastRow = 0;
                    if (StringUtils.hasLength(value) && StringUtils.hasLength(previousValue) && value.equalsIgnoreCase(previousValue)) {
                        fieldCountMap.put(field, ++count);
                        if (i + 1 == list.size()) {
                            lastRow = i;
                        }
                    } else if (count > 0) {
                        lastRow = i - 1;
                    }
                    if (lastRow > 0) {
                        int firstRow = startRow + lastRow - count;
                        CellRangeAddress cellAddresses = new CellRangeAddress(firstRow, lastRow + startRow, column, column);
                        rangeList.add(cellAddresses);
                        fieldCountMap.put(field, 0);
                    }
                }
            }
        }

        return rangeList;
    }

    /**
     * @auth robert
     * @param sheet
     * @param startRow
     * @param cls
     * @return
     */
    public static <T> List<T> readSimpleExcel(Sheet sheet, int startRow, Class<T> cls) throws IllegalAccessException, InstantiationException,
            NoSuchMethodException, InvocationTargetException {
        List<T> list = new ArrayList<>();
        Field[] fields = cls.getDeclaredFields();
        int lastRow = sheet.getLastRowNum();
        for (; startRow <= lastRow; startRow++) {
            Row row = sheet.getRow(startRow);
            T bean = cls.getConstructor().newInstance();
            for (Field field : fields) {
                ReflectionUtils.makeAccessible(field);
                TCPropName tcProp = field.getAnnotation(TCPropName.class);
                if (tcProp != null && tcProp.cell() >= 0) {
                    Cell cell = row.getCell(tcProp.cell());
                    String cellVal = getCellValueToString(cell);
                    cellVal = cellVal != null ? cellVal.trim() : "";
                    field.set(bean, cellVal);
                }
            }
            list.add(bean);
        }
        return list;
    }

    public static void copyCellStyle(CellStyle fromStyle, CellStyle toStyle) {
        toStyle.setAlignment(fromStyle.getAlignment());
        toStyle.setBorderBottom(fromStyle.getBorderBottom());
        toStyle.setBorderLeft(fromStyle.getBorderLeft());
        toStyle.setBorderRight(fromStyle.getBorderRight());
        toStyle.setBorderTop(fromStyle.getBorderTop());
        toStyle.setVerticalAlignment(fromStyle.getVerticalAlignment());
        toStyle.setWrapText(fromStyle.getWrapText());
    }
}
