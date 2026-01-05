// 文件位置：src/main/java/com/example/aihealthcheck/service/excel/ExcelParseResult.java
package com.example.aihealthcheck.service.excel;

public class ExcelParseResult {
    private int totalRows;
    private int bloodRecords;
    private int urineRecords;
    private int liverRecords;
    private int kidneyRecords;
    private int indicatorRecords;
    private String recognitionReport;

    // 构造函数
    public ExcelParseResult() {}

    // Getter和Setter
    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }

    public int getBloodRecords() { return bloodRecords; }
    public void setBloodRecords(int bloodRecords) { this.bloodRecords = bloodRecords; }

    public int getUrineRecords() { return urineRecords; }
    public void setUrineRecords(int urineRecords) { this.urineRecords = urineRecords; }

    public int getLiverRecords() { return liverRecords; }
    public void setLiverRecords(int liverRecords) { this.liverRecords = liverRecords; }

    public int getKidneyRecords() { return kidneyRecords; }
    public void setKidneyRecords(int kidneyRecords) { this.kidneyRecords = kidneyRecords; }

    public int getIndicatorRecords() { return indicatorRecords; }
    public void setIndicatorRecords(int indicatorRecords) { this.indicatorRecords = indicatorRecords; }

    public String getRecognitionReport() { return recognitionReport; }
    public void setRecognitionReport(String recognitionReport) { this.recognitionReport = recognitionReport; }

    public int getTotalRecords() {
        return bloodRecords + urineRecords + liverRecords + kidneyRecords;
    }

    @Override
    public String toString() {
        return String.format(
            "解析结果: 共%d行数据\n" +
            "血检记录: %d条\n" +
            "尿检记录: %d条\n" +
            "肝功能记录: %d条\n" +
            "肾功能记录: %d条\n" +
            "总览记录: %d条\n" +
            "%s",
            totalRows, bloodRecords, urineRecords, liverRecords, kidneyRecords, indicatorRecords, recognitionReport
        );
    }
}