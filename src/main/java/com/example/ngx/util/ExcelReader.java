package com.example.ngx.util;

import com.example.ngx.model.DeviceInfo;
import org.apache.poi.ss.usermodel.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ExcelReader {
    public static List<DeviceInfo> readDevices(String filePath) throws Exception {
        List<DeviceInfo> devices = new ArrayList<>();
        Workbook workbook = WorkbookFactory.create(new File(filePath));
        Sheet sheet = workbook.getSheetAt(0);
        
        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue; // 跳过表头
            String group = row.getCell(2).getStringCellValue();    // 第三列
            String deviceId = row.getCell(4).getStringCellValue(); // 第五列
            String password = row.getCell(5).getStringCellValue(); // 第六列
            devices.add(new DeviceInfo(group, deviceId, password));
        }
        workbook.close();
        return devices;
    }
}