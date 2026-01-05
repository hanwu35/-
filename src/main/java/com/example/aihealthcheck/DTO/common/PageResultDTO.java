package com.example.aihealthcheck.dto;

import lombok.Data;
import java.util.List;

@Data
public class PageResultDTO<T> {
    private List<T> data;
    private Integer currentPage;
    private Integer pageSize;
    private Long total;
    private Integer totalPages;

    public PageResultDTO() {}

    public PageResultDTO(List<T> data, Integer currentPage, Integer pageSize, Long total) {
        this.data = data;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.total = total;
        this.totalPages = (int) Math.ceil((double) total / pageSize);
    }
}