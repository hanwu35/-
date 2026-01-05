package com.example.aihealthcheck.controller;

import com.example.aihealthcheck.service.HealthDataExcelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/excel")
public class ExcelUploadController {

    // 使用新的HealthDataExcelService而不是旧的ExcelService
    @Autowired
    private HealthDataExcelService healthDataExcelService;

    /**
     * 上传Excel文件并解析（新版-行格式数据）
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Integer userId) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 1. 检查文件是否为空
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "请选择一个文件上传");
                return ResponseEntity.badRequest().body(response);
            }

            // 2. 检查文件格式
            String fileName = file.getOriginalFilename();
            if (fileName == null ||
                (!fileName.toLowerCase().endsWith(".xlsx") &&
                 !fileName.toLowerCase().endsWith(".xls"))) {
                response.put("success", false);
                response.put("message", "只支持.xlsx或.xls格式的Excel文件");
                return ResponseEntity.badRequest().body(response);
            }

            System.out.println("使用HealthDataExcelService处理文件: " + fileName);

            // 3. 使用新版服务解析Excel文件（行格式数据）
            HealthDataExcelService.ExcelImportResult result =
                healthDataExcelService.processHealthDataExcel(file, userId);

            // 4. 构建响应
            response.put("success", true);
            response.put("message", "文件解析完成");
            response.put("fileName", fileName);
            response.put("result", result.toString());
            response.put("count", result.getTotalItems());

            // 详细信息
            Map<String, Object> details = new HashMap<>();
            details.put("总项目数", result.getTotalItems());
            details.put("血检项目数", result.getBloodItems());
            details.put("尿检项目数", result.getUrineItems());
            details.put("肝功能项目数", result.getLiverItems());
            details.put("肾功能项目数", result.getKidneyItems());
            details.put("错误行数", result.getErrorCount());
            details.put("特征向量提取数", result.getFeaturesExtracted());

            response.put("details", details);

            System.out.println("处理完成: " + result);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();

            response.put("success", false);
            response.put("message", "文件处理失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 健康检查端点
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Health Data Excel Service v3.0");
        response.put("feature", "行格式数据 + 智能分离 + 特征提取");
        response.put("description", "支持7列格式：卡号,性别,年龄,小项名称,检验结果,单位,检查日期");
        return ResponseEntity.ok(response);
    }

    /**
     * 测试端点 - 验证服务是否工作
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testEndpoint() {
        Map<String, Object> response = new HashMap<>();

        try {
            response.put("success", true);
            response.put("service", "HealthDataExcelService");
            response.put("status", "ACTIVE");
            response.put("features", new String[] {
                "行格式数据解析",
                "智能数据分离（血/尿/肝/肾）",
                "项目名称映射",
                "特征向量提取"
            });

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}