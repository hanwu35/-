package com.example.aihealthcheck.service;

import com.example.aihealthcheck.entity.ItemMappingConfig;
import com.example.aihealthcheck.repository.ItemMappingConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ItemMappingService {

    @Autowired
    private ItemMappingConfigRepository mappingConfigRepository;

    // 缓存映射关系
    private Map<String, ItemMappingConfig> chineseToConfigMap = new HashMap<>();
    private Map<String, ItemMappingConfig> featureToConfigMap = new HashMap<>();
    private boolean cacheInitialized = false;

    /**
     * 初始化缓存
     */
    private synchronized void initializeCache() {
        if (cacheInitialized) return;

        List<ItemMappingConfig> allConfigs = mappingConfigRepository.findAll();
        for (ItemMappingConfig config : allConfigs) {
            // 缓存特征名到配置的映射
            featureToConfigMap.put(config.getFeatureName(), config);

            // 缓存所有中文别名到配置的映射
            String[] chineseNames = config.getChineseNames().split(",");
            for (String chineseName : chineseNames) {
                chineseToConfigMap.put(chineseName.trim(), config);
            }
        }

        cacheInitialized = true;
        System.out.println("项目映射缓存初始化完成，共加载 " + allConfigs.size() + " 条配置");
    }

    /**
     * 根据中文项目名获取映射配置
     */
    public Optional<ItemMappingConfig> getConfigByChineseName(String chineseName) {
        if (!cacheInitialized) {
            initializeCache();
        }

        // 先从缓存查找
        ItemMappingConfig config = chineseToConfigMap.get(chineseName);
        if (config != null) {
            return Optional.of(config);
        }

        // 缓存未命中，尝试模糊匹配
        for (Map.Entry<String, ItemMappingConfig> entry : chineseToConfigMap.entrySet()) {
            if (entry.getKey().contains(chineseName) || chineseName.contains(entry.getKey())) {
                return Optional.of(entry.getValue());
            }
        }

        return Optional.empty();
    }

    /**
     * 根据特征名获取映射配置
     */
    public Optional<ItemMappingConfig> getConfigByFeatureName(String featureName) {
        if (!cacheInitialized) {
            initializeCache();
        }

        ItemMappingConfig config = featureToConfigMap.get(featureName);
        return Optional.ofNullable(config);
    }

    /**
     * 获取所有特征名
     */
    public List<String> getAllFeatureNames() {
        if (!cacheInitialized) {
            initializeCache();
        }
        return List.copyOf(featureToConfigMap.keySet());
    }

    /**
     * 根据中文名获取特征名
     */
    public Optional<String> getFeatureNameByChineseName(String chineseName) {
        return getConfigByChineseName(chineseName)
                .map(ItemMappingConfig::getFeatureName);
    }

    /**
     * 根据特征名获取显示名称（中文）
     */
    public Optional<String> getDisplayNameByFeatureName(String featureName) {
        return getConfigByFeatureName(featureName)
                .map(ItemMappingConfig::getDisplayName)
                .or(() -> Optional.of(featureName));
    }

    /**
     * 刷新缓存
     */
    public void refreshCache() {
        cacheInitialized = false;
        chineseToConfigMap.clear();
        featureToConfigMap.clear();
        initializeCache();
    }

    /**
     * 获取分类的所有特征名
     */
    public List<String> getFeatureNamesByCategory(ItemMappingConfig.Category category) {
        if (!cacheInitialized) {
            initializeCache();
        }

        return featureToConfigMap.values().stream()
                .filter(config -> config.getCategory() == category)
                .map(ItemMappingConfig::getFeatureName)
                .toList();
    }

    /**
     * 获取所有配置
     */
    public List<ItemMappingConfig> getAllConfigs() {
        if (!cacheInitialized) {
            initializeCache();
        }
        return List.copyOf(featureToConfigMap.values());
    }
}