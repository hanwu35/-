package com.example.aihealthcheck.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class LoggingConfig {

    private static final Logger logger = LoggerFactory.getLogger(LoggingConfig.class);

    @PostConstruct
    public void init() {
        try {
            // 创建日志目录
            Path logDir = Paths.get("logs");
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
                logger.info("创建日志目录: {}", logDir.toAbsolutePath());
            }

            // 启动日志
            logger.info("==============================");
            logger.info("AI健康检查系统启动");
            logger.info("日志级别配置完成");
            logger.info("日志文件: logs/application.log");
            logger.info("==============================");

        } catch (Exception e) {
            logger.error("初始化日志配置失败", e);
        }
    }
}