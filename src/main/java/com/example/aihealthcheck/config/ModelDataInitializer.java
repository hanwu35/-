// src/main/java/com/example/aihealthcheck/config/ModelDataInitializer.java
package com.example.aihealthcheck.config;

import com.example.aihealthcheck.entity.ModelFile;
import com.example.aihealthcheck.repository.ModelFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ModelDataInitializer implements CommandLineRunner {

    @Autowired
    private ModelFileRepository modelFileRepository;

    @Override
    public void run(String... args) throws Exception {
        // 检查是否已有数据
        if (modelFileRepository.count() == 0) {
            // 添加示例模型记录
            ModelFile model1 = new ModelFile(
                "健康风险预测模型",
                "health_risk_predictor.pkl",
                "classification",
                "基于用户健康数据预测健康风险的机器学习模型"
            );
            model1.setIsActive(true);

            ModelFile model2 = new ModelFile(
                "疾病分类模型",
                "disease_classifier.h5",
                "classification",
                "根据症状分类疾病的深度学习模型"
            );

            ModelFile model3 = new ModelFile(
                "症状分析模型",
                "symptom_analyzer.joblib",
                "clustering",
                "对症状进行聚类分析的模型"
            );

            modelFileRepository.save(model1);
            modelFileRepository.save(model2);
            modelFileRepository.save(model3);

            System.out.println("示例模型数据已初始化");
        }
    }
}