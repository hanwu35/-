// 文件位置：src/main/java/com/example/aihealthcheck/service/ValueConversionService.java
package com.example.aihealthcheck.service;

import com.example.aihealthcheck.entity.ItemMappingConfig;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class ValueConversionService {

    // 文本结果到数值的映射
    private static final Map<String, Double> TEXT_VALUE_MAPPING = new HashMap<>();

    static {
        // 阴性/负号 → 0
        TEXT_VALUE_MAPPING.put("阴性", 0.0);
        TEXT_VALUE_MAPPING.put("neg", 0.0);
        TEXT_VALUE_MAPPING.put("-", 0.0);
        TEXT_VALUE_MAPPING.put("negative", 0.0);

        // 弱阳性/加减号 → 0.5
        TEXT_VALUE_MAPPING.put("弱阳性", 0.5);
        TEXT_VALUE_MAPPING.put("±", 0.5);
        TEXT_VALUE_MAPPING.put("+/-", 0.5);
        TEXT_VALUE_MAPPING.put("trace", 0.5);

        // 阳性/加号 → 1
        TEXT_VALUE_MAPPING.put("阳性", 1.0);
        TEXT_VALUE_MAPPING.put("pos", 1.0);
        TEXT_VALUE_MAPPING.put("+", 1.0);
        TEXT_VALUE_MAPPING.put("positive", 1.0);

        // 强阳性 → 2
        TEXT_VALUE_MAPPING.put("强阳性", 2.0);
        TEXT_VALUE_MAPPING.put("++", 2.0);
        TEXT_VALUE_MAPPING.put("+++", 3.0);
        TEXT_VALUE_MAPPING.put("++++", 4.0);
    }

    // 数字模式
    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+(\\.\\d+)?");

    /**
     * 转换检验结果为数值
     */
    public Double convertToNumericValue(String itemValue, ItemMappingConfig.ValueType valueType) {
        if (itemValue == null || itemValue.trim().isEmpty()) {
            return null;
        }

        String trimmedValue = itemValue.trim();

        // 文本类型结果转换
        if (valueType == ItemMappingConfig.ValueType.text_positive) {
            // 查找映射表中的值（不区分大小写）
            for (Map.Entry<String, Double> entry : TEXT_VALUE_MAPPING.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(trimmedValue)) {
                    return entry.getValue();
                }
            }

            // 尝试匹配加号数量
            if (trimmedValue.contains("+")) {
                int plusCount = (int) trimmedValue.chars().filter(ch -> ch == '+').count();
                return plusCount <= 4 ? (double) plusCount : 4.0;
            }

            // 无法识别，返回null
            return null;
        }

        // 数值类型结果转换
        else {
            try {
                // 处理带单位的数值（如 "216 10^9/L"）
                if (trimmedValue.contains(" ")) {
                    String[] parts = trimmedValue.split(" ");
                    if (NUMBER_PATTERN.matcher(parts[0]).matches()) {
                        return Double.parseDouble(parts[0]);
                    }
                }

                // 直接解析数值
                return Double.parseDouble(trimmedValue);
            } catch (NumberFormatException e) {
                // 尝试清理非数字字符
                String numericPart = trimmedValue.replaceAll("[^0-9.-]", "");
                if (!numericPart.isEmpty()) {
                    try {
                        return Double.parseDouble(numericPart);
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                }
                return null;
            }
        }
    }

    /**
     * 转换数值为显示文本
     */
    public String convertToDisplayValue(Double numericValue, ItemMappingConfig.ValueType valueType) {
        if (numericValue == null) {
            return "";
        }

        if (valueType == ItemMappingConfig.ValueType.text_positive) {
            if (numericValue == 0.0) return "阴性";
            if (numericValue == 0.5) return "弱阳性";
            if (numericValue == 1.0) return "阳性";
            if (numericValue == 2.0) return "强阳性";
            if (numericValue == 3.0) return "+++";
            if (numericValue == 4.0) return "++++";
            return String.valueOf(numericValue);
        } else {
            return String.valueOf(numericValue);
        }
    }

    /**
     * 单位转换
     */
    public Double convertUnit(Double value, String fromUnit, String toUnit) {
        if (value == null) return null;

        // 相同单位不需要转换
        if (fromUnit == null || toUnit == null || fromUnit.equals(toUnit)) {
            return value;
        }

        // 常见单位转换
        Map<String, Map<String, Double>> conversionRules = new HashMap<>();

        // 血红蛋白：g/dL → g/L (乘以10)
        conversionRules.put("g/dL", Map.of("g/L", 10.0));
        conversionRules.put("g/L", Map.of("g/dL", 0.1));

        // 血糖：mmol/L → mg/dL (乘以18)
        conversionRules.put("mmol/L", Map.of("mg/dL", 18.0));
        conversionRules.put("mg/dL", Map.of("mmol/L", 1.0/18.0));

        // 肌酐：mg/dL → μmol/L (乘以88.4)
        conversionRules.put("mg/dL", Map.of("μmol/L", 88.4));
        conversionRules.put("μmol/L", Map.of("mg/dL", 1.0/88.4));

        // 尿酸：mg/dL → μmol/L (乘以59.48)
        conversionRules.put("mg/dL", Map.of("μmol/L", 59.48));
        conversionRules.put("μmol/L", Map.of("mg/dL", 1.0/59.48));

        // 应用转换规则
        if (conversionRules.containsKey(fromUnit) && conversionRules.get(fromUnit).containsKey(toUnit)) {
            double conversionFactor = conversionRules.get(fromUnit).get(toUnit);
            return value * conversionFactor;
        }

        // 无法转换，返回原值
        return value;
    }
}