// src/main/java/com/example/aihealthcheck/service/ModelFileService.java
package com.example.aihealthcheck.service;

import com.example.aihealthcheck.entity.ModelFile;
import com.example.aihealthcheck.repository.ModelFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Service
public class ModelFileService {

    @Autowired
    private ModelFileRepository modelFileRepository;

    private final Path modelsDirectory = Paths.get("models");

    public List<ModelFile> getAllModels() {
        return modelFileRepository.findAll();
    }

    public Optional<ModelFile> getModelById(Long id) {
        return modelFileRepository.findById(id);
    }

    public Optional<ModelFile> getActiveModel() {
        return modelFileRepository.findByIsActiveTrue();
    }

    public List<ModelFile> getModelsByType(String modelType) {
        return modelFileRepository.findByModelType(modelType);
    }

    public Resource loadModelFile(String filename) {
        try {
            Path file = modelsDirectory.resolve(filename);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("无法读取文件: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("文件路径错误: " + filename, e);
        }
    }

    public ModelFile activateModel(Long modelId) {
        // 停用所有模型
        List<ModelFile> activeModels = modelFileRepository.findByIsActive(true);
        for (ModelFile model : activeModels) {
            model.setIsActive(false);
            modelFileRepository.save(model);
        }

        // 激活指定模型
        ModelFile modelToActivate = modelFileRepository.findById(modelId)
            .orElseThrow(() -> new RuntimeException("模型未找到"));
        modelToActivate.setIsActive(true);
        return modelFileRepository.save(modelToActivate);
    }

    public ModelFile saveModel(ModelFile modelFile) {
        return modelFileRepository.save(modelFile);
    }

    public void deleteModel(Long id) {
        modelFileRepository.deleteById(id);
    }
}