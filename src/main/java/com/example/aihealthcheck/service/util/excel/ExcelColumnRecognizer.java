// 文件位置：src/main/java/com/example/aihealthcheck/service/excel/ExcelColumnRecognizer.java
package com.example.aihealthcheck.service.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.HashMap;
import java.util.Map;

public class ExcelColumnRecognizer {

    // 列名映射定义
    public enum ColumnType {
        CHECK_DATE,      // 检查日期
        HEMOGLOBIN,      // 血红蛋白
        WHITE_BLOOD_CELL, // 白细胞
        PLATELET,        // 血小板
        BLOOD_GLUCOSE,   // 血糖
        URINE_PROTEIN,   // 尿蛋白
        URINE_GLUCOSE,   // 尿糖
        URINE_GRAVITY,   // 尿比重
        ALT,             // ALT
        AST,             // AST
        TOTAL_BILIRUBIN, // 总胆红素
        CREATININE,      // 肌酐
        UREA_NITROGEN,   // 尿素氮
        URIC_ACID        // 尿酸
    }

    // 列名关键词映射（支持中英文）
    private static final Map<String, ColumnType> COLUMN_KEYWORDS = new HashMap<>();

    static {
        // 检查日期
        COLUMN_KEYWORDS.put("检查日期", ColumnType.CHECK_DATE);
        COLUMN_KEYWORDS.put("检查时间", ColumnType.CHECK_DATE);
        COLUMN_KEYWORDS.put("date", ColumnType.CHECK_DATE);
        COLUMN_KEYWORDS.put("checkdate", ColumnType.CHECK_DATE);
        COLUMN_KEYWORDS.put("检测日期", ColumnType.CHECK_DATE);

        // 血检指标
        COLUMN_KEYWORDS.put("血红蛋白", ColumnType.HEMOGLOBIN);
        COLUMN_KEYWORDS.put("血色素", ColumnType.HEMOGLOBIN);
        COLUMN_KEYWORDS.put("hemoglobin", ColumnType.HEMOGLOBIN);
        COLUMN_KEYWORDS.put("hgb", ColumnType.HEMOGLOBIN);
        COLUMN_KEYWORDS.put("hb", ColumnType.HEMOGLOBIN);

        COLUMN_KEYWORDS.put("白细胞", ColumnType.WHITE_BLOOD_CELL);
        COLUMN_KEYWORDS.put("白细胞计数", ColumnType.WHITE_BLOOD_CELL);
        COLUMN_KEYWORDS.put("白血球", ColumnType.WHITE_BLOOD_CELL);
        COLUMN_KEYWORDS.put("whitebloodcell", ColumnType.WHITE_BLOOD_CELL);
        COLUMN_KEYWORDS.put("wbc", ColumnType.WHITE_BLOOD_CELL);

        COLUMN_KEYWORDS.put("血小板", ColumnType.PLATELET);
        COLUMN_KEYWORDS.put("血小板计数", ColumnType.PLATELET);
        COLUMN_KEYWORDS.put("platelet", ColumnType.PLATELET);
        COLUMN_KEYWORDS.put("plt", ColumnType.PLATELET);

        COLUMN_KEYWORDS.put("血糖", ColumnType.BLOOD_GLUCOSE);
        COLUMN_KEYWORDS.put("葡萄糖", ColumnType.BLOOD_GLUCOSE);
        COLUMN_KEYWORDS.put("bloodglucose", ColumnType.BLOOD_GLUCOSE);
        COLUMN_KEYWORDS.put("glu", ColumnType.BLOOD_GLUCOSE);
        COLUMN_KEYWORDS.put("glucose", ColumnType.BLOOD_GLUCOSE);

        // 尿检指标
        COLUMN_KEYWORDS.put("尿蛋白", ColumnType.URINE_PROTEIN);
        COLUMN_KEYWORDS.put("蛋白", ColumnType.URINE_PROTEIN);
        COLUMN_KEYWORDS.put("urineprotein", ColumnType.URINE_PROTEIN);
        COLUMN_KEYWORDS.put("pro", ColumnType.URINE_PROTEIN);
        COLUMN_KEYWORDS.put("protein", ColumnType.URINE_PROTEIN);

        COLUMN_KEYWORDS.put("尿糖", ColumnType.URINE_GLUCOSE);
        COLUMN_KEYWORDS.put("urineglucose", ColumnType.URINE_GLUCOSE);

        COLUMN_KEYWORDS.put("尿比重", ColumnType.URINE_GRAVITY);
        COLUMN_KEYWORDS.put("比重", ColumnType.URINE_GRAVITY);
        COLUMN_KEYWORDS.put("urinespecificgravity", ColumnType.URINE_GRAVITY);
        COLUMN_KEYWORDS.put("sg", ColumnType.URINE_GRAVITY);

        // 肝功能指标
        COLUMN_KEYWORDS.put("alt", ColumnType.ALT);
        COLUMN_KEYWORDS.put("谷丙转氨酶", ColumnType.ALT);
        COLUMN_KEYWORDS.put("丙氨酸氨基转移酶", ColumnType.ALT);

        COLUMN_KEYWORDS.put("ast", ColumnType.AST);
        COLUMN_KEYWORDS.put("谷草转氨酶", ColumnType.AST);
        COLUMN_KEYWORDS.put("天门冬氨酸氨基转移酶", ColumnType.AST);

        COLUMN_KEYWORDS.put("总胆红素", ColumnType.TOTAL_BILIRUBIN);
        COLUMN_KEYWORDS.put("胆红素", ColumnType.TOTAL_BILIRUBIN);
        COLUMN_KEYWORDS.put("totalbilirubin", ColumnType.TOTAL_BILIRUBIN);
        COLUMN_KEYWORDS.put("tbil", ColumnType.TOTAL_BILIRUBIN);

        // 肾功能指标
        COLUMN_KEYWORDS.put("肌酐", ColumnType.CREATININE);
        COLUMN_KEYWORDS.put("creatinine", ColumnType.CREATININE);
        COLUMN_KEYWORDS.put("crea", ColumnType.CREATININE);

        COLUMN_KEYWORDS.put("尿素氮", ColumnType.UREA_NITROGEN);
        COLUMN_KEYWORDS.put("尿素", ColumnType.UREA_NITROGEN);
        COLUMN_KEYWORDS.put("ureanitrogen", ColumnType.UREA_NITROGEN);
        COLUMN_KEYWORDS.put("bun", ColumnType.UREA_NITROGEN);

        COLUMN_KEYWORDS.put("尿酸", ColumnType.URIC_ACID);
        COLUMN_KEYWORDS.put("uricacid", ColumnType.URIC_ACID);
        COLUMN_KEYWORDS.put("ua", ColumnType.URIC_ACID);
    }

