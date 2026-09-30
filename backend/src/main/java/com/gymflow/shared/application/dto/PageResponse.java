package com.gymflow.shared.application.dto;

import java.util.List;

import com.gymflow.shared.domain.model.PageResult;

/** Página estable para la API (no se serializa Page de Spring). page empieza en 0. */
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    public static <T> PageResponse<T> of(PageResult<?> page, List<T> items) {
        return new PageResponse<>(items, page.page(), page.size(), page.totalItems(), page.totalPages());
    }
}
