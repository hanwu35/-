package com.example.aihealthcheck.service;

import com.example.aihealthcheck.entity.*;
import com.example.aihealthcheck.entity.health.examination.Blood.*;
import com.example.aihealthcheck.entity.health.examination.Kindey.*;
import com.example.aihealthcheck.entity.health.examination.Liver.*;
import com.example.aihealthcheck.entity.health.examination.Urine.*;
import com.example.aihealthcheck.entity.health.record.*;
import com.example.aihealthcheck.entity.health.indicator.*;
import com.example.aihealthcheck.repository.*;
import com.example.aihealthcheck.repository.health.examination.blood.*;
import com.example.aihealthcheck.repository.health.examination.Kindey.*;
import com.example.aihealthcheck.repository.health.examination.Liver.*;
import com.example.aihealthcheck.repository.health.examination.Urine.*;
import com.example.aihealthcheck.repository.health.record.*;
import com.example.aihealthcheck.repository.health.indicator.*;
import com.example.aihealthcheck.repository.user.UserRepository;
import com.example.aihealthcheck.service.prediction.ModelPredictionService; // 新增导入
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class HealthDataExcelService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HealthBloodItemRepository bloodItemRepository;

    @Autowired
    private HealthUrineItemRepository urineItemRepository;

    @Autowired
    private HealthLiverItemRepository liverItemRepository;

    @Autowired
    private HealthKidneyItemRepository kidneyItemRepository;

    @Autowired
    private ItemMappingService mappingService;

    @Autowired
    private ValueConversionService conversionService;

    @Autowired
    private FeatureExtractionService featureExtractionService;

    @Autowired  // 新增：注入模型预测服务
    private ModelPredictionService modelPredictionService;

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        DateTimeFormatter.ofPattern("yyyy年MM月dd日"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("yyyy.M.d"),
        DateTimeFormatter.ofPattern("M/d/yyyy")
    };

    /**
     * 处理Excel文件（行格式数据）
     */
    @Transactional
    public ExcelImportResult processHealthDataExcel(MultipartFile file, Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + userId));

        ExcelImportResult result = new ExcelImportResult();

        System.out.println("=== 开始处理Excel文件 ===");
        System.out.println("文件名: " + file.getOriginalFilename());
        System.out.println("文件大小: " + file.getSize() + " bytes");

        try (InputStream inputStream = file.getInputStream()) {
            Workbook workbook = WorkbookFactory.create(inputStream);

            System.out.println("工作簿中的工作表数量: " + workbook.getNumberOfSheets());

            // 处理每个工作表
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String sheetName = sheet.getSheetName();

                System.out.println("\n处理工作表[" + (i+1) + "]: " + sheetName);

                // 两种模式都支持：
                // 1. 根据工作表名称判断类型
                // 2. 通用处理（根据数据内容判断）

                if (sheetName.contains("血") || sheetName.toLowerCase().contains("blood")) {
                    System.out.println("✅ 根据工作表名称识别为：血检数据");
                    processBloodSheet(sheet, user, result);
                } else if (sheetName.contains("尿") || sheetName.toLowerCase().contains("urine")) {
                    System.out.println("✅ 根据工作表名称识别为：尿检数据");
                    processUrineSheet(sheet, user, result);
                } else if (sheetName.contains("肝") || sheetName.toLowerCase().contains("liver")) {
                    System.out.println("✅ 根据工作表名称识别为：肝功能数据");
                    processLiverSheet(sheet, user, result);
                } else if (sheetName.contains("肾") || sheetName.toLowerCase().contains("kidney")) {
                    System.out.println("✅ 根据工作表名称识别为：肾功能数据");
                    processKidneySheet(sheet, user, result);
                } else {
                    // 通用处理：根据数据内容自动识别类型
                    System.out.println("⚠️ 未识别工作表名称，开始智能识别数据内容");
                    processIntelligentSheet(sheet, user, result);
                }
            }

            workbook.close();

            // 提取特征向量
            if (result.getTotalItems() > 0) {
                try {
                    int featuresExtracted = featureExtractionService.extractFeaturesForUser(user);
                    result.setFeaturesExtracted(featuresExtracted);
                    System.out.println("特征向量提取完成: " + featuresExtracted + " 条");
                } catch (Exception e) {
                    System.err.println("特征提取失败，但不影响数据导入: " + e.getMessage());
                    result.setFeaturesExtracted(0);
                }
            }

            // ============ 新增：触发模型预测 ============
            // 只有当成功导入数据后才触发预测
            if (result.getTotalItems() > 0) {
                try {
                    System.out.println("\n🎯 开始触发模型预测，用户ID: " + userId);

                    // 获取检查日期（使用最近一次的检查日期）
                    LocalDate latestCheckDate = findLatestCheckDateForUser(userId);
                    if (latestCheckDate != null) {
                        String checkDate = latestCheckDate.toString();
                        System.out.println("检查日期: " + checkDate);

                        // 异步触发模型预测（不等待完成）
                        modelPredictionService.triggerAllModelPredictions(userId, checkDate);
                        System.out.println("✅ 模型预测已触发（异步执行）");

                        // 记录到结果中
                        result.setModelPredictionsTriggered(true);
                        System.out.println("预测模型: 血检、尿检、肝功能、肾功能");
                    } else {
                        System.out.println("⚠️ 未找到检查日期，跳过模型预测");
                    }
                } catch (Exception e) {
                    System.err.println("触发模型预测失败，但不影响数据导入: " + e.getMessage());
                    e.printStackTrace();
                    result.setModelPredictionsTriggered(false);
                }
            } else {
                System.out.println("⚠️ 没有成功导入数据，跳过模型预测");
            }
            // ============ 新增结束 ============

            System.out.println("\n=== Excel文件处理完成 ===");
            System.out.println(result);

            return result;

        } catch (Exception e) {
            System.err.println("处理Excel文件失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("处理Excel文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 查找用户最近的检查日期
     */
    private LocalDate findLatestCheckDateForUser(Integer userId) {
        try {
            System.out.println("查找用户 " + userId + " 的最新检查日期");

            // 尝试从血检数据获取
            Optional<HealthBloodItem> latestBloodItem = bloodItemRepository
                .findTopByUserIdOrderByCheckDateDesc(userId);

            if (latestBloodItem.isPresent()) {
                LocalDate date = latestBloodItem.get().getCheckDate();
                System.out.println("从血检数据找到检查日期: " + date);
                return date;
            }

            // 如果血检没有，尝试尿检
            Optional<HealthUrineItem> latestUrineItem = urineItemRepository
                .findTopByUserIdOrderByCheckDateDesc(userId);

            if (latestUrineItem.isPresent()) {
                LocalDate date = latestUrineItem.get().getCheckDate();
                System.out.println("从尿检数据找到检查日期: " + date);
                return date;
            }

            // 尝试肝功能
            Optional<HealthLiverItem> latestLiverItem = liverItemRepository
                .findTopByUserIdOrderByCheckDateDesc(userId);

            if (latestLiverItem.isPresent()) {
                LocalDate date = latestLiverItem.get().getCheckDate();
                System.out.println("从肝功能数据找到检查日期: " + date);
                return date;
            }

            // 尝试肾功能
            Optional<HealthKidneyItem> latestKidneyItem = kidneyItemRepository
                .findTopByUserIdOrderByCheckDateDesc(userId);

            if (latestKidneyItem.isPresent()) {
                LocalDate date = latestKidneyItem.get().getCheckDate();
                System.out.println("从肾功能数据找到检查日期: " + date);
                return date;
            }

            System.out.println("未找到任何检查日期");
            return null;

        } catch (Exception e) {
            System.err.println("查找最新检查日期失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 智能识别数据表（根据项目名称判断数据类型）- 优化版
     */
    private void processIntelligentSheet(Sheet sheet, User user, ExcelImportResult result) {
        System.out.println("=== 开始智能识别处理 ===");

        Iterator<Row> rowIterator = sheet.iterator();

        // 跳过表头
        if (rowIterator.hasNext()) {
            Row headerRow = rowIterator.next();
            System.out.println("表头行内容:");
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                System.out.println("  列" + i + ": " + getCellStringValue(headerRow.getCell(i)));
            }
        }

        int rowCount = 0;
        int processedCount = 0;

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            rowCount++;

            if (isRowEmpty(row)) continue;

            System.out.println("\n--- 处理第" + rowCount + "行 ---");

            // 获取项目名称（第3列）和检验结果（第4列）
            String itemName = getCellStringValue(row.getCell(3));
            String itemValue = getCellStringValue(row.getCell(4));

            if (itemName == null || itemName.trim().isEmpty()) {
                System.out.println("❌ 项目名称为空，跳过");
                result.incrementErrorCount();
                continue;
            }

            itemName = itemName.trim();
            System.out.println("项目名称: " + itemName);
            System.out.println("检验结果: " + itemValue);

            // ✅ 修复：优先尝试映射服务
            Optional<ItemMappingConfig> config = mappingService.getConfigByChineseName(itemName);
            if (config.isPresent()) {
                System.out.println("✅ 通过映射配置识别为：" + config.get().getCategory() + " 项目");
                processRowByConfig(row, user, result, config.get());
                processedCount++;
                continue;
            }

            // ✅ 修复：如果映射失败，使用改进的硬编码逻辑（传递检验结果）
            System.out.println("⚠️ 映射配置未找到，使用改进的硬编码逻辑识别");

            if (isBloodItem(itemName)) {
                System.out.println("✅ 识别为：血检项目");
                processBloodRowWithUpdate(row, user, result);
            } else if (isUrineItem(itemName, itemValue)) {
                System.out.println("✅ 识别为：尿检项目（根据结果格式判断）");
                processUrineRowWithUpdate(row, user, result);
            } else if (isLiverItem(itemName, itemValue)) {
                System.out.println("✅ 识别为：肝功能项目（根据结果格式判断）");
                processLiverRowWithUpdate(row, user, result);
            } else if (isKidneyItem(itemName)) {
                System.out.println("✅ 识别为：肾功能项目");
                processKidneyRowWithUpdate(row, user, result);
            } else {
                System.out.println("❌ 错误：无法识别的项目类型: " + itemName);
                result.incrementErrorCount();
                continue;
            }

            processedCount++;
        }

        System.out.println("\n=== 智能识别处理完成 ===");
        System.out.println("总行数: " + rowCount);
        System.out.println("成功处理: " + processedCount);
        System.out.println("失败行数: " + result.getErrorCount());
    }

    /**
     * 根据配置处理数据行 - 优化版
     */
    private void processRowByConfig(Row row, User user, ExcelImportResult result, ItemMappingConfig config) {
        try {
            System.out.println("开始按配置处理: " + config.getChineseNames() + " -> " + config.getFeatureName());

            // ✅ 获取当前行的项目名称和检验结果
            String itemName = getCellStringValue(row.getCell(3));
            String itemValue = getCellStringValue(row.getCell(4));

            // ✅ 特殊处理：胆红素（关键修复）
            if (itemName != null && itemName.trim().equals("胆红素")) {
                System.out.println("⚠️ 发现胆红素项目，检验结果: " + itemValue);

                // 如果是文本结果（阴性、阳性等），强制作为尿检处理
                if (isTextResult(itemValue)) {
                    System.out.println("✅ 胆红素是文本结果，应作为尿检项目");

                    // 手动创建尿检胆红素配置
                    ItemMappingConfig urineBilConfig = new ItemMappingConfig();
                    urineBilConfig.setChineseNames("胆红素");
                    urineBilConfig.setFeatureName("bilirubin");
                    urineBilConfig.setCategory(ItemMappingConfig.Category.urine);
                    urineBilConfig.setValueType(ItemMappingConfig.ValueType.text_positive);
                    urineBilConfig.setDisplayName("胆红素");

                    // 使用修正后的配置
                    config = urineBilConfig;

                    // 刷新映射服务缓存（可选）
                    mappingService.refreshCache();
                } else {
                    System.out.println("✅ 胆红素是数值结果，作为肝功能项目");
                }
            }

            switch (config.getCategory()) {
                case blood:
                    HealthBloodItem bloodItem = parseBloodRow(row, user);
                    if (bloodItem != null) {
                        // 确保featureName正确设置
                        if (bloodItem.getFeatureName() == null || !bloodItem.getFeatureName().equals(config.getFeatureName())) {
                            bloodItem.setFeatureName(config.getFeatureName());
                            System.out.println("设置血检特征名: " + config.getFeatureName());
                        }

                        Optional<HealthBloodItem> bloodExisting = bloodItemRepository
                            .findByUserIdAndCheckDateAndItemName(
                                user.getUserId(),
                                bloodItem.getCheckDate(),
                                bloodItem.getItemName().trim());

                        if (bloodExisting.isPresent()) {
                            updateBloodItem(bloodExisting.get(), bloodItem);
                            bloodItemRepository.save(bloodExisting.get());
                            System.out.println("🔄 更新血检项目: " + bloodItem.getItemName());
                        } else {
                            bloodItemRepository.save(bloodItem);
                            System.out.println("✅ 保存血检项目: " + bloodItem.getItemName());
                        }

                        result.incrementBloodItems();
                    }
                    break;

                case urine:
                    HealthUrineItem urineItem = parseUrineRow(row, user);
                    if (urineItem != null) {
                        if (urineItem.getFeatureName() == null || !urineItem.getFeatureName().equals(config.getFeatureName())) {
                            urineItem.setFeatureName(config.getFeatureName());
                            System.out.println("设置尿检特征名: " + config.getFeatureName());
                        }

                        Optional<HealthUrineItem> urineExisting = urineItemRepository
                            .findByUserIdAndCheckDateAndItemName(
                                user.getUserId(),
                                urineItem.getCheckDate(),
                                urineItem.getItemName().trim());

                        if (urineExisting.isPresent()) {
                            updateUrineItem(urineExisting.get(), urineItem);
                            urineItemRepository.save(urineExisting.get());
                            System.out.println("🔄 更新尿检项目: " + urineItem.getItemName());
                        } else {
                            urineItemRepository.save(urineItem);
                            System.out.println("✅ 保存尿检项目: " + urineItem.getItemName());
                        }

                        result.incrementUrineItems();
                    }
                    break;

                case liver:
                    HealthLiverItem liverItem = parseLiverRow(row, user);
                    if (liverItem != null) {
                        if (liverItem.getFeatureName() == null || !liverItem.getFeatureName().equals(config.getFeatureName())) {
                            liverItem.setFeatureName(config.getFeatureName());
                            System.out.println("设置肝功能特征名: " + config.getFeatureName());
                        }

                        Optional<HealthLiverItem> liverExisting = liverItemRepository
                            .findByUserIdAndCheckDateAndItemName(
                                user.getUserId(),
                                liverItem.getCheckDate(),
                                liverItem.getItemName().trim());

                        if (liverExisting.isPresent()) {
                            updateLiverItem(liverExisting.get(), liverItem);
                            liverItemRepository.save(liverExisting.get());
                            System.out.println("🔄 更新肝功能项目: " + liverItem.getItemName());
                        } else {
                            liverItemRepository.save(liverItem);
                            System.out.println("✅ 保存肝功能项目: " + liverItem.getItemName());
                        }

                        result.incrementLiverItems();
                    }
                    break;

                case kidney:
                    HealthKidneyItem kidneyItem = parseKidneyRow(row, user);
                    if (kidneyItem != null) {
                        if (kidneyItem.getFeatureName() == null || !kidneyItem.getFeatureName().equals(config.getFeatureName())) {
                            kidneyItem.setFeatureName(config.getFeatureName());
                            System.out.println("设置肾功能特征名: " + config.getFeatureName());
                        }

                        Optional<HealthKidneyItem> kidneyExisting = kidneyItemRepository
                            .findByUserIdAndCheckDateAndItemName(
                                user.getUserId(),
                                kidneyItem.getCheckDate(),
                                kidneyItem.getItemName().trim());

                        if (kidneyExisting.isPresent()) {
                            updateKidneyItem(kidneyExisting.get(), kidneyItem);
                            kidneyItemRepository.save(kidneyExisting.get());
                            System.out.println("🔄 更新肾功能项目: " + kidneyItem.getItemName());
                        } else {
                            kidneyItemRepository.save(kidneyItem);
                            System.out.println("✅ 保存肾功能项目: " + kidneyItem.getItemName());
                        }

                        result.incrementKidneyItems();
                    }
                    break;
            }
        } catch (Exception e) {
            System.err.println("按配置处理数据行失败: " + e.getMessage());
            e.printStackTrace();
            result.incrementErrorCount();
        }
    }

    /**
     * 判断是否为血检项目
     */
    private boolean isBloodItem(String itemName) {
        String lowerName = itemName.toLowerCase();
        return lowerName.contains("中性粒细胞百分比") ||
               lowerName.contains("中性粒细胞") ||
               lowerName.contains("血小板总数") ||
               lowerName.contains("血小板") ||
               lowerName.contains("白细胞总数") ||
               lowerName.contains("白细胞") ||
               lowerName.contains("血红蛋白浓度") ||
               lowerName.contains("血红蛋白") ||
               lowerName.contains("血色素") ||
               lowerName.contains("红细胞总数") ||
               lowerName.contains("红细胞") ||
               lowerName.contains("淋巴细胞百分比") ||
               lowerName.contains("淋巴细胞") ||
               lowerName.contains("单核细胞百分比") ||
               lowerName.contains("单核细胞") ||
               lowerName.contains("嗜酸性细胞百分比") ||
               lowerName.contains("嗜酸性细胞") ||
               lowerName.contains("嗜碱性细胞百分比") ||
               lowerName.contains("嗜碱性细胞") ||
               // 添加英文缩写支持
               lowerName.contains("baso%") ||
               lowerName.contains("eos%") ||
               lowerName.contains("hb") ||
               lowerName.contains("hgb") ||
               lowerName.contains("lym%") ||
               lowerName.contains("mono%") ||
               lowerName.contains("neut%") ||
               lowerName.contains("plt") ||
               lowerName.contains("rbc") ||
               lowerName.contains("wbc");
    }

    /**
     * 判断是否为尿检项目
     */
    private boolean isUrineItem(String itemName) {
        // 兼容旧版本调用
        return isUrineItem(itemName, null);
    }

    /**
     * 判断是否为尿检项目（带检验结果判断）
     */
    private boolean isUrineItem(String itemName, String itemValue) {
        String lowerName = itemName.toLowerCase();
        String lowerValue = itemValue != null ? itemValue.toLowerCase() : "";

        // 特殊处理：胆红素
        if (lowerName.contains("胆红素") && !lowerName.contains("总") &&
            !lowerName.contains("直接") && !lowerName.contains("间接")) {

            // 如果检验结果是文本类型（阴性、阳性等），则判断为尿检
            if (isTextResult(lowerValue)) {
                return true;  // 尿检的胆红素（文本结果）
            } else {
                return false; // 可能是肝功能的胆红素（数值结果）
            }
        }

        return lowerName.contains("亚硝酸盐") ||
               lowerName.contains("尿蛋白") ||
               lowerName.contains("蛋白") ||
               lowerName.contains("尿比重") ||
               lowerName.contains("比重") ||
               lowerName.contains("尿酮体") ||
               lowerName.contains("尿葡萄糖") ||
               lowerName.contains("尿糖") ||
               lowerName.contains("白细胞脂酶") ||
               lowerName.contains("维生素c") ||
               lowerName.contains("vc") ||
               lowerName.contains("镜检") ||
               lowerName.contains("尿ph") ||
               lowerName.contains("ph值") ||
               lowerName.contains("尿潜血") ||
               lowerName.contains("尿胆原") ||
               // 添加英文缩写支持
               lowerName.contains("nit") ||
               lowerName.contains("pro") ||
               lowerName.contains("sg") ||
               lowerName.contains("ket") ||
               lowerName.contains("glu") ||
               lowerName.contains("leu") ||
               lowerName.contains("bil") ||
               lowerName.contains("bld") ||
               lowerName.contains("uro");
    }

    /**
     * 判断是否为肝功能项目
     */
    private boolean isLiverItem(String itemName) {
        // 兼容旧版本调用
        return isLiverItem(itemName, null);
    }

    /**
     * 判断是否为肝功能项目（带检验结果判断）
     */
    private boolean isLiverItem(String itemName, String itemValue) {
        String lowerName = itemName.toLowerCase();
        String lowerValue = itemValue != null ? itemValue.toLowerCase() : "";

        // 处理胆红素
        if (lowerName.contains("胆红素")) {
            // 排除肝功能相关的胆红素类型
            if (lowerName.contains("总胆红素") ||
                lowerName.contains("直接胆红素") ||
                lowerName.contains("间接胆红素") ||
                lowerName.contains("tbil") ||
                lowerName.contains("dbil") ||
                lowerName.contains("ibil")) {
                return false; // 这些属于肝功能
            }
            return true;
        }

        return lowerName.contains("谷氨酰转酞酶") ||
               lowerName.contains("a/g") ||
               lowerName.contains("谷草/谷丙") ||
               lowerName.contains("碱性磷酸酶") ||
               lowerName.contains("谷草转氨酶") ||
               lowerName.contains("谷丙转氨酶") ||
               lowerName.contains("白蛋白") ||
               lowerName.contains("总蛋白") ||
               lowerName.contains("球蛋白") ||
               // 添加英文缩写支持
               lowerName.contains("alt") ||
               lowerName.contains("ast") ||
               lowerName.contains("tbil") ||
               lowerName.contains("dbil") ||
               lowerName.contains("ibil") ||
               lowerName.contains("alb") ||
               lowerName.contains("tp") ||
               lowerName.contains("glob") ||
               lowerName.contains("ag_ratio") ||
               lowerName.contains("ast_alt_ratio") ||
               lowerName.contains("ggt") ||
               lowerName.contains("alp");
    }

    /**
     * 判断是否为肾功能项目
     */
    private boolean isKidneyItem(String itemName) {
        String lowerName = itemName.toLowerCase();
        return lowerName.contains("尿酸") ||
               lowerName.contains("肌酐") ||
               lowerName.contains("尿素氮") ||
               lowerName.contains("尿素") ||
               // 添加英文缩写支持
               lowerName.contains("crea") ||
               lowerName.contains("cr") ||
               lowerName.contains("bun") ||
               lowerName.contains("ua");
    }

    /**
     * 判断是否为文本结果
     */
    private boolean isTextResult(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        String lowerValue = value.toLowerCase().trim();

        // 常见的文本结果
        return lowerValue.contains("阴性") ||
               lowerValue.contains("阳性") ||
               lowerValue.contains("弱阳性") ||
               lowerValue.contains("强阳性") ||
               lowerValue.contains("neg") ||
               lowerValue.contains("pos") ||
               lowerValue.contains("-") ||
               lowerValue.contains("+") ||
               lowerValue.contains("±") ||
               lowerValue.contains("+/-") ||
               lowerValue.contains("negative") ||
               lowerValue.contains("positive") ||
               lowerValue.contains("trace") ||
               // 其他可能的文本表示
               lowerValue.contains("正常") ||
               lowerValue.contains("异常") ||
               lowerValue.contains("未见") ||
               lowerValue.contains("有") ||
               lowerValue.contains("无");
    }

    /**
     * 处理血检数据表
     */
    private void processBloodSheet(Sheet sheet, User user, ExcelImportResult result) {
        System.out.println("=== 开始处理血检工作表 ===");

        Iterator<Row> rowIterator = sheet.iterator();

        // 跳过表头行
        if (rowIterator.hasNext()) {
            Row headerRow = rowIterator.next();
            System.out.println("表头行（共" + headerRow.getLastCellNum() + "列）:");
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                String value = getCellStringValue(cell);
                System.out.println("  列" + i + ": " + value);
            }
        }

        int rowCount = 0;
        int savedCount = 0;
        int updatedCount = 0;

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            rowCount++;

            System.out.println("\n--- 处理第" + rowCount + "行 ---");

            // 跳过空行
            if (isRowEmpty(row)) {
                System.out.println("跳过空行");
                continue;
            }

            HealthBloodItem newItem = parseBloodRow(row, user);
            if (newItem != null) {
                System.out.println("✅ 解析成功: " + newItem.getItemName() + " = " + newItem.getItemValue());

                // 检查是否已存在相同记录
                Optional<HealthBloodItem> existingOpt = bloodItemRepository
                    .findByUserIdAndCheckDateAndItemName(
                        user.getUserId(),
                        newItem.getCheckDate(),
                        newItem.getItemName().trim());

                if (existingOpt.isPresent()) {
                    // 更新现有记录
                    HealthBloodItem existingItem = existingOpt.get();
                    updateBloodItem(existingItem, newItem);
                    bloodItemRepository.save(existingItem);
                    updatedCount++;
                    System.out.println("🔄 更新现有记录: " + existingItem.getItemName());
                } else {
                    // 保存新记录
                    bloodItemRepository.save(newItem);
                    savedCount++;
                    System.out.println("✅ 保存新记录: " + newItem.getItemName());
                }

                result.incrementBloodItems();

            } else {
                System.out.println("❌ 解析失败");
                result.incrementErrorCount();
            }
        }

        System.out.println("\n=== 血检工作表处理完成 ===");
        System.out.println("总行数: " + rowCount);
        System.out.println("新增记录: " + savedCount);
        System.out.println("更新记录: " + updatedCount);
        System.out.println("失败行数: " + result.getErrorCount());
    }

    /**
     * 处理血检行（带更新逻辑）
     */
    private void processBloodRowWithUpdate(Row row, User user, ExcelImportResult result) {
        HealthBloodItem newItem = parseBloodRow(row, user);
        if (newItem != null) {
            Optional<HealthBloodItem> existingOpt = bloodItemRepository
                .findByUserIdAndCheckDateAndItemName(
                    user.getUserId(),
                    newItem.getCheckDate(),
                    newItem.getItemName().trim());

            if (existingOpt.isPresent()) {
                // 更新
                HealthBloodItem existing = existingOpt.get();
                updateBloodItem(existing, newItem);
                bloodItemRepository.save(existing);
                System.out.println("🔄 更新血检项目: " + existing.getItemName());
            } else {
                // 新增
                bloodItemRepository.save(newItem);
                System.out.println("✅ 保存血检项目: " + newItem.getItemName());
            }

            result.incrementBloodItems();
        }
    }

    /**
     * 解析血检数据行 - 优化版
     */
    private HealthBloodItem parseBloodRow(Row row, User user) {
        System.out.println("开始解析血检行，列数: " + row.getLastCellNum());

        HealthBloodItem item = new HealthBloodItem();
        item.setUser(user);

        // 第0列：卡号
        String cardNumber = getCellStringValue(row.getCell(0));
        item.setCardNumber(cardNumber);
        System.out.println("卡号: " + cardNumber);

        // 第1列：性别
        String gender = getCellStringValue(row.getCell(1));
        item.setGender(gender);
        System.out.println("性别: " + gender);

        // 第2列：年龄
        String ageStr = getCellStringValue(row.getCell(2));
        Integer age = parseAge(ageStr);
        item.setAge(age);
        System.out.println("年龄: " + age);

        // 第3列：小项名称
        String itemName = getCellStringValue(row.getCell(3));
        if (itemName == null || itemName.trim().isEmpty()) {
            System.out.println("❌ 项目名称为空，跳过此行");
            return null;
        }
        itemName = itemName.trim();
        item.setItemName(itemName);
        System.out.println("项目名称: " + itemName);

        // 第4列：检验结果
        String itemValue = getCellStringValue(row.getCell(4));
        item.setItemValue(itemValue);
        System.out.println("检验结果: " + itemValue);

        // 第5列：单位
        String unit = getCellStringValue(row.getCell(5));
        item.setUnit(unit);
        System.out.println("单位: " + unit);

        // 第6列：检查日期
        String dateStr = getCellStringValue(row.getCell(6));
        System.out.println("原始日期字符串: '" + dateStr + "'");

        LocalDate checkDate = parseDate(dateStr);
        if (checkDate == null) {
            checkDate = LocalDate.now();
            System.out.println("日期解析失败，使用当前日期: " + checkDate);
        } else {
            System.out.println("解析后的日期: " + checkDate);
        }
        item.setCheckDate(checkDate);

        // ✅ 修复：项目映射 - 添加更详细的日志
        Optional<ItemMappingConfig> config = mappingService.getConfigByChineseName(itemName);
        if (config.isPresent()) {
            String featureName = config.get().getFeatureName();
            item.setFeatureName(featureName);

            Double numericValue = conversionService.convertToNumericValue(
                itemValue, config.get().getValueType());
            item.setNumericValue(numericValue);

            System.out.println("✅ 映射成功: " + itemName + " -> " + featureName + " = " + numericValue +
                              " (类型: " + config.get().getCategory() + ")");
        } else {
            // ✅ 修复：如果映射失败，尝试使用项目名称作为特征名
            System.out.println("⚠️ 警告：未找到项目映射配置，使用项目名称作为特征名: " + itemName);
            item.setFeatureName(itemName);

            // 尝试转换数值
            try {
                Double numericValue = conversionService.convertToNumericValue(
                    itemValue, ItemMappingConfig.ValueType.numeric);
                item.setNumericValue(numericValue);
                System.out.println("数值转换结果: " + numericValue);
            } catch (Exception e) {
                System.out.println("数值转换失败: " + e.getMessage());
                item.setNumericValue(null);
            }
        }

        System.out.println("✅ 血检项目解析完成");
        return item;
    }

    /**
     * 更新血检项目（保留ID，更新其他字段）
     */
    private void updateBloodItem(HealthBloodItem existing, HealthBloodItem newItem) {
        // 保留ID和用户关联
        // 更新其他字段
        existing.setCardNumber(newItem.getCardNumber());
        existing.setGender(newItem.getGender());
        existing.setAge(newItem.getAge());
        existing.setItemValue(newItem.getItemValue());
        existing.setUnit(newItem.getUnit());
        existing.setFeatureName(newItem.getFeatureName());
        existing.setNumericValue(newItem.getNumericValue());
        // 注意：不更新checkDate，因为这是查找条件
    }

    /**
     * 处理尿检数据表
     */
    private void processUrineSheet(Sheet sheet, User user, ExcelImportResult result) {
        System.out.println("=== 开始处理尿检工作表 ===");

        Iterator<Row> rowIterator = sheet.iterator();

        // 跳过表头
        if (rowIterator.hasNext()) {
            rowIterator.next();
        }

        int rowCount = 0;
        int savedCount = 0;
        int updatedCount = 0;

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            rowCount++;

            if (isRowEmpty(row)) continue;

            HealthUrineItem newItem = parseUrineRow(row, user);
            if (newItem != null) {
                // 检查是否已存在
                Optional<HealthUrineItem> existingOpt = urineItemRepository
                    .findByUserIdAndCheckDateAndItemName(
                        user.getUserId(),
                        newItem.getCheckDate(),
                        newItem.getItemName().trim());

                if (existingOpt.isPresent()) {
                    // 更新现有记录
                    HealthUrineItem existingItem = existingOpt.get();
                    updateUrineItem(existingItem, newItem);
                    urineItemRepository.save(existingItem);
                    updatedCount++;
                    System.out.println("🔄 更新尿检项目: " + existingItem.getItemName());
                } else {
                    // 保存新记录
                    urineItemRepository.save(newItem);
                    savedCount++;
                    System.out.println("✅ 保存尿检项目: " + newItem.getItemName());
                }

                result.incrementUrineItems();
            }
        }

        System.out.println("\n=== 尿检工作表处理完成 ===");
        System.out.println("总行数: " + rowCount);
        System.out.println("新增记录: " + savedCount);
        System.out.println("更新记录: " + updatedCount);
        System.out.println("失败行数: " + result.getErrorCount());
    }

    /**
     * 处理尿检行（带更新逻辑）
     */
    private void processUrineRowWithUpdate(Row row, User user, ExcelImportResult result) {
        HealthUrineItem newItem = parseUrineRow(row, user);
        if (newItem != null) {
            Optional<HealthUrineItem> existingOpt = urineItemRepository
                .findByUserIdAndCheckDateAndItemName(
                    user.getUserId(),
                    newItem.getCheckDate(),
                    newItem.getItemName().trim());

            if (existingOpt.isPresent()) {
                // 更新
                HealthUrineItem existing = existingOpt.get();
                updateUrineItem(existing, newItem);
                urineItemRepository.save(existing);
                System.out.println("🔄 更新尿检项目: " + existing.getItemName());
            } else {
                // 新增
                urineItemRepository.save(newItem);
                System.out.println("✅ 保存尿检项目: " + newItem.getItemName());
            }

            result.incrementUrineItems();
        }
    }

    /**
     * 解析尿检数据行 - 优化版
     */
    private HealthUrineItem parseUrineRow(Row row, User user) {
        System.out.println("开始解析尿检行，列数: " + row.getLastCellNum());

        HealthUrineItem item = new HealthUrineItem();
        item.setUser(user);

        // 第0列：卡号
        String cardNumber = getCellStringValue(row.getCell(0));
        item.setCardNumber(cardNumber);
        System.out.println("卡号: " + cardNumber);

        // 第1列：性别
        String gender = getCellStringValue(row.getCell(1));
        item.setGender(gender);
        System.out.println("性别: " + gender);

        // 第2列：年龄
        String ageStr = getCellStringValue(row.getCell(2));
        Integer age = parseAge(ageStr);
        item.setAge(age);
        System.out.println("年龄: " + age);

        // 第3列：小项名称
        String itemName = getCellStringValue(row.getCell(3));
        if (itemName == null || itemName.trim().isEmpty()) {
            System.out.println("❌ 项目名称为空，跳过此行");
            return null;
        }
        itemName = itemName.trim();
        item.setItemName(itemName);
        System.out.println("项目名称: " + itemName);

        // 第4列：检验结果
        String itemValue = getCellStringValue(row.getCell(4));
        item.setItemValue(itemValue);
        System.out.println("检验结果: " + itemValue);

        // 第5列：单位
        String unit = getCellStringValue(row.getCell(5));
        item.setUnit(unit);
        System.out.println("单位: " + unit);

        // 第6列：检查日期
        String dateStr = getCellStringValue(row.getCell(6));
        System.out.println("原始日期字符串: '" + dateStr + "'");

        LocalDate checkDate = parseDate(dateStr);
        if (checkDate == null) {
            checkDate = LocalDate.now();
            System.out.println("日期解析失败，使用当前日期: " + checkDate);
        } else {
            System.out.println("解析后的日期: " + checkDate);
        }
        item.setCheckDate(checkDate);

        // ✅ 修复：项目映射 - 添加更详细的日志
        Optional<ItemMappingConfig> config = mappingService.getConfigByChineseName(itemName);
        if (config.isPresent()) {
            String featureName = config.get().getFeatureName();
            item.setFeatureName(featureName);

            Double numericValue = conversionService.convertToNumericValue(
                itemValue, config.get().getValueType());
            item.setNumericValue(numericValue);

            System.out.println("✅ 映射成功: " + itemName + " -> " + featureName + " = " + numericValue +
                              " (类型: " + config.get().getCategory() + ")");
        } else {
            // ✅ 修复：如果映射失败，尝试使用项目名称作为特征名
            System.out.println("⚠️ 警告：未找到项目映射配置，使用项目名称作为特征名: " + itemName);
            item.setFeatureName(itemName);

            // 尝试转换数值
            try {
                Double numericValue = conversionService.convertToNumericValue(
                    itemValue, ItemMappingConfig.ValueType.numeric);
                item.setNumericValue(numericValue);
                System.out.println("数值转换结果: " + numericValue);
            } catch (Exception e) {
                System.out.println("数值转换失败: " + e.getMessage());
                item.setNumericValue(null);
            }
        }

        System.out.println("✅ 尿检项目解析完成");
        return item;
    }

    /**
     * 更新尿检项目
     */
    private void updateUrineItem(HealthUrineItem existing, HealthUrineItem newItem) {
        existing.setCardNumber(newItem.getCardNumber());
        existing.setGender(newItem.getGender());
        existing.setAge(newItem.getAge());
        existing.setItemValue(newItem.getItemValue());
        existing.setUnit(newItem.getUnit());
        existing.setFeatureName(newItem.getFeatureName());
        existing.setNumericValue(newItem.getNumericValue());
    }

    /**
     * 处理肝功能数据表
     */
    private void processLiverSheet(Sheet sheet, User user, ExcelImportResult result) {
        System.out.println("=== 开始处理肝功能工作表 ===");

        Iterator<Row> rowIterator = sheet.iterator();

        // 跳过表头
        if (rowIterator.hasNext()) {
            rowIterator.next();
        }

        int rowCount = 0;
        int savedCount = 0;
        int updatedCount = 0;

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            rowCount++;

            if (isRowEmpty(row)) continue;

            HealthLiverItem newItem = parseLiverRow(row, user);
            if (newItem != null) {
                // 检查是否已存在
                Optional<HealthLiverItem> existingOpt = liverItemRepository
                    .findByUserIdAndCheckDateAndItemName(
                        user.getUserId(),
                        newItem.getCheckDate(),
                        newItem.getItemName().trim());

                if (existingOpt.isPresent()) {
                    // 更新现有记录
                    HealthLiverItem existingItem = existingOpt.get();
                    updateLiverItem(existingItem, newItem);
                    liverItemRepository.save(existingItem);
                    updatedCount++;
                    System.out.println("🔄 更新肝功能项目: " + existingItem.getItemName());
                } else {
                    // 保存新记录
                    liverItemRepository.save(newItem);
                    savedCount++;
                    System.out.println("✅ 保存肝功能项目: " + newItem.getItemName());
                }

                result.incrementLiverItems();
            }
        }

        System.out.println("\n=== 肝功能工作表处理完成 ===");
        System.out.println("总行数: " + rowCount);
        System.out.println("新增记录: " + savedCount);
        System.out.println("更新记录: " + updatedCount);
        System.out.println("失败行数: " + result.getErrorCount());
    }

    /**
     * 处理肝功能行（带更新逻辑）
     */
    private void processLiverRowWithUpdate(Row row, User user, ExcelImportResult result) {
        HealthLiverItem newItem = parseLiverRow(row, user);
        if (newItem != null) {
            // 使用Repository中的精确查找方法
            Optional<HealthLiverItem> existingOpt = liverItemRepository
                .findByUserIdAndCheckDateAndItemName(
                    user.getUserId(),
                    newItem.getCheckDate(),
                    newItem.getItemName().trim());

            if (existingOpt.isPresent()) {
                // 更新现有记录
                HealthLiverItem existingItem = existingOpt.get();
                updateLiverItem(existingItem, newItem);
                liverItemRepository.save(existingItem);
                System.out.println("🔄 更新肝功能项目: " + existingItem.getItemName());
            } else {
                // 保存新记录
                liverItemRepository.save(newItem);
                System.out.println("✅ 保存肝功能项目: " + newItem.getItemName());
            }

            result.incrementLiverItems();
        }
    }

    /**
     * 解析肝功能数据行 - 优化版
     */
    private HealthLiverItem parseLiverRow(Row row, User user) {
        System.out.println("开始解析肝功能行，列数: " + row.getLastCellNum());

        HealthLiverItem item = new HealthLiverItem();
        item.setUser(user);

        // 第0列：卡号
        String cardNumber = getCellStringValue(row.getCell(0));
        item.setCardNumber(cardNumber);
        System.out.println("卡号: " + cardNumber);

        // 第1列：性别
        String gender = getCellStringValue(row.getCell(1));
        item.setGender(gender);
        System.out.println("性别: " + gender);

        // 第2列：年龄
        String ageStr = getCellStringValue(row.getCell(2));
        Integer age = parseAge(ageStr);
        item.setAge(age);
        System.out.println("年龄: " + age);

        // 第3列：小项名称
        String itemName = getCellStringValue(row.getCell(3));
        if (itemName == null || itemName.trim().isEmpty()) {
            System.out.println("❌ 项目名称为空，跳过此行");
            return null;
        }
        itemName = itemName.trim();
        item.setItemName(itemName);
        System.out.println("项目名称: " + itemName);

        // 第4列：检验结果
        String itemValue = getCellStringValue(row.getCell(4));
        item.setItemValue(itemValue);
        System.out.println("检验结果: " + itemValue);

        // 第5列：单位
        String unit = getCellStringValue(row.getCell(5));
        item.setUnit(unit);
        System.out.println("单位: " + unit);

        // 第6列：检查日期
        String dateStr = getCellStringValue(row.getCell(6));
        System.out.println("原始日期字符串: '" + dateStr + "'");

        LocalDate checkDate = parseDate(dateStr);
        if (checkDate == null) {
            checkDate = LocalDate.now();
            System.out.println("日期解析失败，使用当前日期: " + checkDate);
        } else {
            System.out.println("解析后的日期: " + checkDate);
        }
        item.setCheckDate(checkDate);

        // ✅ 修复：项目映射 - 添加更详细的日志
        Optional<ItemMappingConfig> config = mappingService.getConfigByChineseName(itemName);
        if (config.isPresent()) {
            String featureName = config.get().getFeatureName();
            item.setFeatureName(featureName);

            Double numericValue = conversionService.convertToNumericValue(
                itemValue, config.get().getValueType());
            item.setNumericValue(numericValue);

            System.out.println("✅ 映射成功: " + itemName + " -> " + featureName + " = " + numericValue +
                              " (类型: " + config.get().getCategory() + ")");
        } else {
            // ✅ 修复：如果映射失败，尝试使用项目名称作为特征名
            System.out.println("⚠️ 警告：未找到项目映射配置，使用项目名称作为特征名: " + itemName);
            item.setFeatureName(itemName);

            // 尝试转换数值
            try {
                Double numericValue = conversionService.convertToNumericValue(
                    itemValue, ItemMappingConfig.ValueType.numeric);
                item.setNumericValue(numericValue);
                System.out.println("数值转换结果: " + numericValue);
            } catch (Exception e) {
                System.out.println("数值转换失败: " + e.getMessage());
                item.setNumericValue(null);
            }
        }

        System.out.println("✅ 肝功能项目解析完成");
        return item;
    }

    /**
     * 更新肝功能项目
     */
    private void updateLiverItem(HealthLiverItem existing, HealthLiverItem newItem) {
        existing.setCardNumber(newItem.getCardNumber());
        existing.setGender(newItem.getGender());
        existing.setAge(newItem.getAge());
        existing.setItemValue(newItem.getItemValue());
        existing.setUnit(newItem.getUnit());
        existing.setFeatureName(newItem.getFeatureName());
        existing.setNumericValue(newItem.getNumericValue());
    }

    /**
     * 处理肾功能数据表
     */
    private void processKidneySheet(Sheet sheet, User user, ExcelImportResult result) {
        System.out.println("=== 开始处理肾功能工作表 ===");

        Iterator<Row> rowIterator = sheet.iterator();

        // 跳过表头
        if (rowIterator.hasNext()) {
            rowIterator.next();
        }

        int rowCount = 0;
        int savedCount = 0;
        int updatedCount = 0;

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            rowCount++;

            if (isRowEmpty(row)) continue;

            HealthKidneyItem newItem = parseKidneyRow(row, user);
            if (newItem != null) {
                // 检查是否已存在
                Optional<HealthKidneyItem> existingOpt = kidneyItemRepository
                    .findByUserIdAndCheckDateAndItemName(
                        user.getUserId(),
                        newItem.getCheckDate(),
                        newItem.getItemName().trim());

                if (existingOpt.isPresent()) {
                    // 更新现有记录
                    HealthKidneyItem existingItem = existingOpt.get();
                    updateKidneyItem(existingItem, newItem);
                    kidneyItemRepository.save(existingItem);
                    updatedCount++;
                    System.out.println("🔄 更新肾功能项目: " + existingItem.getItemName());
                } else {
                    // 保存新记录
                    kidneyItemRepository.save(newItem);
                    savedCount++;
                    System.out.println("✅ 保存肾功能项目: " + newItem.getItemName());
                }

                result.incrementKidneyItems();
            }
        }

        System.out.println("\n=== 肾功能工作表处理完成 ===");
        System.out.println("总行数: " + rowCount);
        System.out.println("新增记录: " + savedCount);
        System.out.println("更新记录: " + updatedCount);
        System.out.println("失败行数: " + result.getErrorCount());
    }

    /**
     * 处理肾功能行（带更新逻辑）
     */
    private void processKidneyRowWithUpdate(Row row, User user, ExcelImportResult result) {
        HealthKidneyItem newItem = parseKidneyRow(row, user);
        if (newItem != null) {
            // 使用Repository中的精确查找方法
            Optional<HealthKidneyItem> existingOpt = kidneyItemRepository
                .findByUserIdAndCheckDateAndItemName(
                    user.getUserId(),
                    newItem.getCheckDate(),
                    newItem.getItemName().trim());

            if (existingOpt.isPresent()) {
                // 更新现有记录
                HealthKidneyItem existingItem = existingOpt.get();
                updateKidneyItem(existingItem, newItem);
                kidneyItemRepository.save(existingItem);
                System.out.println("🔄 更新肾功能项目: " + existingItem.getItemName());
            } else {
                // 保存新记录
                kidneyItemRepository.save(newItem);
                System.out.println("✅ 保存肾功能项目: " + newItem.getItemName());
            }

            result.incrementKidneyItems();
        }
    }

    /**
     * 解析肾功能数据行 - 优化版
     */
    private HealthKidneyItem parseKidneyRow(Row row, User user) {
        System.out.println("开始解析肾功能行，列数: " + row.getLastCellNum());

        HealthKidneyItem item = new HealthKidneyItem();
        item.setUser(user);

        // 第0列：卡号
        String cardNumber = getCellStringValue(row.getCell(0));
        item.setCardNumber(cardNumber);
        System.out.println("卡号: " + cardNumber);

        // 第1列：性别
        String gender = getCellStringValue(row.getCell(1));
        item.setGender(gender);
        System.out.println("性别: " + gender);

        // 第2列：年龄
        String ageStr = getCellStringValue(row.getCell(2));
        Integer age = parseAge(ageStr);
        item.setAge(age);
        System.out.println("年龄: " + age);

        // 第3列：小项名称
        String itemName = getCellStringValue(row.getCell(3));
        if (itemName == null || itemName.trim().isEmpty()) {
            System.out.println("❌ 项目名称为空，跳过此行");
            return null;
        }
        itemName = itemName.trim();
        item.setItemName(itemName);
        System.out.println("项目名称: " + itemName);

        // 第4列：检验结果
        String itemValue = getCellStringValue(row.getCell(4));
        item.setItemValue(itemValue);
        System.out.println("检验结果: " + itemValue);

        // 第5列：单位
        String unit = getCellStringValue(row.getCell(5));
        item.setUnit(unit);
        System.out.println("单位: " + unit);

        // 第6列：检查日期
        String dateStr = getCellStringValue(row.getCell(6));
        System.out.println("原始日期字符串: '" + dateStr + "'");

        LocalDate checkDate = parseDate(dateStr);
        if (checkDate == null) {
            checkDate = LocalDate.now();
            System.out.println("日期解析失败，使用当前日期: " + checkDate);
        } else {
            System.out.println("解析后的日期: " + checkDate);
        }
        item.setCheckDate(checkDate);

        // ✅ 修复：项目映射 - 添加更详细的日志
        Optional<ItemMappingConfig> config = mappingService.getConfigByChineseName(itemName);
        if (config.isPresent()) {
            String featureName = config.get().getFeatureName();
            item.setFeatureName(featureName);

            Double numericValue = conversionService.convertToNumericValue(
                itemValue, config.get().getValueType());
            item.setNumericValue(numericValue);

            System.out.println("✅ 映射成功: " + itemName + " -> " + featureName + " = " + numericValue +
                              " (类型: " + config.get().getCategory() + ")");
        } else {
            // ✅ 修复：如果映射失败，尝试使用项目名称作为特征名
            System.out.println("⚠️ 警告：未找到项目映射配置，使用项目名称作为特征名: " + itemName);
            item.setFeatureName(itemName);

            // 尝试转换数值
            try {
                Double numericValue = conversionService.convertToNumericValue(
                    itemValue, ItemMappingConfig.ValueType.numeric);
                item.setNumericValue(numericValue);
                System.out.println("数值转换结果: " + numericValue);
            } catch (Exception e) {
                System.out.println("数值转换失败: " + e.getMessage());
                item.setNumericValue(null);
            }
        }

        System.out.println("✅ 肾功能项目解析完成");
        return item;
    }

    /**
     * 更新肾功能项目
     */
    private void updateKidneyItem(HealthKidneyItem existing, HealthKidneyItem newItem) {
        existing.setCardNumber(newItem.getCardNumber());
        existing.setGender(newItem.getGender());
        existing.setAge(newItem.getAge());
        existing.setItemValue(newItem.getItemValue());
        existing.setUnit(newItem.getUnit());
        existing.setFeatureName(newItem.getFeatureName());
        existing.setNumericValue(newItem.getNumericValue());
    }

    // ============ 辅助方法 ============

    private String getCellStringValue(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                // 避免科学计数法
                double numValue = cell.getNumericCellValue();
                if (numValue == (long) numValue) {
                    return String.format("%d", (long) numValue);
                } else {
                    return String.format("%s", numValue);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    try {
                        return String.valueOf(cell.getNumericCellValue());
                    } catch (Exception ex) {
                        return cell.getCellFormula();
                    }
                }
            default:
                return "";
        }
    }

    private Integer parseAge(String ageStr) {
        if (ageStr == null || ageStr.trim().isEmpty()) return null;

        try {
            // 提取数字部分
            String numericPart = ageStr.replaceAll("[^0-9]", "");
            if (!numericPart.isEmpty()) {
                return Integer.parseInt(numericPart);
            }
        } catch (NumberFormatException e) {
            // 忽略解析错误
        }

        return null;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        String trimmed = dateStr.trim();
        System.out.println("解析日期: '" + trimmed + "'");

        // 处理 Excel 日期格式（如："Mon Jan 15 00:00:00 CST 2024"）
        if (trimmed.matches("^[A-Za-z]{3} [A-Za-z]{3} \\d{1,2} \\d{2}:\\d{2}:\\d{2} [A-Z]{3} \\d{4}$")) {
            try {
                // 提取日期部分
                String[] parts = trimmed.split(" ");
                String monthStr = parts[1];  // Jan
                String dayStr = parts[2];    // 15
                String yearStr = parts[5];   // 2024

                // 转换月份缩写
                Map<String, String> monthMap = new HashMap<>();
                monthMap.put("Jan", "01"); monthMap.put("Feb", "02"); monthMap.put("Mar", "03");
                monthMap.put("Apr", "04"); monthMap.put("May", "05"); monthMap.put("Jun", "06");
                monthMap.put("Jul", "07"); monthMap.put("Aug", "08"); monthMap.put("Sep", "09");
                monthMap.put("Oct", "10"); monthMap.put("Nov", "11"); monthMap.put("Dec", "12");

                String month = monthMap.get(monthStr);
                if (month != null) {
                    // 确保日期是两位数
                    if (dayStr.length() == 1) {
                        dayStr = "0" + dayStr;
                    }

                    String formattedDate = yearStr + "-" + month + "-" + dayStr;
                    System.out.println("格式化英文日期: " + formattedDate);
                    return LocalDate.parse(formattedDate);
                }
            } catch (Exception e) {
                System.out.println("解析英文日期失败: " + e.getMessage());
            }
        }

        // 尝试不同的日期格式
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                LocalDate date = LocalDate.parse(trimmed, formatter);
                System.out.println("使用格式解析成功: " + formatter.toString());
                return date;
            } catch (DateTimeParseException e) {
                // 继续尝试下一个格式
            }
        }

        // 尝试处理中文日期（如：2024年1月15日）
        if (trimmed.contains("年") && trimmed.contains("月") && trimmed.contains("日")) {
            try {
                String clean = trimmed
                    .replace("年", "-")
                    .replace("月", "-")
                    .replace("日", "")
                    .replace(" ", "");
                return LocalDate.parse(clean);
            } catch (Exception e) {
                // 继续
            }
        }

        System.out.println("❌ 无法解析日期: " + trimmed);
        return null;
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;

        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getCellStringValue(cell);
                if (value != null && !value.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Excel导入结果类
     */
    public static class ExcelImportResult {
        private int bloodItems = 0;
        private int urineItems = 0;
        private int liverItems = 0;
        private int kidneyItems = 0;
        private int errorCount = 0;
        private int featuresExtracted = 0;
        private boolean modelPredictionsTriggered = false;  // 新增字段：模型预测是否已触发

        public void incrementBloodItems() { bloodItems++; }
        public void incrementUrineItems() { urineItems++; }
        public void incrementLiverItems() { liverItems++; }
        public void incrementKidneyItems() { kidneyItems++; }
        public void incrementErrorCount() { errorCount++; }

        public int getTotalItems() {
            return bloodItems + urineItems + liverItems + kidneyItems;
        }

        // Getter和Setter
        public int getBloodItems() { return bloodItems; }
        public void setBloodItems(int bloodItems) { this.bloodItems = bloodItems; }

        public int getUrineItems() { return urineItems; }
        public void setUrineItems(int urineItems) { this.urineItems = urineItems; }

        public int getLiverItems() { return liverItems; }
        public void setLiverItems(int liverItems) { this.liverItems = liverItems; }

        public int getKidneyItems() { return kidneyItems; }
        public void setKidneyItems(int kidneyItems) { this.kidneyItems = kidneyItems; }

        public int getErrorCount() { return errorCount; }
        public void setErrorCount(int errorCount) { this.errorCount = errorCount; }

        public int getFeaturesExtracted() { return featuresExtracted; }
        public void setFeaturesExtracted(int featuresExtracted) { this.featuresExtracted = featuresExtracted; }

        public boolean isModelPredictionsTriggered() { return modelPredictionsTriggered; }
        public void setModelPredictionsTriggered(boolean modelPredictionsTriggered) {
            this.modelPredictionsTriggered = modelPredictionsTriggered;
        }

        @Override
        public String toString() {
            return String.format(
                "Excel导入结果: 共%d个项目\n" +
                "血检项目: %d个\n" +
                "尿检项目: %d个\n" +
                "肝功能项目: %d个\n" +
                "肾功能项目: %d个\n" +
                "错误行数: %d个\n" +
                "特征向量提取: %d个\n" +
                "模型预测触发: %s",
                getTotalItems(), bloodItems, urineItems, liverItems, kidneyItems,
                errorCount, featuresExtracted,
                modelPredictionsTriggered ? "是" : "否"
            );
        }
    }
}