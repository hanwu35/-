// src/main/java/com/example/aihealthcheck/controller/ModelFileController.java
package com.example.aihealthcheck.controller;

import com.example.aihealthcheck.entity.ModelFile;
import com.example.aihealthcheck.service.ModelFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/model-files")
public class ModelFileController {

    @Autowired
    private ModelFileService modelFileService;

    @GetMapping
    public List<ModelFile> getAllModels() {
        return modelFileService.getAllModels();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ModelFile> getModelById(@PathVariable Long id) {
        Optional<ModelFile> model = modelFileService.getModelById(id);
        return model.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/active")
    public ResponseEntity<ModelFile> getActiveModel() {
        Optional<ModelFile> model = modelFileService.getActiveModel();
        return model.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/type/{modelType}")
    public List<ModelFile> getModelsByType(@PathVariable String modelType) {
        return modelFileService.getModelsByType(modelType);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadModel(@PathVariable Long id) {
        Optional<ModelFile> model = modelFileService.getModelById(id);
        if (model.isPresent()) {
            Resource file = modelFileService.loadModelFile(model.get().getFileName());
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
                .body(file);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<ModelFile> createModel(@RequestBody ModelFile modelFile) {
        try {
            ModelFile savedModel = modelFileService.saveModel(modelFile);
            return ResponseEntity.ok(savedModel);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<ModelFile> activateModel(@PathVariable Long id) {
        try {
            ModelFile activatedModel = modelFileService.activateModel(id);
            return ResponseEntity.ok(activatedModel);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteModel(@PathVariable Long id) {
        modelFileService.deleteModel(id);
        return ResponseEntity.ok().build();
    }
}