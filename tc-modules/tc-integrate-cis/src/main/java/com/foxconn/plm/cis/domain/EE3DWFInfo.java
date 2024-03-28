package com.foxconn.plm.cis.domain;

import com.foxconn.plm.entity.constants.TCPropName;
import com.foxconn.plm.utils.excel.ExcelUtil;
import lombok.Data;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

@Data
public class EE3DWFInfo {
    @TCPropName(cell = 0)
    private String wfName;
    @TCPropName(cell = 1)
    private String workItem;
    @TCPropName(cell = 2)
    private String taskName;
    @TCPropName(cell = 3)
    private String taskDesZh;
    @TCPropName(cell = 4)
    private String taskDesEn;
    @TCPropName(cell = 5)
    private String func;

}
