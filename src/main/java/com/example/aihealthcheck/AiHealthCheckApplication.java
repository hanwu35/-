package com.example.aihealthcheck;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = {"com.example.aihealthcheck.entity"})
@EnableJpaRepositories(basePackages = {"com.example.aihealthcheck.repository"})
public class AiHealthCheckApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiHealthCheckApplication.class, args);
    }

}