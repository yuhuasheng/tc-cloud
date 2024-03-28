package com.foxconn.plm.utils.util;

import cn.hutool.core.collection.CollUtil;
import com.spire.pdf.FileFormat;
import com.spire.pdf.PdfDocument;
import com.spire.pdf.PdfPageBase;
import com.spire.pdf.PdfPageSize;
import com.spire.pdf.graphics.*;
import com.spire.pdf.tables.PdfColumn;
import com.spire.pdf.tables.PdfHeaderSource;
import com.spire.pdf.tables.PdfTable;
import com.spire.pdf.utilities.PdfTableExtractor;

import java.awt.*;
import java.awt.geom.Point2D;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @ClassName: PDFUtils
 * @Description:
 * @Author DY
 * @Create 2023/4/24
 */
public class PDFUtils {

    private static void exportPdf(Map<String, String> map, OutputStream out) {
        //创建PDF文档
        PdfDocument doc = new PdfDocument();
        // 添加一页
        PdfPageBase page = doc.getPages().add(PdfPageSize.A4, new PdfMargins(10, 0));
        //添加表格
        PdfTable table = new PdfTable();
        table.getStyle().setBorderPen(new PdfPen(new PdfSolidBrush(new PdfRGBColor(Color.black)), 0.5f));
        table.getStyle().getHeaderStyle().setStringFormat(new PdfStringFormat(PdfTextAlignment.Center));
        table.getStyle().setHeaderSource(PdfHeaderSource.Rows);
        table.getStyle().setHeaderRowCount(1);
        table.getStyle().setShowHeader(true);
        table.getStyle().setCellPadding(2);
        //设置表头字体和样式
        table.getStyle().getHeaderStyle().setFont(new PdfTrueTypeFont(new Font("黑体", Font.BOLD, 16)));
        table.getStyle().getHeaderStyle().setBackgroundBrush(PdfBrushes.getCadetBlue());
        PdfTrueTypeFont fontBody = new PdfTrueTypeFont(new Font("宋体", Font.PLAIN, 12));
        //设置偶数行字体
        table.getStyle().getDefaultStyle().setFont(fontBody);
        //设置奇数行字体
        table.getStyle().getAlternateStyle().setFont(fontBody);

        List<String> keys = CollUtil.newArrayList(map.keySet());
        String[][] dataSource = new String[keys.size() + 1][3];
        dataSource[0] = new String[]{"Item", "Feature", "Value"};
        for (int i = 0; i < keys.size(); i++) {
            dataSource[i + 1][0] = String.valueOf(i + 1);
            dataSource[i + 1][1] = keys.get(i);
            dataSource[i + 1][2] = map.get(keys.get(i));
        }
        table.setDataSource(dataSource);

        for (int i = 0; i < table.getColumns().getCount(); i++) {
            PdfColumn column = table.getColumns().get(i);
            if (i == 0) {
                column.setWidth(1.5f);
            } else if (i == 1) {
                column.setWidth(4f);
            } else {
                column.setWidth(8f);
            }
            column.setStringFormat(new PdfStringFormat(PdfTextAlignment.Center, PdfVerticalAlignment.Middle));
        }
        //添加表格
        table.draw(page, new Point2D.Float(0, 50));
        doc.saveToStream(out, FileFormat.PDF);
    }


    private static Map<String, String> readTable(InputStream in, Set<String> keySet) {
        PdfDocument doc = new PdfDocument();
        doc.loadFromStream(in);
        PdfTableExtractor extractor = new PdfTableExtractor(doc);
        com.spire.pdf.utilities.PdfTable[] tableLists = extractor.extractTable(0);
        Map<String, String> map = new HashMap<>(keySet.size());
        if (tableLists != null && tableLists.length > 0) {
            //遍历表格
            for (com.spire.pdf.utilities.PdfTable table : tableLists) {
                //获取表格行
                int row = table.getRowCount();
                //获取表格列
                int column = table.getColumnCount();
                for (int i = 0; i < row; i++) {
                    for (int j = 0; j < column; j++) {
                        //获取表格中的文本内容
                        String text = table.getText(i, j).trim();
                        if (keySet.contains(text)) {
                            map.put(text, table.getText(i, j + 1));
                            break;
                        }
                    }
                }
            }
        }
        return map;
    }
}
