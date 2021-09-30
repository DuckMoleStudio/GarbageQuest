package GarbageQuest.service;

import GarbageQuest.mosData.XLSPaidParkings;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class XLSImport {
    public static List<XLSPaidParkings> loadXLSPaidParkings(String fileName) throws IOException
    { // read XLS & parse to data

        List<XLSPaidParkings> requestList = new ArrayList<>();

        // Read XLS file (Excel 97-2003)
        FileInputStream inputStream = new FileInputStream(new File(fileName));

        // Get the workbook instance for XLS file
        HSSFWorkbook workbook = new HSSFWorkbook(inputStream);

        // Get first sheet from the workbook
        HSSFSheet sheet = workbook.getSheetAt(0);

        // Get iterator to all the rows in current sheet
        Iterator<Row> rowIterator = sheet.iterator();
        rowIterator.next(); // skip firs row with headers
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            // Get iterator to all cells of current row
            Iterator<Cell> cellIterator = row.cellIterator();

            // Get significant fields for ClassifiableText class
            requestList.add(new XLSPaidParkings(
                    Long.parseLong(row.getCell(0).getStringCellValue().substring(1)) // weird input
                    ,(row.getCell(4).getStringCellValue())
                    ,(row.getCell(5).getStringCellValue())
                    , "some address"//(row.getCell(6).getStringCellValue())
                    ,row.getCell(18).getStringCellValue()));
        }
        return (requestList);
    }
}
