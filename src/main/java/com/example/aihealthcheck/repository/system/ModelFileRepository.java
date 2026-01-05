// src/main/java/com/example/aihealthcheck/repository/ModelFileRepository.java
package com.example.aihealthcheck.repository;

import com.example.aihealthcheck.entity.ModelFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModelFileRepository extends JpaRepository<ModelFile, Long> {
    Optional<ModelFile> findByIsActiveTrue();
    List<ModelFile> findByIsActive(Boolean isActive);
    Optional<ModelFile> findByName(String name);
    List<ModelFile> findByModelType(String modelType);
}