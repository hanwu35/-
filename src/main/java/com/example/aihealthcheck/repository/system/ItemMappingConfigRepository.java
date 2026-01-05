package com.example.aihealthcheck.repository;

import com.example.aihealthcheck.entity.ItemMappingConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemMappingConfigRepository extends JpaRepository<ItemMappingConfig, Integer> {

    Optional<ItemMappingConfig> findByFeatureName(String featureName);

    // 修复查询：使用 LIKE 而不是 MEMBER OF
    @Query("SELECT imc FROM ItemMappingConfig imc WHERE " +
           "imc.chineseNames LIKE %:itemName% OR " +
           "imc.chineseNames LIKE %:itemNameWithComma%")
    Optional<ItemMappingConfig> findByChineseName(
        @Param("itemName") String itemName,
        @Param("itemNameWithComma") String itemNameWithComma);

    // 添加一个简化的查询方法
    @Query("SELECT imc FROM ItemMappingConfig imc WHERE imc.chineseNames LIKE %:keyword%")
    List<ItemMappingConfig> findByKeyword(@Param("keyword") String keyword);

    List<ItemMappingConfig> findByCategory(ItemMappingConfig.Category category);

    @Query("SELECT imc FROM ItemMappingConfig imc WHERE imc.category = :category AND imc.valueType = :valueType")
    List<ItemMappingConfig> findByCategoryAndValueType(
        @Param("category") ItemMappingConfig.Category category,
        @Param("valueType") ItemMappingConfig.ValueType valueType);

    @Query("SELECT imc.featureName FROM ItemMappingConfig imc")
    List<String> findAllFeatureNames();

    @Query("SELECT imc FROM ItemMappingConfig imc WHERE imc.featureName IN :featureNames")
    List<ItemMappingConfig> findByFeatureNames(@Param("featureNames") List<String> featureNames);
}