    private Map<ColumnType, Integer> columnIndexMap = new HashMap<>();
    private boolean initialized = false;

    /**
     * 从表头行识别列
     */
    public void recognizeColumns(Row headerRow) {
        if (headerRow == null) {
            throw new IllegalArgumentException("表头行为空");
        }

        columnIndexMap.clear();

        // 遍历表头行的每个单元格
        for (int i = headerRow.getFirstCellNum(); i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                String cellValue = getCellStringValue(cell).trim().toLowerCase();

                // 遍历关键词映射，找到匹配的列
                for (Map.Entry<String, ColumnType> entry : COLUMN_KEYWORDS.entrySet()) {
                    String keyword = entry.getKey().toLowerCase();
                    if (cellValue.contains(keyword)) {
                        columnIndexMap.put(entry.getValue(), i);
                        break;
                    }
                }
            }
        }

        initialized = true;
    }

    /**
     * 检查是否有某种类型的列
     */
    public boolean hasColumn(ColumnType columnType) {
        return columnIndexMap.containsKey(columnType);
    }

    /**
     * 获取列的索引
     */
    public Integer getColumnIndex(ColumnType columnType) {
        return columnIndexMap.get(columnType);
    }

    /**
     * 检查是否有血检数据
     */
    public boolean hasBloodData() {
        return hasColumn(ColumnType.HEMOGLOBIN) ||
               hasColumn(ColumnType.WHITE_BLOOD_CELL) ||
               hasColumn(ColumnType.PLATELET) ||
               hasColumn(ColumnType.BLOOD_GLUCOSE);
    }

    /**
     * 检查是否有尿检数据
     */
    public boolean hasUrineData() {
        return hasColumn(ColumnType.URINE_PROTEIN) ||
               hasColumn(ColumnType.URINE_GLUCOSE) ||
               hasColumn(ColumnType.URINE_GRAVITY);
    }

    /**
     * 检查是否有肝功能数据
     */
    public boolean hasLiverData() {
        return hasColumn(ColumnType.ALT) ||
               hasColumn(ColumnType.AST) ||
               hasColumn(ColumnType.TOTAL_BILIRUBIN);
    }

    /**
     * 检查是否有肾功能数据
     */
    public boolean hasKidneyData() {
        return hasColumn(ColumnType.CREATININE) ||
               hasColumn(ColumnType.UREA_NITROGEN) ||
               hasColumn(ColumnType.URIC_ACID);
    }

    /**
     * 获取识别结果报告
     */
    public String getRecognitionReport() {
        StringBuilder report = new StringBuilder();
        report.append("Excel列识别结果:\n");

        if (hasBloodData()) {
            report.append("✅ 检测到血检数据: ");
            if (hasColumn(ColumnType.HEMOGLOBIN)) report.append("血红蛋白 ");
            if (hasColumn(ColumnType.WHITE_BLOOD_CELL)) report.append("白细胞 ");
            if (hasColumn(ColumnType.PLATELET)) report.append("血小板 ");
            if (hasColumn(ColumnType.BLOOD_GLUCOSE)) report.append("血糖 ");
            report.append("\n");
        }

        if (hasUrineData()) {
            report.append("✅ 检测到尿检数据: ");
            if (hasColumn(ColumnType.URINE_PROTEIN)) report.append("尿蛋白 ");
            if (hasColumn(ColumnType.URINE_GLUCOSE)) report.append("尿糖 ");
            if (hasColumn(ColumnType.URINE_GRAVITY)) report.append("尿比重 ");
            report.append("\n");
        }

        if (hasLiverData()) {
            report.append("✅ 检测到肝功能数据: ");
            if (hasColumn(ColumnType.ALT)) report.append("ALT ");
            if (hasColumn(ColumnType.AST)) report.append("AST ");
            if (hasColumn(ColumnType.TOTAL_BILIRUBIN)) report.append("总胆红素 ");
            report.append("\n");
        }

        if (hasKidneyData()) {
            report.append("✅ 检测到肾功能数据: ");
            if (hasColumn(ColumnType.CREATININE)) report.append("肌酐 ");
            if (hasColumn(ColumnType.UREA_NITROGEN)) report.append("尿素氮 ");
            if (hasColumn(ColumnType.URIC_ACID)) report.append("尿酸 ");
            report.append("\n");
        }

        if (!hasBloodData() && !hasUrineData() && !hasLiverData() && !hasKidneyData()) {
            report.append("❌ 未检测到任何有效的健康指标列\n");
        }

        return report.toString();
    }

    /**
     * 辅助方法：获取单元格字符串值
     */
    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    public boolean isInitialized() {
        return initialized;
    }
}