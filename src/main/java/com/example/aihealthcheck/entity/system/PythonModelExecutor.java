package com.example.aihealthcheck.service.prediction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
public class PythonModelExecutor {

    private static final Logger logger = LoggerFactory.getLogger(PythonModelExecutor.class);

    // 模型文件路径
    private static final String MODEL_BASE_PATH = "src/main/resources/static/models/";

    // Python脚本路径
    private static final String SCRIPT_BASE_PATH = "scripts/";

    /**
     * 执行Python模型预测
     */
    public Map<String, Object> executeModel(String modelType, Integer userId, String checkDate) {
        logger.info("开始执行{}模型预测，用户ID: {}，检查日期: {}", modelType, userId, checkDate);

        Map<String, Object> result = new HashMap<>();

        try {
            // 构建Python命令
            String pythonScript = getScriptPath(modelType);
            String modelPath = getModelPath(modelType);

            // 检查Python脚本是否存在
            if (!checkFileExists(pythonScript)) {
                logger.error("Python脚本不存在: {}", pythonScript);
                result.put("success", false);
                result.put("error", "Python脚本不存在: " + pythonScript);
                return result;
            }

            // 检查模型文件是否存在
            if (!checkFileExists(modelPath)) {
                logger.error("模型文件不存在: {}", modelPath);
                result.put("success", false);
                result.put("error", "模型文件不存在: " + modelPath);
                return result;
            }

            // 构建命令行参数
            String[] command = {
                "python3",
                pythonScript,
                "--user-id", userId.toString(),
                "--check-date", checkDate,
                "--model-path", modelPath
            };

            // 执行命令 - 指定字符编码
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);

            // 设置环境变量
            Map<String, String> env = processBuilder.environment();
            env.put("PYTHONIOENCODING", "utf-8");
            env.put("PYTHONUTF8", "1");
            env.put("LANG", "zh_CN.UTF-8");
            env.put("LC_ALL", "zh_CN.UTF-8");

            Process process = processBuilder.start();

            // 读取输出 - 使用UTF-8编码
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    logger.debug("Python输出原始行: {}", line);
                }
            }

            // 等待进程完成
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                logger.info("{}模型预测执行成功", modelType);
                result.put("success", true);
                result.put("output", output.toString());
                result.put("exitCode", exitCode);
                logger.debug("Python完整输出:\n{}", output.toString());
            } else {
                logger.error("{}模型预测执行失败，退出码: {}", modelType, exitCode);
                result.put("success", false);
                result.put("error", "Python进程退出码: " + exitCode);
                result.put("output", output.toString());
            }

        } catch (IOException | InterruptedException e) {
            logger.error("执行{}模型预测时发生错误", modelType, e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * 批量执行所有模型预测
     */
    public Map<String, Map<String, Object>> executeAllModels(Integer userId, String checkDate) {
        logger.info("开始执行所有模型预测，用户ID: {}，检查日期: {}", userId, checkDate);

        Map<String, Map<String, Object>> allResults = new HashMap<>();
        String[] modelTypes = {"liver", "kidney", "blood", "urine"};

        for (String modelType : modelTypes) {
            logger.info("执行{}模型预测...", modelType);
            Map<String, Object> result = executeModel(modelType, userId, checkDate);
            allResults.put(modelType, result);

            // 简单延迟，避免同时启动太多进程
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        logger.info("所有模型预测执行完成");
        return allResults;
    }

    private String getScriptPath(String modelType) {
        return SCRIPT_BASE_PATH + modelType + "_model.py";
    }

    private String getModelPath(String modelType) {
        return MODEL_BASE_PATH + modelType + "_model.pkl";
    }

    private boolean checkFileExists(String filePath) {
        java.io.File file = new java.io.File(filePath);
        return file.exists();
    }
}